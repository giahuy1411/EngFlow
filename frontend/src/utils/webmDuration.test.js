import { describe, it, expect, vi, afterEach } from 'vitest'
import { fixWebmDuration } from '@/utils/webmDuration'

/**
 * audit-v11 F144 — the admin shadowing player used to audibly play recordings at 16x.
 *
 * Reported by the project owner: "khi tôi vào admin chấm shadowing thì nghe thấy âm
 * thanh lạ chạy rất nhanh không nghe rõ".
 *
 * Root cause, measured on the live page: every recording reported
 * `duration === Infinity`, and the guard `Number.isFinite(d) && d > 0` therefore did NOT
 * skip — so the helper ran `el.playbackRate = 16; el.play()`, which is audible.
 *
 * The first three cases below FAIL against the old implementation (it left the rate at
 * 16); they are the regression guard.
 */

function makeAudio({ duration, muted = false, currentTime = 0 } = {}) {
  const el = document.createElement('audio')
  Object.defineProperty(el, 'duration', { value: duration, writable: true, configurable: true })
  Object.defineProperty(el, 'currentTime', { value: currentTime, writable: true, configurable: true })
  Object.defineProperty(el, 'muted', { value: muted, writable: true, configurable: true })
  Object.defineProperty(el, 'volume', { value: 1, writable: true, configurable: true })
  el.playbackRate = 1
  el.play = vi.fn().mockResolvedValue(undefined)
  el.pause = vi.fn()
  return el
}

describe('fixWebmDuration (F144)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('leaves a file with a finite duration completely alone', () => {
    const el = makeAudio({ duration: 30 })
    fixWebmDuration({ target: el })
    expect(el.playbackRate).toBe(1)
    expect(el.play).not.toHaveBeenCalled()
  })

  it('does not leave the element at 16x when duration is Infinity (the real webm case)', () => {
    vi.useFakeTimers()
    const el = makeAudio({ duration: Infinity })
    fixWebmDuration({ target: el })
    vi.runAllTimers()
    expect(el.playbackRate).toBe(1)
  })

  it('does not leave the element at 16x when duration is NaN (undecodable)', () => {
    vi.useFakeTimers()
    const el = makeAudio({ duration: NaN })
    fixWebmDuration({ target: el })
    vi.runAllTimers()
    expect(el.playbackRate).toBe(1)
  })

  it('never lets the accelerated seek be audible — the element is muted while it runs', () => {
    vi.useFakeTimers()
    const el = makeAudio({ duration: Infinity })
    let mutedDuringPlay = null
    el.play = vi.fn().mockImplementation(() => {
      mutedDuringPlay = el.muted
      return Promise.resolve()
    })
    fixWebmDuration({ target: el })
    expect(mutedDuringPlay).toBe(true)      // silent at the moment it plays
    vi.runAllTimers()
    expect(el.muted).toBe(false)            // and restored afterwards
  })

  it('restores a previously-muted element to muted (does not change user intent)', () => {
    vi.useFakeTimers()
    const el = makeAudio({ duration: Infinity, muted: true })
    fixWebmDuration({ target: el })
    vi.runAllTimers()
    expect(el.muted).toBe(true)
  })

  it('stops playing and restores position once the duration probe finishes', () => {
    vi.useFakeTimers()
    const el = makeAudio({ duration: Infinity, currentTime: 4.5 })
    fixWebmDuration({ target: el })
    vi.runAllTimers()
    expect(el.pause).toHaveBeenCalled()
    expect(el.currentTime).toBe(4.5)
    expect(el.playbackRate).toBe(1)
  })

  it('never leaves the element playing on its own for a finite-duration file', () => {
    const el = makeAudio({ duration: 12 })
    fixWebmDuration({ target: el })
    expect(el.play).not.toHaveBeenCalled()
    expect(el.pause).not.toHaveBeenCalled()
  })

  it('survives a missing element without throwing', () => {
    expect(() => fixWebmDuration({})).not.toThrow()
    expect(() => fixWebmDuration(null)).not.toThrow()
  })
})
