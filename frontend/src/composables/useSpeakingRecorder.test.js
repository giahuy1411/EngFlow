import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { useSpeakingRecorder } from './useSpeakingRecorder'

class FakeMediaRecorder {
  static isTypeSupported = vi.fn(type => type === 'audio/webm;codecs=opus')

  constructor(stream, options = {}) {
    this.stream = stream
    this.mimeType = options.mimeType || 'audio/webm'
    this.state = 'inactive'
  }

  start() {
    this.state = 'recording'
  }

  stop() {
    this.state = 'inactive'
    this.ondataavailable?.({ data: new Blob(['voice'], { type: this.mimeType }) })
    this.onstop?.()
  }
}

function mountRecorder(maxDurationSeconds = 30) {
  let recorder
  const wrapper = mount(defineComponent({
    setup() {
      recorder = useSpeakingRecorder(maxDurationSeconds)
      return () => null
    }
  }))
  return { recorder, wrapper }
}

describe('useSpeakingRecorder', () => {
  let track
  let stream

  beforeEach(() => {
    vi.useFakeTimers()
    track = { stop: vi.fn() }
    stream = { getTracks: () => [track] }
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: vi.fn().mockResolvedValue(stream) }
    })
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder)
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:recording'),
      revokeObjectURL: vi.fn()
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('requests audio only by default', async () => {
    const { recorder, wrapper } = mountRecorder()

    await expect(recorder.prepare()).resolves.toBe(true)

    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true, video: false })
    expect(recorder.isReady.value).toBe(true)
    wrapper.unmount()
  })

  it('returns an actionable error when permission is denied', async () => {
    navigator.mediaDevices.getUserMedia.mockRejectedValue({ name: 'NotAllowedError' })
    const { recorder, wrapper } = mountRecorder()

    await expect(recorder.prepare()).resolves.toBe(false)

    expect(recorder.error.value).toContain('Quyền micro bị từ chối')
    wrapper.unmount()
  })

  it('creates a reviewable recording after stop', async () => {
    const { recorder, wrapper } = mountRecorder()
    await recorder.prepare()

    recorder.start()
    recorder.stop()

    expect(recorder.isRecording.value).toBe(false)
    expect(recorder.blob.value.size).toBeGreaterThan(0)
    expect(recorder.previewUrl.value).toBe('blob:recording')
    expect(track.stop).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it('automatically stops at the configured duration', async () => {
    const { recorder, wrapper } = mountRecorder(2)
    await recorder.prepare()

    recorder.start()
    vi.advanceTimersByTime(2000)

    expect(recorder.elapsedSeconds.value).toBe(2)
    expect(recorder.isRecording.value).toBe(false)
    expect(recorder.previewUrl.value).toBe('blob:recording')
    wrapper.unmount()
  })
})