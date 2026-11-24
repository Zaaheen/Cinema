import assert from 'node:assert/strict'
import test from 'node:test'
import {
  loadYouTubeIframeApi,
  trailerEmbedErrorMessage,
  trailerEmbedUrl,
  trailerThumbnailUrl,
  trailerVideoId,
  trailerWatchUrl,
} from './trailerPlayer.js'

test('public trailer parsing accepts only a canonical validated YouTube URL', () => {
  assert.equal(trailerVideoId('https://www.youtube.com/watch?v=-2bFE8Jjm9A'), '-2bFE8Jjm9A')
  for (const value of [
    'https://youtu.be/-2bFE8Jjm9A?si=X-sIam13CLvNk9yL',
    'https://www.youtube.com/watch?v=-2bFE8Jjm9A&autoplay=1',
    'https://www.youtube.com.evil.test/watch?v=-2bFE8Jjm9A',
    'http://www.youtube.com/watch?v=-2bFE8Jjm9A',
    'https://www.youtube.com/watch?v=short',
  ]) assert.equal(trailerVideoId(value), null, value)
})

test('player and external fallback use only the validated video ID', () => {
  const id = trailerVideoId('https://www.youtube.com/watch?v=-2bFE8Jjm9A')
  assert.equal(trailerEmbedUrl(id, 'http://127.0.0.1:15174'),
    'https://www.youtube-nocookie.com/embed/-2bFE8Jjm9A?enablejsapi=1&origin=http%3A%2F%2F127.0.0.1%3A15174')
  assert.equal(trailerWatchUrl(id), 'https://www.youtube.com/watch?v=-2bFE8Jjm9A')
  assert.equal(trailerThumbnailUrl(id), 'https://i.ytimg.com/vi/-2bFE8Jjm9A/maxresdefault.jpg')
  assert.equal(trailerThumbnailUrl(id, 'hqdefault'), 'https://i.ytimg.com/vi/-2bFE8Jjm9A/hqdefault.jpg')
  assert.equal(trailerEmbedUrl('invalid/id', 'http://127.0.0.1:15174'), null)
  assert.equal(trailerWatchUrl('invalid/id'), null)
  assert.equal(trailerThumbnailUrl('invalid/id'), null)
})

test('player errors explain embed restrictions and missing referrer', () => {
  assert.match(trailerEmbedErrorMessage(101), /Chủ video không cho phát/)
  assert.equal(trailerEmbedErrorMessage(150), trailerEmbedErrorMessage(101))
  assert.match(trailerEmbedErrorMessage(153), /nguồn trang phát/)
  assert.match(trailerEmbedErrorMessage(100), /riêng tư/)
  assert.match(trailerEmbedErrorMessage(5), /không thể phát/)
})

test('YouTube IFrame API loader shares one script and resolves the player', async () => {
  const oldWindow = globalThis.window
  const oldDocument = globalThis.document
  const scripts = []
  globalThis.window = { YT: null, onYouTubeIframeAPIReady: null }
  globalThis.document = {
    createElement: () => ({ src: '', async: false }),
    head: { appendChild: (script) => scripts.push(script) },
  }
  try {
    const first = loadYouTubeIframeApi()
    const second = loadYouTubeIframeApi()
    assert.equal(first, second)
    assert.equal(scripts.length, 1)
    assert.equal(scripts[0].src, 'https://www.youtube.com/iframe_api')
    window.YT = { Player: class Player {} }
    window.onYouTubeIframeAPIReady()
    assert.equal(await first, window.YT)
  } finally {
    globalThis.window = oldWindow
    globalThis.document = oldDocument
  }
})
