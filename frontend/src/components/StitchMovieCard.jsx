import { Link } from 'react-router-dom'

export default function StitchMovieCard({ movie }) {
  const detailPath = `/movies/${movie.id}`

  return (
    <article className="group relative flex flex-col overflow-hidden rounded-2xl bg-surface-container shadow-lg transition-all duration-300 hover:shadow-2xl hover:shadow-primary-container/20">
      <div className="relative aspect-[2/3] w-full bg-surface-container-lowest">
        {movie.posterUrl ? (
          <img className="block h-full w-full object-cover transition-transform duration-500 group-hover:scale-105" src={movie.posterUrl} alt={`Poster ${movie.title}`} onError={(event) => { event.currentTarget.hidden = true }} />
        ) : <div className="grid h-full place-items-center px-4 text-center font-body-sm text-outline">Chưa có poster</div>}
        <div aria-hidden="true" className="absolute inset-0 bg-gradient-to-t from-surface via-transparent to-black/40" />
        <span className="absolute left-2.5 top-2.5 rounded bg-surface-container-highest px-2 py-0.5 font-label-sm font-extrabold uppercase text-tertiary-fixed">{movie.ageRating || 'Chưa phân loại'}</span>
        <span className="absolute bottom-2.5 left-2.5 right-2.5 truncate rounded bg-surface-container-high/90 px-2 py-0.5 font-label-sm text-on-surface-variant backdrop-blur-md">{movie.version || 'Chưa cập nhật'}</span>
      </div>
      <div className="flex flex-1 flex-col justify-between p-4">
        <div className="space-y-1.5"><div className="flex min-w-0 items-center justify-between gap-2 font-label-sm text-outline"><span className="truncate">{movie.genre || 'Chưa cập nhật thể loại'}</span><span className="shrink-0">{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'Chưa cập nhật'}</span></div><h3 className="line-clamp-1 font-headline-sm text-headline-sm font-bold text-on-surface transition-colors group-hover:text-primary"><Link className="before:absolute before:inset-0" to={detailPath}>{movie.title}</Link></h3><p className="line-clamp-1 font-body-sm text-outline">{movie.synopsis || 'Chưa có mô tả.'}</p></div>
        <footer className="relative z-10 mt-3 flex gap-2 border-t border-surface-container-high/70 pt-4"><Link className="flex-1 rounded-xl bg-primary-container px-3 py-2.5 text-center font-label-md font-bold text-on-primary shadow-[0_0_12px_rgba(255,69,162,.35)]" to={detailPath}>Xem chi tiết</Link><Link aria-label={`Chi tiết phim ${movie.title}`} className="grid h-10 w-10 place-items-center rounded-xl bg-surface-container-high text-on-surface" to={detailPath}>ⓘ</Link></footer>
      </div>
    </article>
  )
}
