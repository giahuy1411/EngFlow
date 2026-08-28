import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import QuizGame from '@/views/luyentu/QuizGame.vue'
import gameService from '@/services/gameService'

vi.mock('@/services/gameService')

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '42' } }) }
})

// Regression test for the QuizGame API-contract bug: the backend returns
// { sessionId, data: [{ vocabId, word, pronunciation, answer, options }] }
// but the component previously read data.questions and q.meaning, rendering 0/0.
describe('QuizGame API contract', () => {
  const apiResponse = {
    sessionId: 'game:session:abc-123',
    name: 'Travel deck',
    data: [
      { vocabId: 10, word: 'airport', pronunciation: '/ˈeə.pɔːt/', answer: 'sân bay', options: ['sân bay', 'bến xe', 'nhà ga'] },
      { vocabId: 11, word: 'ticket', pronunciation: '/ˈtɪk.ɪt/', answer: 'vé', options: ['vé', 'hộ chiếu', 'va li'] },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    gameService.startQuiz.mockResolvedValue(apiResponse)
    gameService.submit.mockResolvedValue({})
  })

  it('renders questions from the data.data array (not data.questions)', async () => {
    const wrapper = mount(QuizGame, {
      global: { stubs: { 'router-link': { template: '<a><slot/></a>' } } },
    })
    await flushPromises()

    // Two answer option buttons should be present for the first question.
    const optionButtons = wrapper.findAll('button')
    expect(optionButtons.length).toBe(3)
    expect(optionButtons[0].text()).toContain('sân bay')
  })

  it('shows the score counter before answering', async () => {
    const wrapper = mount(QuizGame)
    await flushPromises()
    expect(wrapper.text()).toContain('0/0')
  })

  it('advances score and question when the correct answer is selected', async () => {
    const wrapper = mount(QuizGame)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    // First option is the correct answer ('sân bay').
    await buttons[0].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('1/1')
  })

  it('maps q.meaning as a fallback answer for legacy payloads', async () => {
    gameService.startQuiz.mockResolvedValue({
      sessionId: 's1',
      name: 'Legacy',
      questions: [{ vocabId: 1, word: 'old', meaning: 'cũ', options: ['cũ', 'mới'] }],
    })
    const wrapper = mount(QuizGame)
    await flushPromises()
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBe(2)
    expect(buttons[0].text()).toContain('cũ')
  })
})
