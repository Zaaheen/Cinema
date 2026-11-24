
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
    getPromotionById,
    createPromotion,
    updatePromotion,
} from '../api/promotionAdmin'
import { getErrorMessage } from '../lib/errors'

const EMPTY_FORM = {
    title: '',
    description: '',
    discountType: 'PERCENT',
    discountValue: '',
    startsAt: '',
    endsAt: '',
    status: 'ACTIVE',
}

const FIELD_IDS = { title: 'promotion-title', discountValue: 'promotion-value', startsAt: 'promotion-start', endsAt: 'promotion-end' }

function formatDateTime(value) {
    if (!value) return ''

    const date = new Date(value)
    const localDate = new Date(
        date.getTime() - date.getTimezoneOffset() * 60000
    )

    return localDate.toISOString().slice(0, 16)
}

export function PromotionFormPage() {
    const { id } = useParams()
    const navigate = useNavigate()
    const isEditing = Boolean(id)

    const [form, setForm] = useState(EMPTY_FORM)
    const [loading, setLoading] = useState(isEditing)
    const [loadError, setLoadError] = useState('')
    const [fieldErrors, setFieldErrors] = useState({})
    const [submitError, setSubmitError] = useState('')
    const [loadAttempt, setLoadAttempt] = useState(0)
    const [initialForm, setInitialForm] = useState(EMPTY_FORM)
    const [reviewReady, setReviewReady] = useState(false)
    const reviewRef = useRef(null)
    const saveInFlight = useRef(false)
    const isDirty = JSON.stringify(form) !== JSON.stringify(initialForm)

    useEffect(() => {
        if (!isDirty || loading) return undefined
        const warn = (event) => { event.preventDefault(); event.returnValue = '' }
        window.addEventListener('beforeunload', warn)
        return () => window.removeEventListener('beforeunload', warn)
    }, [isDirty, loading])

    useEffect(() => {
        if (!isEditing) return undefined
        let active = true
        getPromotionById(id).then((promotion) => {
            if (!active) return
            if (!promotion) {
                toast.error('Không tìm thấy khuyến mãi!')
                navigate('/admin/promotions')
                return
            }
            const nextForm = {
                title: promotion.title || '',
                description: promotion.description || '',
                discountType: promotion.discountType || 'PERCENT',
                discountValue: promotion.discountValue ?? '',
                startsAt: formatDateTime(promotion.startsAt),
                endsAt: formatDateTime(promotion.endsAt),
                status: promotion.status || 'ACTIVE',
            }
            setForm(nextForm)
            setInitialForm(nextForm)
            setLoading(false)
        }).catch((error) => {
            if (active) {
                setLoadError(getErrorMessage(error))
                setLoading(false)
            }
        })
        return () => { active = false }
    }, [id, isEditing, navigate, loadAttempt])

    function retryLoad() {
        setLoadError('')
        setLoading(true)
        setLoadAttempt((attempt) => attempt + 1)
    }

    function handleChange(event) {
        const { name, value } = event.target

        setForm((current) => ({ ...current, [name]: value }))
        setFieldErrors((current) => ({
            ...current,
            [name]: '',
            ...(name === 'discountType' ? { discountValue: '' } : {}),
            ...(name === 'startsAt' ? { endsAt: '' } : {}),
        }))
        setSubmitError('')
        setReviewReady(false)
    }

    function handleSubmit(event) {
        event.preventDefault()

        const errors = {}
        if (!form.title.trim()) errors.title = 'Nhập tên khuyến mãi.'
        if (!form.discountValue || !Number.isFinite(Number(form.discountValue)) || Number(form.discountValue) <= 0) {
            errors.discountValue = 'Giá trị phải lớn hơn 0.'
        } else if (form.discountType === 'PERCENT' && Number(form.discountValue) > 100) {
            errors.discountValue = 'Mức giảm phần trăm tối đa là 100%.'
        }
        if (!form.startsAt) errors.startsAt = 'Chọn thời điểm bắt đầu.'
        else if (!Number.isFinite(Date.parse(form.startsAt))) errors.startsAt = 'Thời điểm bắt đầu không hợp lệ.'
        if (!form.endsAt) errors.endsAt = 'Chọn thời điểm kết thúc.'
        else if (!Number.isFinite(Date.parse(form.endsAt))) errors.endsAt = 'Thời điểm kết thúc không hợp lệ.'
        if (!errors.startsAt && !errors.endsAt && new Date(form.endsAt) <= new Date(form.startsAt)) {
            errors.endsAt = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.'
        }
        setFieldErrors(errors)
        if (Object.keys(errors).length) {
            document.getElementById(FIELD_IDS[Object.keys(errors)[0]])?.focus()
            return
        }

        setReviewReady(true)
        requestAnimationFrame(() => reviewRef.current?.focus())
    }

    async function savePromotion() {
        if (!reviewReady || saveInFlight.current) return

        const payload = {
            ...form,
            discountValue: Number(form.discountValue),
            startsAt: new Date(form.startsAt).toISOString(),
            endsAt: new Date(form.endsAt).toISOString(),
        }

        try {
            saveInFlight.current = true
            setLoading(true)

            if (isEditing) {
                await updatePromotion(id, payload)
                toast.success('Cập nhật khuyến mãi thành công!')
            } else {
                await createPromotion(payload)
                toast.success('Thêm khuyến mãi thành công!')
            }

            navigate('/admin/promotions')
        } catch (error) {
            setSubmitError(error.response?.data?.message || 'Lưu khuyến mãi thất bại. Vui lòng thử lại.')
        } finally {
            saveInFlight.current = false
            setLoading(false)
        }
    }

    function leaveForm() {
        if (!isDirty || window.confirm('Bạn có thay đổi chưa lưu. Rời khỏi trang?')) navigate('/admin/promotions')
    }

    if (loading && isEditing && !form.title) {
        return <p className="admin-promotion__state" role="status">Đang tải thông tin khuyến mãi…</p>
    }

    if (loadError) return <section className="admin-promotion admin-promotion__empty" role="alert">
        <h2>Không thể tải khuyến mãi</h2><p>{loadError}</p>
        <button type="button" onClick={retryLoad}>Thử lại</button>
    </section>

    return (
        <section className="admin-promotion" aria-labelledby="promotion-form-title">
            <header className="admin-promotion__header">
                <div>
                    <p className="eyebrow">Khuyến mãi</p>
                    <h2 id="promotion-form-title">{isEditing ? 'Chỉnh sửa khuyến mãi' : 'Thêm khuyến mãi'}</h2>
                    <p>Nhập thông tin, kiểm tra mức giảm và thời gian trước khi công bố.</p>
                </div>
            </header>

            <form className="admin-promotion__card admin-promotion__form" onSubmit={handleSubmit} noValidate>
                {reviewReady ? <section className="admin-editor-review" tabIndex={-1} ref={reviewRef} aria-labelledby="promotion-review-title">
                    <p className="admin-editor-review__eyebrow">Kiểm tra trước khi lưu</p><h3 id="promotion-review-title">{isEditing ? 'Xác nhận cập nhật ưu đãi' : 'Xác nhận tạo ưu đãi'}</h3>
                    <p>Đối chiếu mức giảm và khoảng thời gian. Ưu đãi hoạt động có thể hiển thị công khai khi đến thời gian áp dụng.</p>
                    <dl>
                        <div><dt>Tên chương trình</dt><dd>{form.title.trim()}</dd></div>
                        <div><dt>Mô tả</dt><dd>{form.description.trim() || 'Chưa nhập'}</dd></div>
                        <div><dt>Giá trị ưu đãi</dt><dd>{form.discountType === 'PERCENT' ? `${form.discountValue}%` : `${Number(form.discountValue).toLocaleString('vi-VN')} ₫`}</dd></div>
                        <div><dt>Bắt đầu</dt><dd>{form.startsAt.replace('T', ' ')}</dd></div>
                        <div><dt>Kết thúc</dt><dd>{form.endsAt.replace('T', ' ')}</dd></div>
                        <div><dt>Trạng thái</dt><dd>{form.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng hoạt động'}</dd></div>
                    </dl>
                    {form.status === 'ACTIVE' && <p className="admin-editor-review__warning">Trạng thái hoạt động sẽ cho phép ưu đãi xuất hiện công khai trong khoảng thời gian đã chọn.</p>}
                </section> : <div className="admin-promotion__form-fields">
                <div className="admin-promotion__form-intro">
                    <div><h3>Thông tin chương trình</h3><p>Tên và mô tả sẽ giúp nhận biết ưu đãi trong danh sách.</p></div>
                </div>
                <div className="admin-promotion__grid">
                    <label className="admin-promotion__field admin-promotion__field--wide" htmlFor="promotion-title">
                        Tên khuyến mãi
                        <input id="promotion-title" name="title" value={form.title} onChange={handleChange} aria-invalid={Boolean(fieldErrors.title)} aria-describedby={fieldErrors.title ? 'promotion-title-error' : undefined} required />
                        {fieldErrors.title ? <span className="admin-promotion__field-error" id="promotion-title-error">{fieldErrors.title}</span> : null}
                    </label>
                    <label className="admin-promotion__field admin-promotion__field--wide" htmlFor="promotion-description">
                        Mô tả
                        <textarea id="promotion-description" name="description" value={form.description} onChange={handleChange} />
                    </label>
                </div>
                <div className="admin-promotion__form-intro">
                    <div><h3>Giá trị ưu đãi</h3><p>Chọn loại giảm giá rồi nhập giá trị tương ứng.</p></div>
                </div>
                <div className="admin-promotion__grid">
                    <label className="admin-promotion__field" htmlFor="promotion-type">
                        Loại khuyến mãi
                        <select id="promotion-type" name="discountType" value={form.discountType} onChange={handleChange}>
                            <option value="PERCENT">Giảm theo phần trăm</option>
                            <option value="FIXED">Giảm số tiền</option>
                        </select>
                    </label>
                    <label className="admin-promotion__field" htmlFor="promotion-value">
                        Giá trị ưu đãi ({form.discountType === 'PERCENT' ? '%' : '₫'})
                        <input id="promotion-value" type="number" name="discountValue" value={form.discountValue} onChange={handleChange} min="0.01" max={form.discountType === 'PERCENT' ? 100 : undefined} step="any" inputMode="decimal" aria-invalid={Boolean(fieldErrors.discountValue)} aria-describedby={fieldErrors.discountValue ? 'promotion-value-error' : 'promotion-value-hint'} required />
                        {fieldErrors.discountValue ? <span className="admin-promotion__field-error" id="promotion-value-error">{fieldErrors.discountValue}</span> : <span className="admin-promotion__hint" id="promotion-value-hint">{form.discountType === 'PERCENT' ? 'Nhập từ trên 0 đến 100.' : 'Nhập số tiền giảm bằng đồng Việt Nam.'}</span>}
                    </label>
                </div>
                <div className="admin-promotion__form-intro">
                    <div><h3>Thời gian và trạng thái</h3><p>Thời điểm kết thúc phải sau thời điểm bắt đầu.</p></div>
                </div>
                <div className="admin-promotion__grid">
                    <label className="admin-promotion__field" htmlFor="promotion-start">
                        Ngày bắt đầu
                        <input id="promotion-start" type="datetime-local" name="startsAt" value={form.startsAt} onChange={handleChange} aria-invalid={Boolean(fieldErrors.startsAt)} aria-describedby={fieldErrors.startsAt ? 'promotion-start-error' : undefined} required />
                        {fieldErrors.startsAt ? <span className="admin-promotion__field-error" id="promotion-start-error">{fieldErrors.startsAt}</span> : null}
                    </label>
                    <label className="admin-promotion__field" htmlFor="promotion-end">
                        Ngày kết thúc
                        <input id="promotion-end" type="datetime-local" name="endsAt" value={form.endsAt} onChange={handleChange} aria-invalid={Boolean(fieldErrors.endsAt)} aria-describedby={fieldErrors.endsAt ? 'promotion-end-error' : undefined} required />
                        {fieldErrors.endsAt ? <span className="admin-promotion__field-error" id="promotion-end-error">{fieldErrors.endsAt}</span> : null}
                    </label>
                    <label className="admin-promotion__field" htmlFor="promotion-status">
                        Trạng thái
                        <select id="promotion-status" name="status" value={form.status} onChange={handleChange}>
                            <option value="ACTIVE">Hoạt động</option>
                            <option value="INACTIVE">Ngừng hoạt động</option>
                        </select>
                    </label>
                </div>
                </div>}
                {submitError ? <p className="admin-promotion__submit-error" role="alert">{submitError}</p> : null}
                <div className="admin-promotion__actions">
                    {reviewReady ? <><button type="button" disabled={loading} onClick={() => setReviewReady(false)}>Quay lại chỉnh sửa</button><button type="button" className="price-admin__primary" disabled={loading} onClick={savePromotion}>{loading ? 'Đang lưu…' : isEditing ? 'Xác nhận cập nhật' : 'Xác nhận tạo ưu đãi'}</button></> : <><button type="button" onClick={leaveForm}>Hủy</button><button type="submit" className="price-admin__primary" disabled={loading}>Kiểm tra trước khi lưu</button></>}
                </div>
            </form>
        </section>
    )
}
