type Props = {
  title: string
  desc: string
  index: string
}

export function FeatureCard({ title, desc, index }: Props) {
  return (
    <article className="feature-card panel glass">
      <span className="index" aria-hidden>
        {index}
      </span>
      <h3>{title}</h3>
      <p className="muted">{desc}</p>
    </article>
  )
}
