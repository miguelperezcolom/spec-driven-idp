# spec-driven-idp

A spec-driven code generator for DDD (Domain-Driven Design) Java projects. You describe your domain in a YAML file following the Domain DSL schema, and the generator produces the boilerplate Java code for you: aggregates, value objects, enums, events, repositories, domain services, use cases, and test stubs.

## How it works

```
specs/*.yaml  →  DomainGeneratorMain  →  src/main/java  +  src/test/java
```

1. You write one or more YAML spec files under `specs/`.
2. Maven runs the generator automatically during the `generate-sources` phase.
3. For each spec the generator produces two layers of code:
   - **`generated/`** — abstract base classes, always overwritten on every build.
   - **`manual/`** (or the base package) — concrete stubs that are only created once and are yours to edit.

## Project structure

```
spec-driven-idp/
├── pom.xml
├── specs/
│   ├── schema/
│   │   └── domain-dsl-schema-v0-1.json   # JSON Schema for the DSL
│   └── *.yaml                             # Your domain specs go here
└── src/
    └── main/java/com/acme/generator/
        ├── DomainGeneratorMain.java        # Entry point
        ├── generators/                     # One generator per DDD concept
        ├── model/                          # Parsed spec POJOs
        ├── parser/                         # YAML → model (SnakeYAML)
        ├── util/                           # TypeMapper (DSL types → Java types)
        └── writer/                         # SourceFileWriter (generated vs manual)
```

## Running the generator

The generator is wired to Maven's `generate-sources` phase, so a normal build runs it automatically:

```bash
mvn generate-sources
```

Or run a full build including tests:

```bash
mvn verify
```

### Running manually

```bash
java -cp target/classes:... com.acme.generator.DomainGeneratorMain \
  <specsDir> <srcOutputDir> [testOutputDir]
```

| Argument | Description |
|---|---|
| `specsDir` | Directory containing `*.yaml` spec files |
| `srcOutputDir` | Root of `src/main/java` |
| `testOutputDir` | Root of `src/test/java` (optional, defaults to `../test/java`) |

## Writing a spec

Spec files must conform to `specs/schema/domain-dsl-schema-v0-1.json` (JSON Schema 2020-12). The minimum required structure is:

```yaml
$schema: "../schema/domain-dsl-schema-v0-1.json"

domain:
  name: my-domain
  package: com.example.mydomain

valueObjects:
  Money:
    type: object
    fields:
      amount:
        type: decimal
        constraints:
          min: 0
      currency:
        type: string
        constraints:
          maxLength: 3

enums:
  OrderStatus:
    values: [PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED]

aggregates:
  Order:
    id:
      name: orderId
      type: uuid
    fields:
      status:
        type: OrderStatus
      total:
        type: Money
    commands:
      create:
        factory: true
        parameters:
          total: Money
        emits: [OrderCreated]
      confirm:
        preconditions: ["status == PENDING"]
        sets:
          status: CONFIRMED
        emits: [OrderConfirmed]
    stateMachine:
      field: status
      initial: PENDING
      terminal: [DELIVERED, CANCELLED]
      transitions:
        - { from: PENDING,    to: CONFIRMED, command: confirm  }
        - { from: CONFIRMED,  to: SHIPPED,   command: ship     }
        - { from: SHIPPED,    to: DELIVERED, command: deliver  }
        - { from: PENDING,    to: CANCELLED, command: cancel   }

events:
  OrderCreated:
    aggregate: Order
    version: 1
    fields:
      orderId: uuid
      total:   Money

repositories:
  OrderRepository:
    aggregate: Order
    methods:
      findById:
        by: orderId
        parameter: uuid
        returns: Order
        required: true

tests:
  generate:
    domain:
      invariants: true
      stateMachine: true
      commands: true
```

## DSL type reference

| DSL type | Java type |
|---|---|
| `string` | `String` |
| `integer` | `int` |
| `long` | `long` |
| `decimal` | `java.math.BigDecimal` |
| `boolean` | `boolean` |
| `date` | `java.time.LocalDate` |
| `instant` | `java.time.Instant` |
| `uuid` | `java.util.UUID` |
| `list` | `java.util.List<T>` (requires `itemType`) |
| Any other name | Resolved as a type in the domain package |

## What gets generated

| Generator | Generated | Manual stub |
|---|---|---|
| `AggregateGenerator` | `AbstractXxx` with fields, getters, command skeletons, state machine | `Xxx extends AbstractXxx` |
| `ValueObjectGenerator` | Immutable record / class | — |
| `EnumGenerator` | Java `enum` | — |
| `EventGenerator` | Event record with version | — |
| `RepositoryGenerator` | Repository interface | — |
| `DomainServiceGenerator` | Domain service interface | Implementation stub |
| `UseCaseGenerator` | Use-case interface | Implementation stub |
| `TestGenerator` | JUnit 5 test stubs | — |
| `DomainEventInterfaceGenerator` | `DomainEvent` marker interface | — |

## Requirements

- Java 17+
- Maven 3.8+
