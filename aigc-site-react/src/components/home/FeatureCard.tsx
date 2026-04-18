type Props = {
  title: string
  desc: string
  index: string
  tag?: string
  highlight?: string
  points?: string[]
}

export function FeatureCard({ title, desc, index, tag = 'Capability', highlight, points = [] }: Props) {
  return (
    <article className="feature-card panel glass">
      <div className="feature-card__head">
        <span className="index" aria-hidden>
          {index}
        </span>
        <span className="feature-card__tag">{tag}</span>
      </div>
      <h3>{title}</h3>
      <p className="muted">{desc}</p>
      {highlight ? <p className="feature-card__highlight">{highlight}</p> : null}
      {points.length ? (
        <ul className="feature-card__points">
          {points.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : null}
    </article>
  )
}
