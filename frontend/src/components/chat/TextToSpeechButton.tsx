import { Volume2, VolumeX } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

let stopActiveSpeech: (() => void) | null = null

const FEMALE_VOICE_INDICATORS = /\b(?:samantha|karen|victoria|zira|jenny|aria|ava|sonia|female)\b/i
const NATURAL_VOICE_INDICATORS = /\b(?:natural|neural|online|enhanced|premium|samantha|karen|victoria|zira|jenny|aria|ava|sonia)\b/i

type TextToSpeechButtonProps = {
  text: string
  idleLabel?: string
  speakingLabel?: string
  idleAriaLabel?: string
  speakingAriaLabel?: string
  className?: string
}

function speechText(text: string) {
  return text
    .replace(/\[Source\s+\d+\]/gi, '')
    .replace(/[*_`]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function isSpeechSupported() {
  return typeof window !== 'undefined'
    && 'speechSynthesis' in window
    && 'SpeechSynthesisUtterance' in window
}

function getPreferredVoice(voices: SpeechSynthesisVoice[]) {
  const englishVoices = voices.filter((voice) => voice.lang.toLowerCase().startsWith('en'))
  if (englishVoices.length === 0) return null

  return englishVoices.reduce<SpeechSynthesisVoice | null>((bestVoice, voice) => {
    if (!bestVoice || voiceScore(voice) > voiceScore(bestVoice)) return voice
    return bestVoice
  }, null)
}

function voiceScore(voice: SpeechSynthesisVoice) {
  const name = voice.name.toLowerCase()
  const locale = voice.lang.toLowerCase()
  const isFemale = FEMALE_VOICE_INDICATORS.test(name)
  const isNatural = NATURAL_VOICE_INDICATORS.test(name)
  const localeScore = locale === 'en-us' ? 40
    : ['en-gb', 'en-in', 'en-au', 'en-ca'].includes(locale) ? 30
      : 20

  return (isFemale && isNatural ? 1_000 : isFemale ? 700 : isNatural ? 400 : 100)
    + localeScore
    + (voice.localService ? 1 : 0)
}

export function TextToSpeechButton({
  text,
  idleLabel = 'Listen',
  speakingLabel = 'Speaking',
  idleAriaLabel = 'Read answer aloud',
  speakingAriaLabel = 'Stop reading',
  className = 'mt-4',
}: TextToSpeechButtonProps) {
  const [isSpeaking, setIsSpeaking] = useState(false)
  const [isSupported] = useState(isSpeechSupported)
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([])
  const utteranceRef = useRef<SpeechSynthesisUtterance | null>(null)

  useEffect(() => {
    if (!isSupported) return

    const updateVoices = () => setVoices(window.speechSynthesis.getVoices())
    updateVoices()
    window.speechSynthesis.addEventListener('voiceschanged', updateVoices)
    return () => window.speechSynthesis.removeEventListener('voiceschanged', updateVoices)
  }, [isSupported])

  const stop = useCallback(() => {
    const utterance = utteranceRef.current
    if (utterance) {
      utteranceRef.current = null
      window.speechSynthesis.cancel()
    }
    setIsSpeaking(false)
    if (stopActiveSpeech === stop) stopActiveSpeech = null
  }, [])

  useEffect(() => () => stop(), [stop])

  if (!isSupported || !speechText(text)) return null

  function speak() {
    if (isSpeaking) {
      stop()
      return
    }

    stopActiveSpeech?.()
    const utterance = new SpeechSynthesisUtterance(speechText(text))
    const preferredVoice = getPreferredVoice(voices.length > 0 ? voices : window.speechSynthesis.getVoices())
    if (preferredVoice) utterance.voice = preferredVoice
    utterance.rate = 0.95
    utterance.pitch = 1.05
    utterance.volume = 1
    utteranceRef.current = utterance
    utterance.onstart = () => setIsSpeaking(true)
    utterance.onend = () => stop()
    utterance.onerror = () => stop()
    stopActiveSpeech = stop

    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(utterance)
  }

  return (
    <button
      type="button"
      onClick={speak}
      aria-label={isSpeaking ? speakingAriaLabel : idleAriaLabel}
      title={isSpeaking ? speakingAriaLabel : idleAriaLabel}
      className={`inline-flex items-center gap-1.5 rounded-md border border-orange-200 bg-orange-50 px-2.5 py-1.5 text-xs font-medium text-orange-700 transition-colors hover:bg-orange-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2 ${className}`}
    >
      {isSpeaking ? <VolumeX className="size-3.5" aria-hidden="true" /> : <Volume2 className="size-3.5" aria-hidden="true" />}
      <span>{isSpeaking ? speakingLabel : idleLabel}</span>
    </button>
  )
}
