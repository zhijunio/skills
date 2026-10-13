import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { productService } from '@/services/productService'
import { useProductStore } from './productStore'

vi.mock('@/services/productService', () => ({
  productService: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}))

describe('product store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('reports a refresh error without turning a successful create into a failed create', async () => {
    productService.create.mockResolvedValue({ id: 'product-1' })
    productService.list.mockRejectedValue(new Error('Server unavailable'))
    const store = useProductStore()

    await expect(store.create({ name: 'Lamp' })).resolves.toBeUndefined()

    expect(store.error).toBe('Server unavailable')
    expect(store.loading).toBe(false)
  })

  it('keeps a failed initial load in store state without an unhandled rejection', async () => {
    productService.list.mockRejectedValue(new Error('Server unavailable'))
    const store = useProductStore()

    await expect(store.load()).resolves.toBe(false)

    expect(store.error).toBe('Server unavailable')
    expect(store.loading).toBe(false)
  })
})
