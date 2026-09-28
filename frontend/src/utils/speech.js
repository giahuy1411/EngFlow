/**
 * Phương án dự phòng bằng speech-synthesis của trình duyệt cho các bài LISTENING thiếu file audio.
 *
 * Bối cảnh: 89/89 bài listening thiếu audio đều là câu điền khuyết
 * ("You ________ from Australia."). Đọc nguyên văn sẽ lộ đáp án, nên câu đã rút
 * chỗ trống phải được đọc với chỗ trống thu lại thành một quãng ngắt.
 *
 * Cách làm theo đúng ListeningGame.vue (Web Speech API, en-US, rate chậm) để cả
 * codebase dùng chung một hành vi đọc.
 */

/** True khi trình duyệt hiện tại có Web Speech API. */
export function isSpeechAvailable() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * Thu các dấu gạch dưới điền khuyết của markdown thành một quãng ngắt khi đọc.
 * Xử lý cả dãy 2+ gạch dưới (trong DB là 4-8 gạch) lẫn chỗ trống nằm trong inline-code.
 * "You ________ from Australia." -> "You ... from Australia."
 *
 * @param {string} md - câu hỏi markdown còn nguyên chỗ trống
 * @returns {string} câu đã thay chỗ trống bằng "...", đã gộp khoảng trắng
 */
export function blankOutForSpeech(md) {
  if (!md) return ''
  return String(md)
    .replace(/`[^`]*`/g, ' ... ') // chỗ trống nằm trong inline-code (`____`)
    .replace(/_{2,}/g, ' ... ') // dãy gạch dưới (________)
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * Bỏ số thứ tự đầu câu kiểu "1 " hoặc "12." để giọng đọc không đọc luôn số
 * ("6 You ________ ..." -> "You ...").
 *
 * @param {string} text - câu hỏi có thể có số thứ tự ở đầu
 * @returns {string} câu đã bỏ số thứ tự
 */
export function stripLeadingNumber(text) {
  return String(text || '').replace(/^\s*\d+\s*[.)]?\s+/, '').trim()
}

/**
 * Đọc text tiếng Anh bằng speech synthesis của trình duyệt.
 * Huỷ utterance đang đọc dở để bấm liên tục vẫn đọc lại sạch từ đầu.
 * Trả về false khi speech synthesis không khả dụng.
 *
 * Lưu ý khi test: jsdom KHÔNG có SpeechSynthesisUtterance, nên test phải mock nó
 * (cùng window.speechSynthesis) trước khi gọi hàm này, nếu không sẽ ném lỗi.
 *
 * @param {string} text - nội dung cần đọc
 * @param {{rate?: number}} [options] - rate đọc, mặc định 0.85 cho dễ nghe
 * @returns {boolean} true nếu đã gửi utterance, false nếu không đọc được
 */
export function speakEnglish(text, { rate = 0.85 } = {}) {
  if (!isSpeechAvailable() || !text) return false
  const synth = window.speechSynthesis
  synth.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'en-US'
  utterance.rate = rate
  synth.speak(utterance)
  return true
}
