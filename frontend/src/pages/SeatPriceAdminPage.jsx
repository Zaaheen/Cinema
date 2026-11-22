import { useEffect, useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { createSeatPriceDraft, getSeatPrices, updateSeatPrice } from '../api/seatPriceAdmin'

const EMPTY_RULE = { seatType: 'STANDARD', movieVersion: '', amount: '', validFrom: '', validTo: '' }
const EMPTY_FILTERS = { status: '', seatType: '', movieVersion: '', effectiveOn: '' }
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const apiMessage = (error, fallback) => error.response?.data?.message || fallback
const toForm = (rule) => rule ? { seatType: rule.seatType, movieVersion: rule.movieVersion || '', amount: rule.amount, validFrom: rule.validFrom, validTo: rule.validTo || '' } : EMPTY_RULE
const formatAuditTime = (value) => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : 'Chưa có thời điểm'

export function SeatPriceAdminPage() {
  const dialogRef = useRef(null)
  const deactivateDialogRef = useRef(null)
  const submitInFlight = useRef(false)
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [form, setForm] = useState(EMPTY_RULE)
  const [editing, setEditing] = useState(null)
  const [ruleToDeactivate, setRuleToDeactivate] = useState(null)
  const [formErrors, setFormErrors] = useState({})
  const [requestError, setRequestError] = useState('')

  async function load(nextFilters = filters, showLoading = true) {
    try {
      if (showLoading) setLoading(true)
      setRequestError('')
      const params = Object.fromEntries(Object.entries(nextFilters).filter(([, value]) => value))
      const page = await getSeatPrices(params)
      setRules(page.content || [])
    } catch (error) {
      const message = apiMessage(error, 'Không tải được bảng giá.')
      setRequestError(message); toast.error(message)
    } finally { setLoading(false) }
  }

  useEffect(() => { void load(EMPTY_FILTERS, false) }, [])

  function openForm(rule = null) {
    setEditing(rule); setForm(toForm(rule)); setFormErrors({}); setRequestError('')
    dialogRef.current?.showModal()
  }

  function validateForm() {
    const errors = {}; const amount = Number(form.amount)
    if (!Number.isFinite(amount) || amount <= 0) errors.amount = 'Giá vé phải lớn hơn 0 VND.'
    if (!form.validFrom) errors.validFrom = 'Chọn ngày bắt đầu hiệu lực.'
    if (form.validTo && form.validFrom && form.validTo < form.validFrom) errors.validTo = 'Ngày kết thúc không được trước ngày bắt đầu.'
    return errors
  }

  async function submit(event) {
    event.preventDefault()
    if (submitInFlight.current) return
    const errors = validateForm(); setFormErrors(errors)
    if (Object.keys(errors).length) return
    const payload = { ...form, amount: String(form.amount), movieVersion: form.movieVersion.trim() || null, validTo: form.validTo || null }
    try {
      submitInFlight.current = true; setSaving(true); setRequestError('')
      if (editing) {
        await updateSeatPrice(editing.id, { ...payload, rowVersion: editing.rowVersion })
        toast.success('Đã tạo revision hoặc cập nhật quy tắc giá.')
      } else {
        await createSeatPriceDraft(payload); toast.success('Đã tạo bản nháp giá vé.')
      }
      dialogRef.current?.close(); await load()
    } catch (error) {
      const message = apiMessage(error, 'Không thể lưu bảng giá.')
      setRequestError(message)
      setFormErrors(message === 'PRICE_RULE_CHANGED' ? { form: 'Dữ liệu đã thay đổi. Hãy đóng form và tải lại.' } : {})
      toast.error(message)
    } finally { submitInFlight.current = false; setSaving(false) }
  }

  async function updateStatus(rule, status) {
    if (submitInFlight.current) return
    try {
      submitInFlight.current = true; setSaving(true); setRequestError('')
      await updateSeatPrice(rule.id, { rowVersion: rule.rowVersion, status })
      deactivateDialogRef.current?.close()
      toast.success(status === 'ACTIVE' ? 'Đã kích hoạt quy tắc giá.' : 'Đã ngừng áp dụng quy tắc giá.')
      await load()
    } catch (error) {
      const message = apiMessage(error, 'Không thể cập nhật trạng thái.')
      setRequestError(message); toast.error(message)
    } finally { submitInFlight.current = false; setSaving(false) }
  }

  function applyFilters(event) { event.preventDefault(); void load(filters) }
  function resetFilters() { setFilters(EMPTY_FILTERS); void load(EMPTY_FILTERS) }

  return <section className="price-admin" aria-labelledby="seat-price-title">
    <header className="price-admin__header"><div><p className="eyebrow">BẢNG GIÁ</p><h2 id="seat-price-title">Bảng giá vé</h2><p>Chỉ quy tắc ADMIN_APPROVED và ACTIVE mới dùng được cho lần giữ ghế sau này.</p></div><button className="price-admin__primary" type="button" onClick={() => openForm()}>Tạo bản nháp giá</button></header>

    <search className="price-admin__filters"><form aria-labelledby="seat-price-filter-title" onSubmit={applyFilters}><h3 id="seat-price-filter-title" className="visually-hidden">Lọc bảng giá</h3>
      <label htmlFor="price-status">Trạng thái<select id="price-status" name="status" value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}><option value="">Tất cả</option><option value="DRAFT">Bản nháp</option><option value="ACTIVE">Đang áp dụng</option><option value="INACTIVE">Ngừng áp dụng</option></select></label>
      <label htmlFor="price-seat-type">Loại ghế<select id="price-seat-type" name="seatType" value={filters.seatType} onChange={(event) => setFilters({ ...filters, seatType: event.target.value })}><option value="">Tất cả</option><option value="STANDARD">STANDARD</option><option value="VIP">VIP</option></select></label>
      <label htmlFor="price-movie-version">Phiên bản phim<input id="price-movie-version" name="movieVersion" value={filters.movieVersion} onChange={(event) => setFilters({ ...filters, movieVersion: event.target.value })} placeholder="Ví dụ: IMAX Laser 3D" /></label>
      <label htmlFor="price-effective-on">Hiệu lực vào ngày<input id="price-effective-on" type="date" name="effectiveOn" value={filters.effectiveOn} onChange={(event) => setFilters({ ...filters, effectiveOn: event.target.value })} /></label>
      <div className="price-admin__filter-actions"><button type="submit">Áp dụng</button><button type="button" onClick={resetFilters}>Xóa bộ lọc</button></div>
    </form></search>

    <section className="price-admin__surface" aria-labelledby="price-rule-list-title"><div className="price-admin__section-heading"><h3 id="price-rule-list-title">Quy tắc giá đã phê duyệt</h3><p>Lifecycle và hiệu lực theo ngày được hiển thị riêng.</p></div>{requestError ? <p className="price-admin__error" role="alert">{requestError}</p> : null}
      {loading ? <p role="status" className="price-admin__state">Đang tải bảng giá…</p> : rules.length === 0 ? <p className="price-admin__empty">Chưa có bảng giá được phê duyệt. Giá fixture local không xuất hiện ở đây.</p> : <div className="price-admin__table-wrap" tabIndex="0" role="region" aria-labelledby="price-rule-list-title"><table><caption className="visually-hidden">Danh sách quy tắc bảng giá vé</caption><thead><tr><th scope="col">Loại ghế</th><th scope="col">Phiên bản</th><th scope="col">Giá</th><th scope="col">Hiệu lực</th><th scope="col">Trạng thái</th><th scope="col">Revision</th><th scope="col">Thao tác</th></tr></thead><tbody>{rules.map((rule) => <tr key={rule.id}><th scope="row">{rule.seatType}</th><td>{rule.movieVersion || 'Mặc định'}</td><td>{money.format(rule.amount)}</td><td>{rule.validFrom} — {rule.validTo || 'Không thời hạn'}</td><td><span className={`price-admin__status price-admin__status--${rule.effectiveState.toLowerCase()}`}>{rule.status} · {rule.effectiveState}</span></td><td>v{rule.rowVersion}</td><td className="price-admin__actions">{rule.status === 'DRAFT' || rule.status === 'INACTIVE' ? <button type="button" disabled={saving} onClick={() => updateStatus(rule, 'ACTIVE')}>Kích hoạt</button> : <button type="button" disabled={saving} onClick={() => { setRuleToDeactivate(rule); deactivateDialogRef.current?.showModal() }}>Ngừng áp dụng</button>}<button type="button" disabled={saving} onClick={() => openForm(rule)}>Revision</button><details><summary>Lịch sử ({rule.auditHistory?.length || 0})</summary>{rule.auditHistory?.length ? <ul>{rule.auditHistory.map((audit) => <li key={audit.id}><strong>{audit.action}</strong> · v{audit.revision} · {audit.actorUsername || 'Hệ thống'}<br /><time dateTime={audit.createdAt}>{formatAuditTime(audit.createdAt)}</time></li>)}</ul> : <p>Chưa có lịch sử.</p>}</details></td></tr>)}</tbody></table></div>}
    </section>

    <dialog ref={dialogRef} className="price-admin__dialog" aria-labelledby="seat-price-form-title"><form onSubmit={submit} aria-describedby="seat-price-form-help"><header><p className="eyebrow">{editing ? 'SUCCESSOR REVISION' : 'BẢN NHÁP'}</p><h3 id="seat-price-form-title">{editing ? 'Tạo successor revision' : 'Tạo bản nháp bảng giá'}</h3></header><p id="seat-price-form-help" className="price-admin__notice">{editing?.status === 'ACTIVE' ? `Rule #${editing.id} sẽ được ngừng áp dụng và thay bằng revision ACTIVE mới trong cùng giao dịch.` : 'Để trống phiên bản phim để tạo giá generic mặc định.'}</p>{formErrors.form ? <p className="price-admin__error" role="alert">{formErrors.form}</p> : null}<div className="price-admin__form-grid">
      <label htmlFor="rule-seat-type">Loại ghế<select id="rule-seat-type" required name="seatType" value={form.seatType} onChange={(event) => setForm({ ...form, seatType: event.target.value })}><option value="STANDARD">STANDARD</option><option value="VIP">VIP</option></select></label><label htmlFor="rule-movie-version">Phiên bản phim<input id="rule-movie-version" name="movieVersion" value={form.movieVersion} onChange={(event) => setForm({ ...form, movieVersion: event.target.value })} placeholder="Để trống cho giá mặc định" /></label><label htmlFor="rule-amount">Giá vé (VND)<input id="rule-amount" required type="number" min="0.01" step="0.01" inputMode="decimal" name="amount" value={form.amount} aria-invalid={Boolean(formErrors.amount)} aria-describedby={formErrors.amount ? 'rule-amount-error' : undefined} onChange={(event) => setForm({ ...form, amount: event.target.value })} />{formErrors.amount ? <span id="rule-amount-error" className="price-admin__field-error">{formErrors.amount}</span> : null}</label><label htmlFor="rule-valid-from">Ngày bắt đầu<input id="rule-valid-from" required type="date" name="validFrom" value={form.validFrom} aria-invalid={Boolean(formErrors.validFrom)} aria-describedby={formErrors.validFrom ? 'rule-valid-from-error' : undefined} onChange={(event) => setForm({ ...form, validFrom: event.target.value })} />{formErrors.validFrom ? <span id="rule-valid-from-error" className="price-admin__field-error">{formErrors.validFrom}</span> : null}</label><label htmlFor="rule-valid-to">Ngày kết thúc<input id="rule-valid-to" type="date" name="validTo" value={form.validTo} aria-invalid={Boolean(formErrors.validTo)} aria-describedby={formErrors.validTo ? 'rule-valid-to-error' : undefined} onChange={(event) => setForm({ ...form, validTo: event.target.value })} />{formErrors.validTo ? <span id="rule-valid-to-error" className="price-admin__field-error">{formErrors.validTo}</span> : null}</label>
    </div><footer><button type="button" onClick={() => dialogRef.current?.close()} disabled={saving}>Hủy</button><button className="price-admin__primary" type="submit" disabled={saving}>{saving ? 'Đang lưu…' : editing ? 'Tạo revision' : 'Lưu bản nháp'}</button></footer></form></dialog>
    <dialog ref={deactivateDialogRef} className="price-admin__dialog price-admin__dialog--confirm" aria-labelledby="deactivate-title"><section><p className="eyebrow">XÁC NHẬN</p><h3 id="deactivate-title">Ngừng áp dụng quy tắc giá?</h3><p>Rule #{ruleToDeactivate?.id} sẽ không còn được Pricing Service chọn cho các lần giữ ghế sau này. Lịch sử audit vẫn được giữ lại.</p><footer><button type="button" disabled={saving} onClick={() => deactivateDialogRef.current?.close()}>Hủy</button><button className="price-admin__danger" type="button" disabled={saving} onClick={() => updateStatus(ruleToDeactivate, 'INACTIVE')}>{saving ? 'Đang xử lý…' : 'Ngừng áp dụng'}</button></footer></section></dialog>
  </section>
}
