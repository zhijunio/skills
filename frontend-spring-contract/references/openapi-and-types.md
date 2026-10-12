# OpenAPI, Generated Clients, and Wire Types

## Generator selection

Use a generator that matches the HTTP abstraction, not merely the UI framework:

| Client transport | Suitable OpenAPI Generator target |
| --- | --- |
| Native `fetch`, custom fetch wrapper, framework-neutral SDK | `typescript-fetch` |
| Axios shared by React or Vue | `typescript-axios` |
| TanStack Query integration | a maintained generator/plugin that explicitly targets the installed TanStack Query major |

Pin the generator and configuration. Generate into a disposable directory in CI and fail when its
diff does not match the committed output. Review the OpenAPI document diff first; generated code is
an effect of that document.

Generator flags cannot recover semantics absent from the document. If `BigDecimal` and `double`
both appear as `number/double`, a client-side type mapping cannot distinguish them. Correct the
server schema so exact values are `type: string`.

## Exact type pipeline

| PostgreSQL | Java | JSON/OpenAPI | TypeScript wire type |
| --- | --- | --- | --- |
| `bigint` | `Long` | string | branded `string` identifier |
| `integer`, `smallint` | `Integer`, `Short` | number | `number` |
| `numeric`, `money` | `BigDecimal` | string | `string`, mapped to an exact domain type |
| `double precision`, `real` | `Double`, `Float` | number | `number` |
| `uuid` | `UUID` | string/uuid | branded `string` |
| `bytea` | `byte[]` | base64 string | `string` |
| constrained text/enum | Java enum | string enum | literal union |
| `jsonb` | record or `JsonNode` | declared object | explicit interface, never ambient `any` |

JavaScript integers are exact only through `Number.MAX_SAFE_INTEGER` (`9007199254740991`). Exact
decimals such as `0.1` are not exactly representable as binary floating point. Once JSON parsing has
rounded a value, the client cannot repair it.

Keep generated wire DTOs separate from domain models. Parse decimal strings exactly at the API
boundary. Do not use `parseFloat`, multiply a parsed amount by 100, or rely on `toFixed` for
financial arithmetic. Keep identifier brands distinct so a customer id cannot be passed where an
invoice id is expected.

## Dates and times

| Meaning | Java | Wire | TypeScript domain representation |
| --- | --- | --- | --- |
| Calendar date | `LocalDate` | `YYYY-MM-DD` | string or `Temporal.PlainDate` when supported |
| Wall-clock time | `LocalTime` | `HH:mm[:ss]` | string or a dedicated local-time type |
| Instant | `Instant` | ISO 8601 with `Z` | `Date`, Temporal instant, or validated string |
| Offset timestamp | `OffsetDateTime` | ISO 8601 with offset | validated string or instant plus retained offset |
| Local date-time | `LocalDateTime` | ISO local date-time | string plus an explicit zone supplied by the contract |

`new Date('2026-04-06')` interprets the date as an instant at UTC midnight, so formatting it in a
timezone west of UTC can display the previous day. Keep `LocalDate` as a date-only value.

OpenAPI `date-time` cannot distinguish every Java time meaning. If a zone is business data, publish
it as a separate field. JavaScript dates have millisecond precision, so do not round-trip a higher-
precision database timestamp through the browser as an unchanged concurrency value.

## Verification

- Inspect the emitted OpenAPI schema for every changed DTO.
- Inspect generated types for requiredness, numeric strings, enums, and date mappings.
- Round-trip an identifier greater than `Number.MAX_SAFE_INTEGER` and an amount with trailing scale.
- Run date tests in at least one timezone west of UTC.
- Regenerate twice and require no second diff.
