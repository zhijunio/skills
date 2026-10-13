# 05 — A more professional front-end

**In this chapter:**
- Improve the visual hierarchy with Bootstrap
- Add empty, loading, feedback, and validation states
- Review the resulting Vue changes and commit a frontend checkpoint

Read the [Vue integration guide](../references/frontend/vue.md) before changing the frontend. It covers the Vite proxy, normalization, tests, and production packaging.

---

## 1. What “professional-looking” means here

For this workshop, the inventory screen should make the product form and current stock easy to understand at a glance. It should behave well on a phone and desktop, give clear feedback for every action, and keep accessibility and API behavior intact. Treat visual polish as small, reviewable prompts rather than one redesign request.

## 2. Add Bootstrap structure

Ask the agent to improve the basic layout:

```text
Polish the inventory frontend using Bootstrap 5. Add a top navbar with the app
name "Inventory" and a clear page heading. Put the product form and inventory
list in responsive Bootstrap cards. Show SKU, name, category, price, and stock
quantity with useful spacing and readable labels. Keep the current API routes
and behavior. Use Bootstrap Icons only if already installed; otherwise explain
the dependency before adding it.
```

Review the result in a browser at desktop and mobile widths. Commit this layout checkpoint before continuing.

## 3. Empty states

```text
When no products match the current category filter, show a friendly empty state
inside the inventory card. Distinguish “there are no products yet” from “no
products match this filter.” Keep the form available and do not change the API.
```

Verify both the empty inventory and an empty filtered result.

## 4. Toasts for feedback

```text
Add toast notifications for product creation, update, and deletion. Use concise
success messages and a clear error message when a request fails. Place toasts
consistently, make them dismissible, and auto-dismiss them after a few seconds.
Do not hide errors that the user needs to correct.
```

Create, update, and delete a product. Check that success and failure feedback match the operation.

## 5. Loading spinners

```text
Show a small loading indicator while products are being fetched. While a
create/update request is in progress, show a pending label and disable the
submit action. Disable a delete button while its request is in flight. Prevent
duplicate submissions without adding artificial delays.
```

Use browser DevTools network throttling if requests finish too quickly to observe the states.

## 6. Inline form validation

```text
Validate required SKU and name fields before submission. Show field-level
messages, associate them accessibly with their inputs, and focus the first
invalid field after submit. Continue to display server-side validation errors;
the backend remains authoritative. Do not duplicate business rules that the
API does not enforce.
```

Test blank fields, server-rejected input, keyboard navigation, and error-message announcements.

## 7. Review what the agent produced

Inspect the frontend diff and the browser behavior. Check that loading and error states do not show stale data as current, buttons remain usable after failed requests, and the UI still follows the API contract. Confirm the Vue normalizer ran before the first npm install and that the production build writes assets to the Spring Boot static-resource location.

Useful follow-up prompts:

```text
Walk me through the frontend changes file by file. Identify any duplicated
state, accessibility gaps, or API assumptions. Do not edit files yet.
```

```text
The toast reports success before the API request completes. Tie it to the
request result, preserve the error response on failure, and add or update a
focused frontend test.
```

## Useful prompt patterns for UX work

Use small prompts that name the visible behavior and preserve the existing API. Avoid asking for a vague redesign or changing backend and frontend architecture in one step.

**Checkpoint**

- Layout, empty states, toasts, loading feedback, and validation work.
- Frontend lint, tests, and production build pass using scripts in `package.json`.
- The backend package serves the frontend assets.
- The reviewed frontend changes are committed.

**Next →** [Chapter 06 — Testing](06-testing.md)
