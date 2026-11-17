---
globs: "**/*.java"
alwaysApply: false
---

## Table of Contents

1. [Purpose & Scope](#1-purpose--scope)
2. [Index Inventory](#2-index-inventory)
3. [Shared Conventions](#3-shared-conventions)
    - 3.1 [Field Type Conventions](#31-field-type-conventions)
    - 3.2 [Analyzers](#32-analyzers)
    - 3.3 [Tenancy & Permission Fields](#33-tenancy--permission-fields)
    - 3.4 [Common Metadata Fields](#34-common-metadata-fields)
4. [Entity Relationship Model](#4-entity-relationship-model)
5. [Searchability Matrix](#5-searchability-matrix)
6. [Index: Company](#6-index-company)
7. [Index: Contact](#7-index-contact)
8. [Index: Deal](#8-index-deal)
9. [Index: Product](#9-index-product)
10. [Index: Activity](#10-index-activity)
11. [Index: Note & File](#11-index-note--file)
12. [Known Inconsistencies & Gotchas](#12-known-inconsistencies--gotchas)
13. [Indexing, Refresh & Reindex](#13-indexing-refresh--reindex)
14. [Appendix: Field Glossary](#14-appendix-field-glossary)

---

## 1. Purpose & Scope

Global Search provides a single query entry point that returns ranked, permission-filtered, de-duplicated results from any CRM record type. Instead of joining relational tables at query time, each index stores a **fully denormalized snapshot** of the record plus the related entities needed to *render* a result card (company name, contact name, deal name, etc.).

Two design principles drive every mapping in this document:

1. **The root entity of an index is searchable; related entities are mostly for display and filtering.**
    - Root names use `text` + `customWhitespaceAnalyzer` → full-text searchable.
    - Related entity names are frequently `keyword` + `index: false` → stored in `_source` for rendering, **not** searchable and **not** filterable.
2. **Identity is by numeric id; presentation is by denormalized name.**
    - `company.id`, `contact.id`, `deal.id` are `long` and remain indexable/filterable so a search hit can be hydrated or used to fetch siblings.
    - The human-readable `name` next to them may or may not be indexed (see the [Searchability Matrix](#5-searchability-matrix)).

**Out of scope:** write-path / CDC pipeline, ranking models, UI behaviour, and any index not listed below.

---

## 2. Index Inventory

| # | Index | Root entity | Cardinality of relationships | Primary search field(s) |
|---|-------|-------------|------------------------------|--------------------------|
| 1 | `company` | Company (individual entity) | No parent. Children referenced by `id` only. | `name`, `website`, `address.*`, `notes.name`, `customFields.name` |
| 2 | `contact` | Contact (may contain **one** company) | `contact.company` (0..1) | `name`, `emails.value`, `phones.number`, `primaryEmail.value`, `company.name` |
| 3 | `deal` | Deal (**should** contain a primary contact, a company) | `deal.contact` (1), `deal.company` (1), `deal.contact.company` (0..1, thin; optional/sparsely populated) | `name`, `company.name`, `contact.name`, `contact.emails.value`, `contact.phones.number` |
| 4 | `product` | Product (individual entity) | None stored | `name`, `productCode`, `customFields.name` |
| 5 | `activity` | Activity (optional Deal, Contact, Company) | `activity.deal` (0..1), `activity.contact` (0..1; may include `contact.company`), `activity.contactIds[]` (0..N), `activity.company` (0..1), `activity.deal.company` / `activity.deal.contact` (flat — **no** `deal.contact.company`) | `name`, `status`, `activityType.name` |
| 6 | `note` / `file` | Note or File (based on Company, Contact, or Deal) | `entitySource` + `entitySourceId` → parent; optional `company` / `contact` (with `contact.company`) / `deal` (`deal.company` + `deal.contact` id+name only — **no** `deal.contact.company`) | `name`, `entitySourceName`, `entitySource` |

---

## 3. Shared Conventions

### 3.1 Field Type Conventions

| Type | Used for | Notes |
|------|----------|-------|
| `long` | All identifiers, foreign keys, epoch timestamps, id arrays | `ownerId`, `orgId`, `teamIds`, `territoryId`, `followerIds`, `expectedCloseDate`, `startTime`, `endTime`, `checkInTime`, `checkOutTime` |
| `text` + `customWhitespaceAnalyzer` | Human-readable names and free-text codes that must be **searchable** | `name`, `website`, `productCode`, `entitySourceName`, `customFields.name`, `notes.name` |
| `text` + `customKeywordAnalyzer` | Values that must be matched **exactly** but still stored as text | `emails.value`, `phones.number`, `primaryEmail.value`, `status`, `entitySource` |
| `keyword` + `index: false` | Denormalized related-entity values used **only for display** | `activity.company.name`, `deal.contact.company.name`, `note.deal.name`, `profileUrl`, all embedded `address.*` in non-root positions |
| `double` | Money amounts | `dealValue.value` |
| `integer` | Counts | `totalProductCount` |
| `boolean` | Flags | `isArchived` |
| `object` (nested-free) | Embedded snapshots | Addresses, company/contact/deal summaries |

> **Important:** none of the mappings use the `nested` type. All embedded objects are plain `object`, which means arrays of objects are flattened. This is acceptable here because every embedded snapshot in practice is **single-valued** (`deal.company` is one company, `contact.company` is one company). If a snapshot ever becomes an array, cross-field matching inside it will become unreliable and `nested` must be introduced.

### 3.2 Analyzers

Two custom analyzers are referenced throughout the mappings. Their definitions live in the index settings, not the mappings.

| Analyzer | Expected behaviour | Applied to | Consequence |
|----------|--------------------|------------|-------------|
| `customWhitespaceAnalyzer` | Whitespace tokenization; tokens preserve the source characters (typically with a lowercase filter applied at settings level). No stemming, no stop words. | `name`, `website`, `productCode`, `notes.name`, `customFields.name`, `entitySourceName`, `activityType.name` | Supports multi-word free-text search such as `"Acme Renewal"` and prefix matching per token. Hyphenated values are **not** split (`"Acme-Corp"` is one token). |
| `customKeywordAnalyzer` | Emits the entire input as a **single token**. | `emails.value`, `phones.number`, `primaryEmail.value`, `status`, `entitySource` | Behaves like a `keyword` field for matching purposes: exact, case-sensitive unless a normalizer/lowercase filter is configured. Correct choice for `jane.doe@acme.com`, `+1-415-555-0100`, `OPEN`. |

**Verification note:** because both analyzers are referenced but not defined in the supplied mappings, confirm the actual tokenizer/filter chain in the index settings before relying on case-insensitivity. If `customWhitespaceAnalyzer` does **not** include a lowercase filter, then `"acme"` will not match `"Acme"` and a `lowercase` normalizer must be added.

### 3.3 Tenancy & Permission Fields

Every index carries the same permission envelope. Any Global Search query **must** include an `orgId` term filter; omitting it leaks data across tenants.

| Field | Type | Semantics | Present in |
|-------|------|-----------|------------|
| `orgId` | `long` | Tenant / organization. Mandatory filter on every query. | All 6 indexes |
| `ownerId` | `long` | Single record owner (the "Owner" lookup). | All except `activity` (which also uses `ownerIds`) |
| `ownerIds` | `long` | Multi-owner array used by activities. | `activity` only |
| `followerIds` | `long` | Users following the record (implicit array — same field name maps many values). | `company`, `contact`, `deal`, `product`, `activity`* |
| `teamIds` | `long` | Teams with access. Implicit array. | `company`, `contact`, `deal`, `product`, `note`; also on every embedded company/contact snapshot |
| `territoryId` | `long` | Territory scoping (single). | `company`, `contact`, `deal`, `product`, `note`; also on embedded snapshots |
| `territoryIds` | `long` | Territory scoping (multi). | `activity` only |

\* `activity` does not declare a root `followerIds`; it carries `ownerIds` and `territoryIds` instead.

**Visibility predicate (typical):**

```
orgId == :org
AND ( ownerId IN :visibleOwners
   OR followerIds IN :visibleUsers
   OR teamIds IN :visibleTeams
   OR territoryId IN :visibleTerritories )
```

Because `activity` uses the plural forms, the predicate must be built **per index** (see [§12](#12-known-inconsistencies--gotchas)).

### 3.4 Common Metadata Fields

| Field | Type | Notes |
|-------|------|-------|
| `id` | `long` | Primary key of the root entity. |
| `name` | `text` (`customWhitespaceAnalyzer`) | Primary display + primary search field for the root entity. |
| `profileUrl` | `keyword`, `index: false` | Deep link into the CRM UI. Never searched. |
| `customFields` | `object[]` with `name` (`text`) | Flattened list of custom-field **values**. Only `name` is mapped; if the UI needs the custom field's key/label, it must be added to the mapping. |
| `notes` | `object[]` with `id` (`long`) + `name` (`text`) | Denormalized note titles attached to the root record. |
| `address` | `object` (single) | `addressLine1/2`, `city`, `state`, `country`, `zipcode`, `id`. Indexed only when the address belongs to the **root** entity. |

---

## 4. Entity Relationship Model

```
                        ┌────────────────────────┐
                        │        COMPANY         │   (individual entity)
                        │  id, name, website,    │
                        │  address, ownerId,     │
                        │  teamIds, territoryId  │
                        └───────────┬────────────┘
                                    │ 1
        ┌───────────────┬───────────┼────────────┬─────────────────┐
        │ N             │ N         │ N          │ N               │ N
┌───────▼──────┐ ┌──────▼──────┐ ┌──▼────────┐ ┌─▼──────────┐ ┌────▼──────┐
│   CONTACT    │ │    DEAL     │ │ ACTIVITY  │ │ NOTE/FILE  │ │  PRODUCT  │
│ company(0..1)│ │ company(1)  │ │ company?  │ │ company?   │ │ (no rel.) │
└───────┬──────┘ │ contact(1)  │ │ contact?  │ │ contact?   │ └───────────┘
        │ 1      │  ├─company? │ │ deal?     │ │ deal?      │
        │        └──┬──────────┘ │ contactIds│ └────────────┘
        └───────────┘  N         └─────┬─────┘
             (primary contact)          │ N
                                    (activity → deal)
```

**Cardinality summary**

| Relationship | Cardinality | Optional? | Where it is stored |
|--------------|-------------|-----------|--------------------|
| Company → Contact | 1 : N | Yes (contact may have no company) | `contact.company`, `contact.company.id` |
| Company → Deal | 1 : N | No (deal should have a company) | `deal.company` |
| Contact → Deal | 1 : N | No (deal should have a **primary** contact) | `deal.contact` |
| Contact → Contact's Company | N : 1 | Yes | `contact.company`; also `deal.contact.company` (deal index only, thin); `activity.contact.company` / `note.contact.company` (top-level contact snapshots). **Not** on `activity.deal.contact` or `note.deal.contact`. |
| Deal → Activity | 1 : N | Yes | `activity.deal` |
| Contact → Activity | 1 : N | Yes | `activity.contact` (primary) + `activity.contactIds[]` (associated) |
| Company → Activity | 1 : N | Yes | `activity.company` |
| Company/Contact/Deal → Note & File | 1 : N | No (a note always has a source) | `entitySource`, `entitySourceId`, `entitySourceName` |
| Deal ↔ Product | M : N | — | **Not modeled in the search indexes**; only `deal.totalProductCount` is present |

---

## 5. Searchability Matrix

This is the single most important table for query authoring. "Searchable" = the field participates in full-text match. "Filterable" = the field can be used in `term`/`terms` filters. "Display-only" = retrievable from `_source` but neither searchable nor filterable.

| Index | Searchable (full-text) | Filterable (exact) | Display-only (`index: false`) |
|-------|------------------------|--------------------|-------------------------------|
| `company` | `name`, `website`, `address.addressLine1/2`, `address.city`, `address.state`, `address.country`, `address.zipcode`, `notes.name`, `customFields.name` | `id`, `orgId`, `ownerId`, `followerIds`, `teamIds`, `territoryId`, `address.id` | `profileUrl` |
| `contact` | `name`, `emails.value`, `phones.number`, `primaryEmail.value`, `notes.name`, `customFields.name`, **`company.name`**, **`company.website`** | `id`, `orgId`, `ownerId`, `followerIds`, `teamIds`, `territoryId`, `company.id`, `company.ownerId`, `company.followerIds`, `company.teamIds`, `company.territoryId` | `company.address.*`, `profileUrl`, `facebook`, `linkedin`, `emails.type`, `phones.type`, `primaryEmail.type` |
| `deal` | `name`, `notes.content`, `notes.name`, `customFields.name`, **`company.name`**, **`company.website`**, **`contact.name`**, **`contact.emails.value`**, **`contact.phones.number`**, **`contact.primaryEmail.value`** | `id`, `orgId`, `ownerId`, `followerIds`, `teamIds`, `territoryId`, `isArchived`, `status`, `dealValue.value`, `expectedCloseDate`, `totalProductCount`, `company.id`, `contact.id`, `contact.company.id`, `pipeline.id`, `pipelineStage.id`, `dealState.id` | `company.address.*`, `contact.company.name`, `contact.facebook`, `contact.linkedin`, `profileUrl`, `pipeline.name`, `pipelineStage.name`, `dealState.name`, `dealValue.currency` |
| `product` | `name`, `productCode`, `customFields.name` | `id`, `orgId`, `ownerId`, `followerIds`, `teamIds`, `territoryId` | `profileUrl` |
| `activity` | **`name`**, **`status`**, **`activityType.name`** | `id`, `orgId`, `ownerId`, `ownerIds`, `territoryIds`, `startTime`, `endTime`, `checkInTime`, `checkOutTime`, `contactIds`, `activityType.id`, `company.id`, `contact.id`, `contact.company.id`, `deal.id`, `deal.company.id`, `deal.contact.id`, `deal.pipeline.id`, `deal.pipelineStage.id` | `activityType.icon.name`, `company.*` (including `name`, `website`, `address.*`), `contact.*` (including `name`, `contact.company.name`, emails/phones), `deal.name`, `deal.company.name`, `deal.contact.name` (**no** `deal.contact.company` mapping), `isDone`, `checkInInfo`, `checkOutInfo` |
| `note` / `file` | **`name`**, **`entitySourceName`**, **`entitySource`** | `id`, `orgId`, `ownerId`, `followerIds`, `teamIds`, `territoryId`, `entitySourceId`, `company.id`, `contact.id`, `contact.company.id`, `deal.id`, `deal.company.id`, `deal.contact.id` | `company.name`, `company.website`, `company.address.*`, `contact.name`, `contact.company.name`, `contact.*`, `deal.name`, `deal.company.name`, `deal.contact.name` (**no** `deal.contact.company` mapping) |

**Key takeaways**

- **`activity` and `note`/`file` are "thin search" indexes.** You can only match an activity by its own `name`, `status`, or `activityType.name`, and a note only by its own `name` or its parent's `entitySourceName`. Searching for `"Acme"` will **not** return activities or notes belonging to Acme Corp unless the activity/note title itself contains "Acme".
- **`deal` is the richest cross-entity index.** It is the only index where a search for a company name and a search for a contact name can both return the same document.
- **`contact` is the bridge index for "who works at X".** `contact.company.name` is searchable, so a company-name query returns the Company document *and* all of its contacts.
- **`product` is fully isolated.** No relationship fields exist, so product↔deal navigation must be handled outside the search index.

---

## 6. Index: Company

### 6.1 Purpose
The canonical record for an organization/account. It is the top of the hierarchy and the anchor for contacts, deals, activities, and notes.

### 6.2 Mapping

```json
{
  "company": {
    "mappings": {
      "properties": {
        "address": {
          "properties": {
            "addressLine1": { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "addressLine2": { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "city":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "country":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "id":           { "type": "long" },
            "state":        { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "zipcode":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "customFields": {
          "properties": {
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "followerIds":  { "type": "long" },
        "id":           { "type": "long" },
        "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "notes": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "orgId":        { "type": "long" },
        "ownerId":      { "type": "long" },
        "profileUrl":   { "type": "keyword", "index": false },
        "teamIds":      { "type": "long" },
        "territoryId":  { "type": "long" },
        "website":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
      }
    }
  }
}
```

### 6.3 In-depth notes on entities involved

- **Company is a true individual entity.** The document contains no embedded copy of any other entity. Contacts, deals, activities, and notes are **not** denormalized into this index — they exist only as `id` foreign keys inside those other indexes.
- **Consequence for search:** a company hit cannot be expanded into child records without a second query. If the UI shows "3 contacts, 5 open deals" on a company card, those counts must come from a separate `contact`/`deal` query filtered by `company.id`, or from a stored counter that is not part of this mapping.
- **The address is single-valued and root-owned.** Unlike `contact.company.address` (which is `index: false`), a company's own address **is** searchable. This makes `company` the only index where "find the account at 94107" works.
- **`notes` is an array of note stubs** (`id` + `name`). Only the title is indexed; note bodies live in the `note` index. A company can have many notes.
- **`customFields.name` is a multi-valued text field** — it is the flattened list of custom-field values for the company. Because there is no `key`/`label` sibling, `customFields.name` cannot be filtered by field. Adding `customFieldId` (`long`) would be required to scope a query to a specific custom field.
- **`followerIds` and `teamIds` are implicit arrays** (single-valued field definition, multi-valued document). They are filterable via `terms`.
- **`profileUrl` is display-only** by design; deep links are never part of the search corpus.

### 6.4 Sample document

```json
{
  "id": 1001,
  "orgId": 42,
  "name": "Acme Corp",
  "website": "acme.com",
  "ownerId": 7,
  "followerIds": [7, 12, 19],
  "teamIds": [3, 5],
  "territoryId": 11,
  "address": {
    "id": 88,
    "addressLine1": "1 Market St",
    "addressLine2": "Suite 400",
    "city": "San Francisco",
    "state": "CA",
    "country": "USA",
    "zipcode": "94105"
  },
  "customFields": [{ "name": "Enterprise" }, { "name": "Tier 1" }],
  "notes": [{ "id": 501, "name": "Renewal kickoff call" }],
  "profileUrl": "/companies/1001"
}
```

---

## 7. Index: Contact

### 7.1 Purpose
Person records. A contact **may** belong to exactly **one** company.

### 7.2 Mapping

```json
{
  "contact": {
    "mappings": {
      "properties": {
        "address": {
          "properties": {
            "addressLine1": { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "addressLine2": { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "city":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "country":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "id":           { "type": "long" },
            "state":        { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "zipcode":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "company": {
          "properties": {
            "address": {
              "properties": {
                "addressLine1": { "type": "keyword", "index": false },
                "addressLine2": { "type": "keyword", "index": false },
                "city":         { "type": "keyword", "index": false },
                "country":      { "type": "keyword", "index": false },
                "id":           { "type": "long" },
                "state":        { "type": "keyword", "index": false },
                "zipcode":      { "type": "keyword", "index": false }
              }
            },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "ownerId":      { "type": "long" },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" },
            "website":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "customFields": {
          "properties": {
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "emails": {
          "properties": {
            "id":    { "type": "long" },
            "type":  { "type": "keyword", "index": false },
            "value": { "type": "text", "analyzer": "customKeywordAnalyzer" }
          }
        },
        "facebook":     { "type": "keyword", "index": false },
        "followerIds":  { "type": "long" },
        "id":           { "type": "long" },
        "linkedin":     { "type": "keyword", "index": false },
        "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "notes": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "orgId":        { "type": "long" },
        "ownerId":      { "type": "long" },
        "phones": {
          "properties": {
            "id":     { "type": "long" },
            "number": { "type": "text", "analyzer": "customKeywordAnalyzer" },
            "type":   { "type": "keyword", "index": false }
          }
        },
        "primaryEmail": {
          "properties": {
            "id":    { "type": "long" },
            "type":  { "type": "keyword", "index": false },
            "value": { "type": "text", "analyzer": "customKeywordAnalyzer" }
          }
        },
        "profileUrl":   { "type": "keyword", "index": false },
        "teamIds":      { "type": "long" },
        "territoryId":  { "type": "long" }
      }
    }
  }
}
```

### 7.3 In-depth notes on entities involved

**Contact → Company (0..1)**

The contact is the **only** entity in the CRM search model that carries a *single, optional* parent company.

| Case | `company` object | Interpretation | Search behaviour |
|------|------------------|----------------|------------------|
| 1 | Present with `id` + `name` | Contact is employed at a known company | `"Acme"` matches the contact via `company.name` |
| 2 | Absent / `null` | Standalone contact (no company on record, or company not yet linked) | Contact is only findable by its own fields |
| 3 | Present but `id` only | Snapshot partially populated | `company.name` will be missing from `_source`; UI must fall back to a lookup by `company.id` |

**Why the embedded company has split behaviour**

- `company.name` and `company.website` are `text` + `customWhitespaceAnalyzer` → **searchable**. This is deliberate: "show me everyone at Acme" must work from the contact index without a join.
- `company.address.*` is `keyword` + `index: false` → **not searchable**. Searching for contacts by company address is therefore impossible in this index; you must first search the `company` index, collect `company.id` values, then run a `terms` query on `contact.company.id`.
- `company.id`, `company.ownerId`, `company.followerIds`, `company.teamIds`, `company.territoryId` remain **filterable**. This enables permission inheritance (e.g. "show contacts whose company is in my territory").

**Primary email duplication**

`primaryEmail` and `emails[]` overlap — the primary email typically also appears in the `emails` array. Both are indexed, so a query for an email address can produce a single document with two matching clauses. Use `_score` carefully (or de-duplicate by `id`) if ranking is affected.

**One-to-many collections**

| Collection | Cardinality | Notes |
|------------|-------------|-------|
| `emails` | 0..N | Each has `id`, `type` (display-only), `value` (exact-match) |
| `phones` | 0..N | `number` is exact-match, `type` is display-only |
| `notes` | 0..N | Title-only stubs |
| `customFields` | 0..N | Flattened values only |

**Address ownership**

The contact's own `address` is fully searchable (unlike the embedded company address). This is the same root-vs-embedded rule as in the Company index.

### 7.4 Sample documents

**Case A — contact with a company**

```json
{
  "id": 2001,
  "orgId": 42,
  "name": "Jane Doe",
  "ownerId": 7,
  "followerIds": [7, 12],
  "teamIds": [3],
  "territoryId": 11,
  "primaryEmail": { "id": 1, "type": "WORK", "value": "jane.doe@acme.com" },
  "emails": [{ "id": 1, "type": "WORK", "value": "jane.doe@acme.com" }],
  "phones": [{ "id": 9, "type": "MOBILE", "number": "+14155550100" }],
  "company": {
    "id": 1001,
    "name": "Acme Corp",
    "website": "acme.com",
    "ownerId": 7,
    "followerIds": [7],
    "teamIds": [3],
    "territoryId": 11,
    "address": { "id": 88, "city": "San Francisco", "state": "CA", "country": "USA", "zipcode": "94105",
                 "addressLine1": "1 Market St", "addressLine2": "Suite 400" }
  },
  "profileUrl": "/contacts/2001"
}
```

**Case B — standalone contact (no company)**

```json
{
  "id": 2002,
  "orgId": 42,
  "name": "John Smith",
  "ownerId": 7,
  "teamIds": [3],
  "territoryId": 11,
  "emails": [{ "id": 4, "type": "WORK", "value": "john.smith@example.com" }],
  "profileUrl": "/contacts/2002"
}
```

---

## 8. Index: Deal

### 8.1 Purpose
The deal index is the **widest** document in the model. It denormalizes the deal, its company, its primary contact, and that contact's company so a single hit can render a complete deal card without joins.

### 8.2 Mapping

```json
{
  "deal": {
    "mappings": {
      "properties": {
        "company": {
          "properties": {
            "address": {
              "properties": {
                "addressLine1": { "type": "keyword", "index": false },
                "addressLine2": { "type": "keyword", "index": false },
                "city":         { "type": "keyword", "index": false },
                "country":      { "type": "keyword", "index": false },
                "id":           { "type": "keyword", "index": false },
                "state":        { "type": "keyword", "index": false },
                "zipcode":      { "type": "keyword", "index": false }
              }
            },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "ownerId":      { "type": "long" },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" },
            "website":      { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "contact": {
          "properties": {
            "company": {
              "properties": {
                "followerIds":  { "type": "long" },
                "id":           { "type": "long" },
                "name":         { "type": "keyword", "index": false },
                "ownerId":      { "type": "long" }
              }
            },
            "emails": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "text", "analyzer": "customKeywordAnalyzer" }
              }
            },
            "facebook":     { "type": "keyword", "index": false },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "linkedin":     { "type": "keyword", "index": false },
            "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
            "ownerId":      { "type": "long" },
            "phones": {
              "properties": {
                "id":     { "type": "long" },
                "number": { "type": "text", "analyzer": "customKeywordAnalyzer" },
                "type":   { "type": "keyword", "index": false }
              }
            },
            "primaryEmail": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "text", "analyzer": "customKeywordAnalyzer" }
              }
            },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" }
          }
        },
        "customFields": {
          "properties": {
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "dealState": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "keyword", "index": false }
          }
        },
        "dealValue": {
          "properties": {
            "currency": { "type": "keyword", "index": false },
            "id":       { "type": "long" },
            "value":    { "type": "double" }
          }
        },
        "expectedCloseDate": { "type": "long" },
        "followerIds":       { "type": "long" },
        "id":                { "type": "long" },
        "isArchived":        { "type": "boolean" },
        "name":              { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "notes": {
          "properties": {
            "content": {
              "type": "text",
              "fields": {
                "keyword": { "type": "keyword", "ignore_above": 256 }
              }
            },
            "id":   { "type": "long" },
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "orgId":       { "type": "long" },
        "ownerId":     { "type": "long" },
        "pipeline": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "keyword", "index": false }
          }
        },
        "pipelineStage": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "keyword", "index": false }
          }
        },
        "profileUrl":        { "type": "keyword", "index": false },
        "status":            { "type": "text", "analyzer": "customKeywordAnalyzer" },
        "teamIds":           { "type": "long" },
        "territoryId":       { "type": "long" },
        "totalProductCount": { "type": "integer" }
      }
    }
  }
}
```

### 8.3 In-depth notes on entities involved

A deal document contains **up to three company references** and **two contact roles**:

```
Deal
 ├── company                ← the "deal company" / account the deal is booked against
 └── contact                ← the PRIMARY contact (single, required)
      └── company           ← thin employer snapshot (id, name, ownerId, followerIds); optional / sparsely populated
```

> **Live mapping note:** `deal.contact.company` **is** mapped on the deal index (thin: `id`, `name`, `ownerId`, `followerIds`). It is optional and often absent in documents. Do **not** assume the same path exists under `activity.deal.contact` or `note.deal.contact` — those are flat id/name only.

#### The core statement

> A deal **should** have a primary contact, and that primary contact **may** have a company.
> A deal **can also** have a company, which **may be different** from the primary contact's company.
> They may all be the same company, or they can vary.
> Treat `deal.contact.company` as a display/filter aid on the **deal card only**, not a universal employer path across indexes.

#### Case matrix

| # | `deal.company` | `deal.contact` | `deal.contact.company` | Meaning | Rendering rule |
|---|----------------|----------------|------------------------|---------|----------------|
| 1 | Acme Corp | Jane Doe | Acme Corp (when populated) | **Happy path.** One account, contact employed there. | Show "Acme Corp" once; contact secondary line is redundant — suppress it. |
| 2 | Acme Corp | Jane Doe | Globex Consulting (when populated) | Deal is booked against Acme, but the primary contact is an employee of a different company (agency, consultant, subsidiary, or bad data). | Show deal company as the primary account; show "Jane Doe · Globex Consulting" as the contact line when the thin snapshot is present. **Do not merge the two names.** |
| 3 | Acme Corp | Jane Doe | *(absent)* | Contact has no company on record, or the thin snapshot was not written. | Show deal company only. |
| 4 | *(absent / null)* | Jane Doe | Globex Consulting (when populated) | Deal has no account linked yet. | Fall back to displaying `contact.company.name` as the inferred account when present. Flag as "inferred" if the UI distinguishes it. |
| 5 | Acme Corp | *(absent / null)* | — | Deal exists without a primary contact (should be exceptional). | Show deal company only; surface a data-quality warning in the CRM UI, not in search. |
| 6 | *(absent)* | *(absent)* | — | Orphan deal. | Only `name`, owner, pipeline and value are displayable. |

#### Search consequences of the case matrix

- **`deal.company.name` is searchable** (whitespace analyzer). Searching `"Acme"` returns the deal in cases 1–3 and 5.
- **`deal.contact.name`, `deal.contact.emails.value`, `deal.contact.phones.number`, `deal.contact.primaryEmail.value` are searchable.** Searching `"Jane Doe"` or `"jane.doe@acme.com"` returns the deal.
- **`deal.contact.company.name` is `keyword` + `index: false` — NOT searchable.** When the thin snapshot is present in **case 2**, searching `"Globex"` will **not** return this deal. Only the Contact index (where `company.name` *is* indexed) will surface that relationship.
- **`deal.contact.company.id` is `long` and filterable** when present, so a "deals where the primary contact works at company X" facet can still be built — it just cannot be driven by free text.
- **`deal.company.address.id` is `keyword` + `index: false`** while every other `address.id` in the model is `long`. This field is effectively unusable for filtering; use `deal.company.id` instead.

#### Other embedded entities

| Path | Searchable? | Notes |
|------|-------------|-------|
| `deal.notes.content` | ✅ standard analyzer + `.keyword` subfield (`ignore_above: 256`) | The **only** free-text note body indexed in the entire model. Note bodies attached to companies/contacts are not indexed anywhere. |
| `deal.notes.name` | ✅ whitespace | Note title |
| `pipeline.name`, `pipelineStage.name`, `dealState.name` | ❌ `keyword` + `index: false` | Display-only. To filter by stage, use `pipelineStage.id`. |
| `dealValue.value` | ✅ `double` (range filter) | `dealValue.currency` is display-only. |
| `status` | ✅ `customKeywordAnalyzer` | Exact match on the status token (e.g. `OPEN`, `WON`, `LOST`). |
| `isArchived` | ✅ `boolean` | **Global Search should default to `isArchived: false`.** |
| `totalProductCount` | ✅ `integer` | Count only; no product ids are stored, so deal→product navigation requires a separate query. |

#### Timestamps

`expectedCloseDate` is a `long` (epoch milliseconds), not a `date`. Date-range queries must therefore be expressed as numeric ranges, and date math (`now-30d`) is unavailable unless the field is re-mapped to `date` with `format: epoch_millis`.

### 8.4 Sample documents

**Case 1 — same company throughout**

```json
{
  "id": 9001,
  "orgId": 42,
  "name": "Acme Corp - Renewal 2026",
  "ownerId": 7,
  "followerIds": [7, 12],
  "teamIds": [3],
  "territoryId": 11,
  "isArchived": false,
  "status": "OPEN",
  "expectedCloseDate": 1782864000000,
  "dealValue": { "id": 55, "value": 250000.0, "currency": "USD" },
  "pipeline": { "id": 1, "name": "Enterprise" },
  "pipelineStage": { "id": 4, "name": "Negotiation" },
  "dealState": { "id": 2, "name": "In Progress" },
  "totalProductCount": 3,
  "company": {
    "id": 1001,
    "name": "Acme Corp",
    "website": "acme.com",
    "ownerId": 7,
    "followerIds": [7],
    "teamIds": [3],
    "territoryId": 11
  },
  "contact": {
    "id": 2001,
    "name": "Jane Doe",
    "ownerId": 7,
    "followerIds": [7],
    "teamIds": [3],
    "territoryId": 11,
    "emails": [{ "id": 1, "type": "WORK", "value": "jane.doe@acme.com" }],
    "primaryEmail": { "id": 1, "type": "WORK", "value": "jane.doe@acme.com" },
    "phones": [{ "id": 9, "type": "MOBILE", "number": "+14155550100" }],
    "company": { "id": 1001, "name": "Acme Corp", "ownerId": 7, "followerIds": [7] }
  },
  "profileUrl": "/deals/9001"
}
```

**Case 2 — deal company ≠ primary contact's company**

```json
{
  "id": 9002,
  "orgId": 42,
  "name": "Acme Corp - Advisory Engagement",
  "ownerId": 7,
  "status": "OPEN",
  "expectedCloseDate": 1782864000000,
  "dealValue": { "id": 56, "value": 48000.0, "currency": "USD" },
  "company": { "id": 1001, "name": "Acme Corp", "website": "acme.com" },
  "contact": {
    "id": 2003,
    "name": "Priya Raman",
    "emails": [{ "id": 11, "type": "WORK", "value": "priya@globex.com" }],
    "company": { "id": 1002, "name": "Globex Consulting" }
  },
  "profileUrl": "/deals/9002"
}
```

> **Search note for case 2:** `"Acme"` ✅ matches (via `company.name`). `"Priya"` ✅ matches (via `contact.name`). `"Globex"` ❌ does **not** match this deal (`contact.company.name` is `index: false`). `"Globex"` **does** match the Contact index document for Priya Raman.

**Case 4 — no deal company, inferred from contact**

```json
{
  "id": 9003,
  "orgId": 42,
  "name": "Untitled - Data Migration",
  "ownerId": 9,
  "status": "OPEN",
  "contact": {
    "id": 2002,
    "name": "John Smith",
    "emails": [{ "id": 4, "type": "WORK", "value": "john.smith@example.com" }],
    "company": { "id": 1002, "name": "Globex Consulting" }
  },
  "profileUrl": "/deals/9003"
}
```

---

## 9. Index: Product

### 9.1 Purpose
A standalone catalog entity. Products can be added to deals as line items, but the search index does **not** model that relationship.

### 9.2 Mapping

```json
{
  "product": {
    "mappings": {
      "properties": {
        "customFields": {
          "properties": {
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "followerIds":  { "type": "long" },
        "id":           { "type": "long" },
        "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "orgId":        { "type": "long" },
        "ownerId":      { "type": "long" },
        "productCode":  { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "profileUrl":   { "type": "keyword", "index": false },
        "teamIds":      { "type": "long" },
        "territoryId":  { "type": "long" }
      }
    }
  }
}
```

### 9.3 In-depth notes on entities involved

- **There are no entity relationships in this index.** No company, contact, deal, or activity references are stored. `Product` is a pure individual entity.
- **The only deal↔product linkage in the whole model is `deal.totalProductCount` (integer).** Deal line items are not denormalized. As a result:
    - A deal hit can say "3 products" but cannot name them.
    - A product hit cannot say which deals it belongs to.
    - Any "products in this deal" or "deals containing this product" view requires a separate query against a line-item store outside these six indexes.
- `productCode` uses the whitespace analyzer, so partial codes like `"SKU-100"` tokenize on whitespace only — `"SKU-100"` stays one token and a query for `"SKU"` will **not** match it. Consider a `prefix`/`wildcard` or an `edge_ngram` sub-field if SKU prefix search is a requirement.
- Permission envelope is identical to Company (`ownerId`, `followerIds`, `teamIds`, `territoryId`).

### 9.4 Sample document

```json
{
  "id": 3001,
  "orgId": 42,
  "name": "Enterprise Seat",
  "productCode": "SKU-100",
  "ownerId": 7,
  "followerIds": [7],
  "teamIds": [3],
  "territoryId": 11,
  "customFields": [{ "name": "Recurring" }],
  "profileUrl": "/products/3001"
}
```

---

## 10. Index: Activity

### 10.1 Purpose
Activities (calls, meetings, tasks, check-ins) are **optional** on all three axes: they may or may not be linked to a deal, a contact, and/or a company. The index stores snapshots of whichever links exist so the activity card renders without joins.

### 10.2 Mapping

```json
{
  "activity": {
    "mappings": {
      "properties": {
        "activityType": {
          "properties": {
            "icon": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "id":   { "type": "long" },
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "checkInInfo":    { "type": "keyword", "index": false },
        "checkInTime":    { "type": "long" },
        "checkOutInfo":   { "type": "keyword", "index": false },
        "checkOutTime":   { "type": "long" },
        "company": {
          "properties": {
            "address": {
              "properties": {
                "addressLine1": { "type": "keyword", "index": false },
                "addressLine2": { "type": "keyword", "index": false },
                "city":         { "type": "keyword", "index": false },
                "country":      { "type": "keyword", "index": false },
                "id":           { "type": "long" },
                "state":        { "type": "keyword", "index": false },
                "zipcode":      { "type": "keyword", "index": false }
              }
            },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "name":         { "type": "keyword", "index": false },
            "ownerId":      { "type": "long" },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" },
            "website":      { "type": "keyword", "index": false }
          }
        },
        "contact": {
          "properties": {
            "company": {
              "properties": {
                "followerIds":  { "type": "long" },
                "id":           { "type": "long" },
                "name":         { "type": "keyword", "index": false },
                "ownerId":      { "type": "long" },
                "teamIds":      { "type": "long" },
                "territoryId":  { "type": "long" }
              }
            },
            "emails": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "keyword", "index": false }
              }
            },
            "facebook":     { "type": "keyword", "index": false },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "linkedin":     { "type": "keyword", "index": false },
            "name":         { "type": "keyword", "index": false },
            "ownerId":      { "type": "long" },
            "phones": {
              "properties": {
                "id":     { "type": "long" },
                "number": { "type": "keyword", "index": false },
                "type":   { "type": "keyword", "index": false }
              }
            },
            "primaryEmail": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "keyword", "index": false }
              }
            },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" }
          }
        },
        "contactIds": { "type": "long" },
        "deal": {
          "properties": {
            "company": {
              "properties": {
                "followerIds":  { "type": "long" },
                "id":           { "type": "long" },
                "name":         { "type": "keyword", "index": false },
                "ownerId":      { "type": "long" },
                "teamIds":      { "type": "long" },
                "territoryId":  { "type": "long" }
              }
            },
            "contact": {
              "properties": {
                "followerIds":  { "type": "long" },
                "id":           { "type": "long" },
                "name":         { "type": "keyword", "index": false },
                "ownerId":      { "type": "long" },
                "teamIds":      { "type": "long" },
                "territoryId":  { "type": "long" }
              }
            },
            "dealState": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "dealValue": {
              "properties": {
                "currency": { "type": "keyword", "index": false },
                "id":       { "type": "long" },
                "value":    { "type": "double" }
              }
            },
            "expectedCloseDate": { "type": "long" },
            "followerIds":       { "type": "long" },
            "id":                { "type": "long" },
            "name":              { "type": "keyword", "index": false },
            "ownerId":           { "type": "long" },
            "pipeline": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "pipelineStage": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "teamIds":           { "type": "long" },
            "territoryId":       { "type": "long" },
            "totalProductCount": { "type": "integer" }
          }
        },
        "endTime":      { "type": "long" },
        "id":           { "type": "long" },
        "isDone":       { "type": "keyword", "index": false },
        "name":         { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "note": {
          "properties": {
            "id":   { "type": "long" },
            "name": { "type": "text", "analyzer": "customWhitespaceAnalyzer" }
          }
        },
        "orgId":        { "type": "long" },
        "ownerId":      { "type": "long" },
        "ownerIds":     { "type": "long" },
        "startTime":    { "type": "long" },
        "status":       { "type": "text", "analyzer": "customKeywordAnalyzer" },
        "territoryIds": { "type": "long" }
      }
    }
  }
}
```

### 10.3 In-depth notes on entities involved

An activity can hold **three independent entity references**, each with its own nested snapshot:

```
Activity
 ├── deal          (optional)
 │    ├── contact  (flat: id/name/owner/followers/teams/territory — NO nested company)
 │    └── company  (separate deal-company snapshot)
 ├── contact       (optional primary contact for this activity)
 │    └── company  (mapped: that contact's employer snapshot)
 ├── contactIds[]  (optional additional associated contacts — ids only)
 └── company       (optional separate company reference)
```

> **Live mapping note:** `activity.contact.company` **exists**. `activity.deal.company` **exists**. `activity.deal.contact.company` **does not exist** — employer under a deal-linked activity contact is not nested; use top-level `activity.contact.company` or `activity.deal.company` instead.

#### The core statement

> A deal object has its own contact/company (siblings under `deal`, not `deal.contact.company`).
> A contact object has its own company (`activity.contact.company`).
> A separate company can be added.
> They all may be the same contact/company, or they can vary.

#### Case matrix

| # | `deal` | `activity.contact` | `activity.company` | Interpretation | Display rule |
|---|--------|--------------------|--------------------|----------------|--------------|
| 1 | Deal D (company A, contact Jane@A) | Jane (company A) | A | **Fully consistent.** All three resolve to the same company and contact. | Collapse to a single "Acme Corp · Jane Doe" line. |
| 2 | Deal D (company A, contact Jane@A) | — | — | Activity logged against a deal only. | Company/contact inferred from `deal.company` / `deal.contact`. Show as inferred. |
| 3 | Deal D (company A) | Bob (company B) | — | Activity on a deal, but the attending contact is from another company. | Show deal's account as primary; show Bob@B as the attendee. |
| 4 | Deal D (company A) | — | B | An explicit, different company is attached to the activity (e.g. a partner site visit). | Show **both** A (from deal) and B (activity company). Do not merge. |
| 5 | — | Jane (company A) | A | Contact-only activity (call, email) with the company explicitly attached. | Single company line. |
| 6 | — | Jane (company A) | — | Contact-only activity, company inferred from `contact.company`. | Infer; flag if the UI distinguishes explicit vs. inferred. |
| 7 | — | — | B | Company-only activity (e.g. a site visit / check-in at a location). | Company line only. Often paired with `checkInTime`/`checkOutTime`. |
| 8 | Deal D | Jane + `contactIds: [Bob, Carol]` | A | A meeting with multiple attendees on a deal. | Primary attendee from `contact`; additional attendees resolved from `contactIds` (requires a second query — names are not stored). |
| 9 | — | — | — | Orphan activity (personal task). | Only `name`, `status`, `activityType`, owner fields. |

#### Critical search limitation

**Almost everything inside an activity that refers to another entity is `keyword` + `index: false`.** Specifically:

- `activity.company.name` — **not searchable**
- `activity.contact.name`, `.emails.value`, `.phones.number`, `.primaryEmail.value` — **not searchable**
- `activity.deal.name`, `activity.deal.company.name`, `activity.deal.contact.name` — **not searchable**

Therefore, in an activity document:

| Searchable | Not searchable |
|------------|----------------|
| `name` | `company.*` (all of it) |
| `status` | `contact.*` (all of it) |
| `activityType.name` | `deal.*` (all of it, including `deal.name`) |
| `note.name` | `isDone`, `checkInInfo`, `checkOutInfo` |

**Practical consequence:** searching `"Acme"` returns Acme's Company document and Acme's Contacts and Deals — but **not** the activities that happened at Acme, unless the activity's own `name` contains "Acme". To surface activities for an entity, Global Search must either:

1. run a second, entity-scoped query (`term: { "deal.id": <id> }` or `term: { "company.id": <id> }`), or
2. rely on the user navigating from the entity result to its activity timeline.

Filtering **is** still possible by id: `company.id`, `contact.id`, `contactIds`, `deal.id`, `deal.company.id`, `deal.contact.id`, `deal.pipeline.id`, `deal.pipelineStage.id`, `activityType.id`.

#### Other notable points

- **`contactIds` vs `contact`.** `contact` is the single primary contact; `contactIds` is the full attendee list. Names for `contactIds` are **not** stored, so rendering an attendee list requires a `terms` query against the `contact` index.
- **`deal.expectedCloseDate` is duplicated** into the activity snapshot, so an activity can be date-ranged by the deal's close date without a join.
- **`isDone` is `keyword` + `index: false`.** It cannot be filtered. If "show only open activities" is a requirement, `isDone` must be re-mapped to `boolean` with `index: true`.
- **`territoryIds` / `ownerIds` are plural** here while all other indexes use singular `territoryId` / `ownerId`. Any shared permission filter must branch on index name.
- **`activityType.name` is searchable** (whitespace), so `"call"`, `"meeting"`, `"site visit"` work as free-text queries. `activityType.icon.name` is display-only.
- **`status`** uses `customKeywordAnalyzer` → exact token match (`"COMPLETED"`, not `"completed"`, unless a lowercase filter is configured).
- **Check-in/check-out** fields support field-service style activities: `checkInTime` / `checkOutTime` are numeric (epoch ms) and filterable; `checkInInfo` / `checkOutInfo` are display-only payloads.

### 10.4 Sample documents

**Case 1 — everything consistent**

```json
{
  "id": 7001,
  "orgId": 42,
  "name": "Kickoff call with Jane",
  "status": "COMPLETED",
  "activityType": { "id": 1, "name": "Call", "icon": { "id": 10, "name": "phone" } },
  "startTime": 1780000000000,
  "endTime": 1780003600000,
  "ownerId": 7,
  "ownerIds": [7],
  "territoryIds": [11],
  "deal": {
    "id": 9001,
    "name": "Acme Corp - Renewal 2026",
    "company": { "id": 1001, "name": "Acme Corp" },
    "contact": { "id": 2001, "name": "Jane Doe" }
  },
  "contact": {
    "id": 2001,
    "name": "Jane Doe",
    "company": { "id": 1001, "name": "Acme Corp" }
  },
  "company": { "id": 1001, "name": "Acme Corp" }
}
```

**Case 4 — activity company differs from deal company**

```json
{
  "id": 7002,
  "orgId": 42,
  "name": "On-site workshop",
  "status": "PLANNED",
  "activityType": { "id": 2, "name": "Meeting" },
  "startTime": 1781000000000,
  "endTime": 1781036000000,
  "checkInTime": 1781000300000,
  "checkOutTime": 1781035000000,
  "checkInInfo": "{\"lat\":37.79,\"lon\":-122.39}",
  "checkOutInfo": "{\"lat\":37.79,\"lon\":-122.39}",
  "ownerId": 7,
  "ownerIds": [7, 12],
  "territoryIds": [11],
  "deal": {
    "id": 9001,
    "name": "Acme Corp - Renewal 2026",
    "company": { "id": 1001, "name": "Acme Corp" },
    "contact": { "id": 2001, "name": "Jane Doe" }
  },
  "contact": { "id": 2001, "name": "Jane Doe", "company": { "id": 1001, "name": "Acme Corp" } },
  "company": { "id": 1002, "name": "Globex Consulting" },
  "contactIds": [2001, 2002, 2004]
}
```

> **Interpretation of case 4:** the deal belongs to Acme; the primary attendee Jane works at Acme; but the activity itself took place at / was hosted by Globex Consulting. The UI must show **both** companies, clearly labelled ("Deal account: Acme Corp" / "Activity company: Globex Consulting").

**Case 7 — company-only, no deal, no contact**

```json
{
  "id": 7003,
  "orgId": 42,
  "name": "Quarterly site inspection",
  "status": "COMPLETED",
  "activityType": { "id": 3, "name": "Site Visit" },
  "startTime": 1782000000000,
  "endTime": 1782007200000,
  "company": { "id": 1001, "name": "Acme Corp" },
  "ownerId": 9,
  "ownerIds": [9],
  "territoryIds": [11]
}
```

---

## 11. Index: Note & File

### 11.1 Purpose
Notes and files are **always attached to a source entity** — a Company, a Contact, or a Deal. A single shared mapping serves both `note` and `file`; the `entitySource` field discriminates.

### 11.2 Mapping

```json
{
  "note": {
    "mappings": {
      "properties": {
        "company": {
          "properties": {
            "address": {
              "properties": {
                "addressLine1": { "type": "keyword", "index": false },
                "addressLine2": { "type": "keyword", "index": false },
                "city":         { "type": "keyword", "index": false },
                "country":      { "type": "keyword", "index": false },
                "id":           { "type": "long" },
                "state":        { "type": "keyword", "index": false },
                "zipcode":      { "type": "keyword", "index": false }
              }
            },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "name":         { "type": "keyword", "index": false },
            "ownerId":      { "type": "long" },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" },
            "website":      { "type": "keyword", "index": false }
          }
        },
        "contact": {
          "properties": {
            "company": {
              "properties": {
                "followerIds":  { "type": "long" },
                "id":           { "type": "long" },
                "name":         { "type": "keyword", "index": false },
                "ownerId":      { "type": "long" },
                "teamIds":      { "type": "long" },
                "territoryId":  { "type": "long" }
              }
            },
            "emails": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "keyword", "index": false }
              }
            },
            "facebook":     { "type": "keyword", "index": false },
            "followerIds":  { "type": "long" },
            "id":           { "type": "long" },
            "linkedin":     { "type": "keyword", "index": false },
            "name":         { "type": "keyword", "index": false },
            "ownerId":      { "type": "long" },
            "phones": {
              "properties": {
                "id":     { "type": "long" },
                "number": { "type": "keyword", "index": false },
                "type":   { "type": "keyword", "index": false }
              }
            },
            "primaryEmail": {
              "properties": {
                "id":    { "type": "long" },
                "type":  { "type": "keyword", "index": false },
                "value": { "type": "keyword", "index": false }
              }
            },
            "teamIds":      { "type": "long" },
            "territoryId":  { "type": "long" }
          }
        },
        "deal": {
          "properties": {
            "company": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "contact": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "dealState": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "dealValue": {
              "properties": {
                "currency": { "type": "keyword", "index": false },
                "id":       { "type": "long" },
                "value":    { "type": "double" }
              }
            },
            "expectedCloseDate": { "type": "long" },
            "followerIds":       { "type": "long" },
            "id":                { "type": "long" },
            "name":              { "type": "keyword", "index": false },
            "ownerId":           { "type": "long" },
            "pipeline": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "pipelineStage": {
              "properties": {
                "id":   { "type": "long" },
                "name": { "type": "keyword", "index": false }
              }
            },
            "teamIds":           { "type": "long" },
            "territoryId":       { "type": "long" },
            "totalProductCount": { "type": "integer" }
          }
        },
        "entitySource":     { "type": "text", "analyzer": "customKeywordAnalyzer" },
        "entitySourceId":   { "type": "long" },
        "entitySourceName": { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "followerIds":      { "type": "long" },
        "id":               { "type": "long" },
        "name":             { "type": "text", "analyzer": "customWhitespaceAnalyzer" },
        "orgId":            { "type": "long" },
        "ownerId":          { "type": "long" },
        "teamIds":          { "type": "long" },
        "territoryId":      { "type": "long" }
      }
    }
  }
}
```

The `file` index uses the **same property set**, differing only in the root index name. Files add no extra searchable fields in this mapping — a file is matched by its `name` (filename) and by its parent's `entitySourceName`.

### 11.3 In-depth notes on entities involved

#### The attachment triple

Every note/file carries three fields that together identify its parent:

| Field | Type | Role |
|-------|------|------|
| `entitySource` | `text` + `customKeywordAnalyzer` (searchable, exact) | The parent **type**: e.g. `COMPANY`, `CONTACT`, `DEAL` |
| `entitySourceId` | `long` (filterable) | The parent **id** |
| `entitySourceName` | `text` + `customWhitespaceAnalyzer` (searchable, free text) | The parent **name**, denormalized so the note can be rendered and searched without a join |

`entitySourceName` is the **only** reason a note is findable by parent name: all of the `company.name`, `contact.name`, and `deal.name` snapshots are `keyword` + `index: false`.

#### Case matrix

| # | `entitySource` | `entitySourceId` | Snapshots populated | Interpretation | Display rule |
|---|----------------|------------------|---------------------|----------------|--------------|
| 1 | `COMPANY` | companyId | `company` (with address, website) | Note attached directly to an account. | Show company name from `company.name` (or `entitySourceName`). |
| 2 | `CONTACT` | contactId | `contact` + `contact.company` | Note on a person. The person's employer is denormalized on top-level `contact.company`. | Show "Jane Doe · Acme Corp". |
| 3 | `CONTACT` | contactId | `contact` only (no `contact.company`) | Contact has no company on record. | Show "Jane Doe". |
| 4 | `DEAL` | dealId | `deal` + `deal.company` + `deal.contact` (id+name) | Note on a deal. Deal account + primary contact names present; **no** `deal.contact.company`. | Show deal account + contact name from flat snapshots. |
| 5 | `DEAL` | dealId | `deal` with `deal.contact` but no `deal.company` | Deal has a primary contact but no account linked. | Contact name only (or `entitySourceName`). |
| 6 | `DEAL` | dealId | `deal` with both `deal.company` and `deal.contact` | Deal account and primary contact both present. Employer-of-contact is **not** nested under `deal.contact` on note/file. | Show deal account primary; contact name secondary. Do not invent a `deal.contact.company` path. |
| 7 | `CONTACT` | contactId | `contact` + `contact.company` + a populated `deal` | A note on a contact that is also related to a deal in the same context. | Show contact as the anchor; deal as secondary context. |
| 8 | — | — | — | Invalid — a note/file must always have a source. | Reject at index time. |

#### Search consequences

- Searching `"Acme"`:
    - ✅ matches `entitySourceName` when `entitySource` is `COMPANY`/`DEAL` and the parent is Acme.
    - ✅ matches `name` if the note title contains "Acme".
    - ❌ does **not** match via `company.name`, `contact.company.name`, or `deal.company.name` — all `index: false`.
- `entitySource` uses `customKeywordAnalyzer`, so a filter such as `{ "term": { "entitySource": "DEAL" } }` is the correct way to scope a note search to deal notes.
- The `deal` snapshot inside a note is **narrow**: `deal.company` and `deal.contact` are **id + name only**. There is **no** `note.deal.contact.company` (or `file.deal.contact.company`). Top-level `contact.company` exists for contact-sourced notes. `deal.totalProductCount` is still carried.
- **Note bodies are not in this index.** Only `name` (the note title / filename) is mapped. Deal note *content* lives in `deal.notes.content` (see [§8.3](#83-in-depth-notes-on-entities-involved)); company and contact note bodies are not indexed anywhere in this model.

### 11.4 Sample documents

**Case 2 — note on a contact who has a company**

```json
{
  "id": 4001,
  "orgId": 42,
  "name": "Discovery call summary",
  "ownerId": 7,
  "followerIds": [7],
  "teamIds": [3],
  "territoryId": 11,
  "entitySource": "CONTACT",
  "entitySourceId": 2001,
  "entitySourceName": "Jane Doe",
  "contact": {
    "id": 2001,
    "name": "Jane Doe",
    "ownerId": 7,
    "company": { "id": 1001, "name": "Acme Corp", "ownerId": 7 }
  },
  "profileUrl": "/notes/4001"
}
```

**Case 6 — note on a deal where the contact's employer differs**

```json
{
  "id": 4002,
  "orgId": 42,
  "name": "Advisory scope agreed",
  "ownerId": 7,
  "entitySource": "DEAL",
  "entitySourceId": 9002,
  "entitySourceName": "Acme Corp - Advisory Engagement",
  "deal": {
    "id": 9002,
    "name": "Acme Corp - Advisory Engagement",
    "company": { "id": 1001, "name": "Acme Corp" },
    "contact": { "id": 2003, "name": "Priya Raman" }
  },
  "profileUrl": "/notes/4002"
}
```

---

## 12. Known Inconsistencies & Gotchas

| # | Issue | Impact | Recommendation |
|---|-------|--------|----------------|
| 1 | `activity` uses `ownerIds` and `territoryIds`; all other indexes use `ownerId` and `territoryId` | A shared permission filter will silently exclude all activities | Build the permission clause per index, or add aliases (`ownerId` → `ownerIds`) to normalise |
| 2 | `activity.company.name` / `activity.contact.name` / `activity.deal.name` are `keyword` + `index: false` | Activities are **not** findable by related entity name — a major UX surprise | Either re-map to `text` + `customWhitespaceAnalyzer`, or make the Global Search UI always issue a secondary entity-scoped activity query |
| 3 | `note.company.name` / `note.contact.name` / `note.deal.name` are `index: false` | Notes/files are not findable by parent name except through `entitySourceName` | Prefer `entitySourceName` for matching; consider indexing the snapshots for parity with `deal` |
| 4 | `deal.company.address.id` is `keyword` + `index: false`, while every other `address.id` is `long` | Cannot filter deals by company address id | Change to `long`, or filter by `deal.company.id` |
| 5 | `deal.contact.company` is thin/optional; `.name` is `index: false`. `activity.deal.contact.company` / `note.deal.contact.company` are **not mapped** | Employer text search under deal-nested contact does not work; activity/note deal.contact has no nested company | On deal index: filter by `contact.company.id` when present. On activity/note: use `contact.company` (top-level) or `deal.company`, never `deal.contact.company` |
| 6 | `activity.isDone` is `keyword` + `index: false` | Cannot filter activities by completion | Re-map as `boolean` with `index: true` |
| 7 | `pipeline.name`, `pipelineStage.name`, `dealState.name` are `index: false` | Cannot search for "Negotiation" and get the deals in that stage | Filter by `.id`; optionally add a searchable copy |
| 8 | Timestamps are `long` epoch ms, not `date` | No date math (`now-30d`) | Re-map to `date` with `"format": "epoch_millis"`, or compute epochs client-side |
| 9 | No `nested` types anywhere | If any embedded object ever becomes an array, cross-field queries inside it will match across array elements | Introduce `nested` for the affected object, or guarantee single-valued snapshots |
| 10 | `Product` has no relationship fields; deals expose only `totalProductCount` | Deal↔Product navigation is impossible from these six indexes | Maintain a separate line-item index or add `productIds` to `deal` |
| 11 | `deal.notes.content` is the **only** indexed note body | Note bodies on companies/contacts are invisible to search | Standardise: either index bodies everywhere or nowhere |
| 12 | `customFields` maps only `name` (the value) | Cannot scope a search to a specific custom field, and `key`/`label` cannot be displayed from `_source` | Add `customFieldId` (`long`) and `customFieldLabel` (`keyword`, `index: false`) |
| 13 | Analyzer definitions (`customWhitespaceAnalyzer`, `customKeywordAnalyzer`) are referenced but not defined here | Case sensitivity and tokenization behaviour are unverified | Confirm index settings; ensure a lowercase filter is applied to the whitespace analyzer |
| 14 | `primaryEmail` duplicates an entry in `emails[]` | Double-counting in scoring / aggregation | De-duplicate by `id` at the application layer or drop one of the indexed copies |

---

## 13. Indexing, Refresh & Reindex

- **Denormalization implies fan-out on update.** Changing a company's `name` must update the **writable nested paths that actually exist**:
    - `company.name` (1 doc)
    - `contact.company.name` for every contact at that company (N docs)
    - `deal.company.name` for every deal at that company (N docs)
    - `deal.contact.company.name` for every deal whose primary contact works there (N docs; thin path, when populated)
    - `note.company.name`, `note.contact.company.name`, `note.deal.company.name` (N docs) — **not** `note.deal.contact.company`
    - `activity.company.name`, `activity.deal.company.name`, `activity.contact.company.name` (N docs) — **not** `activity.deal.contact.company`

  A company rename is therefore a bulk operation across all six indexes. Plan for an asynchronous, idempotent update job keyed by `company.id`.

- **Use `_bulk` with `doc_as_upsert`** and drive from the same CDC stream so partial failures can be retried.

- **Alias strategy.** Expose a read alias (`global_search`) pointing at the six physical indexes, and write to versioned indexes (`company_v2`, …) so a mapping change can be rolled out with zero downtime.

- **`refresh_interval`.** For a search-heavy CRM, keep `1s` (default) or set `-1` with an explicit `_refresh` after bulk loads.

- **Routing.** `orgId` is a natural routing key — every query filters on it, and it keeps a tenant's documents on a single shard, which reduces fan-out.

- **Sorting.** Add explicit `sort` on `_score` plus a tiebreaker (`id`) for deterministic pagination. Use `search_after` rather than `from`/`size` for deep pagination.

- **Highlighting.** Only fields that are indexed can be highlighted. Highlighting will silently return nothing for `activity.company.name`, `note.deal.name`, etc.

---

## 14. Appendix: Field Glossary

| Field | Type | Searchable | Meaning |
|-------|------|------------|---------|
| `orgId` | `long` | ✅ filter | Tenant key. Mandatory on every query. |
| `id` | `long` | ✅ filter | Root entity primary key. |
| `name` | `text` (whitespace) | ✅ match | Root entity display name. |
| `ownerId` | `long` | ✅ filter | Record owner. |
| `ownerIds` | `long` | ✅ filter | Multi-owner (activities only). |
| `followerIds` | `long` | ✅ filter | Users following the record. |
| `teamIds` | `long` | ✅ filter | Teams with access. |
| `territoryId` | `long` | ✅ filter | Territory scoping. |
| `territoryIds` | `long` | ✅ filter | Multi-territory (activities only). |
| `profileUrl` | `keyword`, `index: false` | ❌ | Deep link to the CRM record. |
| `customFields.name` | `text` (whitespace) | ✅ match | Flattened custom-field values. |
| `notes.id` / `notes.name` | `long` / `text` | ✅ (`name`) | Attached note stubs. |
| `notes.content` | `text` (+ `.keyword`) | ✅ match | Note body — **deal index only**. |
| `company.id` | `long` | ✅ filter | Related company id. |
| `company.name` | `text` (whitespace) | ✅ match | Related company name. |
| `company.website` | `text` (whitespace) | ✅ match | Related company website. |
| `company.address.*` | `keyword`, `index: false` (embedded) | ❌ | Display-only address snapshot. |
| `contact.id` | `long` | ✅ filter | Related contact id. |
| `contact.name` | `text` (whitespace) | ✅ match | Related contact name. |
| `contact.company.id` | `long` | ✅ filter | Contact's employer id (root contact index, deal.contact.company when present, activity/note top-level `contact.company` — **not** under `*.deal.contact`). |
| `contact.company.name` | `keyword`, `index: false` | ❌ | Contact's employer name (display only; same path availability rules as `.id`). |
| `emails[].value` | `text` (keyword analyzer) | ✅ exact | Contact email addresses. |
| `phones[].number` | `text` (keyword analyzer) | ✅ exact | Contact phone numbers. |
| `primaryEmail.value` | `text` (keyword analyzer) | ✅ exact | Contact's primary email. |
| `dealValue.value` | `double` | ✅ range | Deal amount. |
| `dealValue.currency` | `keyword`, `index: false` | ❌ | Currency code. |
| `expectedCloseDate` | `long` | ✅ range | Epoch ms. |
| `isArchived` | `boolean` | ✅ filter | Deal archive flag. |
| `status` | `text` (keyword analyzer) | ✅ exact | Status token. |
| `dealState.id` / `.name` | `long` / `keyword` | ✅ (`id`) / ❌ | Deal state. |
| `pipeline.id` / `.name` | `long` / `keyword` | ✅ (`id`) / ❌ | Pipeline. |
| `pipelineStage.id` / `.name` | `long` / `keyword` | ✅ (`id`) / ❌ | Pipeline stage. |
| `totalProductCount` | `integer` | ✅ range | Number of products on a deal. |
| `productCode` | `text` (whitespace) | ✅ match | SKU / product code. |
| `activityType.id` / `.name` | `long` / `text` | ✅ both | Activity type. |
| `activityType.icon.name` | `keyword`, `index: false` | ❌ | Icon identifier. |
| `startTime` / `endTime` | `long` | ✅ range | Epoch ms. |
| `checkInTime` / `checkOutTime` | `long` | ✅ range | Epoch ms. |
| `checkInInfo` / `checkOutInfo` | `keyword`, `index: false` | ❌ | Check-in payload. |
| `isDone` | `keyword`, `index: false` | ❌ | Activity completion flag. |
| `contactIds` | `long` | ✅ filter | Additional activity attendees. |
| `entitySource` | `text` (keyword analyzer) | ✅ exact | Note/file parent type. |
| `entitySourceId` | `long` | ✅ filter | Note/file parent id. |
| `entitySourceName` | `text` (whitespace) | ✅ match | Note/file parent name. |
| `facebook` / `linkedin` | `keyword`, `index: false` | ❌ | Social profiles. |

---