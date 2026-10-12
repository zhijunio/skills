# React and Vue Adapters

Apply the same contract rules through the project's existing HTTP abstraction. Introduce a new
global abstraction only when the project has no consistent boundary.

## React

- Put wire-to-domain mapping in an API module, generated-client adapter, or query function—not in
  components or render paths.
- With TanStack Query, keep transport concerns in the query/mutation function. Let feature code
  handle domain failures and invalidation; keep refresh single-flight below the query cache.
- With Axios, use one configured instance and interceptor chain. With `fetch`, use one wrapper that
  checks status and media type before decoding.
- A React Router loader or protected-layout component is navigation UX. It must mirror, never
  replace, backend authorization.
- For Next.js or Remix, decide whether the browser calls Spring directly or through the rendering
  server/BFF. Document cookie forwarding, origin, CSRF, cache, and error-shape consequences.

## Vue

- Put wire-to-domain mapping in an API module or composable boundary—not in templates.
- With Pinia or TanStack Query for Vue, keep request transport and refresh single-flight below store
  actions/query functions so concurrent callers share one refresh operation.
- Use one Axios instance or fetch wrapper and normalize failures before they enter components.
- Vue Router navigation guards improve navigation flow; Spring Security protects data.
- For Nuxt, decide whether calls originate from the browser or Nitro server. Forward cookies and
  headers deliberately, and keep server-rendered and hydrated authentication state consistent.

## Generator rule

Choose `typescript-fetch` for fetch-based clients or `typescript-axios` for Axios-based clients.
React/Vue framework names alone do not determine the generator. If a query-library-specific
generator is used, verify its output against the installed major rather than assuming compatibility.

## Boundary test

For either framework, one test should call the generated/transport layer, normalize the response,
and assert the domain result. Component tests may mock that domain boundary; they should not each
reimplement JSON, date, money, or error parsing.
