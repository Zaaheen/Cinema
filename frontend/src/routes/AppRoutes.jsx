import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { MainLayout } from '../layouts/MainLayout'
import { AccountPage } from '../pages/AccountPage'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
import { EmployeeDashboardPage } from '../pages/EmployeeDashboardPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RegisterPage } from '../pages/RegisterPage'
import { ProtectedRoute } from './ProtectedRoute'
import { MemberDashboardPage } from '../pages/MemberDashboardPage'
import { RoleHomeRoute } from './RoleHomeRoute'

const MoviesPage = lazy(() => import('../pages/MoviesPage').then((module) => ({ default: module.MoviesPage })))
const MovieDetailPage = lazy(() => import('../pages/MovieDetailPage').then((module) => ({ default: module.MovieDetailPage })))

function CatalogRoute({ children }) {
  return <Suspense fallback={<p className="page-status">Đang tải trang...</p>}>{children}</Suspense>
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="movies" element={<CatalogRoute><MoviesPage /></CatalogRoute>} />
        <Route path="movies/:movieId" element={<CatalogRoute><MovieDetailPage /></CatalogRoute>} />
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<RoleHomeRoute />} />
        </Route>
        <Route element={<ProtectedRoute roles={['MEMBER']} />}>
          <Route path="account" element={<AccountPage />} />
          <Route path="member" element={<MemberDashboardPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="admin" element={<AdminDashboardPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['EMPLOYEE']} />}>
          <Route path="employee" element={<EmployeeDashboardPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
