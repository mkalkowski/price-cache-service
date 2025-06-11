# Matsuri Price Cache Service - Design Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Sequence Diagrams](#sequence-diagrams)
3. [Domain Model](#domain-model)
4. [Architecture Design](#architecture-design)
5. [Design Patterns](#design-patterns)
6. [Implementation Details](#implementation-details)
7. [API Documentation](#api-documentation)

## System Overview

The Matsuri Price Cache Service is a high-performance, scalable solution for caching and distributing financial instrument price data from multiple vendors. The system provides:

- **Price Publishing**: Vendors can publish price updates via REST API
- **Price Retrieval**: Clients can query prices by vendor, instrument, or specific combinations
- **Real-time Distribution**: Price updates are distributed to downstream systems using Aeron.io
- **Automatic Cleanup**: Prices older than 30 days are automatically removed
- **High Performance**: In-memory storage with O(1) lookups and efficient indexing

## Sequence Diagrams

### 1. Price Publication Flow

```mermaid
sequenceDiagram
    participant Vendor as Price Vendor
    participant API as REST Controller
    participant Service as Price Cache Service
    participant Repo as Price Repository
    participant Dist as Distribution Service
    participant Aeron as Aeron Publisher
    participant Client as Downstream Client

    Vendor->>API: POST /api/prices (PriceRequest)
    API->>API: Validate request
    API->>Service: publishPrice(Price)
    Service->>Repo: save(Price)
    Repo->>Repo: Update indexes
    Service->>Dist: distributePrice(Price)
    Dist->>Aeron: Publish message
    Aeron->>Client: Price update notification
    Service->>API: Success
    API->>Vendor: 201 Created
```

### 2. Price Retrieval Flow

```mermaid
sequenceDiagram
    participant Client as API Client
    participant API as REST Controller
    participant Service as Price Cache Service
    participant Repo as Price Repository

    Client->>API: GET /api/prices/{instrument}/{vendor}
    API->>Service: getPrice(instrumentId, vendorId)
    Service->>Repo: findByInstrumentAndVendor(id, vendor)
    Repo->>Repo: Lookup in price store
    Repo->>Service: Optional<Price>
    Service->>API: Optional<Price>
    API->>Client: 200 OK with Price or 404 Not Found
```

### 3. Bulk Query Flow

```mermaid
sequenceDiagram
    participant Client as API Client
    participant API as REST Controller
    participant Service as Price Cache Service
    participant Repo as Price Repository

    Client->>API: GET /api/prices/vendor/{vendorId}
    API->>Service: getPricesByVendor(vendorId)
    Service->>Repo: findByVendor(vendorId)
    Repo->>Repo: Use vendor index
    Repo->>Service: List<Price>
    Service->>API: List<Price>
    API->>Client: 200 OK with price list
```

### 4. Cleanup Process Flow

```mermaid
sequenceDiagram
    participant Scheduler as Spring Scheduler
    participant Cleanup as Cleanup Service
    participant Service as Price Cache Service
    participant Repo as Price Repository

    Scheduler->>Cleanup: @Scheduled trigger (daily 2 AM)
    Cleanup->>Service: cleanupOldPrices()
    Service->>Repo: count() [before]
    Service->>Repo: deleteOlderThan(cutoffDate)
    Repo->>Repo: Remove old prices and update indexes
    Service->>Repo: count() [after]
    Service->>Cleanup: Cleanup complete
```

## Domain Model

### Core Domain Objects

```mermaid
classDiagram
    class Price {
        -String instrumentId
        -String vendorId
        -BigDecimal bidPrice
        -BigDecimal askPrice
        -LocalDateTime timestamp
        -String currency
        +getCompositeKey() String
    }

    class PriceRequest {
        -String instrumentId
        -String vendorId
        -BigDecimal bidPrice
        -BigDecimal askPrice
        -LocalDateTime timestamp
        -String currency
        +toPrice() Price
    }

    PriceRequest --> Price : converts to
    Price --> PriceMessage : wrapped in
```

### Key Design Decisions for Domain Model

1. **Immutable Price Objects**: The `Price` class is immutable to ensure thread safety and prevent accidental modifications
2. **Composite Key Strategy**: Using `instrumentId_vendorId` as a composite key for efficient lookups
3. **BigDecimal for Prices**: Using `BigDecimal` instead of `double` to avoid floating-point precision issues
4. **Validation at Construction**: Domain rules are enforced at object creation time
5. **Separation of Concerns**: DTOs (`PriceRequest`) separate from domain objects (`Price`)

## Architecture Design

### High-Level Architecture

```mermaid
graph TB
    subgraph "External Systems"
        V1[Vendor 1]
        V2[Vendor 2]
        V3[Vendor N]
        C1[Consumer 1]
        C2[Consumer 2]
        C3[Consumer N]
    end

    subgraph "Price Cache Service"
        subgraph "Presentation Layer"
            REST[REST Controller]
        end
        
        subgraph "Business Layer"
            PCS[Price Cache Service]
            PDS[Price Distribution Service]
            CS[Cleanup Service]
        end
        
        subgraph "Data Layer"
            PR[Price Repository]
            IM[In-Memory Store]
        end
        
        subgraph "Integration Layer"
            AERON[Aeron Publisher]
        end
    end

    V1 --> REST
    V2 --> REST
    V3 --> REST
    
    REST --> PCS
    PCS --> PR
    PCS --> PDS
    PR --> IM
    PDS --> AERON
    CS --> PCS
    
    AERON --> C1
    AERON --> C2
    AERON --> C3
```

### Layer Responsibilities

1. **Presentation Layer**: REST API endpoints, request validation, response formatting
2. **Business Layer**: Core business logic, orchestration, price caching rules
3. **Data Layer**: Data persistence, indexing, query optimization
4. **Integration Layer**: External system communication, message distribution

## Design Patterns

### 1. Repository Pattern
**Implementation**: `PriceRepository` interface with `InMemoryPriceRepository`

**Benefits**:
- Abstracts data access logic
- Easy to swap implementations (database, cache, etc.)
- Testable through mocking
- Consistent API for data operations

### 2. Strategy Pattern
**Implementation**: `PriceDistributionService` interface with `AeronPriceDistributionService`

**Benefits**:
- Pluggable distribution mechanisms
- Can add multiple distribution channels
- Easy to test and mock

### 3. Service Layer Pattern
**Implementation**: `PriceCacheService` coordinates between repository and distribution

**Benefits**:
- Centralized business logic
- Transaction management
- Clear separation of concerns

### 4. Data Transfer Object (DTO) Pattern
**Implementation**: `PriceRequest` for API communication

**Benefits**:
- API versioning flexibility
- Input validation
- Decoupling of internal domain from external API

### 5. Observer Pattern / Pub-Sub (via Aeron)
**Implementation**: Aeron-based message distribution

**Benefits**:
- Loose coupling between publishers and consumers
- Scalable event distribution
- Real-time notifications

## Implementation Details

### Thread Safety

1. **ConcurrentHashMap**: Used for all internal storage to ensure thread safety
2. **Immutable Domain Objects**: `Price` objects are immutable
3. **Atomic Operations**: Repository operations are atomic
4. **Aeron Thread Safety**: Aeron handles concurrent publishing internally

###

## API Documentation

API Documentation can be found at http://localhost:8080/swagger-ui.html