/**
 * Duration handling for MediaRecorder-produced WebM played in an <audio> element.
 *
 * Extracted from AdminVideoAttempts.vue (audit-v11 F144) so the behaviour can be tested
 * directly — the component only reaches it through a template event handler.
 */

/**
 * MediaRecorder webm has no Duration in its header, so the native control shows "∞".
 *
 * audit-v11 F144 — this function used to AUDIBLY PLAY the recording at 16x.
 *
 * The old body was:
 *
 *     if (Number.isFinite(el.duration) && el.duration > 0) return
 *     const originalRate = el.playbackRate
 *     el.onended = () => { ...; el.playbackRate = originalRate }
 *     el.currentTime = 1e10
 *     el.playbackRate = 16
 *     el.play().catch(() => {})
 *
 * Two independent mistakes made the admin hear "âm thanh lạ chạy rất nhanh":
 *
 *   1. The guard only skipped FINITE durations. A MediaRecorder WebM reports
 *      `duration === Infinity` — the exact case the helper exists for — so the guard
 *      fell straight through into the workaround. Measured on the real page: every
 *      recording reported Infinity and the workaround ran on all of them.
 *   2. `el.playbackRate = 16` followed by `el.play()` makes the element AUDIBLY PLAY at
 *      16x. The seek-and-rewind trick only needs the element to advance its timeline; it
 *      never needed to be audible, and it never needed to leave the rate changed.
 *
 * The fix keeps the intent (let the browser compute a duration) without the side effect:
 * mute the element for the duration of the trick, restore both the mute state and the
 * playback rate, and never leave it playing. If the browser cannot compute a duration
 * this way, the element is simply left as-is — a player that shows "∞" is a cosmetic
 * problem; a player that screams at 16x is a real one.
 */
export function fixWebmDuration(event) {
  const el = event && event.target
  if (!el) return

  // Nothing to do when the browser already knows the duration.
  if (Number.isFinite(el.duration) && el.duration > 0) return

  const originalRate = el.playbackRate
  const originalMuted = el.muted
  const originalTime = el.currentTime

  let restored = false
  const restore = () => {
    if (restored) return
    restored = true
    el.onended = null
    try {
      el.pause()
      el.playbackRate = originalRate
      el.muted = originalMuted
      el.currentTime = originalTime
    } catch {
      /* element detached — nothing to restore */
    }
  }

  // Mute so the accelerated seek is never heard, even if the browser ignores the
  // rate cap or fires `ended` late.
  el.muted = true
  el.onended = restore
  // Belt and braces: some browsers do not fire `ended` for a seek past the end.
  setTimeout(restore, 1000)

  try {
    el.currentTime = 1e10
    el.playbackRate = 16
    const p = el.play()
    if (p && typeof p.catch === 'function') p.catch(restore)
  } catch {
    restore()
  }
}
