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

const IMAGE_FORMATS = [
  { suffixes: ['.png'], mime: 'image/png' },
  { suffixes: ['.jpg', '.jpeg'], mime: 'image/jpeg' },
  { suffixes: ['.webp'], mime: 'image/webp' },
]

function imageFileError(file, label) {
  if (!file) return `Chọn ảnh ${label} để tải lên.`
  const name = file.name?.toLowerCase() || ''
  if (!IMAGE_FORMATS.some(({ suffixes, mime }) => suffixes.some((suffix) => name.endsWith(suffix)) && file.type === mime)) {
    return `${label} phải là ảnh PNG, JPG/JPEG hoặc WebP.`
  }
  if (file.size === 0 || file.size > 5 * 1024 * 1024) return `${label} phải có dung lượng từ 1 byte đến 5 MiB.`
  return ''
}

export function posterFileError(file) {
  return imageFileError(file, 'Poster')
}

export function bannerFileError(file) {
  return imageFileError(file, 'Banner')
}
