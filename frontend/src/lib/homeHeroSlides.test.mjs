import assert from 'node:assert/strict'
import test from 'node:test'
import { heroSlides } from './homeHeroSlides.js'

const nowShowing = { id: 1, status: 'NOW_SHOWING', title: 'Phim đang chiếu', synopsis: 'Nội dung phim', backdropUrl: '/backdrop-1.jpg' }
const anotherNowShowing = { id: 4, status: 'NOW_SHOWING', title: 'Phim khác', posterUrl: '/poster-4.jpg' }
const comingSoon = { id: 2, status: 'COMING_SOON', title: 'Phim sắp chiếu' }
const stopped = { id: 3, status: 'STOPPED', title: 'Phim dừng chiếu' }

test('featured slides include only now showing movies and their banners', () => {
  const slides = heroSlides([
    { id: 10, targetPath: '/movies/1', imageUrl: '/banner-1.jpg', subtitle: 'Giới thiệu phim' },
    { id: 11, targetPath: '/movies/2', imageUrl: '/banner-2.jpg' },
    { id: 12, targetPath: '/movies/3', imageUrl: '/banner-3.jpg' },
    { id: 13, targetPath: '/promotions/1', imageUrl: '/promotion.jpg' },
    { id: 14, targetPath: '/movies/1', imageUrl: '/duplicate.jpg' },
  ], [nowShowing, comingSoon, stopped, anotherNowShowing])

  assert.deepEqual(slides.map((slide) => slide.movie.id), [1, 4])
  assert.deepEqual(slides.map((slide) => slide.key), ['banner-10', 'movie-4'])
  assert.equal(slides[0].image, '/banner-1.jpg')
  assert.equal(slides[0].target, '/movies/1')
  assert.equal(slides[1].image, '/poster-4.jpg')
})

test('featured slides are empty when no movie is currently showing', () => {
  assert.deepEqual(heroSlides([{ id: 20, targetPath: '/movies/2' }], [comingSoon, stopped]), [])
})
