import { Outlet } from 'react-router-dom'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'

export function MainLayout() {
  return <div className="flex min-h-screen flex-col bg-surface font-body-md text-body-md text-on-surface"><CinemaHeader /><main className="flex-1 pb-20 pt-20 md:pb-0"><Outlet /></main><CinemaFooter /></div>
}
