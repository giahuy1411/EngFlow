import { marked } from 'marked'
import DOMPurify from 'dompurify'
import './sanitize-a11y'

// Cấu hình tuỳ chọn cho marked nếu cần
marked.setOptions({
  gfm: true,
  breaks: true,
})

/**
 * Dọn rác text do các trình soạn thảo HTML đời cũ để lại.
 */
function cleanLegacyText(text) {
  let cleaned = text;
  // Bỏ dòng chữ "Audio Player"
  cleaned = cleaned.replace(/Audio Player\s*/gi, '');
  // Bỏ câu hướng dẫn chỉnh âm lượng
  cleaned = cleaned.replace(/Use Up\/Down Arrow keys to increase or decrease volume\./gi, '');
  // Bỏ các span thời gian 00:00 còn sót
  cleaned = cleaned.replace(/00:00/g, '');
  return cleaned;
}

/**
 * Parse text markdown thành HTML đã khử trùng (sanitize).
 * @param {string} text - Nội dung markdown.
 * @returns {string} Chuỗi HTML an toàn.
 */
export function parseMarkdown(text) {
  if (!text) return ''

  const cleanedText = cleanLegacyText(text)
  const rawHtml = marked.parse(cleanedText)

  // Cấu hình DOMPurify cho phép một số thẻ cụ thể
  return DOMPurify.sanitize(rawHtml, {
    ADD_TAGS: ['iframe', 'audio', 'video', 'source', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ADD_ATTR: ['controls', 'src', 'type', 'allow', 'allowfullscreen', 'colspan', 'rowspan']
  })
}

/**
 * Khử trùng một field plain-text (ví dụ nghĩa của từ, câu ví dụ, đề bài).
 * Dùng cho các field nội dung ngắn có thể lẫn HTML rác từ dữ liệu do AI sinh
 * hoặc dữ liệu cũ. Giữ định dạng inline (bold/italic/link) nhưng gỡ các thẻ
 * nguy hiểm và KHÔNG bọc kết quả trong thẻ block.
 * Nhờ vậy tránh việc thẻ HTML thô bị hiện ra thành chữ.
 * @param {string} text - Text/HTML thô.
 * @returns {string} Chuỗi HTML an toàn (rỗng khi đầu vào rỗng).
 */
export function sanitizeText(text) {
  if (!text) return ''
  return DOMPurify.sanitize(text, {
    ALLOWED_TAGS: ['b', 'strong', 'i', 'em', 'u', 's', 'span', 'br', 'a', 'code', 'sub', 'sup', 'small', 'mark'],
    ALLOWED_ATTR: ['href', 'title', 'target', 'rel']
  })
}
