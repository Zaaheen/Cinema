import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { createAdminMovie, getAdminGenres, getAdminMovie, updateAdminMovie } from '../api/adminMovies'
import { AdminMovieMediaPanel } from './AdminMovieMediaPanel'
import { AdminMovieScheduleEditor } from './AdminMovieScheduleEditor'

const EMPTY_FORM = {
  title: '',
  releaseDate: '',
  durationMinutes: '',
  version: '',
  castText: '',
  director: '',
  studio: '',
  synopsis: '',
  ageRating: '',
  genreIds: [],
  primaryGenreId: '',
}

const TEXT_FIELDS = ['title', 'releaseDate', 'durationMinutes', 'version', 'castText', 'director', 'studio', 'synopsis', 'ageRating']

function messageFor(error, fallback) {
  return error.response?.data?.message || fallback
}

function formFromMovie(movie) {
  const selectableGenres = (movie.genres || []).filter((genre) => genre.selectable)
  return {
    title: movie.title || '',
    releaseDate: movie.releaseDate || '',
    durationMinutes: movie.durationMinutes ? String(movie.durationMinutes) : '',
    version: movie.version || '',
    castText: movie.castText || '',
    director: movie.director || '',
    studio: movie.studio || '',
    synopsis: movie.synopsis || '',
    ageRating: movie.ageRating || '',
    genreIds: selectableGenres.map((genre) => genre.id),
    primaryGenreId: movie.primaryGenreId ? String(movie.primaryGenreId) : '',
  }
}

function isCanceled(error) {
  return error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError'
}

export function AdminMovieFormPage() {
  const { movieId } = useParams()
  const navigate = useNavigate()
  const isEditing = Boolean(movieId)
  const [form, setForm] = useState(EMPTY_FORM)
  const [genres, setGenres] = useState([])
  const [loading, setLoading] = useState(isEditing)
  const [genresLoading, setGenresLoading] = useState(true)
  const [requestError, setRequestError] = useState('')
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [initialForm, setInitialForm] = useState(EMPTY_FORM)
  const [ready, setReady] = useState(false)
  const [movieData, setMovieData] = useState(null)

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    async function load() {
      try {
        const requests = [getAdminGenres({ signal: controller.signal })]
        if (isEditing) requests.push(getAdminMovie(movieId, { signal: controller.signal }))
        const [availableGenres, movie] = await Promise.all(requests)
        if (!active) return
        setGenres(availableGenres)
        if (movie) {
          setMovieData(movie)
          const nextForm = formFromMovie(movie)
          setInitialForm(nextForm)
          setForm(nextForm)
        }
        setReady(true)
      } catch (error) {
        if (!active || isCanceled(error)) return
        setRequestError(messageFor(error, isEditing ? 'Không tải được thông tin phim.' : 'Không tải được danh sách thể loại.'))
      } finally {
        if (active) {
          setLoading(false)
          setGenresLoading(false)
        }
      }
    }

    void load()
    return () => { active = false; controller.abort() }
  }, [isEditing, movieId])

  const selectedGenres = useMemo(() => genres.filter((genre) => form.genreIds.includes(genre.id)), [form.genreIds, genres])
  const isDirty = ready && JSON.stringify(form) !== JSON.stringify(initialForm)

  useEffect(() => {
    if (!isDirty || saving) return undefined
    const warnBeforeUnload = (event) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warnBeforeUnload)
    return () => window.removeEventListener('beforeunload', warnBeforeUnload)
  }, [isDirty, saving])

  function changeField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  function toggleGenre(genreId) {
    setForm((current) => {
      const selected = current.genreIds.includes(genreId)
      const genreIds = selected ? current.genreIds.filter((id) => id !== genreId) : [...current.genreIds, genreId]
      const primaryGenreId = String(genreId) === current.primaryGenreId && selected ? '' : current.primaryGenreId
      return { ...current, genreIds, primaryGenreId }
    })
    setErrors((current) => ({ ...current, genreIds: undefined, primaryGenreId: undefined }))
  }

  function validate(nextForm) {
    const nextErrors = {}
    if (!nextForm.title.trim()) nextErrors.title = 'Nhập tên phim.'
    else if (nextForm.title.trim().length > 255) nextErrors.title = 'Tên phim tối đa 255 ký tự.'
    if (!nextForm.durationMinutes || !Number.isInteger(Number(nextForm.durationMinutes)) || Number(nextForm.durationMinutes) <= 0) nextErrors.durationMinutes = 'Thời lượng phải là số phút lớn hơn 0.'
    if (!nextForm.version.trim()) nextErrors.version = 'Nhập phiên bản phim.'
    else if (nextForm.version.trim().length > 50) nextErrors.version = 'Phiên bản tối đa 50 ký tự.'
    if (!isEditing || JSON.stringify(nextForm.genreIds) !== JSON.stringify(initialForm.genreIds) || nextForm.primaryGenreId !== initialForm.primaryGenreId) {
      if (!nextForm.genreIds.length) nextErrors.genreIds = 'Chọn ít nhất một thể loại.'
      if (nextForm.genreIds.length > 6) nextErrors.genreIds = 'Chỉ được chọn tối đa sáu thể loại.'
      if (!nextForm.primaryGenreId || !nextForm.genreIds.includes(Number(nextForm.primaryGenreId))) nextErrors.primaryGenreId = 'Chọn thể loại chính trong các thể loại đã chọn.'
    }
    if (nextForm.releaseDate === '' && isEditing && initialForm.releaseDate) nextErrors.releaseDate = 'API hiện tại không hỗ trợ xóa ngày khởi chiếu; hãy giữ nguyên hoặc chọn ngày khác.'
    if (nextForm.ageRating.length > 10) nextErrors.ageRating = 'Nhãn độ tuổi tối đa 10 ký tự.'
    if (nextForm.director.length > 255) nextErrors.director = 'Đạo diễn tối đa 255 ký tự.'
    if (nextForm.studio.length > 255) nextErrors.studio = 'Hãng phim tối đa 255 ký tự.'
    return nextErrors
  }

  function buildCreatePayload(nextForm) {
    return {
      title: nextForm.title.trim(),
      primaryGenreId: Number(nextForm.primaryGenreId),
      genreIds: nextForm.genreIds,
      releaseDate: nextForm.releaseDate || null,
      durationMinutes: Number(nextForm.durationMinutes),
      version: nextForm.version.trim(),
      castText: nextForm.castText || null,
      director: nextForm.director || null,
      studio: nextForm.studio || null,
      synopsis: nextForm.synopsis || null,
      ageRating: nextForm.ageRating || null,
    }
  }

  function buildPatchPayload(nextForm) {
    const payload = {}
    for (const field of TEXT_FIELDS) {
      if (nextForm[field] !== initialForm[field]) {
        payload[field] = field === 'durationMinutes' ? Number(nextForm[field]) : (field === 'title' || field === 'version' ? nextForm[field].trim() : nextForm[field])
      }
    }
    if (JSON.stringify(nextForm.genreIds) !== JSON.stringify(initialForm.genreIds) || nextForm.primaryGenreId !== initialForm.primaryGenreId) {
      payload.genreIds = nextForm.genreIds
      payload.primaryGenreId = Number(nextForm.primaryGenreId)
    }
    return payload
  }

  async function submit(event) {
    event.preventDefault()
    if (saving) return
    const nextErrors = validate(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return
    const payload = isEditing ? buildPatchPayload(form) : buildCreatePayload(form)
    if (isEditing && Object.keys(payload).length === 0) {
      navigate('/admin/movies')
      return
    }
    try {
      setSaving(true)
      setRequestError('')
      const savedMovie = isEditing ? await updateAdminMovie(movieId, payload) : await createAdminMovie(payload)
      toast.success(isEditing ? 'Cập nhật phim thành công.' : 'Tạo phim thành công.')
      navigate(isEditing ? '/admin/movies' : `/admin/movies/${savedMovie.id}/edit`)
    } catch (error) {
      const message = messageFor(error, isEditing ? 'Cập nhật phim thất bại.' : 'Tạo phim thất bại.')
      setRequestError(message)
      toast.error(message)
    } finally {
      setSaving(false)
    }
  }

  function leaveForm() {
    if (!isDirty || window.confirm('Bạn có thay đổi chưa lưu. Rời khỏi trang?')) navigate('/admin/movies')
  }

  if (loading) return <p className="page-status" role="status">Đang tải thông tin phim…</p>
  if (requestError && !ready) return <section className="admin-movie-form"><p className="price-admin__error" role="alert">{requestError}</p><Link to="/admin/movies">Quay lại danh sách phim</Link></section>

  const fieldError = (name) => errors[name] ? <span className="price-admin__field-error" id={`movie-${name}-error`}>{errors[name]}</span> : null
  const describedBy = (name) => errors[name] ? `movie-${name}-error` : undefined

  return <section className="admin-movie-form" aria-labelledby="admin-movie-form-title">
    <header className="admin-movie-form__header">
      <div><p className="eyebrow">DANH MỤC PHIM</p><h2 id="admin-movie-form-title">{isEditing ? 'Chỉnh sửa phim' : 'Thêm phim'}</h2><p>{isEditing ? 'Chỉ các trường được thay đổi mới gửi lên API quản trị.' : 'Phim mới sẽ được backend tạo ở trạng thái sắp chiếu.'}</p></div>
      <Link className="admin-movie-form__back" to="/admin/movies">Quay lại danh sách</Link>
    </header>

    <form className="admin-movie-form__card" onSubmit={submit} aria-labelledby="admin-movie-form-title">
      {requestError ? <p className="price-admin__error" role="alert">{requestError}</p> : null}
      <p className="admin-movie-form__required"><span aria-hidden="true">*</span> Trường bắt buộc</p>
      <div className="admin-movie-form__grid">
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-title">Tên phim <span aria-hidden="true">*</span><input id="movie-title" name="title" value={form.title} onChange={changeField} maxLength="255" required aria-invalid={Boolean(errors.title)} aria-describedby={describedBy('title')} />{fieldError('title')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-duration">Thời lượng (phút) <span aria-hidden="true">*</span><input id="movie-duration" name="durationMinutes" type="number" min="1" step="1" inputMode="numeric" value={form.durationMinutes} onChange={changeField} required aria-invalid={Boolean(errors.durationMinutes)} aria-describedby={describedBy('durationMinutes')} />{fieldError('durationMinutes')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-version">Phiên bản <span aria-hidden="true">*</span><input id="movie-version" name="version" value={form.version} onChange={changeField} maxLength="50" required aria-invalid={Boolean(errors.version)} aria-describedby={describedBy('version')} />{fieldError('version')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-release-date">Ngày khởi chiếu<input id="movie-release-date" name="releaseDate" type="date" value={form.releaseDate} onChange={changeField} aria-invalid={Boolean(errors.releaseDate)} aria-describedby={describedBy('releaseDate')} />{fieldError('releaseDate')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-age-rating">Nhãn độ tuổi<input id="movie-age-rating" name="ageRating" value={form.ageRating} onChange={changeField} maxLength="10" aria-invalid={Boolean(errors.ageRating)} aria-describedby={describedBy('ageRating')} />{fieldError('ageRating')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-director">Đạo diễn<input id="movie-director" name="director" value={form.director} onChange={changeField} maxLength="255" aria-invalid={Boolean(errors.director)} aria-describedby={describedBy('director')} />{fieldError('director')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-studio">Hãng phim<input id="movie-studio" name="studio" value={form.studio} onChange={changeField} maxLength="255" aria-invalid={Boolean(errors.studio)} aria-describedby={describedBy('studio')} />{fieldError('studio')}</label>
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-cast">Diễn viên<textarea id="movie-cast" name="castText" value={form.castText} onChange={changeField} /></label>
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-synopsis">Tóm tắt nội dung<textarea id="movie-synopsis" name="synopsis" value={form.synopsis} onChange={changeField} /></label>
      </div>

      <fieldset className="admin-movie-form__genres" disabled={genresLoading || saving} aria-describedby={describedBy('genreIds')}>
        <legend>Thể loại {!isEditing ? <span aria-hidden="true">*</span> : null}</legend>
        {genresLoading ? <p role="status">Đang tải thể loại…</p> : null}
        {!genresLoading && genres.length === 0 ? <p className="price-admin__error" role="alert">Chưa có thể loại chuẩn để chọn. Không thể tạo phim mới.</p> : null}
        {!genresLoading && genres.length > 0 ? <div className="admin-movie-form__genre-options">{genres.map((genre) => <label key={genre.id}><input type="checkbox" name="genreIds" checked={form.genreIds.includes(genre.id)} onChange={() => toggleGenre(genre.id)} />{genre.name}</label>)}</div> : null}
        {fieldError('genreIds')}
      </fieldset>

      <label className="admin-movie-form__field" htmlFor="movie-primary-genre">Thể loại chính {!isEditing ? <span aria-hidden="true">*</span> : null}<select id="movie-primary-genre" name="primaryGenreId" value={form.primaryGenreId} onChange={changeField} disabled={genresLoading || saving || selectedGenres.length === 0} aria-invalid={Boolean(errors.primaryGenreId)} aria-describedby={describedBy('primaryGenreId')} required={!isEditing}><option value="">Chọn thể loại chính</option>{selectedGenres.map((genre) => <option key={genre.id} value={genre.id}>{genre.name}</option>)}</select>{fieldError('primaryGenreId')}</label>

      <footer className="admin-movie-form__actions"><button type="button" disabled={saving} onClick={leaveForm}>Hủy</button><button className="price-admin__primary" type="submit" disabled={saving || genresLoading || (!isEditing && genres.length === 0)}>{saving ? 'Đang lưu…' : isEditing ? 'Lưu thay đổi' : 'Tạo phim'}</button></footer>
    </form>
    {isEditing && movieData ? <>
      <AdminMovieMediaPanel movieId={movieId} posterUrl={movieData.posterUrl} trailerUrl={movieData.trailerUrl}
        onChange={(media) => setMovieData((current) => ({ ...current, ...media }))} />
      <AdminMovieScheduleEditor movieId={movieId} movieTitle={movieData.title} durationMinutes={movieData.durationMinutes}
        movieStatus={movieData.status} durationHasUnsavedChanges={form.durationMinutes !== initialForm.durationMinutes} />
    </> : <p className="admin-movie-form__hint">Sau khi tạo phim, trang chỉnh sửa sẽ mở để bạn tải poster, gắn trailer và tạo lịch chiếu.</p>}
  </section>
}
