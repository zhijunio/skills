# Front-End Development with Angular for Spring Boot Applications

## Contents
- [Overview](#overview)
- [Versions (managed via `versions.json`)](#versions-managed-via-versionsjson)
- [Spring Boot integration](#spring-boot-integration)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [Angular Application Structure](#angular-application-structure)
- [Best Practices](#best-practices)
- [Browser DevTools](#browser-devtools)
- [References](#references)

## Overview
This guide covers creating front-end applications for Spring Boot using Angular 22 and Angular CLI, with hot reload during development and optimized production builds integrated into the Spring Boot package.

## Versions (managed via `versions.json`)

> Regenerate this table with `node scripts/sync-versions-in-docs.mjs` after bumping `versions.json`.

<!-- versions:start -->
| Tool | Version |
|------|---------|
| Node.js | 24.21.0 |
| npm | 11.20.0 |
| Angular | 22.x |
| Angular Router | 22.x |
<!-- versions:end -->

> Use `npx @angular/cli@latest` to scaffold; keep alignment with `engines` constraints. No OpenAPI client generation provided.

## Spring Boot integration

The build, run, SPA-forwarding, and API client rules shared by all frameworks are in [Frontend integration with Spring Boot](spring-boot-integration.md). Angular supplies these values:

| Setting | Angular |
|---|---|
| Dev server | `npm start` on `http://localhost:4200` |
| Proxy and build output | `angular.json` (`outputPath`) and `proxy.conf.cjs`, writing to `../src/main/resources/static` |
| Maven `LINT_SCRIPT` | `lint` |
| Maven `TEST_COMMAND` | `run test -- --watch=false` |
| `FRONTEND_DEV_ORIGIN`, only if CORS is needed | `http://localhost:4200` |

## Project Structure

```
my-spring-boot-app/
├── frontend/                    # Angular application
│   ├── src/
│   │   ├── app/
│   │   │   ├── app.component.ts     # Root component
│   │   │   ├── app.component.html   # Root template
│   │   │   ├── app.routes.ts        # Routing configuration
│   │   │   ├── components/          # Shared components
│   │   │   ├── pages/               # Page components
│   │   │   ├── services/            # Services and API calls
│   │   │   └── models/              # TypeScript interfaces
│   │   ├── index.html               # HTML entry point
│   │   ├── main.ts                  # Angular bootstrap
│   │   └── styles.css               # Global styles
│   ├── angular.json                 # Angular configuration
│   ├── package.json                 # Node dependencies
│   ├── tsconfig.json                # TypeScript config
│   └── .gitignore
├── src/
│   └── main/
│       ├── java/                    # Spring Boot backend
│       └── resources/
│           └── static/              # Production build output (auto-generated)
└── pom.xml
```

## Setup Instructions

### 1. Create Angular Project

From your Spring Boot project root:

```bash
# Create Angular project.
# Angular CLI 22 poses interactive prompts (analytics opt-in, zoneless choice)
# that `-y`/`--yes` do NOT silence. In non-TTY shells (CI, agents) you must
# close stdin with `echo |` AND pass `--defaults` so the CLI accepts defaults
# immediately. `--skip-install` avoids a duplicate npm install from the CLI.
echo | npx --yes @angular/cli@22 new frontend \
  --style=css --ssr=false --skip-git --defaults --skip-install

cd frontend
npm install

# Install Bootstrap and Bootstrap Icons
npm install bootstrap@5.3.8 bootstrap-icons@1.13.1
```

> Interactive shells can use the shorter form `ng new frontend --style=css --ssr=false --skip-git` (after `npm install -g @angular/cli@22`).

### 2. Configure Angular for Spring Boot Integration

Update `frontend/angular.json` - modify the `build` section. Angular 22's default builder is `@angular/build:application` (migrated from the older `@angular-devkit/build-angular:application`). When `outputPath` is a plain string it writes to `<outputPath>/browser/`; use the object form with `"browser": ""` to flatten the output so `index.html` lands directly in `src/main/resources/static/`, and use the `browser` key for the entry point:

```json
{
  "projects": {
    "frontend": {
      "architect": {
        "build": {
          "options": {
            "outputPath": {
              "base": "../src/main/resources/static",
              "browser": ""
            },
            "index": "src/index.html",
            "browser": "src/main.ts",
            "polyfills": [],
            "tsConfig": "tsconfig.app.json",
            "assets": [
              "src/favicon.ico",
              "src/assets"
            ],
            "styles": [
              "node_modules/bootstrap/dist/css/bootstrap.min.css",
              "node_modules/bootstrap-icons/font/bootstrap-icons.css",
              "src/styles.css"
            ],
            "scripts": [
              "node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"
            ]
          }
        },
        "serve": {
          "options": {
            "proxyConfig": "proxy.conf.cjs"
          }
        }
      }
    }
  }
}
```

Create `frontend/proxy.conf.cjs`:

```javascript
const { existsSync, readFileSync } = require('node:fs');
const { resolve } = require('node:path');

function readRootEnv() {
  const envPath = resolve(__dirname, '..', '.env');
  if (!existsSync(envPath)) {
    return {};
  }
  return Object.fromEntries(
    readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const index = line.indexOf('=');
        return [line.slice(0, index), line.slice(index + 1)];
      })
  );
}

const env = { ...readRootEnv(), ...process.env };
const springBootPort = env.SPRING_BOOT_PORT ?? '8080';

module.exports = {
  '/api': {
    target: `http://localhost:${springBootPort}`,
    secure: false,
    changeOrigin: true
  }
};
```

### 3. Configure Maven for Frontend Build

Add the frontend-maven-plugin from [Build the frontend with Maven](spring-boot-integration.md#build-the-frontend-with-maven), using the lint and test values in the [integration table](#spring-boot-integration).

### 4. Update Frontend package.json Scripts

Edit `frontend/package.json`:

```json
{
  "name": "frontend",
  "version": "1.0.0",
  "scripts": {
    "ng": "ng",
    "start": "ng serve",
    "build": "ng build --configuration production",
    "watch": "ng build --watch --configuration development",
    "test": "ng test"
  }
}
```

## Angular Application Structure

### App Configuration (frontend/src/app/app.config.ts)

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi())
  ]
};
```

### Routing Configuration (frontend/src/app/app.routes.ts)

```typescript
import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { AboutComponent } from './pages/about/about.component';
import { ItemsComponent } from './pages/items/items.component';
import { ItemDetailComponent } from './pages/item-detail/item-detail.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: 'items', component: ItemsComponent },
  { path: 'items/:id', component: ItemDetailComponent },
  { path: '**', redirectTo: '' }
];
```

### Root Component (frontend/src/app/app.component.ts)

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  template: `
    <div class="app-container">
      <app-navbar></app-navbar>
      <main class="container my-5">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    main {
      flex: 1;
    }
  `]
})
export class AppComponent {
  title = 'My Spring Boot App';
}
```

### Navigation Component (frontend/src/app/components/navbar/navbar.component.ts)

Generate with: `ng generate component components/navbar`

```typescript
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
      <div class="container">
        <a class="navbar-brand" routerLink="/">My Spring Boot App</a>
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
              <a class="nav-link" routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Home</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/about" routerLinkActive="active">About</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/items" routerLinkActive="active">Items</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  `,
  styles: []
})
export class NavbarComponent {}
```

### Footer Component (frontend/src/app/components/footer/footer.component.ts)

Generate with: `ng generate component components/footer`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [],
  template: `
    <footer class="bg-light py-4 mt-auto">
      <div class="container text-center">
        <p class="text-muted mb-0">&copy; 2026 Spring Boot Application</p>
      </div>
    </footer>
  `,
  styles: []
})
export class FooterComponent {}
```

### Models (frontend/src/app/models/item.model.ts)

```typescript
export interface Item {
  id: number;
  name: string;
  description: string;
  createdAt: string;
}
```

### API Service (frontend/src/app/services/item.service.ts)

Generate with: `ng generate service services/item`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Item } from '../models/item.model';

@Injectable({
  providedIn: 'root'
})
export class ItemService {
  private apiUrl = '/api/items';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Item[]> {
    return this.http.get<Item[]>(this.apiUrl)
      .pipe(catchError(this.handleError));
  }

  getById(id: number): Observable<Item> {
    return this.http.get<Item>(`${this.apiUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  create(item: Partial<Item>): Observable<Item> {
    return this.http.post<Item>(this.apiUrl, item)
      .pipe(catchError(this.handleError));
  }

  update(id: number, item: Partial<Item>): Observable<Item> {
    return this.http.put<Item>(`${this.apiUrl}/${id}`, item)
      .pipe(catchError(this.handleError));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }

    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}
```

### Home Page (frontend/src/app/pages/home/home.component.ts)

Generate with: `ng generate component pages/home`

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [],
  template: `
    <div class="home">
      <div class="row">
        <div class="col-lg-8 mx-auto">
          <h1 class="display-4">Welcome to Spring Boot</h1>
          <p class="lead">
            A modern web application built with Spring Boot and Angular 22.
          </p>

          <div class="card mt-4">
            <div class="card-body">
              <h5 class="card-title">Getting Started</h5>
              <p class="card-text">
                Your Spring Boot application with Angular is running! This front-end
                is connected to your REST API endpoints with hot reload during development.
              </p>
              <button class="btn btn-primary" (click)="handleClick()">
                Test Button
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class HomeComponent {
  handleClick() {
    alert('Button clicked! Your Angular app is working.');
  }
}
```

### Items List Page (frontend/src/app/pages/items/items.component.ts)

Generate with: `ng generate component pages/items`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ItemService } from '../../services/item.service';
import { Item } from '../../models/item.model';

@Component({
  selector: 'app-items',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="items">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h1>Items</h1>
        <button class="btn btn-primary" (click)="handleCreate()">
          <i class="bi bi-plus-circle"></i> Create Item
        </button>
      </div>

      <!-- Error Alert -->
      @if (error) {
        <div class="alert alert-danger alert-dismissible" role="alert">
          {{ error }}
          <button
            type="button"
            class="btn-close"
            (click)="error = null"
          ></button>
        </div>
      }

      <!-- Loading Spinner -->
      @if (loading) {
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
        </div>
      }

      <!-- Items List -->
      @if (!loading && items.length > 0) {
        <div class="row">
          @for (item of items; track item.id) {
            <div class="col-md-6 col-lg-4 mb-4">
              <div class="card h-100">
                <div class="card-body">
                  <h5 class="card-title">{{ item.name }}</h5>
                  <p class="card-text">{{ item.description }}</p>
                </div>
                <div class="card-footer bg-transparent">
                  <a
                    [routerLink]="['/items', item.id]"
                    class="btn btn-sm btn-outline-primary"
                  >
                    View Details
                  </a>
                  <button
                    class="btn btn-sm btn-outline-danger ms-2"
                    (click)="handleDelete(item.id)"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          }
        </div>
      }

      <!-- Empty State -->
      @if (!loading && items.length === 0) {
        <div class="text-center py-5">
          <p class="text-muted">No items found. Create your first item to get started.</p>
        </div>
      }
    </div>
  `,
  styles: []
})
export class ItemsComponent implements OnInit {
  items: Item[] = [];
  loading = false;
  error: string | null = null;

  constructor(private itemService: ItemService) {}

  ngOnInit() {
    this.loadItems();
  }

  loadItems() {
    this.loading = true;
    this.error = null;
    this.itemService.getAll().subscribe({
      next: (data) => {
        this.items = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  handleCreate() {
    alert('Create item functionality - implement modal or navigate to form');
  }

  handleDelete(id: number) {
    if (confirm('Are you sure you want to delete this item?')) {
      this.itemService.delete(id).subscribe({
        next: () => {
          this.items = this.items.filter(item => item.id !== id);
        },
        error: (err) => {
          this.error = err.message;
        }
      });
    }
  }
}
```

### Item Detail Page (frontend/src/app/pages/item-detail/item-detail.component.ts)

Generate with: `ng generate component pages/item-detail`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ItemService } from '../../services/item.service';
import { Item } from '../../models/item.model';

@Component({
  selector: 'app-item-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="item-detail">
      <a routerLink="/items" class="btn btn-sm btn-outline-secondary mb-3">
        &larr; Back to Items
      </a>

      <!-- Loading -->
      @if (loading) {
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
        </div>
      }

      <!-- Error -->
      @if (error) {
        <div class="alert alert-danger" role="alert">
          {{ error }}
        </div>
      }

      <!-- Item Details -->
      @if (item) {
        <div class="card">
          <div class="card-header">
            <h2>{{ item.name }}</h2>
          </div>
          <div class="card-body">
            <p>{{ item.description }}</p>
            <dl class="row">
              <dt class="col-sm-3">ID:</dt>
              <dd class="col-sm-9">{{ item.id }}</dd>

              <dt class="col-sm-3">Created:</dt>
              <dd class="col-sm-9">{{ item.createdAt | date }}</dd>
            </dl>
          </div>
          <div class="card-footer">
            <button class="btn btn-primary" (click)="handleEdit()">Edit</button>
            <button class="btn btn-danger ms-2" (click)="handleDelete()">Delete</button>
          </div>
        </div>
      }
    </div>
  `,
  styles: []
})
export class ItemDetailComponent implements OnInit {
  item: Item | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private itemService: ItemService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadItem(id);
  }

  loadItem(id: number) {
    this.loading = true;
    this.error = null;
    this.itemService.getById(id).subscribe({
      next: (data) => {
        this.item = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  handleEdit() {
    alert('Edit functionality - implement edit form');
  }

  handleDelete() {
    if (this.item && confirm('Are you sure you want to delete this item?')) {
      this.itemService.delete(this.item.id).subscribe({
        next: () => {
          this.router.navigate(['/items']);
        },
        error: (err) => {
          this.error = err.message;
        }
      });
    }
  }
}
```

### Main Bootstrap (frontend/src/main.ts)

```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

## Best Practices

### 1. Component Organization

- Standalone Components: Use standalone components (Angular 14+)
- Component Naming: Use kebab-case for file names (e.g., `item-card.component.ts`)
- TypeScript: Leverage TypeScript's type safety
- Single Responsibility: Each component should have a single purpose

### 2. State Management

- Services: Use services for state management
- RxJS: Leverage Observables for reactive programming
- NgRx (Optional): Consider NgRx for complex state management
- Signals (New): Consider Angular Signals for simpler state

### 3. API Integration

- Use `HttpClient` in injectable services, and HTTP interceptors for cross-cutting concerns such as auth headers and error mapping.
- Handle errors with RxJS operators in the service and expose the resulting state to components.
- Follow the [API client rules](spring-boot-integration.md#api-client-rules) for errors, wire values, mutations, and loading state.

### 4. Routing

- RouterModule: Use Angular Router for navigation
- Lazy Loading: Use `loadChildren` for route-based code splitting
- Route Guards: Implement guards for authentication and authorization
- Route Parameters: Use `ActivatedRoute` for accessing route params

### 5. Bootstrap Integration

- Import in angular.json: Add Bootstrap to styles and scripts arrays
- Utility Classes: Leverage Bootstrap's utility classes
- Responsive Grid: Use Bootstrap's grid system for layouts
- Icons: Bootstrap Icons are wired into `angular.json` styles; use `<i class="bi bi-..."></i>` anywhere

### 6. Performance

- **Lazy-loaded routes** — use `loadComponent` (standalone components) in `app.routes.ts`:
  ```typescript
  { path: 'items', loadComponent: () => import('./pages/items/items.component').then(m => m.ItemsComponent) }
  ```
- **OnPush change detection** — set `changeDetection: ChangeDetectionStrategy.OnPush` on components that render from immutable inputs or signals. Drastically cuts dirty-checking cost.
- **`@for` with `track`** — always provide a `track` expression in `@for` (or `trackBy` with `*ngFor`) so Angular reuses DOM nodes.
- **Signals over `async` pipes on hot paths** — signals skip the zone roundtrip and integrate cleanly with OnPush.

### 7. Development Workflow

- Hot Module Replacement: Angular CLI provides instant updates
- Environment Files: Use environment files for configuration
- TypeScript Strict Mode: Enable strict mode for better type safety
- Testing: `ng test` runs Vitest, the Angular CLI default for new projects; `--browsers` applies only to Karma or Vitest browser mode

### 8. Security

- Sanitization: Angular sanitizes content automatically

## Browser DevTools

Use Angular DevTools for components and services, and the network tab to confirm that API calls go to relative `/api` URLs. For integration checks, see [Verify the integration](spring-boot-integration.md#verify-the-integration).

## References
- [Angular Documentation](https://angular.io/)
- [Angular CLI Documentation](https://angular.io/cli)
- [RxJS Documentation](https://rxjs.dev/)
- [Bootstrap 5 Documentation](https://getbootstrap.com/)
- [Frontend Maven Plugin](https://github.com/eirslett/frontend-maven-plugin)
- [Spring Boot Static Content](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.static-content)
