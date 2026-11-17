# Quotation File Generation & Lifecycle Design

**Package:** `com.tlc.crm.core.features.quote`
**Key Files:**
- [`builder/QuotationFile.java`](builder/QuotationFile.java) — HTML + PDF file generation
- [`action/QuotationManagement.java`](action/QuotationManagement.java) — CRUD operations + file upload
- [`action/QuotationRetrievalManagement.java`](action/QuotationRetrievalManagement.java) — preview/download endpoint
- [`util/QuotationURLGenerator.java`](util/QuotationURLGenerator.java) — URL encoding/generation
- [`model/Quotation.java`](model/Quotation.java) — entity with `htmlFileId`, `pdfFileId`

---

## 1. Overview

Every non-DRAFT quotation produces two file artifacts stored in the CRM attachment service:

| Artifact | Format | Purpose | Content-Disposition |
|---|---|---|---|
| Preview file | HTML | Inline browser rendering | `inline; filename=…` |
| Download file | PDF | Saveable customer document | `attachment; filename=…` |

File IDs (`htmlFileId`, `pdfFileId`) are persisted on the `Quotation` DB row after upload.
Preview and download **URLs are never stored** — they are generated dynamically on every `get`.

---

## 2. Quotation Status & File Gate

```
DRAFT (1)  ──► edit only, no files generated or served
     │
     ▼  update (non-DRAFT)
NOT_PUBLISHED (3) ──► files generated, accessible
PUBLISHED (2)     ──► files generated, accessible
     │
     ▼  customer opens link
VIEWED (4)        ──► files generated, accessible
     │
     ▼  recall
DRAFT (1)         ──► files remain in storage but serve endpoint blocked
```

Files are generated only when status is **not DRAFT**.
The retrieval endpoint (`QuotationRetrievalManagement`) enforces this check:
```java
if (Objects.nonNull(quotation) && quotation.getStatus() != Quotation.Status.DRAFT) { … }
```

---

## 3. File Generation (`QuotationFile.Builder`)

Triggered during `update` for non-DRAFT quotations, dispatched to the
`file-event-request-handler-pool` (pool size: **2 threads**, configured in `application.yml`):

```java
fileEventRequestHandler.handle(() -> {
    final QuotationFile quotationFile = new QuotationFile.Builder(quotation).build();
    quotationFile.getPreviewFile().ifPresent(file -> quotation.setHtmlFileId(uploadFile(orgId, file).id()));
    quotationFile.getDownloadFile().ifPresent(file -> quotation.setPdfFileId(uploadFile(orgId, file).id()));
    quotationManager.update(quotation);
});
```

### 3.1 Build Steps (`QuotationFile.Builder.build()`)

```
1. buildTemplateDataHolder(quotation)
   ├── loadSectionData(quoteJson.formAttributes)     — maps HEADER / PARTIES / LINE_ITEMS /
   │                                                    SUMMARY / PAYMENT / ACCEPTANCE /
   │                                                    CUSTOM_SECTION → QuoteTemplateData
   ├── loadProcessors(quoteJson)
   │   ├── PreSectionRemover   — removes disabled sections (isEnabled=false) before render
   │   └── SectionOrderUpdater — reorders sections per user config in the template
   └── owner + currency + profile presigned URL injected

2. templateBuilder.build("quotation", templateBuildRequest)
   └── Velocity template "quotation" rendered → HTML string

3. HTML File (preview)
   ├── path: {workDir}/uploads/org{orgId}/quoteFile/html/quote_file_{id}.html
   └── HtmlContentFileGenerator.get(Extension.HTML).generate(…)

4. PDF File (download)
   ├── Strip TOP_BANNER section from HTML (PostSectionRemover)
   ├── path: {workDir}/uploads/org{orgId}/quoteFile/pdf/quote_file_{id}.pdf
   └── HtmlContentFileGenerator.get(Extension.PDF).generate(…)

5. Returns QuotationFile(htmlFile, pdfFile)
   └── Both are Optional — empty if template rendering produces no output
```

### 3.2 Template Sections

| JSON `type` | Template key | Description |
|---|---|---|
| `HEADER` | `header` | Logo, company name, reference, issue date, expiry date, currency |
| `PARTIES` | `parties` | Seller/buyer companies + contacts |
| `LINE_ITEMS` | `lineItems` | Products table: name, qty, unit price, discount, total |
| `SUMMARY` | `summary` | Summary-level totals |
| `PAYMENT` | `payment` | Payment/billing terms |
| `ACCEPTANCE` | `acceptance` | Signature blocks (buyer + seller contacts) |
| `CUSTOM_SECTION` | `customSections[]` | Rich-text custom sections (N allowed) |

**PDF difference:** The `TOP_BANNER` section is stripped from the HTML before PDF conversion.
The banner is kept in the HTML preview (used for in-browser display with navigation UI).

---

## 4. File Upload & Storage

```java
// QuotationManagement — MAX size constant
private static final long MAX_QUOTATION_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

private Attachment uploadFile(long orgId, File file) {
    if (file.size() > MAX_QUOTATION_FILE_SIZE_BYTES) {
        throw ErrorCode.get(QuotationErrorCodes.QUOTATION_FILE_TOO_LARGE, "i18n.quotation.file.too.large");
    }

    final Long iamOrgId = organizationService.getIamOrgId(orgId);
    final Attachment attachment = new ObjectAttachment();

    attachment.setInputStream(file.inputStream());
    attachment.setMediaType(file.mediaType());
    attachment.setExtension(file.extension());
    attachment.setFileName(file.name());
    attachment.setFileSize(file.size());

    attachmentService.upload(iamOrgId, List.of(attachment), FileCategory.QUOTATION.getName());
    return attachment;   // attachment.id() holds the storage ID
}
```

- **Storage category:** `FileCategory.QUOTATION` (id = 12, tag = `"quotation"`)
- **Org scoping:** uses IAM org ID (not CRM org ID) for storage namespacing
- **Max file size:** **10 MB** per artifact (`MAX_QUOTATION_FILE_SIZE_BYTES`). Exceeding this
  throws `QUOTATION_FILE_TOO_LARGE` (`i18n.quotation.file.too.large`). Applies to both the HTML
  preview and the PDF download independently.
- **Error code:** `QuotationErrorCodes.QUOTATION_FILE_TOO_LARGE` (0x16)

---

## 5. URL Generation

URLs are **never persisted**. They are injected into `quoteJson.settings` on every `get`:

```java
// QuotationManagement.loadQuoteUrl(...)
settingsJson.put("quoteUrl",         quotationUrlGenerator.generatePreviewURL(orgId, userId, id));
settingsJson.put("quoteDownloadUrl", quotationUrlGenerator.generateDownloadURL(orgId, userId, id));
```

### URL Structure

```
{previewBaseURL}  /  base64url(orgId/userId/quotationId)
{downloadBaseURL} /  base64url(orgId/userId/quotationId)
```

- Configured via `tlc.config.quotation.url.preview` and `tlc.config.quotation.url.download`
  (resolved from Azure Key Vault secrets `TLC-CONFIG-QUOTATION-PREVIEW-URL` / `TLC-CONFIG-QUOTATION-DOWNLOAD-URL`)
- Base64 encoding: URL-safe alphabet, no padding

### Decoding (in `QuotationRetrievalManagement`)

```java
final String decoded = new String(Base64.getDecoder().decode(encodedId), UTF_8);
// → "orgId/userId/quotationId"
String[] parts = decoded.split("/");
long orgId       = Long.parseLong(parts[0]);
long userId      = Long.parseLong(parts[1]);
long quotationId = Long.parseLong(parts[2]);
```

---

## 6. Preview / Download Endpoint

```
GET /quotation/{action}/{id}       (CrmPublicAction — no session required)
```

| Parameter | Values |
|---|---|
| `{action}` | `preview` or `download` |
| `{id}` | URL-safe Base64 token (orgId/userId/quotationId) |

**Flow:**

```
Decode token
    ↓
SecureTaskHandler.secureProcess(orgId, userId, task)   ← establishes secure user context
    ↓
quotationManager.get(orgId, quotationId)
    ↓
Guard: status != DRAFT  (otherwise: no redirect)
    ↓
action == "download"?
    ├─ YES → attachmentId = pdfFileId,   Content-Disposition: attachment; filename=…
    └─ NO  → attachmentId = htmlFileId,  Content-Disposition: inline; filename=…
    ↓
attachmentService.exists(attachmentId)   ← throws ATTACHMENT_NOT_FOUND if missing
    ↓
presignedUrl = attachmentService.getPresignedUrl(attachmentId, 10 min, headers)
    ↓
response.setHeader("Cache-Control", "private, max-age=3600")
response.setTemporaryRedirectUrl(presignedUrl)          ← HTTP 307 redirect
response.send()
```
---

## 7. Delete Lifecycle

```java
// QuotationManagement.delete(...)
final Collection<Long> fileIds = quotationManager.getQuoteFileIds(orgId, id);  // [htmlFileId, pdfFileId]
quotationManager.delete(orgId, id);                                             // DB row removed

if (!fileIds.isEmpty()) {
    attachmentManager.delete(fileIds);
}
```

On delete: both `htmlFileId` and `pdfFileId` (if present) are fetched before DB deletion,
then the attachment storage files are removed.

---

## 8. Clone / Recall Behaviour

| Operation | File behaviour |
|---|---|
| `clone` | Cloned quotation starts as DRAFT → no files. New files generated on first non-DRAFT update. |
| `recall` | Status reset to DRAFT. Existing `htmlFileId` / `pdfFileId` remain in storage but the retrieval endpoint blocks access until re-published. |

---

### File size cap
Each quotation artifact (HTML or PDF) is capped at **10 MB** before upload
(`MAX_QUOTATION_FILE_SIZE_BYTES`). Quotations with very large embedded images or hundreds
of line items may approach this limit. Adjust the constant and redeploy if the limit
proves too restrictive; the cap is intentionally conservative to protect heap and storage.

### Thread pool concurrency
File generation runs on `file-event-request-handler-pool` with **2 threads**
(`file-event-request-handler-pool:2` in `application.yml`). Concurrent non-DRAFT updates
will queue on this pool. Increase the pool size if file-generation latency becomes
problematic under load.
