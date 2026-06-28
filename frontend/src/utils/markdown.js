import { marked } from 'marked'
import DOMPurify from 'dompurify'

// Configure marked options if needed
marked.setOptions({
  gfm: true,
  breaks: true,
})

/**
 * Cleans up garbage text from legacy HTML editors.
 */
function cleanLegacyText(text) {
  let cleaned = text;
  // Remove "Audio Player" text
  cleaned = cleaned.replace(/Audio Player\s*/gi, '');
  // Remove the volume instruction text
  cleaned = cleaned.replace(/Use Up\/Down Arrow keys to increase or decrease volume\./gi, '');
  // Remove dangling 00:00 time spans
  cleaned = cleaned.replace(/00:00/g, '');
  return cleaned;
}

/**
 * Parses markdown text to sanitized HTML.
 * @param {string} text - The markdown text.
 * @returns {string} Safe HTML string.
 */
export function parseMarkdown(text) {
  if (!text) return ''
  
  const cleanedText = cleanLegacyText(text)
  const rawHtml = marked.parse(cleanedText)
  
  // Configure DOMPurify to allow specific tags
  return DOMPurify.sanitize(rawHtml, {
    ADD_TAGS: ['iframe', 'audio', 'video', 'source', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ADD_ATTR: ['controls', 'src', 'type', 'allow', 'allowfullscreen', 'colspan', 'rowspan']
  })
}
