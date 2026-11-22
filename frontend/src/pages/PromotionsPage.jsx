import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchPromotions } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'

const formatDate = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value))
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

  return <div className="min-h-screen bg-surface pb-20 font-body-md text-body-md text-on-surface antialiased md:pb-0"><CinemaHeader /><main className="pt-20"><section className="relative overflow-hidden border-b border-surface-container-high bg-surface-container-lowest"><div aria-hidden="true" className="absolute -top-40 left-1/2 h-80 w-[52rem] -translate-x-1/2 rounded-full bg-primary-container/15 blur-[140px]" /><div className="relative mx-auto max-w-7xl px-margin-mobile py-14 lg:px-margin lg:py-20"><nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-2 font-label-md text-outline"><Link className="hover:text-primary" to="/">Trang chủ</Link><span aria-hidden="true">/</span><span className="text-on-surface">Ưu đãi</span></nav><p className="inline-flex rounded-full bg-surface-container px-3 py-1 font-label-sm uppercase tracking-wider text-primary">Ưu đãi tại Cinema</p><h1 className="mt-4 max-w-3xl font-display-lg text-display-lg-mobile font-extrabold tracking-tight md:text-display-lg">Khuyến mãi đang công bố</h1><p className="mt-4 max-w-2xl font-body-lg text-on-surface-variant">Các ưu đãi dưới đây được cập nhật trực tiếp từ hệ thống Cinema khi còn hiệu lực.</p></div></section><section className="mx-auto max-w-7xl px-margin-mobile py-10 lg:px-margin">{state.loading ? <LoadingState /> : null}{!state.loading && state.error ? <ErrorState onRetry={retry} /> : null}{!state.loading && !state.error && state.promotions.length === 0 ? <EmptyState /> : null}{!state.loading && !state.error && state.promotions.length > 0 ? <PromotionGrid promotions={state.promotions} /> : null}</section></main><CinemaFooter /></div>
}

function LoadingState() { return <section aria-live="polite" className="promotion-list__state"><progress aria-label="Đang tải khuyến mãi" className="promotion-list__spinner" /><p>Đang tải ưu đãi…</p></section> }
function ErrorState({ onRetry }) { return <section className="promotion-list__state promotion-list__state--error" role="alert"><h2>Không thể tải khuyến mãi</h2><p>Hãy kiểm tra kết nối rồi thử lại.</p><button type="button" onClick={onRetry}>Thử lại</button></section> }
function EmptyState() { return <section className="promotion-list__state"><h2>Chưa có ưu đãi đang công bố</h2><p>Khi có chương trình phù hợp, danh sách sẽ được cập nhật tại đây.</p></section> }
function PromotionGrid({ promotions }) { return <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{promotions.map((promotion) => <article key={promotion.id} className="overflow-hidden rounded-2xl border border-surface-container-high bg-surface-container shadow-lg"><div className="h-48 bg-surface-container-highest">{promotion.imageUrl ? <img className="h-full w-full object-cover" src={promotion.imageUrl} alt="" onError={(event) => { event.currentTarget.hidden = true }} /> : <div className="grid h-full place-items-center font-label-sm uppercase tracking-widest text-outline">Cinema ưu đãi</div>}</div><div className="flex min-h-64 flex-col p-5"><p className="font-label-sm uppercase tracking-wider text-secondary">{formatDiscount(promotion)}</p><h2 className="mt-3 font-headline-md font-bold text-on-surface">{promotion.title}</h2><p className="mt-3 line-clamp-3 flex-1 text-body-md text-on-surface-variant">{promotion.description || 'Thông tin chi tiết đang được cập nhật.'}</p><p className="mt-5 font-label-sm text-outline">Có hiệu lực đến {formatDate(promotion.endsAt)}</p><Link className="mt-5 inline-flex min-h-11 items-center justify-center rounded-xl bg-primary-container px-4 py-2.5 font-label-lg font-bold text-on-primary" to={`/promotions/${promotion.id}`}>Xem chi tiết</Link></div></article>)}</div> }
