<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useProductStore } from '@/stores/productStore'

const products = useProductStore()
const query = ref('')
const formError = ref('')
const editingId = ref(null)
const productName = ref(null)
const form = reactive({ name: '', sku: '', price: '', quantity: 0 })

const filteredProducts = computed(() => {
  const normalized = query.value.trim().toLowerCase()
  if (!normalized) return products.items
  return products.items.filter((product) =>
    [product.name, product.sku].some((value) => value.toLowerCase().includes(normalized)),
  )
})
const totalUnits = computed(() => products.items.reduce((total, item) => total + item.quantity, 0))
const lowStock = computed(() => products.items.filter((item) => item.quantity < 5).length)
const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function resetForm() {
  editingId.value = null
  form.name = ''
  form.sku = ''
  form.price = ''
  form.quantity = 0
  formError.value = ''
}

function editProduct(product) {
  editingId.value = product.id
  form.name = product.name
  form.sku = product.sku
  form.price = Number(product.price)
  form.quantity = product.quantity
  formError.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function saveProduct() {
  formError.value = ''
  const payload = {
    name: form.name.trim(),
    sku: form.sku.trim().toUpperCase(),
    price: Number(form.price),
    quantity: Number(form.quantity),
  }
  try {
    if (editingId.value) await products.update(editingId.value, payload)
    else await products.create(payload)
    resetForm()
  } catch (error) {
    formError.value = error.problem?.violations?.map((item) => `${item.field}: ${item.message}`).join(' · ')
      || error.message
  }
}

async function removeProduct(product) {
  if (!window.confirm(`Delete ${product.name}? This cannot be undone.`)) return
  try {
    await products.remove(product.id)
  } catch (error) {
    products.error = error.message
  }
}

onMounted(() => products.load())
</script>

<template>
  <main class="page-wrap">
    <section class="page-heading">
      <div>
        <p class="eyebrow">INVENTORY / OVERVIEW</p>
        <h1>Products</h1>
        <p class="subheading">Keep your catalog and stock levels up to date.</p>
      </div>
      <button class="button button-primary" type="button" @click="productName?.focus()">
        <span aria-hidden="true">＋</span> Add product
      </button>
    </section>

    <section class="stats-grid" aria-label="Inventory summary">
      <article class="stat-card"><span class="stat-icon icon-violet" aria-hidden="true">▦</span><div><p>Products on this page</p><strong>{{ products.items.length }}</strong></div></article>
      <article class="stat-card"><span class="stat-icon icon-blue" aria-hidden="true">◫</span><div><p>Total units on this page</p><strong>{{ totalUnits.toLocaleString() }}</strong></div></article>
      <article class="stat-card"><span class="stat-icon icon-amber" aria-hidden="true">!</span><div><p>Low stock items</p><strong>{{ lowStock }}</strong></div></article>
      <article class="stat-card"><span class="stat-icon icon-green" aria-hidden="true">↗</span><div><p>Catalog size</p><strong>{{ products.totalElements }}</strong></div></article>
    </section>

    <div v-if="products.error" class="notice notice-error" role="alert">
      <span>{{ products.error }}</span><button class="text-button" type="button" @click="products.load()">Retry</button>
    </div>

    <div class="content-grid">
      <section class="panel form-panel" aria-labelledby="form-heading">
        <div class="panel-heading">
          <div><p class="eyebrow">CATALOG</p><h2 id="form-heading">{{ editingId ? 'Edit product' : 'Add a product' }}</h2></div>
          <span class="panel-symbol" aria-hidden="true">✳</span>
        </div>
        <form class="product-form" @submit.prevent="saveProduct">
          <label for="product-name">Product name</label>
          <input id="product-name" ref="productName" v-model="form.name" maxlength="120" required placeholder="e.g. Studio desk lamp">
          <label for="product-sku">SKU</label>
          <input id="product-sku" v-model="form.sku" maxlength="32" required placeholder="e.g. LAMP-001" autocomplete="off">
          <p class="field-hint">2–32 uppercase letters, numbers, or hyphens.</p>
          <div class="field-row">
            <div><label for="product-price">Unit price</label><div class="input-prefix"><span>$</span><input id="product-price" v-model.number="form.price" type="number" min="0" max="9999999999.99" step="0.01" required placeholder="0.00"></div></div>
            <div><label for="product-quantity">In stock</label><input id="product-quantity" v-model.number="form.quantity" type="number" min="0" step="1" required></div>
          </div>
          <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
          <div class="form-actions">
            <button v-if="editingId" class="button button-quiet" type="button" @click="resetForm">Cancel</button>
            <button class="button button-primary submit-button" type="submit" :disabled="products.loading">
              {{ products.loading ? 'Saving…' : editingId ? 'Save changes' : 'Create product' }}
            </button>
          </div>
        </form>
      </section>

      <section class="panel inventory-panel" aria-labelledby="inventory-heading">
        <div class="panel-heading inventory-heading">
          <div><p class="eyebrow">YOUR CATALOG</p><h2 id="inventory-heading">All products <span class="count-pill">{{ products.totalElements }}</span></h2></div>
          <label class="search-box"><span class="sr-only">Filter products on this page</span><span aria-hidden="true">⌕</span><input v-model="query" type="search" placeholder="Filter this page…"></label>
        </div>
        <div v-if="products.loading && !products.items.length" class="empty-state" aria-live="polite"><span class="loading-orbit" aria-hidden="true"></span><p>Loading your catalog…</p></div>
        <div v-else-if="!filteredProducts.length" class="empty-state">
          <span class="empty-icon" aria-hidden="true">▤</span>
          <h3>{{ query ? 'No matching products' : 'Your catalog is ready' }}</h3>
          <p>{{ query ? 'Try another name or SKU.' : 'Add your first product to start tracking inventory.' }}</p>
          <button v-if="!query" class="button button-secondary" type="button" @click="productName?.focus()">Add first product</button>
        </div>
        <div v-else class="table-scroll">
          <table>
            <thead><tr><th scope="col">Product</th><th scope="col">SKU</th><th scope="col">Price</th><th scope="col">Stock</th><th scope="col"><span class="sr-only">Actions</span></th></tr></thead>
            <tbody>
              <tr v-for="product in filteredProducts" :key="product.id">
                <td><span class="product-avatar">{{ product.name.slice(0, 1).toUpperCase() }}</span><span class="product-name">{{ product.name }}</span></td>
                <td><code>{{ product.sku }}</code></td><td class="price-cell">{{ money.format(Number(product.price)) }}</td>
                <td><span class="stock-badge" :class="product.quantity < 5 ? 'stock-low' : 'stock-ok'">{{ product.quantity }} {{ product.quantity === 1 ? 'unit' : 'units' }}</span></td>
                <td class="row-actions"><button class="icon-button" type="button" :disabled="products.loading" :aria-label="`Edit ${product.name}`" @click="editProduct(product)">✎</button><button class="icon-button icon-delete" type="button" :disabled="products.loading" :aria-label="`Delete ${product.name}`" @click="removeProduct(product)">×</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer v-if="products.totalPages > 1" class="pagination">
          <span>Page {{ products.page + 1 }} of {{ products.totalPages }}</span>
          <div><button class="button button-quiet" type="button" :disabled="products.page === 0 || products.loading" @click="products.load(products.page - 1)">Previous</button><button class="button button-quiet" type="button" :disabled="products.page + 1 >= products.totalPages || products.loading" @click="products.load(products.page + 1)">Next</button></div>
        </footer>
      </section>
    </div>
    <p class="page-footnote">Prices are shown in USD · Search filters the current page</p>
  </main>
</template>
