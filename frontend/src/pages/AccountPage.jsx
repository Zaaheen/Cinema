import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getErrorMessage } from '../lib/errors'

export function AccountPage() {
  const { user, updateProfile, changePassword } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [profileError, setProfileError] = useState(null)
  const [passwordError, setPasswordError] = useState(null)
  const [profileSaved, setProfileSaved] = useState(false)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isChangingPassword, setIsChangingPassword] = useState(false)

  if (!user) {
    return null
  }

  const submitProfile = async (event) => {
    event.preventDefault()
    setProfileError(null)
    setProfileSaved(false)
    setIsSavingProfile(true)
    try {
      await updateProfile({
        fullName: fullName.trim(),
        email: email.trim() || null,
        phone: phone.trim() || null,
      })
      setProfileSaved(true)
    } catch (error) {
      setProfileError(getErrorMessage(error))
    } finally {
      setIsSavingProfile(false)
    }
  }

  const submitPassword = async (event) => {
    event.preventDefault()
    setPasswordError(null)
    setIsChangingPassword(true)
    try {
      await changePassword({ currentPassword, newPassword })
      navigate('/login', { replace: true })
    } catch (error) {
      setPasswordError(getErrorMessage(error))
    } finally {
      setIsChangingPassword(false)
    }
  }

  return (
    <section className="form-card">
      <h1>Tài khoản</h1>
      <dl className="detail-list">
        <div>
          <dt>Họ và tên</dt>
          <dd>{user.fullName}</dd>
        </div>
        <div>
          <dt>Tên đăng nhập</dt>
          <dd>{user.username}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{user.email ?? 'Chưa cập nhật'}</dd>
        </div>
        <div>
          <dt>Số điện thoại</dt>
          <dd>{user.phone ?? 'Chưa cập nhật'}</dd>
        </div>
        <div>
          <dt>Vai trò</dt>
          <dd>{user.role}</dd>
        </div>
        <div>
          <dt>Mã thành viên</dt>
          <dd>{user.memberCode ?? 'Không có'}</dd>
        </div>
      </dl>

      <form onSubmit={submitProfile}>
        <h2>Cập nhật hồ sơ</h2>
        <label>
          Họ và tên
          <input value={fullName} onChange={(event) => setFullName(event.target.value)} maxLength="100" required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength="255" />
        </label>
        <label>
          Số điện thoại
          <input value={phone} onChange={(event) => setPhone(event.target.value)} inputMode="tel" />
        </label>
        {profileError ? <p className="form-error">{profileError}</p> : null}
        {profileSaved ? <p className="muted">Đã cập nhật hồ sơ.</p> : null}
        <button type="submit" className="button" disabled={isSavingProfile}>
          {isSavingProfile ? 'Đang lưu...' : 'Lưu hồ sơ'}
        </button>
      </form>

      <form onSubmit={submitPassword}>
        <h2>Đổi mật khẩu</h2>
        <label>
          Mật khẩu hiện tại
          <input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required />
        </label>
        <label>
          Mật khẩu mới
          <input type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength="8" maxLength="72" required />
        </label>
        {passwordError ? <p className="form-error">{passwordError}</p> : null}
        <button type="submit" className="button" disabled={isChangingPassword}>
          {isChangingPassword ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu'}
        </button>
      </form>
    </section>
  )
}
