import FilterPill from './ui/FilterPill'

/** @deprecated Prefer `FilterPill` from `./ui`: kept for existing imports. */
export default function FilterChip({
  label,
  active,
  count,
  onClick,
}: {
  label: string
  active: boolean
  count?: number
  onClick: () => void
}) {
  return <FilterPill label={label} active={active} count={count} onClick={onClick} />
}
