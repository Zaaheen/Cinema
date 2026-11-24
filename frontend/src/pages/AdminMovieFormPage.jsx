import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { createAdminMovie, getAdminGenres, getAdminMovie, updateAdminMovie } from '../api/adminMovies'
import { AdminMovieMediaPanel } from './AdminMovieMediaPanel'
import { AdminMovieBannerPanel } from './AdminMovieBannerPanel'
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
  status: 'COMING_SOON',
  genreIds: [],
  primaryGenreId: '',
}

const TEXT_FIELDS = ['title', 'releaseDate', 'durationMinutes', 'version', 'castText', 'director', 'studio', 'synopsis', 'ageRating', 'status']
const MOVIE_STATUSES = [
  { value: 'COMING_SOON', label: 'Sắp chiếu' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'STOPPED', label: 'Dừng chiếu' },
]
const FIELD_IDS = {
  title: 'movie-title', durationMinutes: 'movie-duration', version: 'movie-version',
  releaseDate: 'movie-release-date', status: 'movie-status', ageRating: 'movie-age-rating',
  director: 'movie-director', studio: 'movie-studio', genreIds: 'movie-genres', primaryGenreId: 'movie-primary-genre',
}

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
    status: movie.status || 'COMING_SOON',
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
  const [activeSection, setActiveSection] = useState('details')
  const [reviewReady, setReviewReady] = useState(false)
  const errorSummaryRef = useRef(null)
  const reviewRef = useRef(null)
  const saveInFlight = useRef(false)

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
    setReviewReady(false)
  }

  function toggleGenre(genreId) {
    setForm((current) => {
      const selected = current.genreIds.includes(genreId)
      const genreIds = selected ? current.genreIds.filter((id) => id !== genreId) : [...current.genreIds, genreId]
      const primaryGenreId = String(genreId) === current.primaryGenreId && selected ? '' : current.primaryGenreId
      return { ...current, genreIds, primaryGenreId }
    })
    setErrors((current) => ({ ...current, genreIds: undefined, primaryGenreId: undefined }))
    setReviewReady(false)
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
    if (isEditing && !MOVIE_STATUSES.some((option) => option.value === nextForm.status)) nextErrors.status = 'Chọn trạng thái phim hợp lệ.'
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

  function submit(event) {
    event.preventDefault()
    if (saving) return
    const nextErrors = validate(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) {
      setReviewReady(false)
      requestAnimationFrame(() => errorSummaryRef.current?.focus())
      return
    }
    setReviewReady(true)
    requestAnimationFrame(() => reviewRef.current?.focus())
  }

  async function save() {
    if (saveInFlight.current || !reviewReady) return
    const nextErrors = validate(form)
    if (Object.keys(nextErrors).length) {
      setErrors(nextErrors)
      setReviewReady(false)
      requestAnimationFrame(() => errorSummaryRef.current?.focus())
      return
    }
    const payload = isEditing ? buildPatchPayload(form) : buildCreatePayload(form)
    if (isEditing && Object.keys(payload).length === 0) {
      navigate('/admin/movies')
      return
    }
    try {
      saveInFlight.current = true
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
      saveInFlight.current = false
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
  const selectedGenreNames = selectedGenres.map((genre) => genre.name).join(', ') || 'Chưa chọn'
  const primaryGenreName = selectedGenres.find((genre) => String(genre.id) === form.primaryGenreId)?.name || 'Chưa chọn'
  const movieSections = [
    { id: 'details', label: 'Thông tin phim', note: isDirty ? 'Có thay đổi chưa lưu' : 'Thông tin và trạng thái' },
    ...(isEditing ? [
      { id: 'media', label: 'Hình ảnh & trailer', note: 'Lưu từng mục riêng' },
      { id: 'schedule', label: 'Lịch chiếu', note: 'Quản lý suất chiếu' },
    ] : []),
  ]

  return <section className="admin-movie-form" aria-labelledby="admin-movie-form-title">
    <header className="admin-movie-form__header">
      <div><p className="eyebrow">Danh mục phim</p><h2 id="admin-movie-form-title">{isEditing ? `Chỉnh sửa ${movieData?.title || 'phim'}` : 'Thêm phim'}</h2><p>{isEditing ? 'Chọn từng phần để quản lý. Thông tin phim, hình ảnh và lịch chiếu được lưu riêng.' : 'Nhập thông tin phim, kiểm tra lại rồi tạo phim. Phim mới ở trạng thái sắp chiếu.'}</p></div>
      <button className="admin-movie-form__back" type="button" onClick={leaveForm}>Quay lại danh sách</button>
    </header>

    {isEditing && <nav className="admin-editor-nav" aria-label="Các phần quản trị phim">
      {movieSections.map((section, index) => <button key={section.id} type="button" className={activeSection === section.id ? 'admin-editor-nav__item admin-editor-nav__item--active' : 'admin-editor-nav__item'} aria-current={activeSection === section.id ? 'step' : undefined} onClick={() => setActiveSection(section.id)}><span className="admin-editor-nav__index">{index + 1}</span><span><strong>{section.label}</strong><small>{section.note}</small></span></button>)}
    </nav>}

    <div hidden={activeSection !== 'details'}>
    <form className="admin-movie-form__card" onSubmit={submit} aria-labelledby="admin-movie-form-title" noValidate>
      {requestError ? <p className="price-admin__error" role="alert">{requestError}</p> : null}
      {reviewReady ? <section className="admin-editor-review" tabIndex={-1} ref={reviewRef} aria-labelledby="movie-review-title">
        <p className="admin-editor-review__eyebrow">Kiểm tra trước khi lưu</p><h3 id="movie-review-title">{isEditing ? 'Xác nhận thay đổi phim' : 'Xác nhận tạo phim'}</h3>
        <p>Đọc lại các thông tin quan trọng. Poster, banner, trailer và lịch chiếu được lưu ở các phần riêng sau khi tạo phim.</p>
        <dl>
          <div><dt>Tên phim</dt><dd>{form.title.trim()}</dd></div>
          <div><dt>Trạng thái</dt><dd>{MOVIE_STATUSES.find((item) => item.value === form.status)?.label || 'Sắp chiếu'}</dd></div>
          <div><dt>Thời lượng · phiên bản</dt><dd>{form.durationMinutes} phút · {form.version.trim()}</dd></div>
          <div><dt>Thể loại</dt><dd>{selectedGenreNames}</dd></div>
          <div><dt>Thể loại chính</dt><dd>{primaryGenreName}</dd></div>
          <div><dt>Ngày khởi chiếu</dt><dd>{form.releaseDate || 'Chưa nhập'}</dd></div>
          <div><dt>Nhãn độ tuổi</dt><dd>{form.ageRating || 'Chưa nhập'}</dd></div>
          <div><dt>Đạo diễn · hãng phim</dt><dd>{[form.director, form.studio].filter(Boolean).join(' · ') || 'Chưa nhập'}</dd></div>
          <div><dt>Diễn viên</dt><dd>{form.castText || 'Chưa nhập'}</dd></div>
          <div><dt>Tóm tắt</dt><dd>{form.synopsis || 'Chưa nhập'}</dd></div>
        </dl>
        {form.status === 'STOPPED' && <p className="admin-editor-review__warning">Dừng chiếu sẽ ẩn phim khỏi danh mục công khai và khóa bán vé trực tuyến. Suất chiếu đã tạo không tự hủy.</p>}
      </section> : <div className="admin-movie-form__edit">
      {Object.keys(errors).length > 0 && <div className="admin-editor-errors" ref={errorSummaryRef} tabIndex={-1} role="alert"><strong>Kiểm tra lại {Object.keys(errors).length} trường</strong><ul>{Object.entries(errors).map(([name, message]) => <li key={name}><button type="button" onClick={() => document.getElementById(FIELD_IDS[name])?.focus()}>{message}</button></li>)}</ul></div>}
      <p className="admin-movie-form__required"><span aria-hidden="true">*</span> Trường bắt buộc</p>
      <div className="admin-movie-form__grid">
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-title">Tên phim <span aria-hidden="true">*</span><input id="movie-title" name="title" value={form.title} onChange={changeField} maxLength="255" required aria-invalid={Boolean(errors.title)} aria-describedby={describedBy('title')} />{fieldError('title')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-duration">Thời lượng (phút) <span aria-hidden="true">*</span><input id="movie-duration" name="durationMinutes" type="number" min="1" step="1" inputMode="numeric" value={form.durationMinutes} onChange={changeField} required aria-invalid={Boolean(errors.durationMinutes)} aria-describedby={describedBy('durationMinutes')} />{fieldError('durationMinutes')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-version">Phiên bản <span aria-hidden="true">*</span><input id="movie-version" name="version" value={form.version} onChange={changeField} maxLength="50" required aria-invalid={Boolean(errors.version)} aria-describedby={describedBy('version')} />{fieldError('version')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-release-date">Ngày khởi chiếu<input id="movie-release-date" name="releaseDate" type="date" value={form.releaseDate} onChange={changeField} aria-invalid={Boolean(errors.releaseDate)} aria-describedby={describedBy('releaseDate')} />{fieldError('releaseDate')}</label>
        {isEditing && <label className="admin-movie-form__field" htmlFor="movie-status">Trạng thái phim
          <select id="movie-status" name="status" value={form.status} onChange={changeField} aria-invalid={Boolean(errors.status)} aria-describedby={errors.status ? describedBy('status') : form.status === 'STOPPED' ? 'movie-status-hint' : undefined}>
            {MOVIE_STATUSES.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>{fieldError('status')}
        </label>}
        {isEditing && form.status === 'STOPPED' && <p className="admin-movie-form__status-hint" id="movie-status-hint">Dừng chiếu sẽ ẩn phim khỏi danh mục công khai và khóa bán vé trực tuyến. Các suất chiếu đã tạo vẫn còn trong quản trị; nếu cần hủy, hãy hủy từng suất riêng.</p>}
        <label className="admin-movie-form__field" htmlFor="movie-age-rating">Nhãn độ tuổi<input id="movie-age-rating" name="ageRating" value={form.ageRating} onChange={changeField} maxLength="10" aria-invalid={Boolean(errors.ageRating)} aria-describedby={describedBy('ageRating')} />{fieldError('ageRating')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-director">Đạo diễn<input id="movie-director" name="director" value={form.director} onChange={changeField} maxLength="255" aria-invalid={Boolean(errors.director)} aria-describedby={describedBy('director')} />{fieldError('director')}</label>
        <label className="admin-movie-form__field" htmlFor="movie-studio">Hãng phim<input id="movie-studio" name="studio" value={form.studio} onChange={changeField} maxLength="255" aria-invalid={Boolean(errors.studio)} aria-describedby={describedBy('studio')} />{fieldError('studio')}</label>
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-cast">Diễn viên<textarea id="movie-cast" name="castText" value={form.castText} onChange={changeField} /></label>
        <label className="admin-movie-form__field admin-movie-form__field--wide" htmlFor="movie-synopsis">Tóm tắt nội dung<textarea id="movie-synopsis" name="synopsis" value={form.synopsis} onChange={changeField} /></label>
      </div>

      <fieldset id="movie-genres" tabIndex={-1} className="admin-movie-form__genres" disabled={genresLoading || saving} aria-describedby={describedBy('genreIds')}>
        <legend>Thể loại {!isEditing ? <span aria-hidden="true">*</span> : null}</legend>
        {genresLoading ? <p role="status">Đang tải thể loại…</p> : null}
        {!genresLoading && genres.length === 0 ? <p className="price-admin__error" role="alert">Chưa có thể loại chuẩn để chọn. Không thể tạo phim mới.</p> : null}
        {!genresLoading && genres.length > 0 ? <div className="admin-movie-form__genre-options">{genres.map((genre) => <label key={genre.id}><input type="checkbox" name="genreIds" checked={form.genreIds.includes(genre.id)} onChange={() => toggleGenre(genre.id)} />{genre.name}</label>)}</div> : null}
        {fieldError('genreIds')}
      </fieldset>

      <label className="admin-movie-form__field" htmlFor="movie-primary-genre">Thể loại chính {!isEditing ? <span aria-hidden="true">*</span> : null}<select id="movie-primary-genre" name="primaryGenreId" value={form.primaryGenreId} onChange={changeField} disabled={genresLoading || saving || selectedGenres.length === 0} aria-invalid={Boolean(errors.primaryGenreId)} aria-describedby={describedBy('primaryGenreId')} required={!isEditing}><option value="">Chọn thể loại chính</option>{selectedGenres.map((genre) => <option key={genre.id} value={genre.id}>{genre.name}</option>)}</select>{fieldError('primaryGenreId')}</label>
      </div>}
      <footer className="admin-movie-form__actions">
        {reviewReady ? <><button type="button" disabled={saving} onClick={() => setReviewReady(false)}>Quay lại chỉnh sửa</button><button className="price-admin__primary" type="button" disabled={saving} onClick={save}>{saving ? 'Đang lưu…' : isEditing ? 'Xác nhận lưu phim' : 'Xác nhận tạo phim'}</button></> : <><button type="button" disabled={saving} onClick={leaveForm}>Hủy</button><button className="price-admin__primary" type="submit" disabled={saving || genresLoading || (!isEditing && genres.length === 0)}>{isEditing ? 'Kiểm tra thay đổi' : 'Kiểm tra trước khi tạo'}</button></>}
      </footer>
    </form>
    </div>
    {isEditing && movieData ? <>
      <div hidden={activeSection !== 'media'} className="admin-editor-panel" aria-label="Hình ảnh và trailer của phim">
      <AdminMovieMediaPanel movieId={movieId} posterUrl={movieData.posterUrl} trailerUrl={movieData.trailerUrl}
        active={activeSection === 'media'} onChange={(media) => setMovieData((current) => ({ ...current, ...media }))} />
      <AdminMovieBannerPanel movieId={movieId} backdropUrl={movieData.backdropUrl}
        onChange={(media) => setMovieData((current) => ({ ...current, ...media }))} />
      </div>
      <div hidden={activeSection !== 'schedule'} className="admin-editor-panel" aria-label="Lịch chiếu của phim">
      <AdminMovieScheduleEditor movieId={movieId} movieTitle={movieData.title} durationMinutes={movieData.durationMinutes}
        movieStatus={movieData.status} movieHasUnsavedScheduleSettings={form.durationMinutes !== initialForm.durationMinutes || form.status !== initialForm.status} />
      </div>
    </> : <p className="admin-movie-form__hint">Sau khi tạo phim, trang chỉnh sửa sẽ mở để bạn tải poster, banner, gắn trailer và tạo lịch chiếu.</p>}
  </section>
}
