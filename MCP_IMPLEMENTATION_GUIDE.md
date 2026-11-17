# CRM MCP Implementation Guide

## Overview

This document describes the implementation of Model Context Protocol (MCP) tools for CRM product management APIs. The implementation provides AI clients (e.g., Claude) with access to product CRUD operations through standardized MCP tools.

## Architecture

### Components Implemented

1. **CrmMcpTool** (Base Class)
   - Location: `tlc-crm-server/core/src/main/java/com/tlc/crm/core/mcp/CrmMcpTool.java`
   - Extends: `AbstractMcpTool`
   - Purpose: Provides CRM-specific authorization and context handling for all product tools
   - Key Methods:
     - `authorize(McpToolContext)` - Validates CRM user identity
     - `getCrmUser(McpToolContext)` - Extracts CrmUser from context
     - `errorResult(String)` / `textResult(String)` - Helper methods for tool responses

2. **Product Tools** (Four CRUD Operations)

   a. **CreateProductTool**
      - Location: `tlc-crm-server/core/src/main/java/com/tlc/crm/core/mcp/tools/CreateProductTool.java`
      - Annotation: `@McpTool(name = "create_product", ...)`
      - Parameters: name (required), description, skuNumber, productCode, quantityInStock, quantityOrdered, activeStatus, additionalData
      - Returns: JSON with created product ID, name, orgId, and other details

   b. **UpdateProductTool**
      - Location: `tlc-crm-server/core/src/main/java/com/tlc/crm/core/mcp/tools/UpdateProductTool.java`
      - Annotation: `@McpTool(name = "update_product", ...)`
      - Parameters: productId (required), and same optional fields as create
      - Returns: JSON with update confirmation and product ID

   c. **GetProductTool**
      - Location: `tlc-crm-server/core/src/main/java/com/tlc/crm/core/mcp/tools/GetProductTool.java`
      - Annotation: `@McpTool(name = "get_product", ...)`
      - Parameters: productId (required)
      - Returns: Comprehensive JSON with all product details including relationships

   d. **DeleteProductTool**
      - Location: `tlc-crm-server/core/src/main/java/com/tlc/crm/core/mcp/tools/DeleteProductTool.java`
      - Annotation: `@McpTool(name = "delete_product", ...)`
      - Parameters: productId (required), confirmDelete (required safety flag)
      - Returns: Deletion confirmation with product ID and name

3. **CrmMcpIdentityExtractor**
   - Location: `tlc-crm-server/security/src/main/java/com/tlc/crm/security/mcp/CrmMcpIdentityExtractor.java`
   - Annotation: `@McpIdentityProvider`
   - Implements: `McpIdentityExtractor`
   - Purpose: Extracts and resolves CRM user identity from JWT tokens in HTTP requests
   - Current State: Placeholder implementation ready for TokenResolver integration

## Integration

### Dependencies Added

**Core Module** (`tlc-crm-server/core/pom.xml`):
```xml
<dependency>
    <groupId>com.tlc</groupId>
    <artifactId>mcp</artifactId>
    <scope>provided</scope>
</dependency>
```

**Security Module** (`tlc-crm-server/security/pom.xml`):
```xml
<dependency>
    <groupId>com.tlc</groupId>
    <artifactId>mcp</artifactId>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>com.tlc.iam</groupId>
    <artifactId>iam-client</artifactId>
    <scope>provided</scope>
</dependency>
```

### Auto-Discovery

Tools are automatically discovered through MCP's classpath scanning mechanism:
- Location pattern: `/mcp/tool/` for tool classes
- The framework will scan and register all classes extending `AbstractMcpTool` and annotated with `@McpTool`
- Identity extractor is auto-discovered via `@McpIdentityProvider` annotation

### JSON Payload Structure

All tools use CRM field constants from `ProductFields.java` for consistent field naming:
- `ID`, `ORG_ID`, `NAME`, `DESCRIPTION`, `SKU_NUMBER`, `PRODUCT_CODE`, `QUANTITY_IN_STOCK`, `QUANTITY_ORDERED`, `ACTIVE_STATUS`, `VALID_TILL`, `TAXABLE`, `SALES_OWNER`, `CREATED_BY`, `UPDATED_BY`, `CREATED_ON`, `UPDATED_AT`, `VARIATION`

### API Integration

Each tool leverages existing CRM APIs:
- **ProductManager**: Singleton managing CRUD operations
- **ProductConverter**: Bidirectional JSON ↔ Product entity conversion
- **ProductRestReqFormatter**: REST field name mapping (when integrated)

## Usage Examples

### Create Product (MCP Request Format)
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "create_product",
    "arguments": {
      "name": "Enterprise License",
      "description": "Annual enterprise software license",
      "skuNumber": "ENT-001",
      "productCode": "EL-2024",
      "quantityInStock": 100,
      "quantityOrdered": 50,
      "activeStatus": true
    }
  }
}
```

### Get Product (MCP Request Format)
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "get_product",
    "arguments": {
      "productId": 12345
    }
  }
}
```

### Update Product (MCP Request Format)
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "update_product",
    "arguments": {
      "productId": 12345,
      "quantityInStock": 150,
      "quantityOrdered": 75
    }
  }
}
```

### Delete Product (MCP Request Format)
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "delete_product",
    "arguments": {
      "productId": 12345,
      "confirmDelete": true
    }
  }
}
```

## Authentication & Authorization

### Authorization Flow
1. HTTP request arrives at MCP endpoint (`POST /rest/api/mcp`)
2. `CrmMcpIdentityExtractor.extract()` is invoked asynchronously
3. Extracts JWT token from `accessToken` cookie
4. Resolves token to CrmUser (placeholder - needs TokenResolver integration)
5. CrmUser is passed to tool as `McpToolContext.identity()`
6. Each tool invokes `authorize(ctx)` which validates:
   - Identity is CrmUser instance
   - CrmUser.getIamId() is not null
   - CrmUser.getCrmOrg() is not null

### Security Context

Tools can access thread-local security context when available:
- `SecurityContext.exists()` - Check if context is initialized
- `SecurityContext.load().getUser()` - Get current user

## Error Handling

All tools implement consistent error handling:
- Authorization failures: `McpToolResult.error("Unauthorized")`
- Invalid input: `McpToolResult.error("Invalid parameter: <details>")`
- Business logic errors: `McpToolResult.error("Failed to <action>: <reason>")`
- Success: `McpToolResult.json(responseJson)` or `McpToolResult.text(...)`

## Testing & Validation

### Schema Validation
The MCP framework automatically:
1. Reflects on tool `execute()` method signatures
2. Generates JSON Schema from `@ToolParam` annotations
3. Validates incoming arguments at runtime
4. Provides clear error messages for validation failures

### Parameter Validation

Tools validate inputs before processing:
- Required parameters must be non-null
- Product IDs must be > 0
- Partial update requires product ID
- Delete requires explicit confirmDelete=true flag

## Future Integration Points

1. **TokenResolver Integration**
   - Update `CrmMcpIdentityExtractor.extract()` to call `TokenResolver.get().resolve()`
   - Initialize CrmUser from resolved User object
   - Set SecurityContext with extracted user

2. **Enhanced Filtering**
   - Implement list/query tools to retrieve multiple products
   - Add filtering by category, type, sales owner
   - Implement pagination support

3. **Relationship Management**
   - Support for product variations (now just stub in responses)
   - Price tags management
   - Deal associations

4. **Audit & Compliance**
   - Tool execution logging via MCP framework hooks
   - User action tracking (already in ProductManager)
   - Compliance report generation

5. **Bulk Operations**
   - Batch create/update tools for efficiency
   - Async execution with task tracking
   - Progress monitoring via `McpToolResult.trackingId()`

## Deployment Checklist

- [ ] Maven dependencies resolved (core and security POMs updated)
- [ ] Tools placed in discoverable package locations
- [ ] `@McpTool` annotations present on all tool classes
- [ ] `@McpIdentityProvider` annotation on identity extractor
- [ ] All imports use correct packages (Json, HttpCookie, WebExchange, etc.)
- [ ] Error handling implemented for all paths
- [ ] Integration tests validate tool registration
- [ ] End-to-end tests with MCP client
- [ ] Documentation updated for supported operations
- [ ] TokenResolver integration completed (if needed)

## File Structure

```
tlc-crm-server/
├── core/
│   ├── pom.xml (updated with mcp dependency)
│   └── src/main/java/com/tlc/crm/core/
│       └── mcp/
│           ├── CrmMcpTool.java (base class)
│           └── tools/
│               ├── CreateProductTool.java
│               ├── UpdateProductTool.java
│               ├── GetProductTool.java
│               └── DeleteProductTool.java
└── security/
    ├── pom.xml (updated with mcp and iam-client dependencies)
    └── src/main/java/com/tlc/crm/security/
        └── mcp/
            └── CrmMcpIdentityExtractor.java
```

## References

- **MCP Module Design**: `/home/thalaimalai_pandiyan/Desktop/may/tlc-components/mcp/MCP_MODULE_DESIGN.md`
- **MCP Framework Source**: `/home/thalaimalai_pandiyan/Desktop/may/tlc-components/mcp/src/main/java/com/tlc/mcp/`
- **CRM Product APIs**: `/home/thalaimalai_pandiyan/Desktop/may/tlc-crm-server/core/src/main/java/com/tlc/crm/core/api/product/`
- **Security Context**: `/home/thalaimalai_pandiyan/Desktop/may/tlc-crm-server/security/src/main/java/com/tlc/crm/security/context/`

## Support

For implementation questions, refer to:
1. MCP framework documentation in design docs
2. Existing REST API implementations in `ProductRestManagementV1.java`
3. CRM converter patterns in `ProductConverter.java`
4. Field definitions in `ProductFields.java`

