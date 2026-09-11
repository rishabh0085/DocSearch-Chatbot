import { FileText, LoaderCircle, Upload, X } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { uploadDocument, UnexpectedDocumentResponseError } from '../services/documentService'
import { formatFileSize, formatFileType } from '../utils/documentFormatters'

const maximumFileSize = 20 * 1024 * 1024
const supportedExtensions = ['pdf', 'docx', 'txt']
const supportedMimeTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain']

function validateFile(file: File) {
  const extension = file.name.split('.').pop()?.toLowerCase()
  if (!extension || !supportedExtensions.includes(extension) || (file.type && !supportedMimeTypes.includes(file.type))) return 'Please select a PDF, DOCX, or TXT file.'
  if (file.size > maximumFileSize) return 'File is too large. Maximum size is 20 MB.'
  return null
}

export function UploadPage() {
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  function selectFile(file: File | undefined) {
    if (!file) return
    const validationError = validateFile(file)
    setError(validationError)
    setSuccess(false)
    setSelectedFile(validationError ? null : file)
  }

  async function handleUpload() {
    if (!selectedFile || isUploading) return
    setIsUploading(true)
    setError(null)
    setSuccess(false)
    try {
      await uploadDocument(selectedFile)
      setSuccess(true)
      setSelectedFile(null)
    } catch (requestError) {
      console.error('Document upload failed', requestError)
      setError(requestError instanceof UnexpectedDocumentResponseError ? "Couldn't upload the document because the server returned an unexpected response." : "Couldn't upload the document. Please try again.")
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <section className="mx-auto w-full max-w-3xl px-4 py-6 sm:px-6 sm:py-8 lg:px-10">
      <Link to="/documents" className="inline-flex items-center text-sm font-medium text-blue-700 hover:text-blue-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">← Back to documents</Link>
      <p className="mt-6 text-sm font-semibold text-blue-600">Knowledge base</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">Upload document</h1>
      <p className="mt-2 text-sm leading-6 text-slate-600">Add a PDF, DOCX, or TXT document to your knowledge base.</p>

      <input ref={inputRef} type="file" accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain" className="sr-only" aria-describedby="upload-file-hint" onChange={(event) => selectFile(event.target.files?.[0])} />
      <div onDragEnter={(event) => { event.preventDefault(); setIsDragging(true) }} onDragOver={(event) => event.preventDefault()} onDragLeave={(event) => { event.preventDefault(); setIsDragging(false) }} onDrop={(event) => { event.preventDefault(); setIsDragging(false); selectFile(event.dataTransfer.files[0]) }} className={`mt-8 rounded-xl border-2 border-dashed p-8 text-center shadow-sm transition-colors sm:p-12 ${isDragging ? 'border-blue-500 bg-blue-50' : 'border-slate-300 bg-white'}`} aria-label="Document upload drop zone">
        <span className="mx-auto flex size-12 items-center justify-center rounded-xl bg-blue-50 text-blue-700 ring-1 ring-blue-100"><Upload className="size-5" aria-hidden="true" /></span>
        <h2 className="mt-4 text-base font-semibold text-slate-800">Drag and drop your file here</h2>
        <p className="mt-2 text-sm text-slate-500">or</p>
        <button type="button" onClick={() => inputRef.current?.click()} className="mt-4 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">Choose file</button>
        <p id="upload-file-hint" className="mt-4 text-xs text-slate-500">PDF, DOCX or TXT · Max 20 MB</p>
      </div>

      {error && <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 shadow-sm" role="alert">{error}</div>}
      {selectedFile && <div className="mt-5 flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/40"><span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-700"><FileText className="size-5" aria-hidden="true" /></span><div className="min-w-0 flex-1"><p className="break-words text-sm font-semibold text-slate-800">{selectedFile.name}</p><p className="mt-1 text-sm text-slate-500">{formatFileType(selectedFile.type, selectedFile.name)} <span aria-hidden="true">·</span> {formatFileSize(selectedFile.size)}</p></div><button type="button" disabled={isUploading} onClick={() => { setSelectedFile(null); setError(null) }} className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" aria-label={`Remove ${selectedFile.name}`}><X className="size-4" aria-hidden="true" /></button></div>}
      {success && <div className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 shadow-sm" role="status"><span className="font-medium">Document uploaded successfully.</span><button type="button" onClick={() => navigate('/documents')} className="ml-2 font-medium underline underline-offset-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2">View documents</button></div>}
      <div className="mt-6 flex justify-end"><button type="button" disabled={!selectedFile || isUploading} onClick={() => void handleUpload()} className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500">{isUploading && <LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />} {isUploading ? 'Uploading...' : 'Upload document'}</button></div>
    </section>
  )
}
