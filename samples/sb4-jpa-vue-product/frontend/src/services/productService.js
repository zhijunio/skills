import { request } from './api'

export const productService = {
  list: (page = 0, size = 20) => request(`/products?page=${page}&size=${size}`),
  create: (product) => request('/products', { method: 'POST', body: JSON.stringify(product) }),
  update: (id, product) => request(`/products/${id}`, { method: 'PUT', body: JSON.stringify(product) }),
  remove: (id) => request(`/products/${id}`, { method: 'DELETE' }),
}
