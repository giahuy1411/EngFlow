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
    // audit-v11 F132: the highlighted word used `text-accent` (#8B5CF6), which measures
    // 4.23:1 on the cream/white page — under the 4.5:1 WCAG 1.4.3 requires for body text.
    // It now uses the AA-compliant `text-accent-ink` (#6D28D9 = 7.10:1).
    expect(wrapper.find('h1 .text-accent-ink').text()).toBe('nói')
    expect(wrapper.text()).toContain('Chọn một đề và bắt đầu.')
    expect(wrapper.text()).toContain('CTA')
    expect(wrapper.classes()).toContain('border-b-2')
  })
})
