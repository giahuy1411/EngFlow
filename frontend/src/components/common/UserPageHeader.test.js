import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import UserPageHeader from './UserPageHeader.vue'

describe('UserPageHeader', () => {
  it('renders lesson-style title, highlighted final word, actions, and optional divider', () => {
    const wrapper = mount(UserPageHeader, {
      props: {
        title: 'Luyện nói',
        subtitle: 'Chọn một đề và bắt đầu.',
        divided: true
      },
      slots: {
        actions: '<button>CTA</button>'
      }
    })

    const titleText = wrapper.find('h1').text().replace(/\s+/g, ' ').trim()

    expect(titleText).toBe('Luyện nói')
    expect(wrapper.find('h1 .text-accent').text()).toBe('nói')
    expect(wrapper.text()).toContain('Chọn một đề và bắt đầu.')
    expect(wrapper.text()).toContain('CTA')
    expect(wrapper.classes()).toContain('border-b-2')
  })
})
