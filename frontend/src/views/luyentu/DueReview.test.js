import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DueReview from '@/views/luyentu/DueReview.vue'
import srsService from '@/services/srsService'
import deckService from '@/services/deckService'

/**
 * audit-v12 Item A: the due-words review screen. `GET /api/srs/due/{deckId}` had no caller
 * before this view. These tests pin the behaviour that matters: the card renders the due
 * words, and each rating button sends the SM-2 quality the backend expects.
 *
 * The field mapping (vocabId -> id, definitionVi -> meaning, …) is covered in
 * src/services/srsService.test.js; here the service is mocked and already returns card shape.
 */
vi.mock('@/services/srsService')
vi.mock('@/services/deckService')

// Same pattern as QuizGame.test.js — useRoute() is imported by the component, so it must be
// mocked at the module level rather than injected via global.mocks.
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '10006' } }) }
})

const CARD = {
  id: 10020, word: 'determine', phonetic: '/dɪ/', meaning: 'xác định',
  definitionEn: '', example: 'ex', audioUrl: '', wordType: 'verb', level: 0
}

function mountView() {
  return mount(DueReview, {
    global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } }
  })
}

const buttonByText = (wrapper, text) => wrapper.findAll('button').find(b => b.text() === text)

describe('DueReview.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    deckService.getDeckById.mockResolvedValue({ name: 'Academic Word List' })
  })

  it('renders the due words and sends quality 1 for "Lại"', async () => {
    srsService.getDueWords.mockResolvedValue([CARD])
    srsService.review.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    // The card front shows the word as stored; the uppercase look is CSS-only.
    expect(wrapper.text()).toContain('determine')
    expect(srsService.getDueWords).toHaveBeenCalledWith('10006')

    await buttonByText(wrapper, 'Lại').trigger('click')
    expect(srsService.review).toHaveBeenCalledWith(10020, 1)
  })

  it('sends quality 5 for "Dễ" and 4 for "Tiếp theo" — the two must differ', async () => {
    srsService.getDueWords.mockResolvedValue([
      CARD,
      { ...CARD, id: 10021, word: 'analyse', wordType: 'noun' }
    ])
    srsService.review.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Dễ').trigger('click')
    expect(srsService.review).toHaveBeenLastCalledWith(10020, 5)

    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    expect(srsService.review).toHaveBeenLastCalledWith(10021, 4)
  })

  it('shows the empty state when nothing is due', async () => {
    srsService.getDueWords.mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Không có từ nào đến hạn')
    expect(srsService.review).not.toHaveBeenCalled()
  })

  it('shows the completion state after the last card', async () => {
    srsService.getDueWords.mockResolvedValue([CARD])
    srsService.review.mockResolvedValue({})

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Tiếp theo').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Đã ôn xong 1 từ')
  })
})
