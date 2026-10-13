import { afterEach, describe, expect, it, vi } from 'vitest'
import { productService } from './productService'

afterEach(() => vi.unstubAllGlobals())

describe('productService', () => {
  it('loads a page of products from the API', async () => {
    const payload = { content: [], page: 0, totalElements: 0, totalPages: 0 }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => payload,
    }))

    await expect(productService.list(2, 10)).resolves.toEqual(payload)
    expect(fetch).toHaveBeenCalledWith('/api/products?page=2&size=10', expect.any(Object))
  })

  it('preserves validation details from Problem Details responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      headers: { get: () => 'application/problem+json' },
      json: async () => ({ title: 'Validation failed', detail: 'Request validation failed.', violations: [{ field: 'sku', message: 'invalid' }] }),
    }))

    await expect(productService.create({ sku: 'bad sku' })).rejects.toMatchObject({
      message: 'Request validation failed.',
      status: 400,
      problem: { violations: [{ field: 'sku', message: 'invalid' }] },
    })
  })
})
