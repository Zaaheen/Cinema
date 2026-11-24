import { Link } from 'react-router-dom'

export function LoginCard({ username, password, error, isSubmitting, onUsernameChange, onPasswordChange, onSubmit }) {
  return (
    <section className="auth-page">
      <div className="auth-aside" aria-hidden="true">
        <span className="brand-mark">C</span>
        <p>ĐÊM NAY XEM GÌ?</p>
        <h1>Chạm để bước vào thế giới điện ảnh.</h1>
      </div>
      <div className="form-card auth-card">
        <p className="eyebrow">Chào mừng trở lại</p>
        <h1>Đăng nhập</h1>
        <p className="muted">Đăng nhập để tiếp tục đặt vé và quản lý tài khoản.</p>
        <form onSubmit={onSubmit}>
          <label>
            Tên đăng nhập
            <input value={username} onChange={(event) => onUsernameChange(event.target.value)} placeholder="Nhập tên đăng nhập" autoComplete="username" name="username" required />
          </label>
          <label>
            Mật khẩu
            <input type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} placeholder="Nhập mật khẩu" autoComplete="current-password" name="password" required />
          </label>
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          <button className="button" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}</button>
        </form>
        <p className="muted">Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link></p>
      </div>
    </section>
  )
}
