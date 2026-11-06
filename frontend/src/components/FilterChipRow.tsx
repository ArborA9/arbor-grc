import { FilterPillRow, type FilterPillOption } from './ui/FilterPill'

export type FilterOption<T extends string> = FilterPillOption<T>

/** @deprecated Prefer `FilterPillRow` from `./ui`: kept for existing imports. */
export default function FilterChipRow<T extends string>({
  value,
  options,
  onChange,
}: {
  value: T
  options: FilterOption<T>[]
  onChange: (value: T) => void
}) {
  return <FilterPillRow value={value} options={options} onChange={onChange} />
}
