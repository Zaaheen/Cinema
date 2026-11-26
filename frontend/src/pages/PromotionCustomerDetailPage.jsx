
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchPromotion } from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

export function PromotionCustomerDetailPage() {
    const { promotionId } = useParams()

    const [promotion, setPromotion] = useState(null)
    const [status, setStatus] = useState('loading')
    const [error, setError] = useState(null)

    useEffect(() => {
        let active = true

        async function loadPromotion() {
            try {
                setStatus('loading')
                setError(null)

                const result = await fetchPromotion(promotionId)

                if (!active) return

                setPromotion(result)
                setStatus('ready')
            } catch (loadError) {
                if (!active) return

                setError(getErrorMessage(loadError))
                setStatus('error')
            }
        }

        loadPromotion()

        return () => {
            active = false
        }
    }, [promotionId])

    function getPromotionType(type) {
        if (type === 'PERCENT') {
            return 'Giảm theo phần trăm'
        }

        if (type === 'FIXED') {
            return 'Giảm số tiền'
        }

        return type
    }

    function getPromotionStatus(item) {
        const now = new Date()

        if (item.status === 'INACTIVE') {
            return 'Chưa công bố / Ngừng hoạt động'
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
        if (!value) return 'Chưa cập nhật'

        return new Date(value).toLocaleString('vi-VN')
    }

    function formatDiscountValue(item) {
        if (item.discountType === 'PERCENT') {
            return `${item.discountValue}%`
        }

        return `${Number(item.discountValue).toLocaleString('vi-VN')} VNĐ`
    }

    if (status === 'loading') {
        return (
            <p className="page-status">
                Đang tải thông tin khuyến mãi...
            </p>
        )
    }

    if (status === 'error') {
        return (
            <section className="catalog-page">
                <h1>Không thể tải khuyến mãi</h1>
                <p className="form-error">{error}</p>

                <Link to="/promotions" className="button">
                    Quay lại danh sách
                </Link>
            </section>
        )
    }

    if (!promotion) {
        return (
            <section className="catalog-page">
                <h1>Không tìm thấy khuyến mãi</h1>

                <Link to="/promotions" className="button">
                    Quay lại danh sách
                </Link>
            </section>
        )
    }

    return (
        <section className="catalog-page">
            <p className="eyebrow">Khuyến mãi</p>

            <h1>{promotion.title}</h1>

            <div className="feature-card">
                <p>
                    <strong>Mã khuyến mãi:</strong>{' '}
                    KM{String(promotion.id).padStart(4, '0')}
                </p>

                <p>
                    <strong>Mô tả:</strong>{' '}
                    {promotion.description || 'Chưa có mô tả'}
                </p>

                <p>
                    <strong>Loại khuyến mãi:</strong>{' '}
                    {getPromotionType(promotion.discountType)}
                </p>

                <p>
                    <strong>Giá trị ưu đãi:</strong>{' '}
                    {formatDiscountValue(promotion)}
                </p>

                <p>
                    <strong>Ngày bắt đầu:</strong>{' '}
                    {formatDate(promotion.startsAt)}
                </p>

                <p>
                    <strong>Ngày kết thúc:</strong>{' '}
                    {formatDate(promotion.endsAt)}
                </p>

                <p>
                    <strong>Trạng thái:</strong>{' '}
                    {getPromotionStatus(promotion)}
                </p>

                <Link to="/promotions" className="button">
                    Quay lại danh sách khuyến mãi
                </Link>
            </div>
        </section>
    )
}
