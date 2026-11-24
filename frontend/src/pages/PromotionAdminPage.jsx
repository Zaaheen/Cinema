import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPromotions } from '../api/promotionAdmin'
import { getErrorMessage } from '../lib/errors'
import { promotionVisibilityState, selectPromotionPage } from '../lib/adminPromotionList'

const formatDate = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
  : '—'

function statusText(state) {
  return {
    inactive: 'Ngừng hoạt động',
    expired: 'Đã hết hạn',
    upcoming: 'Sắp diễn ra',
    effective: 'Đang diễn ra',
  }[state]
}

function PromotionCard({ promotion }) {
  const state = promotionVisibilityState(promotion)
  const discount = promotion.discountType === 'PERCENT'
    ? `${promotion.discountValue}%`
    : `${Number(promotion.discountValue).toLocaleString('vi-VN')} ₫`

  return <article className="admin-promotion__item">
    <div className="admin-promotion__item-top">
      <span className="admin-promotion__code">KM{String(promotion.id).padStart(4, '0')}</span>
      <span className={`admin-promotion__status admin-promotion__status--${state}`}>{statusText(state)}</span>
    </div>
    <h4>{promotion.title}</h4>
    <p className="admin-promotion__item-description">{promotion.description || 'Chưa có mô tả'}</p>
    <div className="admin-promotion__item-facts">
      <div><span>Giá trị ưu đãi</span><strong>{discount}</strong></div>
      <div><span>Loại</span><strong>{promotion.discountType === 'PERCENT' ? 'Theo phần trăm' : 'Theo số tiền'}</strong></div>
      <div><span>Thời gian</span><strong>{formatDate(promotion.startsAt)} – {formatDate(promotion.endsAt)}</strong></div>
    </div>
    <div className="admin-promotion__item-actions">
      <Link to={`/admin/promotions/${promotion.id}`}>Xem chi tiết</Link>
      <Link className="admin-promotion__item-edit" to={`/admin/promotions/${promotion.id}/edit`}>Chỉnh sửa</Link>
    </div>
  </article>
}

export function PromotionAdminPage() {
  const [request, setRequest] = useState({ loading: true, error: '', promotions: [] })
  const [reload, setReload] = useState(0)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [page, setPage] = useState(0)

  const retry = () => {
    setRequest((current) => ({ ...current, loading: true, error: '' }))
    setReload((current) => current + 1)
  }

  useEffect(() => {
    let active = true
    getPromotions()
      .then((promotions) => { if (active) setRequest({ loading: false, error: '', promotions: promotions || [] }) })
      .catch((error) => { if (active) setRequest({ loading: false, error: getErrorMessage(error), promotions: [] }) })
    return () => { active = false }
  }, [reload])

  const { loading, error, promotions } = request
  const pageSize = 6
  const { filtered, pageCount, currentPage, visiblePromotions } = selectPromotionPage(promotions, { query, status: statusFilter, page, pageSize })

  return <section className="admin-promotion" aria-labelledby="promotion-admin-title">
    <header className="admin-promotion__header">
      <div>
        <p className="eyebrow">Khuyến mãi</p>
        <h2 id="promotion-admin-title">Quản lý khuyến mãi</h2>
        <p>Theo dõi chương trình, thời gian áp dụng và giá trị ưu đãi.</p>
      </div>
      <Link className="admin-promotion__create" to="/admin/promotions/new">Thêm khuyến mãi</Link>
    </header>

    <div className="admin-promotion__card admin-promotion__list">
      <div className="admin-promotion__section-heading"><div><h3>Danh sách khuyến mãi</h3><p>{loading ? 'Đang tải dữ liệu…' : error ? 'Chưa tải được dữ liệu' : `${filtered.length} / ${promotions.length} chương trình`}</p></div></div>
      {!loading && !error && promotions.length > 0 && <search className="admin-promotion__filters" aria-label="Lọc khuyến mãi"><label htmlFor="promotion-search">Tìm theo tên hoặc mã<input id="promotion-search" type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Tên hoặc mã KM" /></label><label htmlFor="promotion-filter-status">Trạng thái<select id="promotion-filter-status" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0) }}><option value="all">Tất cả</option><option value="effective">Đang diễn ra</option><option value="upcoming">Sắp diễn ra</option><option value="expired">Đã hết hạn</option><option value="inactive">Ngừng hoạt động</option></select></label></search>}
      {loading ? <p className="admin-promotion__state" role="status">Đang tải danh sách khuyến mãi…</p> : null}
      {!loading && error ? <div className="admin-promotion__empty" role="alert"><h4>Không thể tải danh sách</h4><p>{error}</p><button type="button" onClick={retry}>Thử lại</button></div> : null}
      {!loading && !error && promotions.length === 0 ? <div className="admin-promotion__empty"><h4>Chưa có khuyến mãi</h4><p>Tạo chương trình đầu tiên để hiển thị tại đây.</p><Link to="/admin/promotions/new">Thêm khuyến mãi</Link></div> : null}
      {!loading && !error && promotions.length > 0 && filtered.length === 0 ? <p className="admin-promotion__state">Không có khuyến mãi phù hợp bộ lọc.</p> : null}
      {!loading && !error && filtered.length > 0 ? <><div className="admin-promotion__items">{visiblePromotions.map((promotion) => <PromotionCard key={promotion.id} promotion={promotion} />)}</div><nav className="admin-promotion__pagination" aria-label="Phân trang khuyến mãi"><p>Hiển thị {currentPage * pageSize + 1}–{Math.min((currentPage + 1) * pageSize, filtered.length)} / {filtered.length}</p><div><button type="button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Trang trước</button><span>Trang {currentPage + 1} / {pageCount}</span><button type="button" disabled={currentPage >= pageCount - 1} onClick={() => setPage(currentPage + 1)}>Trang sau</button></div></nav></> : null}
    </div>
  </section>
}
