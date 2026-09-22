import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import LessonExerciseTab from '@/views/lessons/LessonExerciseTab.vue'
import lessonService from '@/services/lessonService'

vi.mock('@/services/lessonService')
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '91920' } }) }
})

// Regression tests for audit-v13 F-13-01, revised after adversarial review.
//
// The first version of the fix turned every MULTIPLE_CHOICE row without usable
// options into a dead, unanswerable card. That is a mass regression: measured in
// the live DB (2026-09-22) there are 33,556 MULTIPLE_CHOICE rows, of which 32,814
// (97.8%) have options IS NULL and 0 have bare-letter options, plus 319 LISTENING
// rows without options. Those legacy rows were always answered by typing the answer
// and graded server-side, so the text fallback MUST remain.
//
// These tests pin the real behaviour for every shape found in the DB.
describe('LessonExerciseTab — F-13-01 exercise type must not silently change', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mountWith = async (exercises) => {
    lessonService.getExercises.mockResolvedValue(exercises)
    lessonService.getExercisesWithAnswers.mockResolvedValue(exercises)
    const wrapper = mount(LessonExerciseTab, {
      global: {
        plugins: [createPinia()],
        stubs: {
          MatchingExercise: true,
          // keep the real disabled binding so "card is dead" is assertable
          AppButton: { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' },
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  // --- the reported bug: real option text renders as choices ---

  it('renders real option text as selectable choices (row 650583 shape)', async () => {
    const wrapper = await mountWith([
      { id: 650583, lessonId: 91920, question: 'The sun ___ in the east.', exerciseType: 'MULTIPLE_CHOICE',
        options: '["rise","rises","rose","rising"]', correctAnswer: 'rises', difficulty: 'EASY' },
    ])
    const opts = wrapper.findAll('[data-testid="mc-option"]')
    expect(opts.length).toBe(4)
    expect(opts.map(o => o.text()).join(' ')).toContain('rises')
    expect(wrapper.find('input[type="text"]').exists()).toBe(false)
  })

  // --- the mass-regression guard: 32,814 rows with options IS NULL stay answerable ---

  it('keeps a text answer input for MULTIPLE_CHOICE with options IS NULL (32,814 legacy rows)', async () => {
    const wrapper = await mountWith([
      { id: 766031, lessonId: 91920, question: 'Fill in the blank', exerciseType: 'MULTIPLE_CHOICE',
        options: null, correctAnswer: 'Those are our sisters.', difficulty: 'EASY' },
    ])
    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)          // the learner still has a control
    expect(input.attributes('disabled')).toBeUndefined()
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(0)
  })

  it('keeps a text answer input for LISTENING with no options (319 legacy rows)', async () => {
    const wrapper = await mountWith([
      { id: 700001, lessonId: 91920, question: 'Listen and complete: The cat ___ on the mat.',
        exerciseType: 'LISTENING', options: null, correctAnswer: 'sits', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
  })

  it('keeps a text answer input for MULTIPLE_CHOICE with an empty options array (101 rows)', async () => {
    const wrapper = await mountWith([
      { id: 650578, lessonId: 91920, question: 'q', exerciseType: 'MULTIPLE_CHOICE',
        options: '[]', correctAnswer: 'A', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
  })

  // --- never render a blank option button ---

  it('does not render blank option buttons when options contain blanks', async () => {
    const wrapper = await mountWith([
      { id: 800001, lessonId: 91920, question: 'q', exerciseType: 'MULTIPLE_CHOICE',
        options: '["rise","rises","",""]', correctAnswer: 'rises', difficulty: 'EASY' },
    ])
    const opts = wrapper.findAll('[data-testid="mc-option"]')
    expect(opts.length).toBe(2)
    expect(opts.every(o => o.text().trim() !== '')).toBe(true)
  })

  // --- the original complaint: a letter-placeholder MC still shows the right type ---

  it('renders letter-placeholder options as choices (never a silent type change)', async () => {
    const wrapper = await mountWith([
      { id: 777434, lessonId: 91920, question: 'câu2', exerciseType: 'MULTIPLE_CHOICE',
        options: '["a","b","c","d"]', correctAnswer: 'd', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(4)
  })

  // --- "A - <content>" is REAL content, not a placeholder (real row 651717) ---

  it('renders "A - Salad" style options as choices, not a text input', async () => {
    const wrapper = await mountWith([
      { id: 651717, lessonId: 91920, question: 'What did she order?', exerciseType: 'MULTIPLE_CHOICE',
        options: '["A - Salad", "B - Cheeseburger", "C - Pizza", "D - Bread"]',
        correctAnswer: 'A - Salad', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(false)
    const opts = wrapper.findAll('[data-testid="mc-option"]')
    expect(opts.length).toBe(4)
    expect(opts.map(o => o.text()).join(' ')).toContain('Salad')
  })

  // --- LISTENING with letter-only options must not offer meaningless buttons ---

  it('keeps a text answer input for LISTENING whose options are bare letters (39 real rows)', async () => {
    const wrapper = await mountWith([
      { id: 650612, lessonId: 91920, question: 'Listen and choose', exerciseType: 'LISTENING',
        options: '["A", "B", "C", "D"]', correctAnswer: 'The show explores the lives of', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(0)
  })

  it('still offers choices for LISTENING with real options', async () => {
    const wrapper = await mountWith([
      { id: 900001, lessonId: 91920, question: 'Listen and choose', exerciseType: 'LISTENING',
        options: '["plays the guitar","plays the piano"]', correctAnswer: 'plays the guitar', difficulty: 'EASY' },
    ])
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(2)
  })

  // --- FILL_BLANK / TRANSLATION unchanged ---

  it('renders a text input for FILL_BLANK', async () => {
    const wrapper = await mountWith([
      { id: 766038, lessonId: 91920, question: 'Water ___ at 100 degrees.', exerciseType: 'FILL_BLANK',
        options: null, correctAnswer: 'boils', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(0)
  })

  it('renders a text input for FILL_BLANK whose options are letter placeholders (row 650579 shape)', async () => {
    const wrapper = await mountWith([
      { id: 650579, lessonId: 91920, question: 'q', exerciseType: 'FILL_BLANK',
        options: '["A", "B", "C", "D"]', correctAnswer: 'I have got a collection of', difficulty: 'EASY' },
    ])
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="mc-option"]').length).toBe(0)
  })
})
