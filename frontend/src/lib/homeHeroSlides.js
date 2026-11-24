export function heroSlides(banners = [], nowShowing = []) {
  const movies = (Array.isArray(nowShowing) ? nowShowing : [])
    .filter((movie) => movie?.id && movie.status === 'NOW_SHOWING')
  const byPath = new Map(movies.map((movie) => [`/movies/${movie.id}`, movie]))
  const featuredIds = new Set()
  const bannerSlides = (Array.isArray(banners) ? banners : []).flatMap((banner) => {
    const movie = byPath.get(banner.targetPath)
    if (!movie || featuredIds.has(movie.id)) return []
    featuredIds.add(movie.id)
    return [{ key: `banner-${banner.id}`, title: movie.title, description: banner.subtitle || movie.synopsis,
      image: banner.imageUrl || movie.backdropUrl || movie.posterUrl, target: `/movies/${movie.id}`, movie }]
  })
  return [...bannerSlides, ...movies.filter((movie) => !featuredIds.has(movie.id)).map((movie) => ({
    key: `movie-${movie.id}`, title: movie.title, description: movie.synopsis,
    image: movie.backdropUrl || movie.posterUrl, target: `/movies/${movie.id}`, movie,
  }))]
}
