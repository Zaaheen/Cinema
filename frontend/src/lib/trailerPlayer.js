const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/

export function trailerVideoId(value) {
  try {
    const url = new URL(value)
    if (url.protocol !== 'https:' || url.hostname !== 'www.youtube.com' || url.pathname !== '/watch' || url.username || url.password || url.port || url.hash) return null
    const entries = [...url.searchParams.entries()]
    if (entries.length !== 1 || entries[0][0] !== 'v') return null
    return VIDEO_ID.test(entries[0][1]) ? entries[0][1] : null
  } catch {
    return null
  }
}

export function trailerWatchUrl(videoId) {
  return VIDEO_ID.test(videoId) ? `https://www.youtube.com/watch?v=${videoId}` : null
}

export function trailerEmbedUrl(videoId, origin) {
  if (!VIDEO_ID.test(videoId)) return null
  return `https://www.youtube-nocookie.com/embed/${videoId}?enablejsapi=1&origin=${encodeURIComponent(origin)}`
}

export function trailerEmbedErrorMessage(code) {
  if (code === 101 || code === 150) return 'Chủ video không cho phát trên trang này. Hãy mở video trực tiếp trên YouTube.'
  if (code === 153) return 'YouTube không nhận được thông tin nguồn trang phát. Hãy mở video trực tiếp trên YouTube.'
  if (code === 100) return 'Video đã bị xóa hoặc đặt riêng tư. Hãy kiểm tra trên YouTube.'
  return 'YouTube không thể phát video tại đây. Hãy thử mở video trực tiếp trên YouTube.'
}

let apiPromise

export function loadYouTubeIframeApi() {
  if (window.YT?.Player) return Promise.resolve(window.YT)
  if (apiPromise) return apiPromise

  apiPromise = new Promise((resolve, reject) => {
    const previousReady = window.onYouTubeIframeAPIReady
    const script = document.createElement('script')
    window.onYouTubeIframeAPIReady = () => {
      try {
        previousReady?.()
      } finally {
        resolve(window.YT)
      }
    }
    script.src = 'https://www.youtube.com/iframe_api'
    script.async = true
    script.onerror = () => {
      window.onYouTubeIframeAPIReady = previousReady
      script.remove()
      apiPromise = null
      reject(new Error('YouTube IFrame API không khả dụng'))
    }
    document.head.appendChild(script)
  })

  return apiPromise
}
