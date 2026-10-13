# 04 — Adding categories

**In this chapter:**
- Give the agent a cross-layer prompt that changes the database, API, and UI
- Review the diff before running the application
- Iterate with small follow-up prompts and commit the result

By the end, products will belong to a category, the form will let users choose one, and the inventory list will be filterable by category.

---

## 1. Set up the session

Work from the application directory. Confirm the previous checkpoint is clean:

```bash
cd ~/workshop/inventory-app
git status
```

If there are uncommitted changes, review and commit or stash them first so this feature has its own diff.

## 2. The prompt

Ask the agent to add product categories without introducing authentication:

```text
Use spring-boot-developer to add categories to the inventory application. I do not
want authentication or user accounts.

Add categories General, Stationery, and Electronics. Products must belong to one
category. Add a category dropdown when creating or editing a product, and a
category filter above the inventory list. Preserve existing products by
assigning them to General when the schema changes. Keep the category list
available from the backend; do not hardcode separate category data in Vue.

Inspect the current API, persistence mapping, migrations, tests, and Vue
components first. Follow the project's JPA/PostgreSQL setup and conventions.
Keep existing product behavior. Add a migration that handles existing rows,
validate category IDs, use DTOs at the HTTP boundary, and test the category
endpoints and filtering. Do not add Spring Security. Run the relevant checks
and report the exact commands and results.
```

Let the agent complete the backend, schema, and UI changes. If it proposes a plan, check how it will backfill existing rows before accepting implementation.

## 3. What you'll probably see

The implementation may vary, but it should cover these behaviors:

1. A category model and persistence mapping for the three seeded categories.
2. A migration that creates categories, assigns existing products to General, then enforces the required relationship.
3. An API endpoint that lists categories and a category filter on the product list.
4. Create/update requests that accept a category ID and reject unknown IDs.
5. Vue dropdowns that display category names while submitting IDs.
6. Tests for category listing, assignment, filtering, unknown categories, and existing product compatibility.

The agent may choose a join, a string-backed enum, or a different mapping. Ask it to explain the trade-offs and keep one consistent model across persistence, API, and UI.

## 4. Review the diff

Before running the application, inspect the complete change:

```bash
git status
git diff
```

Read the migration and persistence mapping together. Verify existing products receive a category before a non-null constraint is applied. Check that the API returns category names and stable IDs and that Vue uses the backend category list rather than duplicating it.

Ask:

- Does the category filter affect the list request and the visible products?
- Can a product be created and edited without losing its category?
- Does the migration preserve existing products and data?
- Did the agent add authentication or dependencies that were not requested?
- Are errors for unknown category IDs understandable and tested?

## 5. Iterate with follow-up prompts

Ask for one focused correction at a time. For example:

**If existing products fail migration:**

```text
The category column becomes non-null before existing products are assigned a
category. Reorder the migration: create and seed categories, backfill existing
products to General, then add the non-null constraint. Preserve the rest of the
reviewed changes and add a migration-focused test if the project supports one.
```

**If filtering is only client-side:**

```text
The category dropdown changes but the backend list is not filtered. Add a
categoryId query parameter to the existing products endpoint and have the Vue
client request the selected category. Keep the existing unfiltered behavior.
```

Each follow-up should produce a small new diff. Re-read it before continuing.

## 6. Run the app

Start the application with its wrapper:

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Verify the category dropdown, product create/edit behavior, and category filter. Exercise the API too:

```bash
curl -i http://localhost:8080/api/categories
CATEGORY_ID="paste-a-category-id-from-the-response"
curl -i "http://localhost:8080/api/products?categoryId=$CATEGORY_ID"
```

Use an ID returned by the category endpoint. If the agent chose different route names, verify the routes in its controller and OpenAPI output. Stop the application with Ctrl+C after the checks.

## 7. Peek at the database (optional)

Ask the agent to identify the Compose service and show a read-only query for products and categories. Use the actual service name and credentials from the documented configuration; do not guess service names or print secrets.

## 8. Commit the checkpoint

Run the relevant backend and frontend checks, review the final diff, and commit the category feature using the repository's commit convention. Confirm the checkpoint with `git log --oneline`.

---

**Common pitfalls**

| Symptom | Likely cause | Follow-up |
|---|---|---|
| Startup fails because a category is missing | The migration adds a required relation without backfilling old products | Backfill existing rows before enforcing `NOT NULL` |
| The dropdown shows numeric IDs | The client renders the identifier instead of the category name | Use category names as labels and IDs as option values |
| The filter changes the UI but not the results | The query parameter is not sent or the API ignores it | Trace the request in browser DevTools and align the API contract |
| Categories duplicate after restart | Seed logic is not idempotent | Add a uniqueness constraint and seed only missing categories |

**Checkpoint**

- Existing and new products have valid categories.
- Category assignment and filtering work in the UI and API.
- Backend and frontend checks pass, or a specific blocker is recorded.
- The reviewed changes are committed.

**Next →** [Chapter 05 — Professional frontend](05-professional-frontend.md)
