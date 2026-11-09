
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
    getPromotions,
    createPromotion,
    updatePromotion,
} from '../api/promotionAdmin'

const EMPTY_FORM = {
    title: '',
    description: '',
    discountType: 'PERCENT',
    discountValue: '',
    startsAt: '',
    endsAt: '',
    status: 'ACTIVE',
}

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
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (isEditing) {
            loadPromotion()
        }
    }, [id])

    async function loadPromotion() {
        try {
            setLoading(true)

            const promotions = await getPromotions()
            const promotion = promotions.find(
                (item) => String(item.id) === String(id)
            )

            if (!promotion) {
                toast.error('Không tìm thấy khuyến mãi!')
                navigate('/admin/promotions')
                return
            }

            setForm({
                title: promotion.title || '',
                description: promotion.description || '',
                discountType: promotion.discountType || 'PERCENT',
                discountValue: promotion.discountValue ?? '',
                startsAt: formatDateTime(promotion.startsAt),
                endsAt: formatDateTime(promotion.endsAt),
                status: promotion.status || 'ACTIVE',
            })
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Không tải được thông tin khuyến mãi!'
            )
        } finally {
            setLoading(false)
        }
    }

    function handleChange(event) {
        const { name, value } = event.target

        setForm({
            ...form,
            [name]: value,
        })
    }

    async function handleSubmit(event) {
        event.preventDefault()

        if (new Date(form.endsAt) <= new Date(form.startsAt)) {
            toast.error('Ngày kết thúc phải sau ngày bắt đầu!')
            return
        }

        const payload = {
            ...form,
            discountValue: Number(form.discountValue),
            startsAt: new Date(form.startsAt).toISOString(),
            endsAt: new Date(form.endsAt).toISOString(),
        }

        try {
            setLoading(true)

            if (isEditing) {
                await updatePromotion(id, payload)

                navigate('/admin/promotions', {
                    state: {
                        message: 'Cập nhật khuyến mãi thành công!',
                    },
                })
            } else {
                await createPromotion(payload)

                navigate('/admin/promotions', {
                    state: {
                        message: 'Thêm khuyến mãi thành công!',
                    },
                })
            }
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Lưu khuyến mãi thất bại!'
            )
        } finally {
            setLoading(false)
        }
    }

    if (loading && isEditing && !form.title) {
        return <p className="page-status">Đang tải thông tin...</p>
    }

    return (
        <section className="role-dashboard">
            <p className="eyebrow">QUẢN TRỊ</p>

            <h1>
                {isEditing ? 'Sửa khuyến mãi' : 'Thêm khuyến mãi'}
            </h1>

            <form className="feature-card" onSubmit={handleSubmit}>
                <p>
                    <label>Tên khuyến mãi</label>
                    <input
                        name="title"
                        value={form.title}
                        onChange={handleChange}
                        required
                    />
                </p>

                <p>
                    <label>Mô tả</label>
                    <textarea
                        name="description"
                        value={form.description}
                        onChange={handleChange}
                    />
                </p>

                <p>
                    <label>Loại giảm giá</label>
                    <select
                        name="discountType"
                        value={form.discountType}
                        onChange={handleChange}
                    >
                        <option value="PERCENT">Phần trăm</option>
                        <option value="FIXED">Số tiền</option>
                    </select>
                </p>

                <p>
                    <label>Giá trị giảm</label>
                    <input
                        type="number"
                        name="discountValue"
                        value={form.discountValue}
                        onChange={handleChange}
                        min="0.01"
                        step="any"
                        required
                    />
                </p>

                <p>
                    <label>Ngày bắt đầu</label>
                    <input
                        type="datetime-local"
                        name="startsAt"
                        value={form.startsAt}
                        onChange={handleChange}
                        required
                    />
                </p>

                <p>
                    <label>Ngày kết thúc</label>
                    <input
                        type="datetime-local"
                        name="endsAt"
                        value={form.endsAt}
                        onChange={handleChange}
                        required
                    />
                </p>

                <p>
                    <label>Trạng thái</label>
                    <select
                        name="status"
                        value={form.status}
                        onChange={handleChange}
                    >
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="INACTIVE">INACTIVE</option>
                    </select>
                </p>

                <button type="submit" disabled={loading}>
                    {loading
                        ? 'Đang lưu...'
                        : isEditing
                            ? 'Cập nhật'
                            : 'Thêm khuyến mãi'}
                </button>

                <button
                    type="button"
                    onClick={() => navigate('/admin/promotions')}
                >
                    Quay lại
                </button>
            </form>
        </section>
    )
}
