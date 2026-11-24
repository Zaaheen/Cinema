export function promotionVisibilityState(promotion, now = Date.now()) {
  if (promotion.status === 'INACTIVE') return 'inactive'
  if (!promotion.endsAt || now > new Date(promotion.endsAt).getTime()) return 'expired'
  if (promotion.startsAt && now < new Date(promotion.startsAt).getTime()) return 'upcoming'
  return 'effective'
}

export function selectPromotionPage(promotions, { query = '', status = 'all', page = 0, pageSize = 6 } = {}, now = Date.now()) {
  const searchTerm = query.trim().toLocaleLowerCase('vi-VN')
  const filtered = promotions.filter((promotion) => {
    const title = (promotion.title || '').toLocaleLowerCase('vi-VN')
    const code = `km${String(promotion.id).padStart(4, '0')}`
    return (!searchTerm || title.includes(searchTerm) || code.includes(searchTerm))
      && (status === 'all' || promotionVisibilityState(promotion, now) === status)
  })
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const currentPage = Math.max(0, Math.min(page, pageCount - 1))
  return { filtered, pageCount, currentPage, visiblePromotions: filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize) }
}
