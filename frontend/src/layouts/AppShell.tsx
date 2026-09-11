import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Header } from '../components/layout/Header'
import { Sidebar } from '../components/layout/Sidebar'

export function AppShell() {
  const [isMobileNavigationOpen, setIsMobileNavigationOpen] = useState(false)

  return (
    <div className="min-h-screen overflow-x-hidden bg-[#FFFCFA] text-slate-900">
      <Header onOpenNavigation={() => setIsMobileNavigationOpen(true)} />
      <div className="flex">
        <Sidebar className="hidden lg:flex" />
        <Sidebar
          className="lg:hidden"
          isOpen={isMobileNavigationOpen}
          onClose={() => setIsMobileNavigationOpen(false)}
        />
        <main className="min-w-0 flex-1" id="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
