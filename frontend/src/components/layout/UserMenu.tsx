import { ChevronDown } from 'lucide-react'

export function UserMenu() {
  return (
    <button
      type="button"
      disabled
      className="flex cursor-not-allowed items-center gap-2 rounded-lg p-1.5 text-left opacity-90"
      aria-label="User menu is not available yet"
      title="User menu is not available yet"
    >
      <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-600">
        RS
      </span>
      <span className="hidden min-w-0 md:block">
        <span className="block truncate text-sm font-medium text-slate-800">Rishabh Singh</span>
        <span className="block truncate text-xs text-slate-500">Software Engineer</span>
      </span>
      <ChevronDown className="hidden size-4 text-slate-500 sm:block" aria-hidden="true" />
    </button>
  )
}
