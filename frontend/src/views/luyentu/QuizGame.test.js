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
// { sessionId, data: [{ vocabId, word, pronunciation, options }] } where answer is server-only
// answer is server-only, component uses options only.
describe('QuizGame API contract', () => {
  const apiResponse = {
    sessionId: 'game:session:abc-123',
    name: 'Travel deck',
    data: [
      { vocabId: 10, word: 'airport', pronunciation: '/ˈeə.pɔːt/', options: ['sân bay', 'bến xe', 'nhà ga'] },
      { vocabId: 11, word: 'ticket', pronunciation: '/ˈtɪk.ɪt/', options: ['vé', 'hộ chiếu', 'va li'] },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    gameService.startQuiz.mockResolvedValue(apiResponse)
    gameService.submit.mockResolvedValue({ correctAnswers: 1 })
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

  it('advances to next question and records answer without client-side scoring', async () => {
    const wrapper = mount(QuizGame)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('0/1')
    expect(gameService.submit).not.toHaveBeenCalled()
  })

  it('renders legacy payload via questions fallback', async () => {
    gameService.startQuiz.mockResolvedValue({
      sessionId: 's1',
      name: 'Legacy',
      questions: [{ vocabId: 1, word: 'old', options: ['cũ', 'mới'] }],
    })
    const wrapper = mount(QuizGame)
    await flushPromises()
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBe(2)
    expect(buttons[0].text()).toContain('cũ')
  })
})