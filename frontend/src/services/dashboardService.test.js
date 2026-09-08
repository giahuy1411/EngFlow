import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('./api', () => ({
  default: {
    get: vi.fn()
  }
}))

import api from './api'
import dashboardService from './dashboardService'

describe('dashboardService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('calls GET /api/dashboard/stats and unwraps .data', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        completedLessons: 7,
        totalLessons: 30,
        totalPoints: 120,
        currentStreak: 3
      }
    })

    const stats = await dashboardService.getStats()

    expect(api.get).toHaveBeenCalledWith('/api/dashboard/stats')
    expect(stats).toEqual({
      completedLessons: 7,
      totalLessons: 30,
      totalPoints: 120,
      currentStreak: 3
    })
  })

  it('propagates request failure', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'))

    await expect(dashboardService.getStats()).rejects.toThrow('Network error')
  })
})
