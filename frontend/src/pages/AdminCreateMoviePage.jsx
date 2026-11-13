import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createMovie, fetchMovie, updateMovie, uploadMoviePoster } from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

const initialForm = {
  title: '',
  genre: '',
  releaseDate: '',
  durationMinutes: '',
  version: '2D',
  castText: '',
  director: '',
  studio: '',
  synopsis: '',
  trailerUrl: '',
  posterUrl: '',
}

const genres = [
  'Hành động',
  'Hài kịch',
  'Kinh dị',
  'Khoa học viễn tưởng',
  'Tình cảm',
]

export function AdminCreateMoviePage() {
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [posterPreviewError, setPosterPreviewError] = useState(false)
  const [posterFile, setPosterFile] = useState(null)
  const [posterPreviewUrl, setPosterPreviewUrl] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [isLoadingMovie, setIsLoadingMovie] = useState(false)
  const navigate = useNavigate()
  const { movieId } = useParams()
  const isEditing = Boolean(movieId)
  const posterPreviewRef = useRef(null)

  useEffect(() => () => {
    if (posterPreviewRef.current) URL.revokeObjectURL(posterPreviewRef.current)
  }, [])

  useEffect(() => {
    if (!movieId) return undefined

    let active = true
    setLoadError('')
    setIsLoadingMovie(true)
    fetchMovie(movieId)
      .then((movie) => {
        if (!active) return
        if (posterPreviewRef.current) {
          URL.revokeObjectURL(posterPreviewRef.current)
          posterPreviewRef.current = null
        }
        setPosterFile(null)
        setPosterPreviewUrl('')
        setForm({
          title: movie.title ?? '',
          genre: movie.genre ?? '',
          releaseDate: movie.releaseDate ?? '',
          durationMinutes: movie.durationMinutes ?? '',
          version: movie.version ?? '2D',
          castText: movie.castText ?? '',
          director: movie.director ?? '',
          studio: movie.studio ?? '',
          synopsis: movie.synopsis ?? '',
          trailerUrl: movie.trailerUrl ?? '',
          posterUrl: movie.posterUrl ?? '',
        })
      })
      .catch((requestError) => {
        if (active) setLoadError(getErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setIsLoadingMovie(false)
      })

    return () => { active = false }
  }, [movieId])

  function updateField(event) {
    const { name, value } = event.target
    if (name === 'posterUrl') setPosterPreviewError(false)
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handlePosterSelection(event) {
    const selectedFile = event.target.files?.[0]
    if (!selectedFile) return
    if (selectedFile.size > 5 * 1024 * 1024) {
      setError('Ảnh poster không được vượt quá 5 MB')
      event.target.value = ''
      return
    }

    if (posterPreviewRef.current) URL.revokeObjectURL(posterPreviewRef.current)
    const previewUrl = URL.createObjectURL(selectedFile)
    posterPreviewRef.current = previewUrl
    setPosterPreviewUrl(previewUrl)
    setPosterFile(selectedFile)
    setPosterPreviewError(false)
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)

    try {
      const posterUrl = posterFile
        ? (await uploadMoviePoster(posterFile)).posterUrl
        : form.posterUrl
      const movieRequest = {
        ...form,
        genre: form.genre || null,
        releaseDate: form.releaseDate || null,
        durationMinutes: Number(form.durationMinutes),
        posterUrl,
      }
      if (isEditing) {
        await updateMovie(movieId, movieRequest)
        navigate(`/movies/${movieId}`)
      } else {
        await createMovie(movieRequest)
        navigate('/movies')
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="admin-movie-page">
      <Link className="back-link" to={isEditing ? `/movies/${movieId}` : '/admin'}>
        {isEditing ? '← Quay lại chi tiết phim' : '← Quay lại quản trị'}
      </Link>

      <header className="admin-movie-header">
        <p className="eyebrow">Danh mục phim</p>
        <h1>{isEditing ? 'Chỉnh sửa phim' : 'Thêm phim mới'}</h1>
        <p className="hero-copy">
          {isEditing
            ? 'Cập nhật thông tin chi tiết. Trạng thái phim được giữ nguyên.'
            : 'Nhập thông tin phim. Các trường có dấu * là bắt buộc.'}
        </p>
      </header>

      {isLoadingMovie ? <p className="page-status">Đang tải thông tin phim…</p> : null}
      {loadError ? <p className="form-error admin-movie-error" role="alert">{loadError}</p> : null}

      {!isLoadingMovie && !loadError ? (
      <form className="admin-movie-form" onSubmit={handleSubmit}>
        <section className="admin-movie-section">
          <div className="admin-movie-section-heading">
            <span className="admin-movie-step">01</span>
            <div>
              <h2>Thông tin cơ bản</h2>
              <p>Tên phim, thể loại và lịch phát hành.</p>
            </div>
          </div>

          <div className="admin-movie-fields">
            <label className="admin-movie-field admin-movie-field-wide">
              <span>Tên phim <b>*</b></span>
              <input
                name="title"
                value={form.title}
                onChange={updateField}
                maxLength={255}
                placeholder="Ví dụ: Hành tinh xanh"
                required
              />
            </label>

            <label className="admin-movie-field">
              <span>Thể loại</span>
              <select name="genre" value={form.genre} onChange={updateField}>
                <option value="">Chọn thể loại</option>
                {form.genre && !genres.includes(form.genre) ? (
                  <option value={form.genre}>{form.genre} (đang lưu)</option>
                ) : null}
                {genres.map((genre) => <option key={genre} value={genre}>{genre}</option>)}
              </select>
            </label>

            <label className="admin-movie-field">
              <span>Ngày khởi chiếu</span>
              <input
                type="date"
                name="releaseDate"
                value={form.releaseDate}
                onChange={updateField}
              />
            </label>

            <label className="admin-movie-field">
              <span>Thời lượng (phút) <b>*</b></span>
              <input
                type="number"
                name="durationMinutes"
                value={form.durationMinutes}
                onChange={updateField}
                min="1"
                placeholder="120"
                required
              />
            </label>

            <label className="admin-movie-field">
              <span>Phiên bản <b>*</b></span>
              <input
                name="version"
                value={form.version}
                onChange={updateField}
                maxLength={50}
                placeholder="2D, 3D, IMAX..."
                required
              />
            </label>
          </div>
        </section>

        <section className="admin-movie-section">
          <div className="admin-movie-section-heading">
            <span className="admin-movie-step">02</span>
            <div>
              <h2>Chi tiết phim</h2>
              <p>Thông tin ê-kíp và nội dung giới thiệu.</p>
            </div>
          </div>

          <div className="admin-movie-fields">
            <label className="admin-movie-field">
              <span>Đạo diễn</span>
              <input name="director" value={form.director} onChange={updateField} maxLength={255} />
            </label>

            <label className="admin-movie-field">
              <span>Hãng phim</span>
              <input name="studio" value={form.studio} onChange={updateField} maxLength={255} />
            </label>

            <label className="admin-movie-field admin-movie-field-wide">
              <span>Diễn viên</span>
              <textarea
                name="castText"
                value={form.castText}
                onChange={updateField}
                rows="2"
                placeholder="Tên các diễn viên, ngăn cách bằng dấu phẩy"
              />
            </label>

            <label className="admin-movie-field admin-movie-field-wide">
              <span>Nội dung phim</span>
              <textarea
                name="synopsis"
                value={form.synopsis}
                onChange={updateField}
                rows="5"
                placeholder="Viết phần giới thiệu ngắn về phim..."
              />
            </label>
          </div>
        </section>

        <section className="admin-movie-section">
          <div className="admin-movie-section-heading">
            <span className="admin-movie-step">03</span>
            <div>
              <h2>Poster và trailer</h2>
              <p>Chọn ảnh PNG trên máy; ảnh sẽ được tải lên Cloudinary khi lưu phim.</p>
            </div>
          </div>

          <div className="admin-movie-fields admin-movie-media-fields">
            <label className="admin-movie-field">
              <span>Ảnh poster PNG</span>
              <input
                type="file"
                name="posterFile"
                accept="image/png,.png"
                onChange={handlePosterSelection}
              />
              <small>{posterFile ? `${posterFile.name} · tối đa 5 MB` : 'Chỉ nhận ảnh PNG, tối đa 5 MB.'}</small>
            </label>

            <div className="admin-movie-poster-preview" aria-label="Xem trước poster">
              {posterPreviewUrl || form.posterUrl ? (
                posterPreviewError ? (
                  <span className="admin-movie-poster-error">
                    Không tải được ảnh. Hãy dùng URL PNG trực tiếp, truy cập công khai.
                  </span>
                ) : (
                  <img
                    src={posterPreviewUrl || form.posterUrl}
                    alt="Xem trước poster phim"
                    onError={() => setPosterPreviewError(true)}
                  />
                )
              ) : (
                <span>Poster xem trước</span>
              )}
            </div>

            <label className="admin-movie-field admin-movie-field-wide">
              <span>Trailer URL</span>
              <input
                type="url"
                name="trailerUrl"
                value={form.trailerUrl}
                onChange={updateField}
                maxLength={500}
                placeholder="https://example.com/trailer"
              />
            </label>
          </div>
        </section>

        {error && <p className="form-error admin-movie-error" role="alert">{error}</p>}

        <div className="admin-movie-actions">
          <Link className="button button-secondary" to={isEditing ? `/movies/${movieId}` : '/admin'}>Hủy</Link>
          <button className="button" type="submit" disabled={isSaving}>
            {isSaving ? 'Đang lưu…' : isEditing ? 'Lưu thay đổi' : 'Tạo phim'}
          </button>
        </div>
      </form>
      ) : null}
    </section>
  )
}
