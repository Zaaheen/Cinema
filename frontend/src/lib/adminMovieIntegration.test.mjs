import assert from 'node:assert/strict'
import test from 'node:test'
import { canonicalTrailerUrl, posterFileError, validTrailerUrl } from './adminMovieMedia.js'
import { isValidSlot, startsAtWithOffset } from './showtimeSchedule.js'

test('poster input follows the backend PNG and 5 MiB boundary', () => {
  assert.equal(posterFileError({ name: 'poster.png', type: 'image/png', size: 1024 }), '')
  assert.match(posterFileError({ name: 'poster.jpg', type: 'image/jpeg', size: 1024 }), /PNG/)
  assert.match(posterFileError({ name: 'poster.png', type: 'image/png', size: 5 * 1024 * 1024 + 1 }), /5 MiB/)
})

test('trailer accepts canonical YouTube links and rejects arbitrary hosts', () => {
  assert.equal(validTrailerUrl('https://www.youtube.com/watch?v=Abcdef12345'), true)
  assert.equal(validTrailerUrl('https://youtu.be/Abcdef12345'), true)
  assert.equal(canonicalTrailerUrl('https://youtu.be/-2bFE8Jjm9A?si=X-sIam13CLvNk9yL'), 'https://www.youtube.com/watch?v=-2bFE8Jjm9A')
  assert.equal(canonicalTrailerUrl('https://www.youtube.com/watch?si=X-sIam13CLvNk9yL&v=-2bFE8Jjm9A'), 'https://www.youtube.com/watch?v=-2bFE8Jjm9A')
  assert.equal(validTrailerUrl('https://youtube.com.evil.example/watch?v=Abcdef12345'), false)
  assert.equal(validTrailerUrl('https://www.youtube.com/watch?v=Abcdef12345&autoplay=1'), false)
  assert.equal(canonicalTrailerUrl('https://youtu.be/-2bFE8Jjm9A?si=one&si=two'), null)
  assert.equal(canonicalTrailerUrl('https://youtu.be/-2bFE8Jjm9A?redirect=https://evil.example'), null)
})

test('showtime input follows Asia/Ho_Chi_Minh boundaries and 15 minute slots', () => {
  const before = Date.parse('2026-11-25T00:00:00Z')
  assert.equal(isValidSlot('2026-11-26', '08:00', 90, before), true)
  assert.equal(startsAtWithOffset('2026-11-26', '08:00'), '2026-11-26T08:00:00+07:00')
  assert.equal(isValidSlot('2026-11-26', '08:01', 90, before), false)
  assert.equal(isValidSlot('2026-11-26', '22:00', 90, before), false)
  assert.equal(isValidSlot('2026-11-26', '10:00', 90, Date.parse('2026-11-27T00:00:00Z')), false)
})
