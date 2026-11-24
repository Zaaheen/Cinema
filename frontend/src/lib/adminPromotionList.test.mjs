import assert from 'node:assert/strict'
import test from 'node:test'
import { selectPromotionPage } from './adminPromotionList.js'

const now = new Date('2026-11-24T12:00:00+07:00').getTime()
const promotions = Array.from({ length: 13 }, (_, index) => ({
  id: index + 1,
  title: `Ưu đãi mẫu ${index + 1}`,
  status: index === 12 ? 'INACTIVE' : 'ACTIVE',
  startsAt: '2026-11-01T00:00:00+07:00',
  endsAt: '2026-12-01T00:00:00+07:00',
}))

test('admin promotion list paginates a larger dataset without losing items', () => {
  const pages = [0, 1, 2].map((page) => selectPromotionPage(promotions, { page, pageSize: 6 }, now))
  assert.deepEqual(pages.map((result) => result.visiblePromotions.length), [6, 6, 1])
  assert.deepEqual(pages.flatMap((result) => result.visiblePromotions.map((item) => item.id)), promotions.map((item) => item.id))
  assert.equal(pages[2].pageCount, 3)
})

test('search and status filters reset the visible page safely', () => {
  const byCode = selectPromotionPage(promotions, { query: 'KM0013', page: 4 }, now)
  assert.deepEqual(byCode.visiblePromotions.map((item) => item.id), [13])
  assert.equal(byCode.currentPage, 0)
  const inactive = selectPromotionPage(promotions, { status: 'inactive' }, now)
  assert.deepEqual(inactive.visiblePromotions.map((item) => item.id), [13])
  const effective = selectPromotionPage(promotions, { status: 'effective' }, now)
  assert.equal(effective.filtered.length, 12)
})
