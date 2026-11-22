import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import { getErrorMessage } from '../lib/errors'

function destinationFor(role) {
  if (role === 'MEMBER') return '/account/profile'
  if (role === 'ADMIN') return '/admin/promotions'
  return '/'
}

export default function LoginPage() {
  const { login, isAuthenticated, isLoading, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!isLoading && isAuthenticated) return <Navigate to={destinationFor(user?.role)} replace />

  const submit = async (event) => {
    event.preventDefault()
    if (submitting) return
    setError('')
    setSubmitting(true)
    try {
      const result = await login(username.trim(), password)
      const requestedPath = location.state?.from
      navigate(result.user.role === 'MEMBER' && requestedPath ? requestedPath : destinationFor(result.user.role), { replace: true })
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return <div className="min-h-screen bg-surface text-on-surface"><CinemaHeader />
    <main className="mx-auto grid min-h-screen max-w-6xl items-center gap-8 px-4 pb-20 pt-28 sm:px-6 lg:grid-cols-2 lg:px-8">
      <section className="relative hidden min-h-[580px] overflow-hidden rounded-2xl border border-surface-container-highest bg-surface-container-lowest p-8 shadow-2xl lg:flex lg:flex-col lg:justify-end">
        <div className="absolute inset-0 bg-cover bg-center opacity-40" style={{ backgroundImage: "url('https://res.cloudinary.com/pdhtejei/image/upload/v1794759797/cinema/stitch-demo/backdrops/bo-sua-bay-hero.jpg')" }} />
        <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/75 to-transparent" />
        <div className="relative z-10"><p className="mb-3 font-label-sm uppercase tracking-[0.18em] text-primary">Cinema All-Pass</p><h1 className="font-headline-xl text-4xl font-bold leading-tight">Chào mừng trở lại với <span className="bg-gradient-to-r from-primary via-tertiary to-secondary bg-clip-text text-transparent">thế giới điện ảnh</span></h1><p className="mt-4 max-w-md text-on-surface-variant">Đăng nhập để quản lý hồ sơ thành viên và tiếp tục trải nghiệm Cinema.</p></div>
      </section>
      <section className="mx-auto w-full max-w-md rounded-2xl border border-surface-container-highest bg-surface-container-low p-6 shadow-2xl sm:p-8">
        <p className="font-label-sm uppercase tracking-[0.14em] text-primary">Tài khoản Cinema</p><h1 className="mt-2 font-headline-lg text-3xl font-bold">Đăng nhập</h1><p className="mt-2 text-on-surface-variant">Nhập tên đăng nhập và mật khẩu để tiếp tục.</p>
        <form className="mt-7 space-y-5" onSubmit={submit} aria-label="Đăng nhập tài khoản Cinema">
          <div><label className="mb-1.5 block font-label-md font-medium" htmlFor="login-username">Tên đăng nhập <span className="text-primary">*</span></label><div className="relative"><span className="material-symbols-outlined pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-outline">person</span><input id="login-username" name="username" className="w-full rounded-lg border border-surface-container-highest bg-surface-container-lowest py-3 pl-11 pr-4 text-[16px] text-on-surface outline-none transition focus:border-primary-container focus:ring-1 focus:ring-primary-container" value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" autoCapitalize="none" autoCorrect="off" spellCheck="false" maxLength="50" required enterKeyHint="next" /></div></div>
          <div><div className="mb-1.5 flex items-center justify-between"><label className="font-label-md font-medium" htmlFor="login-password">Mật khẩu <span className="text-primary">*</span></label><span className="font-label-sm text-outline">Bảo mật cookie</span></div><div className="relative"><span className="material-symbols-outlined pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-outline">lock</span><input id="login-password" name="password" className="w-full rounded-lg border border-surface-container-highest bg-surface-container-lowest py-3 pl-11 pr-12 text-[16px] text-on-surface outline-none transition focus:border-primary-container focus:ring-1 focus:ring-primary-container" value={password} onChange={(event) => setPassword(event.target.value)} type={showPassword ? 'text' : 'password'} autoComplete="current-password" maxLength="72" required enterKeyHint="done" /><button type="button" className="absolute right-3 top-1/2 -translate-y-1/2 rounded p-1 text-outline hover:text-on-surface focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary" onClick={() => setShowPassword((shown) => !shown)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={showPassword}><span className="material-symbols-outlined">{showPassword ? 'visibility_off' : 'visibility'}</span></button></div></div>
          {error ? <p className="rounded-lg border border-error/50 bg-error-container/30 p-3 text-sm text-error" role="alert">{error}</p> : null}
          <button className="flex min-h-12 w-full items-center justify-center gap-2 rounded-lg bg-primary-container px-4 py-3 font-label-lg font-bold text-on-primary shadow-[0_0_20px_rgba(255,69,162,.35)] transition hover:bg-tertiary-container disabled:cursor-wait disabled:opacity-70" type="submit" disabled={submitting}>{submitting ? <><span className="material-symbols-outlined animate-spin">progress_activity</span>Đang đăng nhập...</> : <>Đăng nhập <span className="material-symbols-outlined">arrow_forward</span></>}</button>
        </form>
        <p className="mt-6 text-center text-on-surface-variant">Chưa có tài khoản? <Link className="font-semibold text-primary hover:underline" to="/register">Đăng ký ngay</Link></p>
      </section>
    </main><CinemaFooter />
  </div>
}
