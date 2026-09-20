/*
  audit-v11 F132 — two functions, because the SAME hue cannot serve both roles.

  `levelColor()` returns the vivid hue and is for FILLS: the progress bar, the dot, the
  SVG stroke. Those are correct — a vivid fill is decoration, not text.

  `levelInkColor()` returns the AA-compliant darker hue and is for TEXT. Measured on white:
  accent 4.23:1, tertiary 1.67:1, secondary 2.65:1, quaternary 1.92:1 — all below the 4.5:1
  WCAG 1.4.3 requires for body text. The ink variants measure 7.10 / 5.02 / 6.04 / 5.48:1.

  Both call sites used to pass the SAME value to `background` AND `color`, which is why a
  single-function fix would have half-landed.
*/
export function levelColor(level) {
  return ({
    ELEMENTARY: 'var(--geo-accent, #8B5CF6)',
    PRE_INTERMEDIATE: 'var(--geo-tertiary, #FBBF24)',
    INTERMEDIATE: 'var(--geo-secondary, #F472B6)',
    UPPER_INTERMEDIATE: 'var(--geo-quaternary, #34D399)',
  })[level] || 'var(--geo-muted-fg, #64748B)'
}

/** Text-safe variant of {@link levelColor} — use wherever the hue carries text. */
export function levelInkColor(level) {
  return ({
    ELEMENTARY: 'var(--geo-accent-ink, #6D28D9)',
    PRE_INTERMEDIATE: 'var(--geo-tertiary-ink, #B45309)',
    INTERMEDIATE: 'var(--geo-secondary-ink, #BE185D)',
    UPPER_INTERMEDIATE: 'var(--geo-quaternary-ink, #047857)',
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
