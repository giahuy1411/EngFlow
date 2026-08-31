/**
 * Browser speech-synthesis fallback for LISTENING exercises without audio files.
 *
 * Context: 89/89 listening exercises missing audio are fill-in-the-blank sentences
 * ("You ________ from Australia."). Reading them aloud as-is would leak the answer,
 * so blanked questions must be spoken with the blank collapsed to a pause.
 *
 * Pattern follows ListeningGame.vue (Web Speech API, en-US, slowed rate) so the
 * whole codebase uses one speech behavior.
 */

/** True when the current browser exposes the Web Speech API. */
export function isSpeechAvailable() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * Collapse markdown fill-in-the-blank underscores to a spoken pause.
 * Handles runs of 2+ underscores (4-8 in DB data) and inline-code blanks.
 * "You ________ from Australia." -> "You ... from Australia."
 */
export function blankOutForSpeech(md) {
  if (!md) return ''
  return String(md)
    .replace(/`[^`]*`/g, ' ... ') // inline-code blanks (`____`)
    .replace(/_{2,}/g, ' ... ') // underscore runs (________)
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * Strip leading exercise numbering like "1 " or "12." from a question so the
 * voice does not read the index ("6 You ________ ..." -> "You ...").
 */
export function stripLeadingNumber(text) {
  return String(text || '').replace(/^\s*\d+\s*[.)]?\s+/, '').trim()
}

/**
 * Speak English text via the browser's speech synthesis.
 * Cancels any utterance still in flight so repeated clicks restart cleanly.
 * Returns false when speech synthesis is unavailable.
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
