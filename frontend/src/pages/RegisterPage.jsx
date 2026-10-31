import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getErrorMessage } from '../lib/errors'

export function RegisterPage() {
  const { register: registerAccount } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await registerAccount({
        fullName: fullName.trim(),
        username: username.trim(),
        password,
        email: email.trim() || undefined,
      })
      navigate('/', { replace: true })
    } catch (submitError) {
      setError(getErrorMessage(submitError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="form-card">
      <h1>Tạo tài khoản</h1>
      <p className="muted">Đăng ký thành viên để tích điểm và đặt vé.</p>

      <form onSubmit={handleSubmit}>
        <label>
          Họ và tên
          <input
            autoComplete="name"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            required
          />
        </label>
        <label>
          Tên đăng nhập
          <input
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            minLength={3}
            required
          />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            minLength={8}
            required
          />
        </label>
        <label>
          Email (không bắt buộc)
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>

        {error ? <p className="form-error">{error}</p> : null}

        <button type="submit" className="button" disabled={isSubmitting}>
          {isSubmitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}
        </button>
      </form>

      <p className="muted">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </section>
  )
}
