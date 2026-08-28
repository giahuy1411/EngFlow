import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Pagination from './Pagination.vue'

describe('Pagination', () => {
  const mountPagination = (props = {}) => mount(Pagination, {
    props: {
      currentPage: 3,
      totalPages: 8,
      totalItems: 76,
      pageSize: 10,
      itemLabel: 'mục',
      ...props
    }
  })

  it('renders requested navigation controls without adjacent page numbers', () => {
    const wrapper = mountPagination()

    expect(wrapper.text()).toContain('Trước')
    expect(wrapper.find('button[aria-label="Trang đầu tiên"]').text()).toBe('1')
    expect(wrapper.find('button[aria-label^="Trang cuối cùng"]').text()).toBe('8')
    expect(wrapper.text()).toContain('Sau')
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.findAll('button').some(button => button.text() === '4')).toBe(false)
  })

  it('uses the rounded candy pagination visual contract', () => {
    const wrapper = mountPagination()
    const selector = wrapper.find('.pagination-page-selector')

    expect(selector.exists()).toBe(true)
    expect(wrapper.find('.pagination-page-input').exists()).toBe(true)
    expect(wrapper.find('.pagination-total-pages').text()).toBe('8')
  })

  it('hides the summary in compact mode', () => {
    const wrapper = mountPagination({ showSummary: false })

    expect(wrapper.text()).not.toContain('Hiển thị')
    expect(wrapper.find('.pagination-page-selector').exists()).toBe(true)
  })

  it('emits page change from input on Enter', async () => {
    const wrapper = mountPagination()
    const input = wrapper.find('input[type="text"]')

    await input.setValue('6')
    await input.trigger('keydown.enter')

    expect(wrapper.emitted('page-change')?.[0]).toEqual([6])
  })

  it('ignores invalid input and keeps the current page', async () => {
    const wrapper = mountPagination()
    const input = wrapper.find('input[type="text"]')

    await input.setValue('99')
    await input.trigger('keydown.enter')

    expect(wrapper.emitted('page-change')).toBeUndefined()
    expect(input.element.value).toBe('3')
  })
})
