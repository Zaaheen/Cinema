import { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { fetchPromotion } from '../api/catalog'

const formatDate = (value) => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'

export function PromotionDetailPage() {
  const { id: adminPromotionId, promotionId } = useParams(); const navigate = useNavigate(); const location = useLocation(); const isAdmin = location.pathname.startsWith('/admin/')
  const resolvedPromotionId = isAdmin ? adminPromotionId : promotionId
  const [promotion, setPromotion] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  const backPath = isAdmin ? '/admin/promotions' : '/promotions'
  const loadPromotion = useCallback(async () => { if (!resolvedPromotionId) { setError('Không tìm thấy mã khuyến mãi.'); setLoading(false); return } try { const result = isAdmin ? (await apiClient.get(`/admin/promotions/${resolvedPromotionId}`)).data.data : await fetchPromotion(resolvedPromotionId); setPromotion(result) } catch (requestError) { const message = requestError.response?.data?.message || 'Không tải được thông tin khuyến mãi.'; setError(message) } finally { setLoading(false) } }, [isAdmin, resolvedPromotionId])
  useEffect(() => { void loadPromotion() }, [loadPromotion])
  if (loading) return <p className="page-status">Đang tải thông tin…</p>
  if (!promotion) return <section className={isAdmin ? 'admin-promotion' : 'role-dashboard'}><h2>Không tìm thấy khuyến mãi</h2>{error ? <p className="price-admin__error" role="alert">{error}</p> : null}<button type="button" onClick={() => navigate(backPath)}>Quay lại danh sách</button></section>
  const type = promotion.discountType === 'PERCENT' ? 'Giảm theo phần trăm' : 'Giảm số tiền'; const discount = promotion.discountType === 'PERCENT' ? `${promotion.discountValue}%` : `${Number(promotion.discountValue).toLocaleString('vi-VN')} VND`
  return <section className={isAdmin ? 'admin-promotion' : 'role-dashboard'} aria-labelledby="promotion-detail-title"><header><p className="eyebrow">{isAdmin ? 'KHUYẾN MÃI' : 'ƯU ĐÃI'}</p><h2 id="promotion-detail-title">Chi tiết khuyến mãi</h2></header><article className={isAdmin ? 'admin-promotion__card admin-promotion__detail' : 'feature-card'}><dl><div><dt>Mã khuyến mãi</dt><dd>KM{String(promotion.id).padStart(4, '0')}</dd></div><div><dt>Tên</dt><dd>{promotion.title}</dd></div><div><dt>Mô tả</dt><dd>{promotion.description || 'Không có mô tả'}</dd></div><div><dt>Loại</dt><dd>{type}</dd></div><div><dt>Giá trị</dt><dd>{discount}</dd></div><div><dt>Bắt đầu</dt><dd>{formatDate(promotion.startsAt)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(promotion.endsAt)}</dd></div><div><dt>Trạng thái</dt><dd>{promotion.status || 'Đang áp dụng'}</dd></div></dl><footer className={isAdmin ? 'admin-promotion__actions' : ''}>{isAdmin ? <button type="button" onClick={() => navigate(`/admin/promotions/${resolvedPromotionId}/edit`)}>Chỉnh sửa</button> : null}<button type="button" onClick={() => navigate(backPath)}>Quay lại</button></footer></article></section>
}
