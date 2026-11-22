const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/

export function canonicalTrailerUrl(value) {
  if (!value || value.length > 500 || value.trim() !== value) return null
  try {
    const url = new URL(value)
    if (url.protocol !== 'https:' || url.username || url.password || url.port || url.hash) return null
    const entries = [...url.searchParams.entries()]
    const share = entries.filter(([key]) => key === 'si')
    if (share.length > 1 || (share.length === 1 && !/^[A-Za-z0-9_-]+$/.test(share[0][1]))) return null
    let videoId
    if (url.hostname === 'youtu.be') {
      if (!/^\/[A-Za-z0-9_-]{11}$/.test(url.pathname) || entries.length !== share.length) return null
      videoId = url.pathname.slice(1)
    } else if (['youtube.com', 'www.youtube.com', 'm.youtube.com'].includes(url.hostname) && url.pathname === '/watch') {
      const video = entries.filter(([key]) => key === 'v')
      if (video.length !== 1 || entries.length !== share.length + 1) return null
      videoId = video[0][1]
    } else {
      return null
    }
    return VIDEO_ID.test(videoId) ? `https://www.youtube.com/watch?v=${videoId}` : null
  } catch {
    return null
  }
}

export function validTrailerUrl(value) {
  return canonicalTrailerUrl(value) !== null
}

export function posterFileError(file) {
  if (!file) return 'Chọn tệp PNG để tải lên.'
  if (!file.name?.toLowerCase().endsWith('.png') || file.type !== 'image/png') return 'Poster phải là tệp PNG.'
  if (file.size === 0 || file.size > 5 * 1024 * 1024) return 'Poster phải có dung lượng từ 1 byte đến 5 MiB.'
  return ''
}
