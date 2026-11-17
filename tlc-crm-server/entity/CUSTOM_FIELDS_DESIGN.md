# Custom Fields Implementation — Design Document

## Table of Contents

- [1. Introduction & Background](#1-introduction--background)
- [2. Database Design](#2-database-design)
- [3. Scalability Considerations](#3-scalability-considerations)
- [4. Table Limits & Sharding Strategy](#4-table-limits--sharding-strategy)
- [5. Column Mapping & Relationships](#5-column-mapping--relationships)
- [6. Data Access & Querying](#6-data-access--querying)
- [7. Code Changes Overview](#7-code-changes-overview)
- [8. Trade-offs & Decisions](#8-trade-offs--decisions)

---

## 1. Introduction & Background

### 1.1 Problem Statement

The CRM system supports custom fields for four entity types: **Contact**, **Company**, **Deal**, and **Product**. Each custom field can be one of **35 different data types** (single string, dropdown, phone, email, address, monetary, etc.).

### 1.2 Legacy Design (Old Approach)

In the previous architecture:

- Each source (Contact, Company, Deal, Product) had **multiple tables** — one per data type (18 different types)
- **Intermediate mapping tables** connected fields to their values
- **Separate value tables** stored actual data per type

**Old Schema (Simplified):**

```
Contact
  ├── mcocontactStringFieldValues        (single string values)
  ├── mcocontactNumberFieldValues        (number values)
  ├── mcocontactDropdownFieldValues      (dropdown option IDs)
  ├── mcocontactPhoneFieldValues         (phone references)
  ├── mcocontactEmailFieldValues         (email references)
  ├── mcocontactAddressFieldValues       (address references)
  ├── mcocontactMonetaryFieldValues      (monetary values)
  ├── mcocontactDateFieldValues          (date values)
  ├── … (18 tables total per source)
  └── mcocontactFieldMapping             (field → value table mapping)
```

**Problems with the old design:**

| Problem | Impact |
|---------|--------|
| Heavy JOINs | Fetching all custom fields required joining 18+ tables |
| Performance degradation | Query time grew linearly with number of custom fields |
| Data fix difficulty | Debugging and fixing data required understanding complex table relationships |
| Schema complexity | 18 tables × 4 sources = 72+ custom field tables |
| Write amplification | Creating/updating an entity touched multiple tables |

### 1.3 New Design (Current Approach)

The new architecture consolidates all custom field values into **4 tables** (one per source), where each table contains columns for all supported data types in a **single wider row**.

**New Schema:**

```
Contact  → mcocontactcustomFieldValues   (1 table, all types in columns)
Company  → mcocompanycustomFieldValues   (1 table, all types in columns)
Deal     → mdodealcustomFieldValues      (1 table, all types in columns)
Product  → mpoproductcustomFieldValues   (1 table, all types in columns)
```

---

## 2. Database Design

### 2.1 Table Structure (One Per Source)

Each entity has a **single wider table** that holds all custom field values in typed columns:

| Entity | Table Name |
|--------|-----------|
| Contact | `mcocontactcustomFieldValues` |
| Company | `mcocompanycustomFieldValues` |
| Deal | `mdodealcustomFieldValues` |
| Product | `mpoproductcustomFieldValues` |

**Physical Table Layout:**

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  mcocontactcustomFieldValues                                                             │
├──────┬───────────┬────────┬──────┬──────┬──────┬─────────────────┬─────────────────┬────┤
│  ID  │ ENTITY_ID │ ORG_ID │ SN1  │ SN2  │ SN3  │ DROPDOWN_OPT_1 │ DROPDOWN_OPT_2 │ …  │
├──────┼───────────┼────────┼──────┼──────┼──────┼─────────────────┼─────────────────┼────┤
│ 101  │   5001    │ 18651  │ val  │ val  │ NULL │     77830       │      NULL       │ …  │
│ 102  │   5002    │ 18651  │ val  │ NULL │ NULL │      NULL       │      NULL       │ …  │
└──────┴───────────┴────────┴──────┴──────┴──────┴─────────────────┴─────────────────┴────┘
```

**Key characteristics:**
- **One row per entity** — each Contact/Company/Deal/Product has exactly one row
- **Typed columns** — columns are prefixed by store type (`SN` for strings, `OPTION` for dropdowns, `PHONE_ID` for phones, etc.)
- **NULL for unused** — unassigned column slots remain NULL
- **ORG_ID isolation** — multi-tenant data separation

### 2.2 Column Naming Convention

Columns follow the pattern: `<PREFIX><GLOBAL_INDEX>`

| Store Type | Column Prefix | Example Columns |
|-----------|--------------|-----------------|
| SINGLE_STRING | `SN` | SN1, SN2, SN3, … SN50 |
| TEXT | `TX` | TX1, TX2, … |
| SINGLE_NUMBER | `NUM` | NUM1, NUM2, … |
| DROPDOWN | `D_OPTION_ID` | D_OPTION_ID1, D_OPTION_ID2, … |
| MULTI_SELECT | `M_OPTION_ID` | M_OPTION_ID1, … |
| PHONE | `PHONE_ID` | PHONE_ID1, PHONE_ID2, … |
| EMAIL | `EMAIL_ID` | EMAIL_ID1, EMAIL_ID2, … |
| ADDRESS | `ADDRESS_ID` | ADDRESS_ID1, … |
| MONETARY | `MONETARY_ID` | MONETARY_ID1, … |
| DATE | `DT` | DT1, DT2, … |
| TIME | `TM` | TM1, TM2, … |
| OWNER | `OWNER_ID` | OWNER_ID1, … |

### 2.3 Supported Store Types (35 Types)

| # | Store Type | Category | Storage |
|---|-----------|----------|---------|
| 1 | SINGLE_STRING | Single Value | VARCHAR column |
| 2 | TEXT | Single Value | TEXT column |
| 3 | SINGLE_NUMBER | Single Value | NUMERIC column |
| 4 | DROPDOWN | Model Store | BIGINT (option ID) |
| 5 | MULTI_SELECT | Multi Value | BIGINT ARRAY |
| 6 | OWNER | Model Store | BIGINT (user ID) |
| 7 | CONTACT | Model Store | BIGINT (contact ID) |
| 8 | COMPANY | Model Store | BIGINT (company ID) |
| 9 | PHONE | Model Store | BIGINT (phone record ID) |
| 10 | EMAIL | Model Store | BIGINT (email record ID) |
| 11 | MONETARY | Model Store | BIGINT (monetary record ID) |
| 12 | TIME | Single Value | TIME column |
| 13 | TIME_RANGE | Model Store | BIGINT (range record ID) |
| 14 | DATE | Single Value | DATE column |
| 15 | DATE_RANGE | Model Store | BIGINT (range record ID) |
| 16 | DATE_TIME_RANGE | Model Store | BIGINT (range record ID) |
| 17 | ADDRESS | Model Store | BIGINT (address record ID) |
| 18 | COMPANY_ONBOARD | Source Dependent | BIGINT |
| 19 | CONTACT_ONBOARD | Source Dependent | BIGINT |
| 20 | DEAL_VALUE | Source Dependent | BIGINT (monetary record ID) |
| 21 | VARIATION | Source Dependent | BIGINT ARRAY |
| 22 | PRICE_TAGS | Source Dependent | BIGINT ARRAY |
| 23 | CONTACTS_ONBOARD | Source Dependent | BIGINT ARRAY |
| 24 | PHONES | Array Type | BIGINT ARRAY |
| 25 | EMAILS | Array Type | BIGINT ARRAY |
| 26 | DEAL | Source Dependent | BIGINT |
| 27 | PRODUCT | Source Dependent | BIGINT |
| 28 | TERRITORY | Source Dependent | BIGINT |
| 29 | TAGS | Source Dependent | BIGINT ARRAY |
| 30 | ICON | Source Dependent | BIGINT |
| 31 | NOTE | Source Dependent | TEXT |
| 32 | OWNERS | Array Type | BIGINT ARRAY |
| 33 | DATE_TIME | Source Dependent | TIMESTAMP |
| 34 | IDENTIFIER | Single Value | VARCHAR |
| 35 | AUTO_NUMBER | Single Value | VARCHAR |

### 2.4 Metadata Tables

#### `METFieldStore` — Store Configuration

Defines the capacity and mapping for each store type per source:

| Column | Type | Description |
|--------|------|-------------|
| ID | BIGINT | Primary key |
| STORE_TYPE | VARCHAR | Store type name (e.g., `SINGLE_STRING`) |
| IS_MODEL_STORE | BOOLEAN | Whether values are stored as model references |
| COLUMN | VARCHAR | Column name prefix (e.g., `SN`, `OPTION`) |
| TABLE_NAME | VARCHAR | Base wider table name |
| COLUMN_RANGE | VARCHAR | Max columns per table (e.g., `1-50`) |
| TABLE_RANGE | VARCHAR | Max number of overflow tables (e.g., `1-3`) |
| SOURCE_ID | BIGINT | FK to entity source |
| IS_PERSISTENT | BOOLEAN | `true` = wider table, `false` = legacy |

#### `OMETFieldColumn` — Field-to-Column Tracking

Maps each custom field to its physical storage location:

| Column | Type | Description |
|--------|------|-------------|
| ID | BIGINT | Primary key |
| FIELD_ID | BIGINT | FK to the field definition |
| STORE_ID | BIGINT | FK to `METFieldStore` |
| SOURCE_ID | BIGINT | FK to entity source |
| ORG_ID | BIGINT | Organization ID |
| TABLE_INDEX | SMALLINT | Which physical table (1, 2, 3, …) |
| COLUMN_INDEX | INTEGER | Global column index across all tables |

### 2.5 Old vs New Schema Comparison

| Aspect | Old Design | New Design |
|--------|-----------|-----------|
| Tables per source | 18+ (one per type) | 1 (wider table) |
| Total tables (4 sources) | 72+ | 4 |
| Rows per entity | 1 per type table (if field exists) | 1 total |
| JOINs to fetch all fields | Up to 18 | 0 (single table scan) |
| Mapping tables | Required | Eliminated |
| NULL overhead | None (no row if no value) | NULL columns for unused slots |
| Schema complexity | High | Low |
| Query performance | Degrades with field count | Constant |

---

## 3. Scalability Considerations

### 3.1 Row Growth

| Factor | Strategy |
|--------|----------|
| Increasing entities | Standard PostgreSQL table growth; `ENTITY_ID + ORG_ID` composite index handles lookups |
| Multi-tenancy | `ORG_ID` column with index ensures tenant isolation without separate tables |
| Read scaling | Single row fetch per entity — no JOIN amplification |
| Write scaling | Single INSERT/UPDATE per entity per table |

### 3.2 Column Expansion (Adding New Data Types)

When a new data type is introduced:

1. Add new column(s) to the wider table in `meta.xml`
2. Register the new store type in `METFieldStore`
3. Existing rows automatically have `NULL` for the new columns (no data migration)

**PostgreSQL advantage:** Adding a nullable column with no default is a metadata-only operation — instant, regardless of table size.

### 3.3 Schema Evolution Strategy

```
New Store Type Required
        │
        ▼
┌────────────────────────────┐
│ 1. Add column prefix in    │
│    METFieldStore           │───► e.g., COLUMN = "NEWTYPE_ID"
└────────────────────────────┘
        │
        ▼
┌────────────────────────────┐
│ 2. Declare columns in      │
│    meta.xml                │───► NEWTYPE_ID1, NEWTYPE_ID2, …
└────────────────────────────┘
        │
        ▼
┌────────────────────────────┐
│ 3. Rebuild & Redeploy      │───► DDL applied at startup
└────────────────────────────┘
```

### 3.4 Backward Compatibility

- Legacy fields with `IS_PERSISTENT = false` continue using old mapping tables via `FieldStorageType.TRANSIENT`
- No data migration required — old and new coexist
- The `FieldInfo.table()` method dynamically resolves to either the wider table or the legacy table based on storage type

---

## 4. Table Limits & Sharding Strategy

### 4.1 PostgreSQL Column Limits

- PostgreSQL supports up to **~1,600 columns** per table
- Each wider table allocates columns per store type (e.g., 50 string columns, 50 dropdown columns)
- Practical limit depends on total column count across all store types in one table

### 4.2 Configurable Capacity

Capacity is controlled by two configuration values in `METFieldStore`:

```
COLUMN_RANGE = "1-50"    → 50 columns per store type per table
TABLE_RANGE  = "1-3"     → Up to 3 tables per store type

Total capacity = 50 × 3 = 150 fields per store type per source
```

### 4.3 Overflow Table Strategy (Horizontal Sharding)

When a table's column slots are exhausted, the system allocates to the **next overflow table**:

```
┌─────────────────────────────────────────────────────┐
│  mcocontactcustomFieldValues  (Table Index 1)        │
│  Columns: SN1 – SN50  (Global Index 1–50)           │
└─────────────────────────────────────────────────────┘
                         │ FULL (all 50 slots used)
                         ▼
┌─────────────────────────────────────────────────────┐
│  mcocontactcustomFieldValues2  (Table Index 2)       │
│  Columns: SN51 – SN100  (Global Index 51–100)       │
└─────────────────────────────────────────────────────┘
                         │ FULL
                         ▼
┌─────────────────────────────────────────────────────┐
│  mcocontactcustomFieldValues3  (Table Index 3)       │
│  Columns: SN101 – SN150  (Global Index 101–150)     │
└─────────────────────────────────────────────────────┘
```

### 4.4 Table Index Computation

```java
// Which table does a global column index belong to?
tableIndex = ((globalColumnIndex - 1) / columnsPerTable) + 1

// What is the local column slot within that table?
columnSlot = ((globalColumnIndex - 1) % columnsPerTable) + 1
```

**Examples (columnsPerTable = 50):**

| Global Index | Table Index | Local Slot |
|:---:|:---:|:---:|
| 1 | 1 | 1 |
| 50 | 1 | 50 |
| 51 | 2 | 1 |
| 100 | 2 | 50 |
| 101 | 3 | 1 |
| 150 | 3 | 50 |

### 4.5 Table Naming Convention

| Table Index | Table Name |
|:-----------:|-----------|
| 1 | `mcocontactcustomFieldValues` (base) |
| 2 | `mcocontactcustomFieldValues2` |
| 3 | `mcocontactcustomFieldValues3` |
| N | `mcocontactcustomFieldValues<N>` |

### 4.6 Steps to Create a New Overflow Table

| Step | Action | Where |
|:----:|--------|-------|
| 1 | Declare the new table with typed columns | `meta.xml` |
| 2 | Increase `TABLE_RANGE` upper bound | `METFieldStore` (SQL/seed data) |
| 3 | Rebuild & redeploy the module | Build pipeline |
| 4 | Verify the table exists in PostgreSQL | Database |

> ⚠️ **The application does NOT auto-create tables or columns.** All physical schema must be declared in `meta.xml`.

### 4.7 Overflow Table meta.xml Declaration

```xml
<table name="mcocontactcustomFieldValues2">
    <column name="ID"         type="BIGINT"  pk="true" />
    <column name="ENTITY_ID"  type="BIGINT"  notnull="true" />
    <column name="ORG_ID"     type="BIGINT"  notnull="true" />

    <!-- Single-string columns -->
    <column name="SN1"  type="VARCHAR" max-size="255" />
    <column name="SN2"  type="VARCHAR" max-size="255" />
    <!-- … up to COLUMN_RANGE upper bound … -->
    <column name="SN50" type="VARCHAR" max-size="255" />

    <!-- Dropdown columns -->
    <column name="D_OPTION_ID1"  type="BIGINT" />
    <column name="D_OPTION_ID2"  type="BIGINT" />
    <!-- … other typed columns … -->
</table>
```

---

## 5. Column Mapping & Relationships

### 5.1 Field → Physical Storage Resolution

Every custom field is tracked in `OMETFieldColumn`, which records:
- Which **table** (via `TABLE_INDEX`)
- Which **column** (via `COLUMN_INDEX`)
- Which **store** (via `STORE_ID` → `METFieldStore`)

```
Field Definition (OMETFields)
        │
        │ FIELD_ID
        ▼
OMETFieldColumn (Tracking Table)
        │
        ├── STORE_ID ──────► METFieldStore (column prefix, table name, ranges)
        ├── TABLE_INDEX ───► Which physical table (1, 2, 3…)
        └── COLUMN_INDEX ──► Global column index
                │
                ▼
Physical Resolution:
    Table = baseTableName + (tableIndex > 1 ? tableIndex : "")
    Column = columnPrefix + globalColumnIndex
```

### 5.2 Resolution Examples

| Field | Store Type | Store Column | Global Index | Table Index | Physical Table | Physical Column |
|-------|-----------|-------------|:---:|:---:|----------------|----------------|
| "Company Size" | SINGLE_STRING | SN | 3 | 1 | mcocontactcustomFieldValues | SN3 |
| "Priority" | DROPDOWN | OPTION | 1 | 1 | mcocontactcustomFieldValues | OPTION1 |
| "Work Phone" | PHONE | PHONE_ID | 51 | 2 | mcocontactcustomFieldValues2 | PHONE_ID51 |

### 5.3 Consistency Across Multiple Tables

- The `OMETFieldColumn` table is the **single source of truth** for all field-to-column mappings
- When a field is **created**, a new row is inserted with the allocated table/column index
- When a field is **deleted**, the corresponding `OMETFieldColumn` row is removed, freeing the slot for reuse
- **No cross-table references** — each store type's overflow tables are independent

### 5.4 Relationship Diagram

```
┌──────────────────┐         ┌──────────────────────┐
│  OMETFields      │         │    METFieldStore      │
│  (Field Def)     │         │  (Store Config)       │
│                  │         │                        │
│  ID ─────────────┼────┐    │  ID ──────────────────┼───┐
│  NAME            │    │    │  STORE_TYPE            │   │
│  TYPE            │    │    │  COLUMN (prefix)       │   │
│  SOURCE_ID       │    │    │  TABLE_NAME (base)     │   │
└──────────────────┘    │    │  COLUMN_RANGE          │   │
                        │    │  TABLE_RANGE           │   │
                        │    │  IS_PERSISTENT         │   │
                        │    └──────────────────────────┘   │
                        │                                    │
                        ▼                                    ▼
               ┌─────────────────────────────────────────────────┐
               │              OMETFieldColumn                      │
               │           (Field ↔ Physical Mapping)             │
               │                                                   │
               │  ID | FIELD_ID | STORE_ID | ORG_ID | TABLE_IDX | COL_IDX │
               │  ---|----------|----------|--------|-----------|---------|
               │  1  │   101    │    5     │ 18651  │     1     │    3    │
               │  2  │   102    │    8     │ 18651  │     1     │    1    │
               │  3  │   201    │    5     │ 18651  │     2     │   51    │
               └─────────────────────────────────────────────────────────────┘
                        │
                        ▼ (resolved at runtime)
               ┌───────────────────────────────────────┐
               │  Physical Tables                       │
               │                                       │
               │  mcocontactcustomFieldValues (idx=1)   │
               │  mcocontactcustomFieldValues2 (idx=2)  │
               │  mcocontactcustomFieldValues3 (idx=3)  │
               └───────────────────────────────────────┘
```

---

## 6. Data Access & Querying

### 6.1 How the New Design Reduces JOINs

**Old approach** — fetching all custom fields for a contact:

```sql
-- Required 18+ JOINs
SELECT c.*,
       s.value AS string_val,
       n.value AS number_val,
       d.option_id AS dropdown_val,
       p.phone_id,
       e.email_id,
       …
FROM contacts c
LEFT JOIN mcocontactStringFieldValues s ON c.ID = s.ENTITY_ID
LEFT JOIN mcocontactNumberFieldValues n ON c.ID = n.ENTITY_ID
LEFT JOIN mcocontactDropdownFieldValues d ON c.ID = d.ENTITY_ID
LEFT JOIN mcocontactPhoneFieldValues p ON c.ID = p.ENTITY_ID
LEFT JOIN mcocontactEmailFieldValues e ON c.ID = e.ENTITY_ID
-- … 13+ more JOINs
WHERE c.ORG_ID = ? AND c.ID = ?;
```

**New approach** — single table scan:

```sql
-- Zero JOINs for custom field values
SELECT *
FROM mcocontactcustomFieldValues
WHERE ENTITY_ID = ? AND ORG_ID = ?;
```

### 6.2 Query Patterns

#### Fetching All Custom Fields for an Entity

```sql
SELECT SN1, SN2, SN3, D_OPTION_ID1, PHONE_ID1, …
FROM mcocontactcustomFieldValues
WHERE ENTITY_ID = :entityId AND ORG_ID = :orgId;
```

If the entity has fields in overflow tables:

```sql
-- Table 1
SELECT * FROM mcocontactcustomFieldValues
WHERE ENTITY_ID = :entityId AND ORG_ID = :orgId;

-- Table 2 (if fields exist there)
SELECT * FROM mcocontactcustomFieldValues2
WHERE ENTITY_ID = :entityId AND ORG_ID = :orgId;
```

#### Inserting a New Entity's Custom Field Values

```sql
-- First custom field write creates the row
INSERT INTO mcocontactcustomFieldValues (ID, ENTITY_ID, ORG_ID, SN1)
VALUES (:id, :entityId, :orgId, :value);
```

#### Updating a Specific Custom Field

```sql
-- Updates only the specific column
UPDATE mcocontactcustomFieldValues
SET SN3 = :newValue
WHERE ENTITY_ID = :entityId AND ORG_ID = :orgId;
```

#### Clearing a Custom Field Value

```sql
UPDATE mcocontactcustomFieldValues
SET SN3 = NULL
WHERE ENTITY_ID = :entityId AND ORG_ID = :orgId;
```

### 6.3 Index Strategy

| Index | Columns | Purpose |
|-------|---------|---------|
| Primary | `ID` | Unique row identifier |
| Lookup | `ENTITY_ID, ORG_ID` | Primary access pattern |
| Org filter | `ORG_ID` | Tenant-level queries |

---

## 7. Code Changes Overview

### 7.1 Components Layer (`tlc-components`)

| Component | Change |
|-----------|--------|
| `sql` module | Added support for wider table column resolution and array data types |
| `entity` module | New `FieldColumn`, `FieldInfo`, `PersistentFieldTableAllocator` classes |
| `fields` module | New `FieldStore` model with `columnRange`/`tableRange`, `StoreType` enum (35 types) |

### 7.2 Server Layer (`tlc-crm-server`)

| Module | Key Changes |
|--------|-------------|
| `entity` | `PersistentFieldTableAllocator` — allocates column slots |
| | `FieldColumn` — runtime model for field → physical table/column |
| | `FieldInfo` / `FieldInfoBuilder` — cached descriptor with dynamic resolution |
| | `AbstractCrmFieldsManager` — field CRUD creates `FieldColumn` entries |
| `fields` | `FieldStore` model — store configuration (ranges, prefixes, tables) |
| | `FieldStoreService` — manages store lookup and caching |
| `core` | `DealMergeManager` — merge with wider-table row cleanup |
| | Field value read/write handlers updated for wider table access |

### 7.3 Key Classes & Their Responsibilities

| Class | Responsibility |
|-------|---------------|
| `PersistentFieldTableAllocator` | Finds the first free column slot across all tables for a store type |
| `FieldColumn` | Resolves physical table name and column name at runtime |
| `FieldInfo` | Cached field descriptor; used throughout the app for table/column resolution |
| `FieldInfoBuilder` | Constructs `FieldInfo` from `Field` + `FieldColumn` + `FieldConfig` |
| `FieldStore` | Configuration record: prefix, base table, column/table ranges |
| `FieldStoreService` | Singleton service managing `FieldStore` lookup and caching |
| `AbstractCrmFieldsManager` | Orchestrates field creation including `FieldColumn` allocation |
| `DuplicateManager` | Handles entity merge with wider-table awareness |

### 7.4 Field Creation Flow (Code Path)

```
API Request: Create Custom Field "Preferred Language" (SINGLE_STRING, Contact)
    │
    ▼
FieldAction.handleCreate(request)
    │
    ▼
AbstractCrmFieldsManager.create(field)
    │
    ├── 1. Validate field definition
    ├── 2. Determine FieldStore (storeType=SINGLE_STRING, isPersistent=true)
    ├── 3. Load used column slots from OMETFieldColumn
    │
    ├── 4. PersistentFieldTableAllocator.allocateColumnSlot(fieldStore, usedSlots)
    │       ├── columnsPerTable = 50  (from COLUMN_RANGE "1-50")
    │       ├── maxTables = 3         (from TABLE_RANGE "1-3")
    │       ├── Iterate global index 1→150, find first free → e.g., 51
    │       ├── tableIndex = ((51-1)/50)+1 = 2
    │       └── return FieldTableAllocation(tableIndex=2, columnSlot=51, tableName="…Values2")
    │
    ├── 5. Insert OMETFieldColumn row:
    │       (FIELD_ID=201, TABLE_INDEX=2, COLUMN_INDEX=51, STORE_ID=5, ORG_ID=18651)
    │
    ├── 6. Build FieldInfo via FieldInfoBuilder:
    │       columnIndex=51, tableIndex=2
    │       table() → "mcocontactcustomFieldValues2"
    │       columnName() → "SN51"
    │
    └── 7. Cache FieldInfo for runtime use
```

### 7.5 Field Value Read Flow

```
API Request: Get Contact #5001 with all custom fields
    │
    ▼
EntityReader.read(contactId=5001, orgId=18651)
    │
    ├── 1. Load field definitions for org (cached FieldInfo list)
    ├── 2. Group fields by tableIndex
    │       Table 1: [SN1, SN2, OPTION1, PHONE_ID1, …]
    │       Table 2: [SN51, SN52, …]
    │
    ├── 3. Query each table:
    │       SELECT SN1, SN2, OPTION1, PHONE_ID1 FROM mcocontactcustomFieldValues
    │           WHERE ENTITY_ID=5001 AND ORG_ID=18651
    │       SELECT SN51, SN52 FROM mcocontactcustomFieldValues2
    │           WHERE ENTITY_ID=5001 AND ORG_ID=18651
    │
    └── 4. Map column values back to field definitions using FieldInfo
```

---

## 8. Trade-offs & Decisions

### 8.1 Why the New Approach Was Chosen

| Decision Factor | Reasoning |
|-----------------|-----------|
| **Query performance** | Single table scan vs 18+ JOINs — 10-20x faster reads |
| **Schema simplicity** | 4 tables vs 72+ tables — dramatically simpler to understand and maintain |
| **Write efficiency** | Single INSERT/UPDATE per table vs multiple mapping table writes |
| **Data debugging** | One row contains all values — easy to inspect and fix |
| **Scalability** | Horizontal overflow via configurable table ranges |

### 8.2 Pros of the New Design

| Advantage | Details |
|-----------|---------|
| ✅ **Dramatically fewer JOINs** | 0 JOINs for custom field fetch vs 18+ |
| ✅ **Simpler data model** | 4 tables instead of 72+ |
| ✅ **Faster reads** | Single row scan per entity |
| ✅ **Faster writes** | Single INSERT/UPDATE |
| ✅ **Easy data debugging** | All values visible in one row |
| ✅ **Configurable limits** | `COLUMN_RANGE` and `TABLE_RANGE` per store type |
| ✅ **No data migration** | New columns are nullable — instant DDL |
| ✅ **Multi-tenant isolation** | `ORG_ID` on every row |
| ✅ **Backward compatible** | Legacy `TRANSIENT` fields still work |

### 8.3 Cons / Trade-offs

| Trade-off | Mitigation |
|-----------|-----------|
| ⚠️ **Wide tables (many columns)** | PostgreSQL handles ~1600 columns; overflow tables split the load |
| ⚠️ **NULL storage overhead** | PostgreSQL NULL bitmap is efficient (1 bit per column); minimal overhead |
| ⚠️ **Manual schema changes** | `meta.xml` declaration required; automated via deployment pipeline |
| ⚠️ **Overflow table complexity** | `PersistentFieldTableAllocator` abstracts this entirely from consuming code |
| ⚠️ **Column slot waste on deletion** | Deleted field's slot is freed and reused by next allocation |

### 8.4 Future Handling

| Scenario | Approach |
|----------|----------|
| **New data type** | Add new column prefix in `METFieldStore`, declare columns in `meta.xml`, redeploy |
| **More fields needed** | Increase `TABLE_RANGE`, declare overflow table in `meta.xml`, redeploy |
| **More columns per table** | Increase `COLUMN_RANGE`, declare new columns in `meta.xml`, redeploy |
| **New entity source** | Create new wider table in `meta.xml`, register in `METFieldStore` |
| **Performance at scale** | Partitioning by `ORG_ID` if needed; overflow tables already provide horizontal split |

---

## Appendix A: Architecture Diagram

```
                         ┌─────────────────────────┐
                         │      METFieldStore       │
                         │  (Configuration Table)   │
                         │                          │
                         │  COLUMN_RANGE = "1-50"   │
                         │  TABLE_RANGE  = "1-3"    │
                         │  COLUMN       = "SN"     │
                         │  TABLE_NAME   = "mco…"   │
                         │  IS_PERSISTENT = true     │
                         └────────────┬──────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                  ▼
         ┌──────────────┐  ┌──────────────┐   ┌──────────────┐
         │ OMETField    │  │ OMETField    │   │ OMETField    │
         │ Column       │  │ Column       │   │ Column       │
         │              │  │              │   │              │
         │ FIELD_ID=101 │  │ FIELD_ID=102 │   │ FIELD_ID=151 │
         │ TABLE_IDX=1  │  │ TABLE_IDX=1  │   │ TABLE_IDX=2  │
         │ COL_IDX=1    │  │ COL_IDX=2    │   │ COL_IDX=51   │
         └──────┬───────┘  └──────┬───────┘   └──────┬───────┘
                │                 │                   │
                ▼                 ▼                   ▼
  ┌──────────────────────────────────────┐  ┌──────────────────────────┐
  │  mcocontactcustomFieldValues         │  │ mcocontactcustomField    │
  │  (Table Index 1)                     │  │ Values2 (Table Index 2)  │
  │                                      │  │                          │
  │  ID | ENTITY_ID | ORG_ID | SN1 | SN2│  │ ID | ENTITY_ID | SN51   │
  │  ---|-----------|--------|-----|-----│  │ ---|-----------|------   │
  │  1  |   5001    | 18651  | abc | def │  │ 2  |   5001    | ghi    │
  └──────────────────────────────────────┘  └──────────────────────────┘
```

---

## Appendix B: Key Source Files

| File | Path | Purpose |
|------|------|---------|
| `PersistentFieldTableAllocator.java` | `entity/src/…/manager/` | Allocates table + column slots for new persistent fields |
| `FieldColumn.java` | `entity/src/…/models/internal/` | Runtime model mapping field → physical table + column |
| `FieldInfo.java` | `entity/src/…/models/` | Cached field descriptor with table/column resolution |
| `FieldInfoBuilder.java` | `entity/src/…/builder/` | Builds `FieldInfo` from `Field` + `FieldColumn` + `FieldConfig` |
| `FieldStore.java` | `fields/src/…/models/` | Store configuration record (range, table, column prefix) |
| `FieldConfig.java` | `entity/src/…/models/internal/` | Per-field preset configuration |
| `StoreType.java` | `fields/src/…/models/` | Enum of all 35 supported store types |
| `METFieldStore` (SQL resource) | `fields/src/…/sql/resource/` | SQL constants for `METFieldStore` table |
| `OMETFieldColumn` (SQL resource) | `fields/src/…/sql/resource/` | SQL constants for `OMETFieldColumn` tracking table |
| `AbstractCrmFieldsManager.java` | `entity/src/…/manager/impl/` | Field CRUD — creates `FieldColumn` entries during creation |
| `DuplicateManager.java` | `entity/src/…/duplicates/api/` | Handles merge/dedup with wider-table awareness |
| `DealMergeManager.java` | `core/src/…/deal/` | Deal-specific merge with wider-table row cleanup |

---

## Appendix C: Configuration Examples

### Increasing Field Capacity

```sql
-- Increase column range from 50 to 100 per table
UPDATE "METFieldStore"
SET "COLUMN_RANGE" = '1-100'
WHERE "TABLE_NAME" = 'mcocontactcustomFieldValues'
  AND "COLUMN" = 'SN';

-- Allow 3 overflow tables (total capacity = 100 × 3 = 300 fields)
UPDATE "METFieldStore"
SET "TABLE_RANGE" = '1-3'
WHERE "TABLE_NAME" = 'mcocontactcustomFieldValues'
  AND "COLUMN" = 'SN';
```

### Verifying Current Configuration

```sql
SELECT "TABLE_NAME", "COLUMN", "COLUMN_RANGE", "TABLE_RANGE", "IS_PERSISTENT"
FROM "METFieldStore"
WHERE "SOURCE_ID" = :contactSourceId
ORDER BY "COLUMN";
```

### Checking Field Slot Usage

```sql
SELECT fc."COLUMN_INDEX", fc."TABLE_INDEX", f."NAME", fs."COLUMN"
FROM "OMETFieldColumn" fc
JOIN "OMETFields" f ON fc."FIELD_ID" = f."ID"
JOIN "METFieldStore" fs ON fc."STORE_ID" = fs."ID"
WHERE fc."ORG_ID" = :orgId AND fc."SOURCE_ID" = :contactSourceId
ORDER BY fs."COLUMN", fc."COLUMN_INDEX";
```

