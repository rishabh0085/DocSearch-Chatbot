import type { LucideIcon } from 'lucide-react'
import { NavLink } from 'react-router-dom'

type NavItemProps = {
  icon: LucideIcon
  label: string
  to?: string
  onNavigate?: () => void
}

const baseClassName = 'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2'

export function NavItem({ icon: Icon, label, to, onNavigate }: NavItemProps) {
  if (!to) {
    return (
      <span className={`${baseClassName} cursor-default text-slate-400`} aria-disabled="true">
        <Icon className="size-5 shrink-0" aria-hidden="true" />
        <span>{label}</span>
      </span>
    )
  }

  return (
    <NavLink
      to={to}
      onClick={onNavigate}
      className={({ isActive }) =>
        `${baseClassName} ${
          isActive
            ? 'bg-blue-50 text-blue-600 shadow-sm shadow-blue-100'
            : 'text-slate-600 hover:bg-[#FFF1E8] hover:text-slate-900'
        }`
      }
    >
      <Icon className="size-5 shrink-0" aria-hidden="true" />
      <span>{label}</span>
    </NavLink>
  )
}
