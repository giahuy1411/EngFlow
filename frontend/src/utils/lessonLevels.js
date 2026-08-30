export function levelColor(level) {
  return ({
    ELEMENTARY: 'var(--geo-accent, #8B5CF6)',
    PRE_INTERMEDIATE: 'var(--geo-tertiary, #FBBF24)',
    INTERMEDIATE: 'var(--geo-secondary, #F472B6)',
    UPPER_INTERMEDIATE: 'var(--geo-quaternary, #34D399)',
  })[level] || 'var(--geo-muted-fg, #64748B)'
}

export function levelLabel(level) {
  return ({
    ELEMENTARY: 'Elementary',
    PRE_INTERMEDIATE: 'Pre-Intermediate',
    INTERMEDIATE: 'Intermediate',
    UPPER_INTERMEDIATE: 'Upper-Intermediate',
  })[level] || level
}
