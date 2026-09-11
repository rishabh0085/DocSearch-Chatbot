const fileTypeLabels: Record<string, string> = {
  'application/pdf': 'PDF',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'DOCX',
  'text/plain': 'TXT',
}

export function formatFileSize(bytes: number | null) {
  if (typeof bytes !== 'number' || bytes < 0) return 'Size unavailable'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length)
  const value = bytes / 1024 ** index
  return `${value >= 10 ? Math.round(value) : value.toFixed(1)} ${units[index - 1]}`
}

export function formatFileType(fileType: string, fileName?: string) {
  const mappedType = fileTypeLabels[fileType.toLowerCase()]
  if (mappedType) return mappedType
  const extension = fileName?.split('.').pop()?.toUpperCase()
  return extension && extension.length <= 5 ? extension : 'Document'
}

export function formatDocumentDate(value: string | null) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(date)
}
