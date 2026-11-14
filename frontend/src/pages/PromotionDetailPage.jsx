
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getPromotionById } from '../api/promotionAdmin'

export function PromotionDetailPage() {
    const { id } = useParams()
    const navigate = useNavigate()

    const [promotion, setPromotion] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        loadPromotion()
    }, [id])

    async function loadPromotion() {
        try {
            setLoading(true)
            const result = await getPromotionById(id)
            setPromotion(result)
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Không tải được thông tin khuyến mãi!'
            )
        } finally {
            setLoading(false)
        }
    }

    function getPromotionType(type) {
        if (type === 'PERCENT') return 'Giảm theo phần trăm'
        if (type === 'FIXED') return 'Giảm số tiền'
        return type
    }

    function getPromotionStatus(item) {
        const now = new Date()

        if (item.status === 'INACTIVE') {
            return 'Ngừng hoạt động'
        }

        if (
            item.status === 'EXPIRED' ||
            now > new Date(item.endsAt)
        ) {
            return 'Đã hết hạn'
        }

        if (now < new Date(item.startsAt)) {
            return 'Sắp diễn ra'
        }

        return 'Đang diễn ra'
    }

    function formatDate(value) {
        if (!value) return ''

        return new Date(value).toLocaleString('vi-VN')
    }

    function formatDiscountValue(item) {
        if (item.discountType === 'PERCENT') {
            return `${item.discountValue}%`
        }

        return `${Number(item.discountValue).toLocaleString('vi-VN')} VNĐ`
    }

    if (loading) {
        return <p className="page-status">Đang tải thông tin...</p>
    }

    if (!promotion) {
        return (
            <section className="role-dashboard">
                <h1>Không tìm thấy khuyến mãi</h1>
                <button
                    type="button"
                    onClick={() => navigate('/admin/promotions')}
                >
                    Quay lại danh sách
                </button>
            </section>
        )
    }

    return (
        <section className="role-dashboard">
            <p className="eyebrow">QUẢN TRỊ</p>

            <h1>Chi tiết khuyến mãi</h1>

            <div className="feature-card">
                <p><strong>Mã khuyến mãi:</strong> KM{String(promotion.id).padStart(4, '0')}</p>

                <p><strong>Tên khuyến mãi:</strong> {promotion.title}</p>

                <p><strong>Mô tả:</strong> {promotion.description || 'Không có mô tả'}</p>

                <p><strong>Loại khuyến mãi:</strong> {getPromotionType(promotion.discountType)}</p>

                <p><strong>Giá trị ưu đãi:</strong> {formatDiscountValue(promotion)}</p>

                <p><strong>Ngày bắt đầu:</strong> {formatDate(promotion.startsAt)}</p>

                <p><strong>Ngày kết thúc:</strong> {formatDate(promotion.endsAt)}</p>

                <p><strong>Trạng thái:</strong> {getPromotionStatus(promotion)}</p>

                <button
                    type="button"
                    onClick={() => navigate(`/admin/promotions/${id}/edit`)}
                >
                    Chỉnh sửa
                </button>

                <button
                    type="button"
                    onClick={() => navigate('/admin/promotions')}
                >
                    Quay lại
                </button>
            </div>
        </section>
    )
}
