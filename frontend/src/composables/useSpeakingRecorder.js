import { computed, onUnmounted, ref } from 'vue'

export function useSpeakingRecorder(maxDurationSeconds = 30) {
  const stream = ref(null)
  const recorder = ref(null)
  const chunks = ref([])
  const blob = ref(null)
  const previewUrl = ref('')
  const isRecording = ref(false)
  const elapsedSeconds = ref(0)
  const error = ref('')
  let timerId

  const isReady = computed(() => Boolean(stream.value))

  async function prepare(cameraEnabled = false) {
    error.value = ''
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      error.value = 'Trình duyệt không hỗ trợ ghi âm. Hãy dùng Chrome hoặc Edge mới nhất.'
      return false
    }
    try {
      stream.value = await navigator.mediaDevices.getUserMedia({ audio: true, video: cameraEnabled })
      return true
    } catch (cause) {
      error.value = cause.name === 'NotAllowedError'
        ? 'Quyền micro bị từ chối. Hãy cho phép trong cài đặt trình duyệt.'
        : 'Không thể mở micro.'
      return false
    }
  }

  function preferredMimeType(cameraEnabled) {
    const types = cameraEnabled
      ? ['video/webm;codecs=vp8,opus', 'video/webm']
      : ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg']
    return types.find(type => MediaRecorder.isTypeSupported(type)) || ''
  }

  function start(cameraEnabled = false) {
    if (!stream.value) return
    clearRecording()
    const mimeType = preferredMimeType(cameraEnabled)
    recorder.value = mimeType
      ? new MediaRecorder(stream.value, { mimeType })
      : new MediaRecorder(stream.value)
    recorder.value.ondataavailable = event => { if (event.data.size) chunks.value.push(event.data) }
    recorder.value.onstop = () => finalize(cameraEnabled)
    recorder.value.start(250)
    isRecording.value = true
    timerId = window.setInterval(() => {
      elapsedSeconds.value += 1
      if (elapsedSeconds.value >= maxDurationSeconds) stop()
    }, 1000)
  }

  function stop() {
    if (recorder.value?.state && recorder.value.state !== 'inactive') recorder.value.stop()
    isRecording.value = false
    window.clearInterval(timerId)
  }

  function finalize(cameraEnabled) {
    const type = recorder.value?.mimeType || (cameraEnabled ? 'video/webm' : 'audio/webm')
    blob.value = new Blob(chunks.value, { type })
    if (!blob.value.size) {
      error.value = 'Bản ghi trống. Hãy ghi lại.'
      return
    }
    previewUrl.value = URL.createObjectURL(blob.value)
    stopTracks()
  }

  function clearRecording() {
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
    blob.value = null
    chunks.value = []
    elapsedSeconds.value = 0
    error.value = ''
  }

  function stopTracks() {
    stream.value?.getTracks().forEach(track => track.stop())
    stream.value = null
  }

  function cleanup() {
    window.clearInterval(timerId)
    stopTracks()
    clearRecording()
  }

  onUnmounted(cleanup)

  return {
    blob, previewUrl, isRecording, elapsedSeconds, error, isReady,
    prepare, start, stop, clearRecording, stopTracks
  }
}