import { Bell, Menu, Search } from 'lucide-react'
import { UserMenu } from './UserMenu'

type HeaderProps = {
  onOpenNavigation: () => void
}

export function Header({ onOpenNavigation }: HeaderProps) {
  return (
    <header className="sticky top-0 z-30 border-b border-slate-200/90 bg-white/95 shadow-sm backdrop-blur">
      <div className="flex h-[68px] items-center gap-2 px-4 sm:h-[72px] sm:gap-3 sm:px-6">
        <button
          type="button"
          className="rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 lg:hidden"
          aria-label="Open navigation"
          onClick={onOpenNavigation}
        >
          <Menu className="size-5" aria-hidden="true" />
        </button>

        <div className="flex min-w-0 shrink-0 items-center gap-3">
          <div className="flex size-9 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm shadow-blue-600/20">
            <Search className="size-5" strokeWidth={2.5} aria-hidden="true" />
          </div>
          <div className="hidden min-w-0 sm:block">
            <p className="truncate text-sm font-semibold text-slate-900">DocSearch</p>
            <p className="truncate text-xs text-slate-500">Find answers. From your own knowledge.</p>
          </div>
        </div>

        <label className="relative mx-auto hidden max-w-2xl flex-1 md:block">
          <span className="sr-only">Search documents and questions</span>
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-blue-600" aria-hidden="true" />
          <input
            type="search"
            readOnly
            placeholder="Search documents, ask questions, or type / for suggestions..."
            aria-label="Global search is not available yet"
            className="h-10 w-full cursor-default rounded-lg border border-[#F3D8C8] bg-[#FFF8F3] py-2 pl-10 pr-16 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-100"
          />
          <kbd className="pointer-events-none absolute right-2 top-1/2 hidden -translate-y-1/2 rounded border border-[#F3D8C8] bg-[#FFF8F3] px-1.5 py-0.5 text-[11px] font-medium text-slate-500 lg:block">
            Ctrl K
          </kbd>
        </label>

        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          <button
            type="button"
            disabled
            className="rounded-lg p-2 text-slate-400 transition-colors disabled:cursor-not-allowed disabled:opacity-70"
            aria-label="Notifications are not available yet"
            title="Notifications are not available yet"
          >
            <Bell className="size-5" aria-hidden="true" />
          </button>
          <UserMenu />
        </div>
      </div>
    </header>
  )
}
