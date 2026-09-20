import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SquiggleDivider from '../SquiggleDivider.vue'
import DecoConfetti from '../DecoConfetti.vue'

/**
 * audit-v10 regression tests. Both components declared props they never used, so
 * passing a value had no effect and nothing failed. These tests assert the props
 * reach the rendered style, which is what the call sites actually rely on.
 */

describe('SquiggleDivider', () => {
  it('applies the color prop to the background colour', () => {
    const wrapper = mount(SquiggleDivider, { props: { color: 'var(--geo-accent)' } })
    expect(wrapper.attributes('style')).toContain('--deco-squiggle-color: var(--geo-accent)')
  })

  it('applies the height prop to the height custom property', () => {
    const wrapper = mount(SquiggleDivider, { props: { height: '24px' } })
    expect(wrapper.attributes('style')).toContain('--deco-height: 24px')
  })

  it('never writes a raw CSS declaration into a custom property', () => {
    // The old implementation set --deco-squiggle-color to the string
    // "color:var(--geo-fg)", which is not a usable value anywhere.
    const wrapper = mount(SquiggleDivider)
    expect(wrapper.attributes('style')).not.toMatch(/--deco-squiggle-color:\s*color:/)
  })

  it('is hidden from assistive technology', () => {
    const wrapper = mount(SquiggleDivider)
    expect(wrapper.attributes('aria-hidden')).toBe('true')
  })
})

describe('DecoConfetti', () => {
  it('renders the requested size in pixels', () => {
    const small = mount(DecoConfetti, { props: { size: 'sm' } })
    const large = mount(DecoConfetti, { props: { size: 'xl' } })
    expect(small.find('svg').attributes('width')).toBe('14')
    expect(large.find('svg').attributes('width')).toBe('32')
  })

  it('gives different sizes different pixel dimensions', () => {
    const sizes = ['sm', 'md', 'lg', 'xl'].map(
      (size) => mount(DecoConfetti, { props: { size } }).find('svg').attributes('width'),
    )
    expect(new Set(sizes).size).toBe(sizes.length)
  })

  it('applies an explicit color prop', () => {
    const wrapper = mount(DecoConfetti, { props: { color: 'var(--geo-quaternary)' } })
    expect(wrapper.attributes('style')).toContain('--deco-confetti-color: var(--geo-quaternary)')
  })

  it('falls back to a palette colour per size when no color is given', () => {
    const wrapper = mount(DecoConfetti, { props: { size: 'lg' } })
    expect(wrapper.attributes('style')).toContain('var(--geo-accent)')
  })

  it('renders the requested shape', () => {
    expect(mount(DecoConfetti, { props: { kind: 'square' } }).find('rect').exists()).toBe(true)
    expect(mount(DecoConfetti, { props: { kind: 'triangle' } }).find('path').exists()).toBe(true)
    expect(mount(DecoConfetti, { props: { kind: 'circle' } }).find('circle').exists()).toBe(true)
  })

  it('is hidden from assistive technology', () => {
    const wrapper = mount(DecoConfetti)
    expect(wrapper.attributes('aria-hidden')).toBe('true')
  })
})
