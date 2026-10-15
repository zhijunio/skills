# Front-End Development with React for Spring Boot Applications

## Contents
- [Overview](#overview)
- [Versions (managed via `versions.json`)](#versions-managed-via-versionsjson)
- [Spring Boot integration](#spring-boot-integration)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [React Application Structure](#react-application-structure)
- [Best Practices](#best-practices)
- [Browser DevTools](#browser-devtools)
- [References](#references)

## Overview
This guide covers creating front-end applications for Spring Boot using React 19 and Vite, with hot reload during development and optimized production builds integrated into the Spring Boot package.

## Versions (managed via `versions.json`)

> Regenerate this table with `node scripts/sync-versions-in-docs.mjs` after bumping `versions.json`.

<!-- versions:start -->
| Tool | Version |
|------|---------|
| Node.js | 24.21.0 |
| npm | 11.20.0 |
| React | 19.x |
| Vite | 8.x |
| React Router | 8.x |
<!-- versions:end -->

> Tip: `corepack enable` for pnpm/yarn if desired. Default instructions assume `npm`. No OpenAPI client generation is provided; use `fetch`/`axios` as needed.

## Spring Boot integration

The build, run, SPA-forwarding, and API client rules shared by all frameworks are in [Frontend integration with Spring Boot](spring-boot-integration.md). React supplies these values:

| Setting | React |
|---|---|
| Dev server | `npm run dev` on `http://localhost:${VITE_PORT:-5173}` |
| Proxy and build output | `vite.config.js` (`server.proxy`, `build.outDir`), writing to `../src/main/resources/static` |
| Maven `LINT_SCRIPT` | `lint:check` |
| Maven `TEST_COMMAND` | `run test -- --run` |
| `FRONTEND_DEV_ORIGIN`, only if CORS is needed | `http://localhost:5173` |

## Project Structure

```
my-spring-boot-app/
├── frontend/                    # React application
│   ├── src/
│   │   ├── main.jsx            # React entry point
│   │   ├── App.jsx             # Root component
│   │   ├── components/         # React components
│   │   ├── pages/              # Page components
│   │   ├── hooks/              # Custom hooks
│   │   ├── context/            # Context providers
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

### 1. Create React Project

From your Spring Boot project root:

```bash
# Create React project with Vite.
# Two prompts must be silenced in non-interactive shells (CI, agents):
#   1. npm's "Ok to proceed?" prompt → `-y` BEFORE the package name (or `--yes` with npx)
#   2. create-vite 8.x's own prompts (e.g. "Use rolldown-vite?") → close stdin with `echo |`
# Without `echo |` the command still hangs in non-TTY shells even with `-y`.
echo | npx --yes create-vite@latest frontend --template react

# Interactive shells can also use:
# npm create -y vite@latest frontend -- --template react

cd frontend
npm install

# Install React Router, Bootstrap and Bootstrap Icons
# React Router 8 removed the `react-router-dom` package: install `react-router` instead
npm install react-router bootstrap@5.3.8 bootstrap-icons@1.13.1
```

### 2. Configure Vite for Spring Boot Integration

Update `frontend/vite.config.js`:

```javascript
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'node:url'

export default defineConfig(({ mode }) => {
  const projectRoot = fileURLToPath(new URL('..', import.meta.url))
  const frontendRoot = fileURLToPath(new URL('.', import.meta.url))
  const env = loadEnv(mode, projectRoot, '')
  const vitePort = Number(env.VITE_PORT ?? 5173)
  const springBootPort = Number(env.SPRING_BOOT_PORT ?? 8080)

  return {
    plugins: [react()],

    resolve: {
      alias: {
        '@': path.resolve(frontendRoot, './src')
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
    "lint": "eslint . --fix",
    "lint:check": "eslint . --report-unused-disable-directives --max-warnings 0"
  }
}
```

## React Application Structure

### Main Entry Point (frontend/src/main.jsx)

```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

// Bootstrap CSS + JS + Icons
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

### Root Component (frontend/src/App.jsx)

```jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import HomePage from './pages/HomePage'
import AboutPage from './pages/AboutPage'
import ItemsPage from './pages/ItemsPage'
import ItemDetailPage from './pages/ItemDetailPage'
import './App.css'

function App() {
  return (
    <Router>
      <div className="app-container">
        <Navbar />
        <main className="container my-5">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/items" element={<ItemsPage />} />
            <Route path="/items/:id" element={<ItemDetailPage />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  )
}

export default App
```

### Navigation Component (frontend/src/components/Navbar.jsx)

```jsx
import { Link } from 'react-router'

export default function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
      <div className="container">
        <Link className="navbar-brand" to="/">My Spring Boot App</Link>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarNav"
        >
          <span className="navbar-toggler-icon"></span>
        </button>
        <div className="collapse navbar-collapse" id="navbarNav">
          <ul className="navbar-nav ms-auto">
            <li className="nav-item">
              <Link className="nav-link" to="/">Home</Link>
            </li>
            <li className="nav-item">
              <Link className="nav-link" to="/about">About</Link>
            </li>
            <li className="nav-item">
              <Link className="nav-link" to="/items">Items</Link>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}
```

### Footer Component (frontend/src/components/Footer.jsx)

```jsx
export default function Footer() {
  return (
    <footer className="bg-light py-4 mt-auto">
      <div className="container text-center">
        <p className="text-muted mb-0">&copy; 2026 Spring Boot Application</p>
      </div>
    </footer>
  )
}
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
      const errorData = await response.json().catch(() => ({}))
      throw new ApiError(
        errorData.message || `HTTP error! status: ${response.status}`,
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

### Custom Hook for Items (frontend/src/hooks/useItems.js)

```javascript
import { useState, useCallback } from 'react'
import { itemService } from '../services/itemService'

export function useItems() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const fetchItems = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await itemService.getAll()
      setItems(data)
    } catch (err) {
      setError(err.message)
      console.error('Failed to fetch items:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  const createItem = useCallback(async (item) => {
    setLoading(true)
    setError(null)
    try {
      const created = await itemService.create(item)
      setItems(prev => [...prev, created])
      return created
    } catch (err) {
      setError(err.message)
      console.error('Failed to create item:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const updateItem = useCallback(async (id, item) => {
    setLoading(true)
    setError(null)
    try {
      const updated = await itemService.update(id, item)
      setItems(prev => prev.map(i => i.id === id ? updated : i))
      return updated
    } catch (err) {
      setError(err.message)
      console.error('Failed to update item:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const deleteItem = useCallback(async (id) => {
    setLoading(true)
    setError(null)
    try {
      await itemService.delete(id)
      setItems(prev => prev.filter(i => i.id !== id))
    } catch (err) {
      setError(err.message)
      console.error('Failed to delete item:', err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])

  return {
    items,
    loading,
    error,
    fetchItems,
    createItem,
    updateItem,
    deleteItem,
    clearError
  }
}
```

### Home Page (frontend/src/pages/HomePage.jsx)

```jsx
export default function HomePage() {
  const handleClick = () => {
    alert('Button clicked! Your React app is working.')
  }

  return (
    <div className="home">
      <div className="row">
        <div className="col-lg-8 mx-auto">
          <h1 className="display-4">Welcome to Spring Boot</h1>
          <p className="lead">
            A modern web application built with Spring Boot and React 19.
          </p>

          <div className="card mt-4">
            <div className="card-body">
              <h5 className="card-title">Getting Started</h5>
              <p className="card-text">
                Your Spring Boot application with React is running! This front-end
                is connected to your REST API endpoints with hot reload during development.
              </p>
              <button className="btn btn-primary" onClick={handleClick}>
                Test Button
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
```

### Items List Page (frontend/src/pages/ItemsPage.jsx)

```jsx
import { useEffect } from 'react'
import { Link } from 'react-router'
import { useItems } from '../hooks/useItems'

export default function ItemsPage() {
  const { items, loading, error, fetchItems, deleteItem, clearError } = useItems()

  useEffect(() => {
    fetchItems()
  }, [fetchItems])

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      await deleteItem(id)
    }
  }

  const handleCreate = () => {
    alert('Create item functionality - implement modal or navigate to form')
  }

  return (
    <div className="items">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1>Items</h1>
        <button className="btn btn-primary" onClick={handleCreate}>
          <i className="bi bi-plus-circle"></i> Create Item
        </button>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="alert alert-danger alert-dismissible" role="alert">
          {error}
          <button
            type="button"
            className="btn-close"
            onClick={clearError}
          ></button>
        </div>
      )}

      {/* Loading Spinner */}
      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      )}

      {/* Items List */}
      {!loading && items.length > 0 && (
        <div className="row">
          {items.map(item => (
            <div key={item.id} className="col-md-6 col-lg-4 mb-4">
              <div className="card h-100">
                <div className="card-body">
                  <h5 className="card-title">{item.name}</h5>
                  <p className="card-text">{item.description}</p>
                </div>
                <div className="card-footer bg-transparent">
                  <Link
                    to={`/items/${item.id}`}
                    className="btn btn-sm btn-outline-primary"
                  >
                    View Details
                  </Link>
                  <button
                    className="btn btn-sm btn-outline-danger ms-2"
                    onClick={() => handleDelete(item.id)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && items.length === 0 && (
        <div className="text-center py-5">
          <p className="text-muted">No items found. Create your first item to get started.</p>
        </div>
      )}
    </div>
  )
}
```

### Item Detail Page (frontend/src/pages/ItemDetailPage.jsx)

```jsx
import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router'
import { itemService } from '../services/itemService'

export default function ItemDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchItem = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await itemService.getById(id)
        setItem(data)
      } catch (err) {
        setError(err.message)
        console.error('Failed to fetch item:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchItem()
  }, [id])

  const handleEdit = () => {
    alert('Edit functionality - implement edit form')
  }

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      try {
        await itemService.delete(id)
        navigate('/items')
      } catch (err) {
        setError(err.message)
      }
    }
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString()
  }

  return (
    <div className="item-detail">
      <Link to="/items" className="btn btn-sm btn-outline-secondary mb-3">
        &larr; Back to Items
      </Link>

      {/* Loading */}
      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {/* Item Details */}
      {item && (
        <div className="card">
          <div className="card-header">
            <h2>{item.name}</h2>
          </div>
          <div className="card-body">
            <p>{item.description}</p>
            <dl className="row">
              <dt className="col-sm-3">ID:</dt>
              <dd className="col-sm-9">{item.id}</dd>

              <dt className="col-sm-3">Created:</dt>
              <dd className="col-sm-9">{formatDate(item.createdAt)}</dd>
            </dl>
          </div>
          <div className="card-footer">
            <button className="btn btn-primary" onClick={handleEdit}>Edit</button>
            <button className="btn btn-danger ms-2" onClick={handleDelete}>Delete</button>
          </div>
        </div>
      )}
    </div>
  )
}
```

## Best Practices

### 1. Component Organization

- Functional Components: Use functional components with hooks
- Component Naming: Use PascalCase for component names (e.g., `ItemCard.jsx`)
- Props Validation: Use PropTypes or TypeScript for type checking
- Single Responsibility: Each component should have a single purpose

### 2. State Management

- Local State: Use `useState` for component-local state
- Custom Hooks: Extract reusable logic into custom hooks
- Context API: Use Context for global state when needed
- React Query (Optional): Consider for advanced data fetching and caching

### 3. API Integration

- Keep API calls in `services/` and expose them through custom hooks such as `useItems`, which own loading and error state.
- Follow the [API client rules](spring-boot-integration.md#api-client-rules) for errors, wire values, mutations, and loading state.

### 4. Routing

- React Router: Use React Router for navigation
- Lazy Loading: Use `React.lazy()` for code splitting
- Named Routes: Consider using named constants for routes
- Route Parameters: Use `useParams` hook for accessing route params

### 5. Bootstrap Integration

- Import Once: Import Bootstrap CSS/JS in `main.jsx`
- Utility Classes: Leverage Bootstrap's utility classes
- Responsive Grid: Use Bootstrap's grid system for layouts
- Icons: Bootstrap Icons are installed and imported in `main.jsx`; use `<i className="bi bi-..."/>` anywhere

### 6. Performance

- **Route-level code splitting** — use `React.lazy` + `<Suspense>` for page components:
  ```jsx
  const ItemsPage = React.lazy(() => import('./pages/ItemsPage'))
  // wrap <Routes> in <Suspense fallback={<Spinner />}>
  ```
- **Memoization** — `React.memo`, `useMemo`, `useCallback` for components and values that are expensive to compute *and* rendered often. Don't wrap everything; unnecessary memoization costs more than it saves.

### 7. Development Workflow

- Hot Module Replacement: Vite provides instant updates
- Environment Variables: Use `.env` files for configuration
- ESLint: Maintain code quality with ESLint
- Component Testing: Consider Vitest or React Testing Library

### 8. Security

- XSS Protection: React escapes content by default in JSX

## Browser DevTools

Use React DevTools for components and state, and the network tab to confirm that API calls go to relative `/api` URLs. For integration checks, see [Verify the integration](spring-boot-integration.md#verify-the-integration).

## References
- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [React Router Documentation](https://reactrouter.com/)
- [Bootstrap 5 Documentation](https://getbootstrap.com/)
- [Frontend Maven Plugin](https://github.com/eirslett/frontend-maven-plugin)
- [Spring Boot Static Content](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.static-content)
