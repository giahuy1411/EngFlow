import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { RouterLink, createMemoryHistory, createRouter } from 'vue-router'
import AppButton from '../AppButton.vue'

const mountButton = (props = {}, slots = {}) =>
  mount(AppButton, {
    props,
    slots: { default: 'Btn', ...slots },
    attachTo: document.body,
  })

describe('AppButton', () => {
  it('renders default button', () => {
    const wrapper = mountButton()
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.text()).toContain('Btn')
  })

  it('renders anchor when as=a', () => {
    const wrapper = mountButton({ as: 'a', href: '/lessons' })
    expect(wrapper.find('a').exists()).toBe(true)
    expect(wrapper.attributes('href')).toBe('/lessons')
  })

  it('applies variant and size classes', () => {
    const wrapper = mountButton({ variant: 'primary', size: 'lg' })
    expect(wrapper.classes()).toContain('app-btn')
    expect(wrapper.classes()).toContain('app-btn--primary')
    expect(wrapper.classes()).toContain('app-btn--lg')
  })

  it('supports all variants without throwing', () => {
    ;['primary', 'secondary', 'tertiary', 'danger', 'ghost'].forEach((variant) => {
      const wrapper = mountButton({ variant })
      expect(wrapper.find('button').exists()).toBe(true)
    })
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mountButton({ disabled: true })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  it('renders a supplied component through as', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        { path: '/lessons', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(AppButton, {
      props: { as: 'router-link', to: '/lessons' },
      slots: { default: 'Btn' },
      global: { plugins: [router] },
    })
    expect(wrapper.find('a').exists()).toBe(true)
    expect(wrapper.findComponent(RouterLink).props('to')).toBe('/lessons')
  })

  it('emits click when enabled', async () => {
    const wrapper = mountButton()
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click when loading', async () => {
    const wrapper = mountButton({ loading: true })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  it('keeps label visible while loading', () => {
    const wrapper = mountButton({ loading: true })
    expect(wrapper.text()).toContain('Btn')
  })

  it('uses type=button by default', () => {
    const wrapper = mountButton()
    expect(wrapper.find('button').attributes('type')).toBe('button')
  })

  it('forwards aria-label for icon-only usage', () => {
    const wrapper = mountButton({ 'aria-label': 'Dong' })
    expect(wrapper.find('button').attributes('aria-label')).toBe('Dong')
  })
})
