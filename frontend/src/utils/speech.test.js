import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { isSpeechAvailable, blankOutForSpeech, stripLeadingNumber, speakEnglish } from './speech'

describe('speech utility', () => {
  describe('blankOutForSpeech', () => {
    it('collapses underscore blank runs to a spoken pause', () => {
      expect(blankOutForSpeech('You ________ from Australia.')).toBe('You ... from Australia.')
    })

    it('collapses inline-code blanks too', () => {
      expect(blankOutForSpeech('She `____` a teacher.')).toBe('She ... a teacher.')
    })

    it('normalizes multiple blanks in one sentence', () => {
      const out = blankOutForSpeech('Tom ____ and Betty ________ in my class.')
      expect(out).toBe('Tom ... and Betty ... in my class.')
      expect(out).not.toContain('_')
    })

    it('leaves normal sentences untouched', () => {
      expect(blankOutForSpeech('My English teacher is Mr Simpson.')).toBe('My English teacher is Mr Simpson.')
    })

    it('handles empty input', () => {
      expect(blankOutForSpeech('')).toBe('')
      expect(blankOutForSpeech(null)).toBe('')
    })
  })

  describe('stripLeadingNumber', () => {
    it('removes a leading exercise index followed by space', () => {
      expect(stripLeadingNumber('6 You ________ from Australia.')).toBe('You ________ from Australia.')
    })

    it('removes index with a dot', () => {
      expect(stripLeadingNumber('12. She is a teacher.')).toBe('She is a teacher.')
    })

    it('keeps sentences that do not start with a number', () => {
      expect(stripLeadingNumber('You are from Australia.')).toBe('You are from Australia.')
    })
  })

  describe('speakEnglish', () => {
    let speakSpy
    let cancelSpy

    beforeEach(() => {
      speakSpy = vi.fn()
      cancelSpy = vi.fn()
      window.speechSynthesis = { speak: speakSpy, cancel: cancelSpy }
      // jsdom has no SpeechSynthesisUtterance constructor
      window.SpeechSynthesisUtterance = class {
        constructor(text) { this.text = text }
      }
    })

    afterEach(() => {
      delete window.speechSynthesis
      delete window.SpeechSynthesisUtterance
    })

    it('speaks en-US at a slowed rate and cancels previous utterance', () => {
      const ok = speakEnglish('You ... from Australia.')

      expect(ok).toBe(true)
      expect(cancelSpy).toHaveBeenCalledOnce()
      expect(speakSpy).toHaveBeenCalledOnce()
      const utterance = speakSpy.mock.calls[0][0]
      expect(utterance.text).toBe('You ... from Australia.')
      expect(utterance.lang).toBe('en-US')
      expect(utterance.rate).toBe(0.85)
    })

    it('supports a custom rate', () => {
      speakEnglish('Hello.', { rate: 1 })
      expect(speakSpy.mock.calls[0][0].rate).toBe(1)
    })

    it('returns false for empty text without speaking', () => {
      expect(speakEnglish('')).toBe(false)
      expect(speakSpy).not.toHaveBeenCalled()
    })
  })

  describe('isSpeechAvailable', () => {
    afterEach(() => {
      if (!('speechSynthesis' in window)) delete window.speechSynthesis
    })

    it('is true when speechSynthesis exists', () => {
      window.speechSynthesis = window.speechSynthesis || { speak: vi.fn(), cancel: vi.fn() }
      expect(isSpeechAvailable()).toBe(true)
    })

    it('is false when speechSynthesis is missing', () => {
      const original = window.speechSynthesis
      delete window.speechSynthesis
      expect(isSpeechAvailable()).toBe(false)
      if (original) window.speechSynthesis = original
    })
  })
})
