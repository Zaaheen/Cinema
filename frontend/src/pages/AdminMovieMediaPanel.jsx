import { useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { updateAdminMovieTrailer, uploadAdminMoviePoster } from '../api/adminMovies'
import { canonicalTrailerUrl, posterFileError, validTrailerUrl } from '../lib/adminMovieMedia'
import { trailerEmbedUrl, trailerVideoId, trailerWatchUrl } from '../lib/trailerPlayer'
import { getErrorMessage } from '../lib/errors'

export function AdminMovieMediaPanel({ movieId, posterUrl, trailerUrl, active = true, onChange }) {
  const [file, setFile] = useState(null)
  const [trailerInput, setTrailerInput] = useState(trailerUrl || '')
  const [uploading, setUploading] = useState(false)
  const [savingTrailer, setSavingTrailer] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [draftPreviewOpen, setDraftPreviewOpen] = useState(false)
  const uploadInFlight = useRef(false)
  const trailerInFlight = useRef(false)
  const normalizedDraftUrl = canonicalTrailerUrl(trailerInput.trim())

  async function uploadPoster(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    if (uploadInFlight.current) return
    const validation = posterFileError(file)
    if (validation) { setError(validation); return }
    uploadInFlight.current = true
    setUploading(true)
    setError('')
    setNotice('')
    try {
      const result = await uploadAdminMoviePoster(movieId, file)
      onChange({ posterUrl: result.posterUrl })
      setFile(null)
      formElement.reset()
      setNotice('Đã cập nhật poster.')
      toast.success('Đã cập nhật poster.')
    } catch (caught) {
      setError(getErrorMessage(caught))
    } finally {
      uploadInFlight.current = false
      setUploading(false)
    }
  }

  async function saveTrailer(event) {
    event.preventDefault()
    if (trailerInFlight.current) return
    const value = trailerInput.trim()
    const canonicalUrl = value ? canonicalTrailerUrl(value) : null
    if (value && !canonicalUrl) {
      setError('Chỉ chấp nhận URL HTTPS dạng youtube.com/watch?v=… hoặc youtu.be/… với video ID hợp lệ.')
      return
    }
    trailerInFlight.current = true
    setSavingTrailer(true)
    setError('')
    setNotice('')
    try {
      const result = await updateAdminMovieTrailer(movieId, canonicalUrl)
      onChange({ trailerUrl: result.trailerUrl })
      setTrailerInput(result.trailerUrl || '')
      setPreviewOpen(false)
      setDraftPreviewOpen(false)
      setNotice(value ? `Đã cập nhật trailer theo URL chuẩn: ${result.trailerUrl}` : 'Đã xóa trailer.')
      toast.success(value ? 'Đã cập nhật trailer.' : 'Đã xóa trailer.')
    } catch (caught) {
      setError(getErrorMessage(caught))
    } finally {
      trailerInFlight.current = false
      setSavingTrailer(false)
    }
  }

  return <section className="admin-movie-media admin-movie-form__card" aria-labelledby="admin-movie-media-title">
    <div><h3 id="admin-movie-media-title">Poster và trailer</h3><p>Ảnh poster được lưu qua API quản trị. Trailer chỉ nhận liên kết YouTube hợp lệ.</p></div>
    {error ? <p className="price-admin__error" role="alert">{error}</p> : null}
    {notice ? <p className="admin-movie-media__notice" role="status">{notice}</p> : null}
    <div className="admin-movie-media__grid">
      <div>
        <h4>Poster hiện tại</h4>
        {posterUrl ? <img className="admin-movie-media__poster" src={posterUrl} alt="Poster hiện tại của phim" /> : <p>Chưa có poster.</p>}
        <form onSubmit={uploadPoster}>
          <label className="admin-movie-form__field" htmlFor="admin-movie-poster">Chọn poster PNG, JPG/JPEG hoặc WebP (tối đa 5 MiB)
            <input id="admin-movie-poster" type="file" accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp" onChange={(event) => { setFile(event.target.files?.[0] || null); setError('') }} />
          </label>
          <button type="submit" disabled={uploading}>{uploading ? 'Đang tải poster…' : 'Tải poster lên'}</button>
        </form>
      </div>
      <div>
        <h4>Trailer hiện tại</h4>
        {trailerUrl && validTrailerUrl(trailerUrl) ? <><a href={trailerUrl} target="_blank" rel="noopener noreferrer">Mở trailer trên YouTube</a><p className="admin-movie-media__hint">Cinema lưu URL YouTube ở dạng chuẩn để hiển thị ổn định.</p><button type="button" className="admin-movie-media__preview-button" onClick={() => { setDraftPreviewOpen(false); setPreviewOpen((open) => !open) }}>{previewOpen ? 'Ẩn xem trước' : 'Xem trước trailer hiện tại'}</button>{active && previewOpen ? <div className="admin-movie-media__preview"><iframe title="Xem trước trailer hiện tại" src={trailerEmbedUrl(trailerVideoId(trailerUrl), window.location.origin)} allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" /><a href={trailerWatchUrl(trailerVideoId(trailerUrl))} target="_blank" rel="noopener noreferrer">Không phát được? Mở trên YouTube</a></div> : null}</> : <p>{trailerUrl ? 'URL trailer hiện tại không hợp lệ; hãy thay thế hoặc xóa.' : 'Chưa có trailer.'}</p>}
        <form onSubmit={saveTrailer}>
          <label className="admin-movie-form__field" htmlFor="admin-movie-trailer">URL trailer YouTube
            <input id="admin-movie-trailer" type="url" value={trailerInput} onChange={(event) => { setTrailerInput(event.target.value); setDraftPreviewOpen(false); setError('') }} placeholder="https://www.youtube.com/watch?v=…" maxLength="500" />
          </label>
          {trailerInput.trim() && !normalizedDraftUrl ? <p className="price-admin__field-error">Liên kết phải là URL YouTube hợp lệ với video ID 11 ký tự.</p> : null}
          {normalizedDraftUrl ? <div className="admin-movie-media__draft">
            <p>Video sẽ lưu: <strong>{trailerVideoId(normalizedDraftUrl)}</strong></p>
            <p>URL chuẩn: <a href={normalizedDraftUrl} target="_blank" rel="noopener noreferrer">{normalizedDraftUrl}</a></p>
            {normalizedDraftUrl !== trailerUrl ? <button type="button" onClick={() => { setPreviewOpen(false); setDraftPreviewOpen((open) => !open) }}>{draftPreviewOpen ? 'Ẩn bản xem trước' : 'Xem trước liên kết mới'}</button> : null}
            {active && draftPreviewOpen ? <div className="admin-movie-media__preview"><iframe title="Xem trước liên kết trailer mới" src={trailerEmbedUrl(trailerVideoId(normalizedDraftUrl), window.location.origin)} allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" /><a href={normalizedDraftUrl} target="_blank" rel="noopener noreferrer">Mở video trên YouTube</a></div> : null}
          </div> : null}
          <div className="admin-movie-media__actions">
            <button type="submit" disabled={savingTrailer || (canonicalTrailerUrl(trailerInput.trim()) || '') === (trailerUrl || '')}>{savingTrailer ? 'Đang lưu…' : 'Lưu trailer'}</button>
            <button type="button" disabled={savingTrailer || !trailerUrl} onClick={() => { setTrailerInput(''); setDraftPreviewOpen(false); setError(''); setNotice('Nhấn “Lưu trailer” để xác nhận xóa.') }}>Xóa trailer</button>
          </div>
        </form>
      </div>
    </div>
  </section>
}
