import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import FlashcardGame from '@/views/luyentu/FlashcardGame.vue'
import deckService from '@/services/deckService'
import flashcardService from '@/services/flashcardService'

/**
 * audit-v12 F153: the flashcard drill was reduced to two buttons — "Quay lại" (back) and
 * "Tiếp theo" (continue). It no longer rates the word, so it sends no SM-2 quality; what it
 * must still do is record the study day exactly once per session, or a learner who only
 * reads flashcards loses their streak (that regression is what these tests guard).
 *
 * The vue-router mock follows QuizGame.test.js — useRoute() is imported by the component,
 * so it must be mocked at module level.
 */
vi.mock('@/services/deckService')
vi.mock('@/services/flashcardService')

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '10006' } }) }
})

const WORDS = [
  { id: 10020, word: 'determine', phonetic: '/dɪ/', meaning: 'xác định', example: 'ex', audioUrl: '', wordType: 'verb' },
  { id: 10021, word: 'analyse', phonetic: '/æ/', meaning: 'phân tích', example: 'ex2', audioUrl: '', wordType: 'noun' },
  { id: 10022, word: 'evident', phonetic: '/e/', meaning: 'hiển nhiên', example: 'ex3', audioUrl: '', wordType: 'adjective' }
]

function mountView() {
  return mount(FlashcardGame, {
    global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } }
  })
}

const buttonByText = (wrapper, text) => wrapper.findAll('button').find(b => b.text() === text)

describe('FlashcardGame.vue (back / continue)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    deckService.getDeckById.mockResolvedValue({ name: 'Oxford 3000', words: WORDS })
    flashcardService.recordStudy.mockResolvedValue({})
  })

  it('renders only the two navigation buttons — no rating buttons', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Quay lại')).toBeTruthy()
    expect(buttonByText(wrapper, 'Tiếp theo')).toBeTruthy()
    // The three rating buttons are gone.
    expect(buttonByText(wrapper, 'Lại')).toBeFalsy()
    expect(buttonByText(wrapper, 'Dễ')).toBeFalsy()
  })

  it('records the study day exactly once, even across several continues', async () => {
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    await flushPromises()

    expect(flashcardService.recordStudy).toHaveBeenCalledTimes(1)
  })

  it('advances to the next card on "Tiếp theo"', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('determine')

    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    expect(wrapper.text()).toContain('analyse')
  })

  it('goes back to the previous card on "Quay lại" without calling the API', async () => {
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    expect(wrapper.text()).toContain('analyse')

    await buttonByText(wrapper, 'Quay lại').trigger('click')
    expect(wrapper.text()).toContain('determine')
    // Going back is a pure view action — it must not record a second study day.
    expect(flashcardService.recordStudy).toHaveBeenCalledTimes(1)
  })

  it('clamps "Quay lại" at the first card', async () => {
    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Quay lại').trigger('click')
    await buttonByText(wrapper, 'Quay lại').trigger('click')

    expect(wrapper.text()).toContain('determine')
    expect(flashcardService.recordStudy).not.toHaveBeenCalled()
  })

  it('shows the completion state after the last card', async () => {
    const wrapper = mountView()
    await flushPromises()

    for (let i = 0; i < WORDS.length; i++) {
      await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    }

    expect(wrapper.text()).toContain('Hoàn thành')
  })
})
