---
name: elegant-architecture
description: Guides clean architecture design with strict 200-line file limits. Use when starting new features, refactoring large files, or planning module structure (TypeScript or Java). Enforces modular design and integration-first testing.
---
# Elegant Architecture

## Core Principles

- **200-line limit** — No file exceeds 200 lines of code
- **Split when exceeded** — Convert to folder or multiple files
- **Plan first, code later** — Design architecture before implementation
- **Single responsibility** — Each module does one thing well
- **Real tests for core logic** — Prefer in-memory or fake adapters over mocking your own modules; mock only external IO when needed

## Execution Flow

### 1. Analyze Requirements

```markdown
Before writing any code:
- List all features/functionalities needed
- Estimate code volume for each module
- Identify shared components
- Map dependencies between modules
```

### 2. Design File Structure

When estimated lines > 200:

- Convert file to folder with index (TypeScript) or package subfolders (Java)
- Split by sub-functionality
- Extract shared utilities

**Example transformation (TypeScript):**

```text
Before (user.ts — 400+ lines):
  user.ts

After (user/ folder):
  user/
  ├── index.ts        # Public exports
  ├── types.ts        # Interfaces, types
  ├── validation.ts   # Input validation
  ├── repository.ts   # Data access
  └── service.ts      # Business logic
```

### 3. Define Interfaces First

```typescript
// Define contracts before implementation
interface UserService {
  create(input: CreateUserInput): Promise<User>;
  findById(id: string): Promise<User | null>;
  update(id: string, input: UpdateUserInput): Promise<User>;
  delete(id: string): Promise<void>;
}

interface UserRepository {
  save(user: User): Promise<User>;
  findById(id: string): Promise<User | null>;
  findByEmail(email: string): Promise<User | null>;
  delete(id: string): Promise<void>;
}
```

### 4. Implement Incrementally

```markdown
For each module:
1. Create type definitions
2. Implement core logic
3. Add error handling
4. Write tests
5. Verify line count < 200
```

### 5. Test Without Mocks

Prefer **real or in-memory implementations** for domain and application code. Mock **external** systems (payment API, email, clock) when a fake is expensive to build.

```typescript
// ❌ Avoid: Mock your own repository/service stack
const mockRepo = jest.fn();
const service = new UserService(mockRepo);

// ✅ Prefer: Real or in-memory adapter
const testDb = createTestDatabase();
const repo = new UserRepository(testDb);
const service = new UserService(repo);

const user = await service.create({ email: 'test@example.com' });
const found = await service.findById(user.id);
expect(found).toEqual(user);
```

```java
// ✅ Prefer: @DataJpaTest + real schema, or in-memory fake implementing the port
@DataJpaTest
class OrderRepositoryTest {
    @Autowired OrderRepository orders;

    @Test
    void savesOrder() {
        var saved = orders.save(new OrderEntity("cust_1", 1000));
        assertThat(orders.findById(saved.getId())).isPresent();
    }
}

// ❌ Avoid: @Mock OrderRepository when testing OrderService business rules
// ✅ OK: @Mock PaymentGateway when charging an external provider
```

## Design Patterns

### Modular Design

```
src/
├── modules/
│   ├── auth/
│   │   ├── index.ts
│   │   ├── types.ts
│   │   ├── service.ts
│   │   └── middleware.ts
│   ├── user/
│   │   ├── index.ts
│   │   ├── types.ts
│   │   ├── service.ts
│   │   └── repository.ts
│   └── order/
│       ├── index.ts
│       ├── types.ts
│       ├── service.ts
│       └── repository.ts
├── shared/
│   ├── database/
│   ├── errors/
│   └── utils/
└── index.ts
```

### Java / Spring Package Layout

Apply the same **200-line rule per `.java` file**. Split by layer and responsibility, not by mechanical `controller/service/repository` folders unless the team already uses that layout.

| TypeScript (this skill) | Java / Spring (typical mapping) |
|-------------------------|----------------------------------|
| `index.ts` (public exports) | Package `package-info.java` or small facade; keep Spring `@Configuration` wiring separate |
| `types.ts` | `domain/` records, entities, value objects, port interfaces |
| `validation.ts` | `validation/` or Bean Validation on request DTOs |
| `repository.ts` (port) | `domain/` or `application/` port interface |
| `service.ts` | `application/` use-case / `@Service` (thin orchestration) |
| `middleware.ts` | `api/` filters, interceptors, `@ControllerAdvice` |
| Composition root | `@Configuration`, `@SpringBootApplication`, or module `@Configuration` classes |

**Example transformation (Java):**

```text
Before (UserService.java — 450+ lines):
  user/UserService.java

After (bounded context package):
  com/example/user/
  ├── api/
  │   └── UserController.java       # HTTP only
  ├── application/
  │   └── UserService.java          # Use cases
  ├── domain/
  │   ├── User.java
  │   ├── UserRepository.java       # Port
  │   └── UserValidator.java
  └── infrastructure/
      └── JpaUserRepository.java    # Adapter

  com/example/shared/
  ├── errors/
  └── config/
```

**Spring slice tests** (see `comprehensive-testing`): `@WebMvcTest` for controllers, `@DataJpaTest` for repositories — still keep each test class focused and files under 200 lines.

### Dependency Injection

```typescript
// Decouple components via constructor injection
class OrderService {
  constructor(
    private readonly orderRepo: OrderRepository,
    private readonly userService: UserService,
    private readonly paymentGateway: PaymentGateway
  ) {}

  async createOrder(userId: string, items: OrderItem[]): Promise<Order> {
    const user = await this.userService.findById(userId);
    if (!user) throw new NotFoundError('User', userId);

    const order = Order.create(user, items);
    await this.paymentGateway.charge(user, order.total);
    return this.orderRepo.save(order);
  }
}

// Wire up in composition root
const orderService = new OrderService(
  new PostgresOrderRepository(db),
  new UserService(userRepo),
  new StripePaymentGateway(stripeClient)
);
```

### Factory Pattern

```typescript
// Complex object creation
class NotificationFactory {
  create(type: NotificationType, data: NotificationData): Notification {
    switch (type) {
      case 'email':
        return new EmailNotification(data, this.emailClient);
      case 'sms':
        return new SmsNotification(data, this.smsClient);
      case 'push':
        return new PushNotification(data, this.pushClient);
      default:
        throw new Error(`Unknown notification type: ${type}`);
    }
  }
}
```

### Strategy Pattern

```typescript
// Replaceable algorithms
interface PricingStrategy {
  calculate(order: Order): Money;
}

class StandardPricing implements PricingStrategy {
  calculate(order: Order): Money {
    return order.items.reduce((sum, item) => sum.add(item.price), Money.zero());
  }
}

class DiscountPricing implements PricingStrategy {
  constructor(private readonly discount: Percentage) {}

  calculate(order: Order): Money {
    const standard = new StandardPricing().calculate(order);
    return standard.subtract(standard.multiply(this.discount));
  }
}

class OrderProcessor {
  constructor(private pricing: PricingStrategy) {}

  setPricing(strategy: PricingStrategy) {
    this.pricing = strategy;
  }

  process(order: Order): ProcessedOrder {
    const total = this.pricing.calculate(order);
    return { ...order, total };
  }
}
```

## File Splitting Guidelines

### When to Split

| Indicator | Action |
|-----------|--------|
| File > 200 lines | Split immediately |
| File > 150 lines | Plan split |
| 3+ distinct responsibilities | Split by responsibility |
| Shared types growing | Extract to types.ts |
| Utility functions accumulating | Extract to utils.ts |

### How to Split

```markdown
1. Identify logical boundaries
2. Create folder with same name as file
3. Move related code to separate files
4. Create index.ts for public exports
5. Update imports in dependent files
```

### Naming Conventions

```
module/
├── index.ts          # Public API exports
├── types.ts          # Interfaces, types, enums
├── constants.ts      # Configuration, magic values
├── utils.ts          # Helper functions
├── service.ts        # Business logic
├── repository.ts     # Data access
├── validation.ts     # Input validation
└── errors.ts         # Custom errors
```

## Related Skills (when to use which)

| Situation | Skill | Why |
|-----------|-------|-----|
| New feature layout, split files, 200-line discipline | **elegant-architecture** (this skill) | Fast structural conventions + DI patterns |
| Mature repo, migration debt, boundary contracts, P0 roadmap | **architecture-foundation** | Evidence, source-of-truth map, borrow/do_not_copy — **does not enforce 200 lines** |
| Test pyramid, JUnit slice tests, mock vs fake guidance | **comprehensive-testing** | **Allows mocks at boundaries**; complements "no mock your own stack" here |
| Full-repo audit (silent degradation, registry drift) | **codebase-audit** | Parallel deep review — not a layout guide |

**How to reconcile**

1. **200-line rule** — Default for hand-written modules. Exceptions (document in PR/RFC): generated code, large enum/registries, migration scripts, framework boilerplate. If a file is >200 lines because of **mixed responsibilities**, split. If it is one cohesive algorithm, consider `architecture-foundation` before mechanical chopping.
2. **Testing** — This skill: integration-first for **your** domain/application layers. `comprehensive-testing`: mock **external** APIs, use fakes for DB, Spring `@MockBean` only for **outside** the slice under test. Do not mock your own repository when testing business rules.
3. **Order of use** — Greenfield slice: **elegant-architecture** (structure) → **comprehensive-testing** (how to test) → implement. Brownfield refactor: **architecture-foundation** (spec + convergence plan) → **elegant-architecture** (split steps) → tests per **comprehensive-testing**.

## Architecture Checklist

```markdown
## Pre-Implementation
- [ ] Requirements analyzed
- [ ] Code volume estimated
- [ ] File structure designed
- [ ] Interfaces defined
- [ ] Dependencies mapped

## Implementation
- [ ] Each file < 200 lines
- [ ] Single responsibility per module
- [ ] Dependencies injected
- [ ] Error handling complete
- [ ] No hardcoded values

## Testing
- [ ] Real or in-memory adapters for core logic
- [ ] Mocks only for external IO (or use fakes from comprehensive-testing)
- [ ] Edge cases covered
- [ ] Integration or slice tests exist

## Review
- [ ] Architecture documented
- [ ] Public APIs clear
- [ ] No circular dependencies
- [ ] Easy to extend
```

## Anti-Patterns to Avoid

```markdown
❌ God files (500+ lines doing everything)
❌ Mocking everything in tests
❌ Coding before planning
❌ Tight coupling between modules
❌ Hardcoded configuration
❌ Circular dependencies
❌ Unclear module boundaries
```

## Key Principles

1. **Measure twice, cut once** — Plan architecture before coding
2. **Small is beautiful** — 200 lines default; document justified exceptions
3. **Test reality** — Integration-first for your code; mock external boundaries only
4. **Inject dependencies** — Loose coupling, easy testing
5. **Single purpose** — One reason to change per module
