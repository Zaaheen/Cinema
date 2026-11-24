import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { fetchPromotion } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import { getErrorMessage } from '../lib/errors'

const dateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
})
const formatDate = (value) => value ? dateTime.format(new Date(value)) : 'Đang cập nhật'
const discountText = (promotion) => promotion.discountType === 'PERCENT'
  ? `${promotion.discountValue}%`
  : `${Number(promotion.discountValue).toLocaleString('vi-VN')}₫`

export function PromotionDetailPage() {
  const { id: adminId, promotionId } = useParams()
  const { pathname } = useLocation()
  const isAdmin = pathname.startsWith('/admin/')
  const id = isAdmin ? adminId : promotionId
  const [request, setRequest] = useState({ key: '', promotion: null, error: '' })
  const [retry, setRetry] = useState(0)
  const key = `${isAdmin ? 'admin' : 'public'}:${id}:${retry}`

  useEffect(() => {
    let active = true
    const load = isAdmin
      ? apiClient.get(`/admin/promotions/${id}`).then(({ data }) => data.data)
      : fetchPromotion(id)
    load.then((promotion) => {
      if (active) setRequest({ key, promotion, error: '' })
    }).catch((error) => {
      if (active) setRequest({ key, promotion: null, error: getErrorMessage(error) })
    })
    return () => { active = false }
  }, [id, isAdmin, key])

  const loading = request.key !== key
  const promotion = loading ? null : request.promotion
  const error = loading ? '' : request.error

  if (isAdmin) return <AdminPromotionDetail promotion={promotion} loading={loading} error={error} onRetry={() => setRetry((value) => value + 1)} />

  return <div className="promotion-detail-page min-h-screen bg-surface text-on-surface">
    <CinemaHeader />
    <main className="pb-20 pt-20 md:pb-0">
      <div className="mx-auto max-w-7xl px-margin-mobile pb-14 pt-8 lg:px-margin lg:pt-12">
        <nav aria-label="Breadcrumb" className="mb-7 flex flex-wrap items-center gap-2 font-label-md text-outline">
          <Link to="/" className="hover:text-primary">Trang chủ</Link><span aria-hidden="true">/</span>
          <Link to="/promotions" className="hover:text-primary">Ưu đãi</Link><span aria-hidden="true">/</span>
          <span className="text-on-surface">Chi tiết</span>
        </nav>
        {loading ? <div className="promotion-detail__state" role="status">Đang tải ưu đãi…</div> : error || !promotion ? <div className="promotion-detail__state" role="alert"><h1>Không thể hiển thị ưu đãi</h1><p>{error || 'Không tìm thấy ưu đãi này.'}</p><div className="promotion-detail__state-actions"><button type="button" onClick={() => setRetry((value) => value + 1)}>Thử lại</button><Link to="/promotions">Xem ưu đãi khác</Link></div></div> : <PublicPromotion key={promotion.id} promotion={promotion} />}
      </div>
    </main>
    <CinemaFooter />
  </div>
}

function PublicPromotion({ promotion }) {
  const [imageFailed, setImageFailed] = useState(false)
  const hasImage = promotion.imageUrl && !imageFailed
  return <article className={`promotion-detail${hasImage ? '' : ' promotion-detail--text-only'}`}>
    {hasImage && <div className="promotion-detail__visual"><img src={promotion.imageUrl} alt="" onError={() => setImageFailed(true)} /></div>}
    <div className="promotion-detail__content">
      <p className="promotion-detail__eyebrow">Ưu đãi tại Cinema</p>
      <h1>{promotion.title}</h1>
      <div className="promotion-detail__discount"><span>Giá trị ưu đãi</span><strong>{discountText(promotion)}</strong><small>{promotion.discountType === 'PERCENT' ? 'Giảm theo phần trăm' : 'Giảm theo số tiền'}</small></div>
      <section className="promotion-detail__section" aria-labelledby="promotion-description"><h2 id="promotion-description">Thông tin ưu đãi</h2><p>{promotion.description || 'Thông tin chi tiết đang được cập nhật.'}</p></section>
      <section className="promotion-detail__section" aria-labelledby="promotion-time"><h2 id="promotion-time">Thời gian áp dụng</h2><dl className="promotion-detail__dates"><div><dt>Bắt đầu</dt><dd>{formatDate(promotion.startsAt)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(promotion.endsAt)}</dd></div></dl></section>
      <p className="promotion-detail__note">Điều kiện áp dụng bổ sung sẽ được hiển thị khi có thông tin từ Cinema.</p>
      <div className="promotion-detail__actions"><Link className="promotion-detail__primary" to="/movies">Khám phá phim <span aria-hidden="true">→</span></Link><Link className="promotion-detail__back" to="/promotions">Xem ưu đãi khác</Link></div>
    </div>
  </article>
}

function AdminPromotionDetail({ promotion, loading, error, onRetry }) {
  if (loading) return <p className="admin-promotion__state" role="status">Đang tải thông tin khuyến mãi…</p>
  if (!promotion) return <section className="admin-promotion"><h2>Không tìm thấy khuyến mãi</h2><p role="alert">{error}</p><div className="admin-promotion__actions"><button type="button" onClick={onRetry}>Thử lại</button><Link to="/admin/promotions">Quay lại danh sách</Link></div></section>
  return <section className="admin-promotion" aria-labelledby="promotion-detail-title"><header><p className="eyebrow">Khuyến mãi</p><h2 id="promotion-detail-title">Chi tiết khuyến mãi</h2></header><article className="admin-promotion__card admin-promotion__detail"><dl><div><dt>Mã khuyến mãi</dt><dd>KM{String(promotion.id).padStart(4, '0')}</dd></div><div><dt>Tên</dt><dd>{promotion.title}</dd></div><div><dt>Mô tả</dt><dd>{promotion.description || 'Không có mô tả'}</dd></div><div><dt>Loại</dt><dd>{promotion.discountType === 'PERCENT' ? 'Giảm theo phần trăm' : 'Giảm số tiền'}</dd></div><div><dt>Giá trị</dt><dd>{discountText(promotion)}</dd></div><div><dt>Bắt đầu</dt><dd>{formatDate(promotion.startsAt)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(promotion.endsAt)}</dd></div><div><dt>Trạng thái</dt><dd>{promotion.status === 'ACTIVE' ? 'Hoạt động' : promotion.status === 'INACTIVE' ? 'Ngừng hoạt động' : promotion.status}</dd></div></dl><footer className="admin-promotion__actions"><Link to={`/admin/promotions/${promotion.id}/edit`}>Chỉnh sửa</Link><Link to="/admin/promotions">Quay lại</Link></footer></article></section>
}
