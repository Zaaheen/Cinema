
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchPromotions } from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

export function PromotionListPage() {
    const [promotions, setPromotions] = useState([])
    const [status, setStatus] = useState('loading')
    const [error, setError] = useState(null)

    useEffect(() => {
        let active = true

        const loadPromotions = async () => {
            try {
                const result = await fetchPromotions()

                if (!active) return

                setPromotions(result)
                setStatus(result.length ? 'ready' : 'empty')
            } catch (loadError) {
                if (!active) return

                setError(getErrorMessage(loadError))
                setStatus('error')
            }
        }

        loadPromotions()

        return () => {
            active = false
        }
    }, [])

    const formatDate = (value) => {
        if (!value) return ''

        return new Date(value).toLocaleDateString('vi-VN')
    }

    const formatDiscountValue = (promotion) => {
        if (promotion.discountType === 'PERCENT') {
            return `${promotion.discountValue}%`
        }

        return `${Number(promotion.discountValue).toLocaleString('vi-VN')} VNĐ`
    }

    return (
        <section className="catalog-page">
            <p className="eyebrow">Khuyến mãi</p>

            <h1>Danh sách khuyến mãi</h1>

            {status === 'loading' && (
                <p className="page-status">Đang tải khuyến mãi...</p>
            )}

            {status === 'error' && (
                <p className="form-error">
                    Không thể tải khuyến mãi: {error}
                </p>
            )}

            {status === 'empty' && (
                <p className="page-status">
                    Hiện chưa có chương trình khuyến mãi.
                </p>
            )}

            {status === 'ready' && (
                <div className="movie-grid">
                    {promotions.map((promotion) => (
                        <article className="movie-card" key={promotion.id}>
                            <div className="movie-poster-placeholder">
                                Khuyến mãi
                            </div>

                            <div>
                <span className="badge">
                  {promotion.discountType === 'PERCENT'
                      ? 'Giảm theo phần trăm'
                      : 'Giảm số tiền'}
                </span>

                                <h2>{promotion.title}</h2>

                                <p>
                                    {promotion.description || 'Không có mô tả'}
                                </p>

                                <p>
                                    <strong>Ưu đãi: </strong>
                                    {formatDiscountValue(promotion)}
                                </p>

                                <p>
                                    <strong>Bắt đầu: </strong>
                                    {formatDate(promotion.startsAt)}
                                </p>

                                <p>
                                    <strong>Kết thúc: </strong>
                                    {formatDate(promotion.endsAt)}
                                </p>

                                <Link
                                    className="button button-secondary"
                                    to={`/promotions/${promotion.id}`}
                                >
                                    Xem chi tiết
                                </Link>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </section>
    )
}
