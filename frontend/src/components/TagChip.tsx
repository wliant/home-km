interface TagChipProps {
  name: string
  color?: string
  onRemove?: () => void
}

export default function TagChip({ name, color = '#6366f1', onRemove }: TagChipProps) {
  return (
    <span
      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium text-white"
      style={{ backgroundColor: color }}
    >
      {name}
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          className="ml-0.5 hover:opacity-75 focus:outline-none"
          aria-label={`Remove tag ${name}`}
        >
          ✕
        </button>
      )}
    </span>
  )
}
