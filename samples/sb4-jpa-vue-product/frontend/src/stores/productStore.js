import { defineStore } from 'pinia'
import { productService } from '@/services/productService'

export const useProductStore = defineStore('products', {
  state: () => ({ items: [], page: 0, totalElements: 0, totalPages: 0, loading: false, error: '' }),
  actions: {
    async refresh(page) {
      try {
        const result = await productService.list(page)
        this.items = result.content
        this.page = result.page
        this.totalElements = result.totalElements
        this.totalPages = result.totalPages
        this.error = ''
        return true
      } catch (error) {
        this.error = error.message
        return false
      }
    },
    async load(page = this.page) {
      this.loading = true
      this.error = ''
      try {
        return await this.refresh(page)
      } finally {
        this.loading = false
      }
    },
    async create(product) {
      this.loading = true
      this.error = ''
      try {
        await productService.create(product)
        await this.refresh(0)
      } finally { this.loading = false }
    },
    async update(id, product) {
      this.loading = true
      this.error = ''
      try {
        await productService.update(id, product)
        await this.refresh(this.page)
      } finally { this.loading = false }
    },
    async remove(id) {
      this.loading = true
      this.error = ''
      try {
        await productService.remove(id)
        const page = this.items.length === 1 && this.page > 0
          ? this.page - 1
          : this.page
        await this.refresh(page)
      } finally { this.loading = false }
    },
  },
})
