import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { MainLayout } from '../layouts/MainLayout'
import { AccountPage } from '../pages/AccountPage'
import { EmployeeDashboardPage } from '../pages/EmployeeDashboardPage'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import NotFoundPage from '../pages/NotFoundPage'
import { RegisterPage } from '../pages/RegisterPage'
import { MemberDashboardPage } from '../pages/MemberDashboardPage'
import { PromotionAdminPage } from '../pages/PromotionAdminPage'
import { PromotionFormPage } from '../pages/PromotionFormPage'
import { PromotionDetailPage } from '../pages/PromotionDetailPage'
import PromotionsPage from '../pages/PromotionsPage'
import { SeatPriceAdminPage } from '../pages/SeatPriceAdminPage'
import SeatsPage from '../pages/SeatsPage'
import { AdminLayout } from '../components/AdminLayout'
import { AdminMovieListPage } from '../pages/AdminMovieListPage'
import { AdminMovieFormPage } from '../pages/AdminMovieFormPage'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleHomeRoute } from './RoleHomeRoute'

const MoviesPage = lazy(() => import('../pages/MoviesPage'))
const MovieDetailPage = lazy(() => import('../pages/MovieDetailPage'))

function CatalogRoute({ children }) {
  return <Suspense fallback={<p className="page-status">Đang tải trang...</p>}>{children}</Suspense>
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="register" element={<RegisterPage />} />
      <Route path="movies" element={<CatalogRoute><MoviesPage /></CatalogRoute>} />
      <Route path="movies/:movieId" element={<CatalogRoute><MovieDetailPage /></CatalogRoute>} />
      <Route path="promotions" element={<PromotionsPage />} />
      <Route path="promotions/:promotionId" element={<PromotionDetailPage />} />
      <Route path="booking/seats" element={<SeatsPage />} />

      <Route element={<MainLayout />}>
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<RoleHomeRoute />} />
        </Route>
        <Route element={<ProtectedRoute roles={['MEMBER']} />}>
          <Route path="account" element={<AccountPage />} />
          <Route path="account/profile" element={<Navigate to="/account" replace />} />
          <Route path="member" element={<MemberDashboardPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['EMPLOYEE']} />}>
          <Route path="employee" element={<EmployeeDashboardPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="movies" replace />} />
          <Route path="movies" element={<AdminMovieListPage />} />
          <Route path="movies/new" element={<AdminMovieFormPage />} />
          <Route path="movies/:movieId/edit" element={<AdminMovieFormPage />} />
          <Route path="promotions" element={<PromotionAdminPage />} />
          <Route path="promotions/new" element={<PromotionFormPage />} />
          <Route path="promotions/:id" element={<PromotionDetailPage />} />
          <Route path="promotions/:id/edit" element={<PromotionFormPage />} />
          <Route path="seat-prices" element={<SeatPriceAdminPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
