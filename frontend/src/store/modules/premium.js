import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import paymentService from '@/services/paymentService'

export const usePremiumStore = defineStore('premium', () => {
  const isPremium = ref(false)
  const premiumExpiry = ref(null)
  const premiumStatus = ref(null)
  const loading = ref(false)

  async function checkStatus() {
    try {
      const data = await paymentService.getStatus()
      isPremium.value = data.isPremium
      premiumExpiry.value = data.premiumExpiry
      premiumStatus.value = data
    } catch (e) {
      // audit-v5 fix: a network blip must NOT flip a paying user to the
      // free-tier UI. Keep the last known state; only an authoritative
      // response (above) changes it.
      console.warn('payment/status failed, keeping last premium state', e)
    }
  }

  async function createOrder(planType) {
    loading.value = true
    try {
      return await paymentService.createOrder(planType)
    } finally {
      loading.value = false
    }
  }

  return { isPremium, premiumExpiry, premiumStatus, loading, checkStatus, createOrder }
})
