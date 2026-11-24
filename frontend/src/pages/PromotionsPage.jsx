import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchPromotions } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'

const formatDate = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
  : 'Đang cập nhật'

const formatDiscount = (promotion) => {
  if (promotion.discountType === 'PERCENT') return `Giảm ${promotion.discountValue}%`
  if (promotion.discountValue != null) return `Giảm ${Number(promotion.discountValue).toLocaleString('vi-VN')} VND`
  return 'Ưu đãi đang áp dụng'
}

export default function PromotionsPage() {
  const [reloadKey, setReloadKey] = useState(0)
  const [state, setState] = useState({ loading: true, error: '', promotions: [] })

  useEffect(() => {
    const controller = new AbortController()
    fetchPromotions({ signal: controller.signal })
      .then((promotions) => setState({ loading: false, error: '', promotions }))
      .catch((error) => {
        if (error.code === 'ERR_CANCELED' || error.name === 'CanceledError') return
        setState({ loading: false, error: error.message || 'Không thể tải danh sách khuyến mãi.', promotions: [] })
      })
    return () => controller.abort()
  }, [reloadKey])

  const retry = () => {
    setState((current) => ({ ...current, loading: true, error: '' }))
    setReloadKey((value) => value + 1)
  }

  return <div className="cinema-public min-h-screen bg-surface pb-20 text-on-surface md:pb-0"><CinemaHeader /><main className="pt-20"><section className="cinema-promotion-heading"><div className="cinema-page-inner"><nav aria-label="Breadcrumb" className="cinema-breadcrumb"><Link to="/">Trang chủ</Link><span aria-hidden="true">/</span><span>Ưu đãi</span></nav><p className="cinema-kicker">Ưu đãi tại Cinema</p><h1>Thêm niềm vui cho buổi xem</h1><p>Khám phá các chương trình đang áp dụng và xem thời hạn của từng ưu đãi.</p></div></section><section className="cinema-page-inner cinema-promotion-list">{state.loading ? <LoadingState /> : null}{!state.loading && state.error ? <ErrorState onRetry={retry} /> : null}{!state.loading && !state.error && state.promotions.length === 0 ? <EmptyState /> : null}{!state.loading && !state.error && state.promotions.length > 0 ? <PromotionGrid promotions={state.promotions} /> : null}</section></main><CinemaFooter /></div>
}

function LoadingState() { return <section aria-live="polite" className="promotion-list__state"><progress aria-label="Đang tải khuyến mãi" className="promotion-list__spinner" /><p>Đang tải ưu đãi…</p></section> }
function ErrorState({ onRetry }) { return <section className="promotion-list__state promotion-list__state--error" role="alert"><h2>Không thể tải khuyến mãi</h2><p>Hãy kiểm tra kết nối rồi thử lại.</p><button type="button" onClick={onRetry}>Thử lại</button></section> }
function EmptyState() { return <section className="promotion-list__state"><h2>Chưa có ưu đãi đang công bố</h2><p>Khi có chương trình phù hợp, danh sách sẽ được cập nhật tại đây.</p></section> }
function PromotionGrid({ promotions }) {
  return <div className={`promotion-list__grid${promotions.length === 1 ? ' promotion-list__grid--single' : ''}`}>
    {promotions.map((promotion) => <PromotionCard key={promotion.id} promotion={promotion} />)}
  </div>
}

function PromotionCard({ promotion }) {
  const [imageFailed, setImageFailed] = useState(false)
  const hasImage = promotion.imageUrl && !imageFailed
  return <article className="promotion-list__card">
    <div className="promotion-list__visual">
      {hasImage
        ? <img src={promotion.imageUrl} alt="" loading="lazy" onError={() => setImageFailed(true)} />
        : <div className="promotion-list__visual-fallback"><span className="promotion-list__visual-label">Ưu đãi Cinema</span><strong>{formatDiscount(promotion)}</strong></div>}
    </div>
    <div className="promotion-list__content">
      {hasImage && <p className="promotion-list__discount">{formatDiscount(promotion)}</p>}
      <h2>{promotion.title}</h2>
      <p className="promotion-list__description">{promotion.description || 'Thông tin chi tiết đang được cập nhật.'}</p>
      <p className="promotion-list__expiry">Có hiệu lực đến {formatDate(promotion.endsAt)}</p>
      <Link to={`/promotions/${promotion.id}`}>Xem chi tiết <span aria-hidden="true">→</span></Link>
    </div>
  </article>
}
