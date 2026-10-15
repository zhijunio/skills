# Front-End Development with Vue.js for Spring Boot Applications

## Contents
- [Overview](#overview)
- [Versions (managed via `versions.json`)](#versions-managed-via-versionsjson)
- [Spring Boot integration](#spring-boot-integration)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [Vue.js Application Structure](#vuejs-application-structure)
- [Best Practices](#best-practices)
- [Browser DevTools](#browser-devtools)
- [References](#references)

## Overview
This guide covers creating front-end applications for Spring Boot using **Vue.js 3** and **Vite 8**, with hot reload during development and optimized production builds integrated into the Spring Boot package.

## Versions (managed via `versions.json`)

> Regenerate this table with `node scripts/sync-versions-in-docs.mjs` after bumping `versions.json`.

<!-- versions:start -->
| Tool | Version |
|------|---------|
| Node.js | 24.21.0 |
| npm | 11.20.0 |
| Vue.js | 3.x |
| Vite | 8.x |
| Pinia | 4.x |
| Vue Router | 5.x |
<!-- versions:end -->

> Tip: `corepack enable` to use `pnpm`/`yarn` if preferred. Default instructions assume `npm`.

## Spring Boot integration

The build, run, SPA-forwarding, and API client rules shared by all frameworks are in [Frontend integration with Spring Boot](spring-boot-integration.md). Vue.js supplies these values:

| Setting | Vue.js |
|---|---|
| Dev server | `npm run dev` on `http://localhost:${VITE_PORT:-5173}` |
| Proxy and build output | `vite.config.js` (`server.proxy`, `build.outDir`), writing to `../src/main/resources/static` |
| Maven `LINT_SCRIPT` | `lint:check` |
| Maven `TEST_COMMAND` | `run test:unit -- --run` |
| `FRONTEND_DEV_ORIGIN`, only if CORS is needed | `http://localhost:5173` |

## Project Structure

```
my-spring-boot-app/
├── frontend/                    # Vue.js application
│   ├── src/
│   │   ├── main.js             # Vue entry point
│   │   ├── App.vue             # Root component
│   │   ├── components/         # Vue components
│   │   ├── views/              # Page views
│   │   ├── router/             # Vue Router
│   │   ├── stores/             # Pinia stores (state management)
│   │   └── services/           # API services
│   ├── public/                 # Public assets
│   ├── index.html              # HTML entry point
│   ├── vite.config.js          # Vite configuration
│   ├── package.json            # Node dependencies
│   └── .gitignore
├── src/
│   └── main/
│       ├── java/               # Spring Boot backend
│       └── resources/
│           └── static/         # Production build output (auto-generated)
└── pom.xml
```

## Setup Instructions

### 1. Create Vue.js Project

From your Spring Boot project root:

```bash
# Create Vue.js project with Vite — interactive (answer the prompts yourself):
# Note: `-y` MUST come before the package name — it tells npm to auto-accept
# its own "Ok to proceed?" install prompt (otherwise the command hangs in
# non-interactive shells like CI or AI agents).
npm create -y vue@latest frontend

# Follow the prompts:
# ✔ Add TypeScript? … No
# ✔ Add JSX Support? … No
# ✔ Add Vue Router for Single Page Application development? … Yes
# ✔ Add Pinia for state management? … Yes
# ✔ Add Vitest for Unit Testing? … Yes
# ✔ Add an End-to-End Testing Solution? › No
# ✔ Add ESLint for code quality? … Yes
# ✔ Add Prettier for code formatting? … Yes

# Fully non-interactive alternative (skips all prompts, picks the features via flags):
# npm create -y vue@latest frontend -- --router --pinia --vitest --eslint --prettier

cd frontend
npm install

# Install Bootstrap and Bootstrap Icons
npm install bootstrap@5.3.8 bootstrap-icons@1.13.1
```

> **Heads-up — create-vue's oxlint dual-linter breaks `npm install`.** Recent
> `create-vue` scaffolds add both `oxlint` and `eslint-plugin-oxlint` as dev
> dependencies, pinned to mismatched minors (e.g.
> `oxlint@~1.74.0` with `eslint-plugin-oxlint@~1.73.0`, whose peer requires
> `oxlint@~1.73.0`). This makes `npm install` fail with an `ERESOLVE` peer
> conflict — which also fails the `frontend-maven-plugin` `npm install` step and
> therefore the **whole Maven build**.
>
> **Always run the normalizer immediately after scaffolding**, before the first
> `npm install`. It drops the oxlint dual-linter, collapses linting to the
> single ESLint pipeline this project standardizes on, and adds the `lint:check`
> script `create-vue` never emits — the Maven build runs `npm run lint:check`,
> so without it the `frontend-maven-plugin` step fails with
> `Missing script: "lint:check"` (see
> [step 4](#4-update-frontend-packagejson-scripts)):
>
> ```bash
> # `scripts/` lives in this skill directory, NOT in your generated
> # project — running this from the project root gives MODULE_NOT_FOUND.
> # Easiest: ask the agent, "run the Vue normalizer on frontend/".
> # Manually, point at the skill's copy and pass your frontend path:
> node /absolute/path/to/spring-boot-developer/scripts/normalize-vue-frontend.mjs /absolute/path/to/project/frontend
> ```
>
> It is idempotent, and `--check` reports without writing (useful in CI). If you
> ever need to do it by hand: remove `oxlint` and `eslint-plugin-oxlint` from
> `devDependencies`, delete `frontend/.oxlintrc.json`, add
> `"lint:check": "eslint ."` to `scripts`, and
> drop the `eslint-plugin-oxlint` import plus the
> `...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json')` entry from
> `frontend/eslint.config.js` (the TypeScript flavour of `create-vue` emits
> `eslint.config.ts` instead — a leftover import there fails `vue-tsc`).
> Leave `npm-run-all2` alone — the TypeScript flavour uses its `run-p` binary
> in the `build` script.

### 2. Configure Vite for Spring Boot Integration

Update `frontend/vite.config.js`:

```javascript
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const projectRoot = fileURLToPath(new URL('..', import.meta.url))
  const env = loadEnv(mode, projectRoot, '')
  const vitePort = Number(env.VITE_PORT ?? 5173)
  const springBootPort = Number(env.SPRING_BOOT_PORT ?? 8080)

  return {
    plugins: [vue()],

    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },

    server: {
      port: vitePort,
      strictPort: true,
      proxy: {
        '/api': {
          target: `http://localhost:${springBootPort}`,
          changeOrigin: true,
          secure: false
        }
      }
    },

    build: {
      outDir: '../src/main/resources/static',
      emptyOutDir: true,
      assetsDir: 'assets',
      sourcemap: false
    }
  }
})
```

> Note: in Vite 8 the esbuild integration was made optional — `minify: 'esbuild'`
> and the `esbuild: { pure: [...] }` block now require a separate `esbuild` install
> (`npm i -D esbuild`). The default minifier in Vite 8 (Rolldown-based) works out
> of the box, so this template omits both to keep the dependency list minimal.

Because this Vite config exports a callback, replace the default `create-vue` Vitest config
(which may pass the callback directly to `mergeConfig`) with `frontend/vitest.config.js` below.
Vitest must merge the resolved config object, not the callback itself. Also add the `.js` extension
to the import — Vite 8 warns about extensionless config imports under the upcoming native config loader:

```javascript
import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config.js'

export default mergeConfig(
  viteConfig({ mode: 'test', command: 'serve' }),
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url))
    }
  })
)
```

> **TypeScript flavour:** the file is `vitest.config.ts` and it sits next to
> `vite.config.ts` — but keep the import specifier as `'./vite.config.js'` exactly as
> written above. It is a module specifier, not a filename, and TypeScript rejects the `.ts`
> form with `TS5097: An import path can only end with a '.ts' extension when
> 'allowImportingTsExtensions' is enabled`, which fails `vue-tsc` and therefore the Maven
> build. "Correcting" it to `.ts` is a common and confusing self-inflicted break.

### 3. Configure Maven for Frontend Build

Add the frontend-maven-plugin from [Build the frontend with Maven](spring-boot-integration.md#build-the-frontend-with-maven), using the lint and test values in the [integration table](#spring-boot-integration).

### 4. Update Frontend package.json Scripts

Edit `frontend/package.json`:

```json
{
  "name": "frontend",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test:unit": "vitest",
    "lint": "eslint . --fix",
    "lint:check": "eslint .",
    "format": "prettier --write src/"
  }
}
```

## Vue.js Application Structure

### Main Entry Point (frontend/src/main.js)

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Bootstrap CSS + JS + Icons
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
```

### Root Component (frontend/src/App.vue)

```vue
<template>
  <div id="app">
    <AppNavbar />
    <main class="container my-5">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import AppNavbar from './components/AppNavbar.vue'
import AppFooter from './components/AppFooter.vue'
import { RouterView } from 'vue-router'
</script>

<style>
:root {
  --primary-color: #0d6efd;
  --secondary-color: #6c757d;
}

body {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

main {
  flex: 1;
}
</style>
```

### Navigation Component (frontend/src/components/AppNavbar.vue)

> Component filenames are multi-word on purpose: ESLint's `vue/multi-word-component-names`
> rule ships enabled in the `create-vue` scaffold, so a single-word `Navbar.vue` fails
> `npm run lint`.

```vue
<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container">
      <RouterLink class="navbar-brand" to="/">My Spring Boot App</RouterLink>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarNav"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNav">
        <ul class="navbar-nav ms-auto">
          <li class="nav-item">
            <RouterLink class="nav-link" to="/" activeClass="active">Home</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/about" activeClass="active">About</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/items" activeClass="active">Items</RouterLink>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
```

### Footer Component (frontend/src/components/AppFooter.vue)

```vue
<template>
  <footer class="bg-light py-4 mt-auto">
    <div class="container text-center">
      <p class="text-muted mb-0">&copy; 2026 Spring Boot Application</p>
    </div>
  </footer>
</template>
```

### Router Configuration (frontend/src/router/index.js)

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/about',
      name: 'about',
      // Lazy-loaded route
      component: () => import('../views/AboutView.vue')
    },
    {
      path: '/items',
      name: 'items',
      component: () => import('../views/ItemsView.vue')
    },
    {
      path: '/items/:id',
      name: 'item-detail',
      component: () => import('../views/ItemDetailView.vue'),
      props: true
    }
  ]
})

export default router
```

### API Service Layer (frontend/src/services/api.js)

```javascript
/**
 * Base API configuration and utilities
 */
const API_BASE_URL = '/api'

class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

/**
 * Generic fetch wrapper with error handling
 */
async function fetchApi(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`

  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  }

  try {
    const response = await fetch(url, config)

    if (!response.ok) {
      const contentType = response.headers.get('content-type') || ''
      const errorData = contentType.includes('json')
        ? await response.json().catch(() => null)
        : null
      throw new ApiError(
        errorData?.detail || errorData?.title || `HTTP error! status: ${response.status}`,
        response.status,
        errorData
      )
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return null
    }

    return await response.json()
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    // status 0 means no HTTP response; keep it distinguishable from HTTP errors.
    throw new ApiError('Network error: ' + error.message, 0, null)
  }
}

/**
 * API methods
 */
export const api = {
  // GET request
  get: (endpoint) => fetchApi(endpoint, { method: 'GET' }),

  // POST request
  post: (endpoint, data) => fetchApi(endpoint, {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // PUT request
  put: (endpoint, data) => fetchApi(endpoint, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // DELETE request
  delete: (endpoint) => fetchApi(endpoint, { method: 'DELETE' })
}

export { ApiError }
```

### Item API Service (frontend/src/services/itemService.js)

```javascript
import { api } from './api'

export const itemService = {
  // Get all items
  async getAll() {
    return await api.get('/items')
  },

  // Get single item by ID
  async getById(id) {
    return await api.get(`/items/${id}`)
  },

  // Create new item
  async create(item) {
    return await api.post('/items', item)
  },

  // Update existing item
  async update(id, item) {
    return await api.put(`/items/${id}`, item)
  },

  // Delete item
  async delete(id) {
    return await api.delete(`/items/${id}`)
  }
}
```

### Pinia Store (frontend/src/stores/itemStore.js)

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { itemService } from '@/services/itemService'

export const useItemStore = defineStore('item', () => {
  // State
  const items = ref([])
  const currentItem = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // Getters
  const itemCount = computed(() => items.value.length)
  const hasItems = computed(() => items.value.length > 0)

  // Actions
  async function fetchItems() {
    loading.value = true
    error.value = null
    try {
      items.value = await itemService.getAll()
    } catch (err) {
      error.value = err.message
      console.error('Failed to fetch items:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchItemById(id) {
    loading.value = true
    error.value = null
    try {
      currentItem.value = await itemService.getById(id)
    } catch (err) {
      error.value = err.message
      console.error('Failed to fetch item:', err)
    } finally {
      loading.value = false
    }
  }

  async function createItem(item) {
    loading.value = true
    error.value = null
    try {
      const created = await itemService.create(item)
      items.value.push(created)
      return created
    } catch (err) {
      error.value = err.message
      console.error('Failed to create item:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateItem(id, item) {
    loading.value = true
    error.value = null
    try {
      const updated = await itemService.update(id, item)
      const index = items.value.findIndex(i => i.id === id)
      if (index !== -1) {
        items.value[index] = updated
      }
      return updated
    } catch (err) {
      error.value = err.message
      console.error('Failed to update item:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteItem(id) {
    loading.value = true
    error.value = null
    try {
      await itemService.delete(id)
      items.value = items.value.filter(i => i.id !== id)
    } catch (err) {
      error.value = err.message
      console.error('Failed to delete item:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    items,
    currentItem,
    loading,
    error,
    // Getters
    itemCount,
    hasItems,
    // Actions
    fetchItems,
    fetchItemById,
    createItem,
    updateItem,
    deleteItem,
    clearError
  }
})
```

### Home View (frontend/src/views/HomeView.vue)

```vue
<template>
  <div class="home">
    <div class="row">
      <div class="col-lg-8 mx-auto">
        <h1 class="display-4">Welcome to Spring Boot</h1>
        <p class="lead">
          A modern web application built with Spring Boot and Vue.js 3.
        </p>

        <div class="card mt-4">
          <div class="card-body">
            <h5 class="card-title">Getting Started</h5>
            <p class="card-text">
              Your Spring Boot application with Vue.js is running! This front-end
              is connected to your REST API endpoints with hot reload during development.
            </p>
            <button class="btn btn-primary" @click="handleClick">
              Test Button
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
function handleClick() {
  alert('Button clicked! Your Vue.js app is working.')
}
</script>
```

### Items List View (frontend/src/views/ItemsView.vue)

```vue
<template>
  <div class="items">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1>Items</h1>
      <button class="btn btn-primary" @click="showCreateModal">
        <i class="bi bi-plus-circle"></i> Create Item
      </button>
    </div>

    <!-- Error Alert -->
    <div v-if="store.error" class="alert alert-danger alert-dismissible" role="alert">
      {{ store.error }}
      <button
        type="button"
        class="btn-close"
        @click="store.clearError()"
      ></button>
    </div>

    <!-- Loading Spinner -->
    <div v-if="store.loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- Items List -->
    <div v-else-if="store.hasItems" class="row">
      <div
        v-for="item in store.items"
        :key="item.id"
        class="col-md-6 col-lg-4 mb-4"
      >
        <div class="card h-100">
          <div class="card-body">
            <h5 class="card-title">{{ item.name }}</h5>
            <p class="card-text">{{ item.description }}</p>
          </div>
          <div class="card-footer bg-transparent">
            <RouterLink
              :to="{ name: 'item-detail', params: { id: item.id } }"
              class="btn btn-sm btn-outline-primary"
            >
              View Details
            </RouterLink>
            <button
              class="btn btn-sm btn-outline-danger ms-2"
              @click="handleDelete(item.id)"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-5">
      <p class="text-muted">No items found. Create your first item to get started.</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useItemStore } from '@/stores/itemStore'

const store = useItemStore()

onMounted(() => {
  store.fetchItems()
})

function showCreateModal() {
  // Implement modal logic or navigate to create page
  alert('Create item functionality - implement modal or navigate to form')
}

async function handleDelete(id) {
  if (confirm('Are you sure you want to delete this item?')) {
    await store.deleteItem(id)
  }
}
</script>
```

### Item Detail View (frontend/src/views/ItemDetailView.vue)

```vue
<template>
  <div class="item-detail">
    <RouterLink to="/items" class="btn btn-sm btn-outline-secondary mb-3">
      &larr; Back to Items
    </RouterLink>

    <!-- Loading -->
    <div v-if="store.loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="alert alert-danger" role="alert">
      {{ store.error }}
    </div>

    <!-- Item Details -->
    <div v-else-if="store.currentItem" class="card">
      <div class="card-header">
        <h2>{{ store.currentItem.name }}</h2>
      </div>
      <div class="card-body">
        <p>{{ store.currentItem.description }}</p>
        <dl class="row">
          <dt class="col-sm-3">ID:</dt>
          <dd class="col-sm-9">{{ store.currentItem.id }}</dd>

          <dt class="col-sm-3">Created:</dt>
          <dd class="col-sm-9">{{ formatDate(store.currentItem.createdAt) }}</dd>
        </dl>
      </div>
      <div class="card-footer">
        <button class="btn btn-primary" @click="handleEdit">Edit</button>
        <button class="btn btn-danger ms-2" @click="handleDelete">Delete</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useItemStore } from '@/stores/itemStore'

const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const store = useItemStore()
const router = useRouter()

onMounted(() => {
  store.fetchItemById(props.id)
})

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString()
}

function handleEdit() {
  alert('Edit functionality - implement edit form')
}

async function handleDelete() {
  if (confirm('Are you sure you want to delete this item?')) {
    await store.deleteItem(props.id)
    router.push('/items')
  }
}
</script>
```

## Best Practices

### 1. Component Organization

- Single File Components (SFC): Use `.vue` files with `<template>`, `<script>`, and `<style>` sections
- Composition API: Prefer `<script setup>` for cleaner, more maintainable code
- Component Naming: Use PascalCase for component names (e.g., `ItemCard.vue`)
- Props Validation: Always define prop types and requirements

### 2. State Management

- Pinia Stores: Use Pinia for application state
- Store Organization: One store per domain entity (e.g., `itemStore.js`, `userStore.js`)
- Composition API Style: Use `ref()` and `computed()` in stores
- Actions for API Calls: Keep API logic in store actions

### 3. API Integration

- Keep API calls in `services/` and call them from Pinia store actions; the store owns loading and error state.
- Follow the [API client rules](spring-boot-integration.md#api-client-rules) for errors, wire values, mutations, and loading state.

### 4. Routing

- Lazy Loading: Use dynamic imports for route components to reduce initial bundle size
- Route Guards: Implement navigation guards for authentication
- Named Routes: Use named routes instead of path strings
- Props Mode: Pass route params as props to components

### 5. Bootstrap Integration

- Import Once: Import Bootstrap CSS/JS in `main.js`
- Utility Classes: Leverage Bootstrap's utility classes
- Responsive Grid: Use Bootstrap's grid system for layouts
- Icons: Bootstrap Icons are installed and imported in `main.js`; use `<i class="bi bi-..."/>` anywhere

### 6. Performance

- **Route-level code splitting** — use dynamic `import()` in the router (already shown in the Router Configuration example). Each route becomes a separate chunk, cutting initial bundle size.
- **Component-level lazy loading** — for heavy components rendered conditionally (charts, rich editors, modals), wrap them with `defineAsyncComponent`:
  ```javascript
  import { defineAsyncComponent } from 'vue'
  const Chart = defineAsyncComponent(() => import('./Chart.vue'))
  ```

### 7. Development Workflow

- Hot Module Replacement: Vite provides instant updates during development
- Environment Variables: Use `.env` files for configuration
- ESLint + Prettier: Maintain code quality and consistent formatting
- Component Testing: Use Vitest for unit testing Vue components

### 8. Security

- Sanitization: Sanitize data before rendering (Vue does this automatically for text)

## Browser DevTools

Use Vue DevTools for components and Pinia stores, and the network tab to confirm that API calls go to relative `/api` URLs. For integration checks, see [Verify the integration](spring-boot-integration.md#verify-the-integration).

## References
- [Vue.js 3 Documentation](https://vuejs.org/)
- [Vite Documentation](https://vitejs.dev/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [Vue Router Documentation](https://router.vuejs.org/)
- [Bootstrap 5 Documentation](https://getbootstrap.com/)
- [Frontend Maven Plugin](https://github.com/eirslett/frontend-maven-plugin)
- [Spring Boot Static Content](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.static-content)
