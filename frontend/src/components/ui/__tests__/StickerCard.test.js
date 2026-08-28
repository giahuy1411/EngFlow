import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StickerCard from '../StickerCard.vue'

describe('StickerCard', () => {
  it('renders content in default slot', () => {
    const wrapper = mount(StickerCard, {
      slots: { default: 'Nội dung thẻ' },
    })
    expect(wrapper.text()).toContain('Nội dung thẻ')
  })

  it('renders div by default', () => {
    const wrapper = mount(StickerCard)
    expect(wrapper.find('div').exists()).toBe(true)
  })

  it('renders section when as=section', () => {
    const wrapper = mount(StickerCard, { props: { as: 'section' } })
    expect(wrapper.find('section').exists()).toBe(true)
  })

  it('applies interactive class when interactive', () => {
    const wrapper = mount(StickerCard, { props: { interactive: true } })
    expect(wrapper.classes()).toContain('app-card--interactive')
  })

  it('applies featured class when featured', () => {
    const wrapper = mount(StickerCard, { props: { featured: true } })
    expect(wrapper.classes()).toContain('app-card--featured')
  })

  it('renders footer slot', () => {
    const wrapper = mount(StickerCard, {
      slots: { footer: 'Footer' },
    })
    expect(wrapper.text()).toContain('Footer')
  })
})
