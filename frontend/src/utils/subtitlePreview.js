/**
 * Bộ parse preview phía client cho transcript SRT/VTT — phản chiếu backend
 * SubtitleParser (src/main/java/com/datn/engflow/service/SubtitleParser.java).
 * Chỉ dùng cho admin xem trước/kiểm tra; backend vẫn parse lại lúc lưu.
 */
const CUE_LINE = /^\s*(?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}\s*-->\s*(?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}/

/**
 * Đổi một match timestamp thành số giây.
 * Nhóm 1 (giờ) là tuỳ chọn — VTT/SRT có thể bỏ phần giờ; phần thập phân được
 * pad/ cắt về đúng 3 chữ số nên "1,5" và "1,500" đều ra 1.5 giây.
 */
function toSeconds(match) {
  const hours = match[1] ? parseInt(match[1], 10) : 0
  const minutes = parseInt(match[2], 10)
  const seconds = parseInt(match[3], 10)
  const fraction = (match[4] || '0').padEnd(3, '0').slice(0, 3)
  return hours * 3600 + minutes * 60 + seconds + parseInt(fraction, 10) / 1000
}

const TIMESTAMP = /(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})/g

/** Gỡ thẻ HTML/khối `{...}`, giải mã vài entity thường gặp, gộp khoảng trắng. */
function cleanText(text) {
  return text
    .replace(/<[^>]+>/g, '')
    .replace(/\{[^}]*\}/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * Parse thô transcript SRT/VTT thành danh sách cue để admin xem trước.
 *
 * Cách làm: quét từng dòng; gặp dòng timestamp thì lấy tối đa 2 mốc thời gian đầu,
 * gom các dòng text phía sau cho tới dòng trống hoặc cue kế tiếp. Cue chỉ được nhận
 * khi có đủ 2 mốc thời gian VÀ text không rỗng — nên dòng rác/header của VTT bị bỏ qua
 * thay vì tạo ra cue hỏng.
 *
 * @param {string} raw - nội dung transcript thô (SRT hoặc VTT)
 * @returns {Array<{start: number, end: number, text: string}>} danh sách cue, giây
 */
export function parseSubtitlePreview(raw) {
  const lines = String(raw || '').replace(/\r\n?/g, '\n').split('\n')
  const cues = []
  let i = 0
  while (i < lines.length) {
    if (CUE_LINE.test(lines[i])) {
      const times = [...lines[i].matchAll(TIMESTAMP)].slice(0, 2).map(toSeconds)
      const textLines = []
      i++
      while (i < lines.length && lines[i].trim() !== '' && !CUE_LINE.test(lines[i])) {
        textLines.push(lines[i])
        i++
      }
      const text = cleanText(textLines.join(' '))
      if (text && times.length === 2) cues.push({ start: times[0], end: times[1], text })
    } else {
      i++
    }
  }
  return cues
}
