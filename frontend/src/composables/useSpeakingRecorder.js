import { computed, onUnmounted, ref } from 'vue'

/**
 * Composable ghi âm (hoặc ghi hình) cho phần luyện nói.
 *
 * MÁY TRẠNG THÁI (state machine) — thứ tự gọi bắt buộc:
 *   1. `prepare(cameraEnabled)`  : xin quyền mic/camera → mở `MediaStream`.
 *                                  Thành công thì `isReady === true`, cho phép ghi.
 *   2. `start(cameraEnabled)`    : tạo `MediaRecorder` trên stream đã có, ghi liên
 *                                  tục theo từng 250 ms (`ondataavailable` gom chunk).
 *                                  `isRecording === true`, đồng hồ đếm giây chạy.
 *   3. `stop()`                  : dừng recorder → sự kiện `onstop` gọi `finalize()`.
 *   4. `finalize()` (nội bộ)     : ghép chunk thành `Blob`, tạo `previewUrl` để nghe
 *                                  lại, rồi `stopTracks()` giải phóng thiết bị.
 *   (Bước "submit" KHÔNG nằm ở đây — component lấy `blob` rồi tự gọi
 *    `speakingService.uploadSubmission`, xem SpeakingRecord.vue.)
 *
 * Tự động hết giờ: hết `maxDurationSeconds` (mặc định 30 s) thì `start` gọi `stop()`
 * qua bộ đếm interval — không cần component tự canh.
 *
 * DỌN DẸP: `cleanup()` chạy trong `onUnmounted` — xoá interval, tắt track (nhả
 * đèn mic), thu hồi `previewUrl`. Nhờ vậy rời trang giữa lúc ghi vẫn không rò rỉ
 * thiết bị/URL object.
 *
 * @param {number} maxDurationSeconds trần thời lượng ghi, mặc định 30 giây.
 * @returns state (ref) + các hàm điều khiển; xem object `return` cuối file.
 */
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

  /** `true` khi đã có MediaStream (mic đã mở) → UI cho phép bấm "Bắt đầu ghi". */
  const isReady = computed(() => Boolean(stream.value))

  /**
   * Bước 1: xin quyền và mở stream. Trả `true` nếu sẵn sàng ghi.
   * - Trình duyệt thiếu `getUserMedia`/`MediaRecorder` → báo lỗi thân thiện, `false`.
   * - Người dùng từ chối quyền (`NotAllowedError`) → thông báo riêng, `false`.
   */
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

  /**
   * Chọn MIME type được trình duyệt hỗ trợ, ưu tiên chất lượng cao nhất.
   * Video: VP8+Opus → WebM. Audio: Opus → WebM → Ogg. Trả `''` nếu không có
   * định dạng nào được hỗ trợ (khi đó `MediaRecorder` dùng mặc định của máy).
   */
  function preferredMimeType(cameraEnabled) {
    const types = cameraEnabled
      ? ['video/webm;codecs=vp8,opus', 'video/webm']
      : ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg']
    return types.find(type => MediaRecorder.isTypeSupported(type)) || ''
  }

  /**
   * Bước 2: bắt đầu ghi trên stream đã mở (gọi `prepare` trước, nếu chưa có
   * stream thì thoát im lặng). `timeslice = 250 ms` để gom dữ liệu dần.
   * Bộ đếm interval tự gọi `stop()` khi chạm trần thời lượng.
   */
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

  /**
   * Bước 3: dừng ghi. Chỉ gọi `.stop()` khi recorder còn sống (state khác
   * `inactive`); `onstop` sẽ tự chạy `finalize`. Luôn tắt cờ và dừng đồng hồ.
   */
  function stop() {
    if (recorder.value?.state && recorder.value.state !== 'inactive') recorder.value.stop()
    isRecording.value = false
    window.clearInterval(timerId)
  }

  /**
   * Bước 4 (nội bộ, chạy từ `onstop`): ghép các chunk thành Blob, tạo URL nghe
   * lại, rồi nhả thiết bị. Nếu bản ghi rỗng (mic câm / chưa kịp có dữ liệu) thì
   * đặt `error` và KHÔNG tạo `previewUrl` để UI không cho nộp file trống.
   */
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

  /**
   * Xoá sạch trạng thái của bản ghi hiện tại (thu hồi URL, reset blob/chunk/giây/lỗi).
   * Dùng khi bấm "Ghi lại" hoặc trước mỗi lần `start`. KHÔNG đụng tới stream.
   */
  function clearRecording() {
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
    blob.value = null
    chunks.value = []
    elapsedSeconds.value = 0
    error.value = ''
  }

  /**
   * Tắt mọi track của stream (nhả mic/camera — tắt đèn báo) và đặt stream = null.
   * Sau lời gọi này `isReady === false`, muốn ghi tiếp phải `prepare()` lại.
   */
  function stopTracks() {
    stream.value?.getTracks().forEach(track => track.stop())
    stream.value = null
  }

  /** Dọn toàn bộ khi component unmount: interval + thiết bị + URL object. */
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