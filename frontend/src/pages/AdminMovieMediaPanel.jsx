import { useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { updateAdminMovieTrailer, uploadAdminMoviePoster } from '../api/adminMovies'
import { canonicalTrailerUrl, posterFileError, validTrailerUrl } from '../lib/adminMovieMedia'
import { getErrorMessage } from '../lib/errors'

export function AdminMovieMediaPanel({ movieId, posterUrl, trailerUrl, onChange }) {
  const [file, setFile] = useState(null)
  const [trailerInput, setTrailerInput] = useState(trailerUrl || '')
  const [uploading, setUploading] = useState(false)
  const [savingTrailer, setSavingTrailer] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const uploadInFlight = useRef(false)
  const trailerInFlight = useRef(false)

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
      setNotice(value ? 'Đã cập nhật trailer.' : 'Đã xóa trailer.')
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
          <label className="admin-movie-form__field" htmlFor="admin-movie-poster">Chọn poster PNG (tối đa 5 MiB)
            <input id="admin-movie-poster" type="file" accept=".png,image/png" onChange={(event) => { setFile(event.target.files?.[0] || null); setError('') }} />
          </label>
          <button type="submit" disabled={uploading}>{uploading ? 'Đang tải poster…' : 'Tải poster lên'}</button>
        </form>
      </div>
      <div>
        <h4>Trailer hiện tại</h4>
        {trailerUrl && validTrailerUrl(trailerUrl) ? <a href={trailerUrl} target="_blank" rel="noopener noreferrer">Mở trailer trên YouTube</a> : <p>{trailerUrl ? 'URL trailer hiện tại không hợp lệ; hãy thay thế hoặc xóa.' : 'Chưa có trailer.'}</p>}
        <form onSubmit={saveTrailer}>
          <label className="admin-movie-form__field" htmlFor="admin-movie-trailer">URL trailer YouTube
            <input id="admin-movie-trailer" type="url" value={trailerInput} onChange={(event) => { setTrailerInput(event.target.value); setError('') }} placeholder="https://www.youtube.com/watch?v=…" maxLength="500" />
          </label>
          <div className="admin-movie-media__actions">
            <button type="submit" disabled={savingTrailer || trailerInput === (trailerUrl || '')}>{savingTrailer ? 'Đang lưu…' : 'Lưu trailer'}</button>
            <button type="button" disabled={savingTrailer || !trailerUrl} onClick={() => { setTrailerInput(''); setError(''); setNotice('Nhấn “Lưu trailer” để xác nhận xóa.') }}>Xóa trailer</button>
          </div>
        </form>
      </div>
    </div>
  </section>
}
