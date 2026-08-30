/**
 * Client-side preview parser for SRT/VTT transcripts — mirrors
 * backend SubtitleParser (src/main/java/com/datn/engflow/service/SubtitleParser.java).
 * Used only for admin preview/validation; the backend re-parses on save.
 */
const CUE_LINE = /^\s*(?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}\s*-->\s*(?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{1,3}/

function toSeconds(match) {
  const hours = match[1] ? parseInt(match[1], 10) : 0
  const minutes = parseInt(match[2], 10)
  const seconds = parseInt(match[3], 10)
  const fraction = (match[4] || '0').padEnd(3, '0').slice(0, 3)
  return hours * 3600 + minutes * 60 + seconds + parseInt(fraction, 10) / 1000
}

const TIMESTAMP = /(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})/g

function cleanText(text) {
  return text
    .replace(/<[^>]+>/g, '')
    .replace(/\{[^}]*\}/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim()
}

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
