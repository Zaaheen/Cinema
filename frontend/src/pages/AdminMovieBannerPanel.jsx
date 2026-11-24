import { useEffect, useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { uploadAdminMovieBanner } from '../api/adminMovies'
import { bannerFileError } from '../lib/adminMovieMedia'
import { getErrorMessage } from '../lib/errors'

export function AdminMovieBannerPanel({ movieId, backdropUrl, onChange }) {
  const [file, setFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const uploadInFlight = useRef(false)
  const previewRef = useRef('')

  useEffect(() => () => { if (previewRef.current) URL.revokeObjectURL(previewRef.current) }, [])

  function chooseFile(event) {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current)
    const selected = event.target.files?.[0] || null
    const nextPreview = selected && !bannerFileError(selected) ? URL.createObjectURL(selected) : ''
    previewRef.current = nextPreview
    setPreviewUrl(nextPreview)
    setFile(selected)
    setError('')
    setNotice('')
  }

  async function upload(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    if (uploadInFlight.current) return
    const validation = bannerFileError(file)
    if (validation) { setError(validation); return }
    uploadInFlight.current = true
    setUploading(true)
    setError('')
    try {
      const result = await uploadAdminMovieBanner(movieId, file)
      onChange({ backdropUrl: result.backdropUrl })
      setFile(null)
      formElement.reset()
      if (previewRef.current) URL.revokeObjectURL(previewRef.current)
      previewRef.current = ''
      setPreviewUrl('')
      setNotice('Banner đã được lưu và có thể hiển thị trong mục phim nổi bật khi phim đang chiếu.')
      toast.success('Đã cập nhật banner phim.')
    } catch (caught) {
      setError(getErrorMessage(caught))
    } finally {
      uploadInFlight.current = false
      setUploading(false)
    }
  }

  return <section className="admin-movie-banner admin-movie-form__card" aria-labelledby="admin-movie-banner-title">
    <div><h3 id="admin-movie-banner-title">Banner phim nổi bật</h3><p>Ảnh ngang này thay cho poster ở mục phim nổi bật khi phim đang chiếu. Trang chủ lấy tên và nội dung từ thông tin phim.</p></div>
    {error && <p className="price-admin__error" role="alert">{error}</p>}
    {notice && <p className="admin-movie-media__notice" role="status">{notice}</p>}
    <div className="admin-movie-banner__grid">
      <div><h4>Banner hiện tại</h4>{backdropUrl ? <img src={backdropUrl} alt="Banner hiện tại của phim" /> : <p>Chưa có banner riêng. Trang chủ sẽ dùng poster nếu có.</p>}</div>
      <form onSubmit={upload}>
        <label className="admin-movie-form__field" htmlFor="admin-movie-banner-file">Chọn banner PNG, JPG/JPEG hoặc WebP (tối đa 5 MiB)
          <input id="admin-movie-banner-file" type="file" accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp" onChange={chooseFile} />
        </label>
        <p>Chọn ảnh ngang, tỉ lệ tối thiểu 7:5; tỉ lệ 16:9 hiển thị đẹp nhất.</p>
        {previewUrl && <div className="admin-movie-banner__draft"><span>Xem trước ảnh mới</span><img src={previewUrl} alt="Bản xem trước banner sẽ tải lên" /></div>}
        <button type="submit" disabled={uploading}>{uploading ? 'Đang tải banner…' : 'Lưu banner phim'}</button>
      </form>
    </div>
  </section>
}
