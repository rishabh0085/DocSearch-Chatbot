import {
  BotMessageSquare,
  Building2,
  CircleHelp,
  FileText,
  FolderKanban,
  History,
  Search,
  Settings,
  Upload,
  UsersRound,
  X,
} from 'lucide-react'
import { NavItem } from '../navigation/NavItem'

type SidebarProps = {
  className?: string
  isOpen?: boolean
  onClose?: () => void
}

const primaryNavigation = [
  { icon: BotMessageSquare, label: 'Chat', to: '/chat' },
  { icon: FileText, label: 'Documents', to: '/documents' },
  { icon: FolderKanban, label: 'Collections' },
  { icon: Upload, label: 'Upload', to: '/upload' },
  { icon: Search, label: 'Search', to: '/search' },
  { icon: History, label: 'Search History', to: '/history' },
  { icon: UsersRound, label: 'Team' },
  { icon: Settings, label: 'Settings' },
]

export function Sidebar({ className = '', isOpen = true, onClose }: SidebarProps) {
  const sidebarContent = (
    <aside className="flex h-full w-72 shrink-0 flex-col border-r border-[#F3D8C8] bg-white p-4 shadow-sm shadow-orange-100/40 lg:w-64 lg:shadow-none" aria-label="Primary navigation">
      <div className="mb-6 flex items-center justify-between border-b border-slate-100 pb-4 lg:hidden">
        <span className="text-sm font-semibold text-slate-900">Navigation</span>
        <button
          type="button"
          className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
          aria-label="Close navigation"
          onClick={onClose}
        >
          <X className="size-5" aria-hidden="true" />
        </button>
      </div>
      <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">Workspace</p>
      <nav className="space-y-1">
        {primaryNavigation.map((item) => (
          <NavItem key={item.label} {...item} onNavigate={onClose} />
        ))}
      </nav>
      <div className="mt-7 border-t border-slate-100 pt-5">
        <NavItem icon={CircleHelp} label="Get Help" />
      </div>
      <div className="mt-auto flex items-center gap-3 rounded-xl border border-[#F3D8C8] bg-[#FFF8F3] p-3.5 shadow-sm shadow-orange-100/40">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-100 text-blue-600"><Building2 className="size-4" aria-hidden="true" /></span>
        <div className="min-w-0"><p className="truncate text-sm font-semibold text-slate-800">Acme Corp</p>
        <p className="mt-1 text-xs text-slate-500">Enterprise Plan</p></div>
      </div>
    </aside>
  )

  if (onClose) {
    return (
      <div className={`${className} ${isOpen ? '' : 'pointer-events-none'}`} aria-hidden={!isOpen} inert={!isOpen}>
        <button
          type="button"
          className={`fixed inset-0 z-40 bg-slate-900/30 transition-opacity ${isOpen ? 'opacity-100' : 'opacity-0'}`}
          aria-label="Close navigation overlay"
          tabIndex={isOpen ? 0 : -1}
          onClick={onClose}
        />
        <div className={`fixed inset-y-0 left-0 z-50 transition-transform duration-200 motion-reduce:transition-none ${isOpen ? 'translate-x-0' : '-translate-x-full'}`} role="dialog" aria-modal="true" aria-label="Primary navigation">
          {sidebarContent}
        </div>
      </div>
    )
  }

  return <div className={`${className} sticky top-[72px] h-[calc(100vh-72px)]`}>{sidebarContent}</div>
}
