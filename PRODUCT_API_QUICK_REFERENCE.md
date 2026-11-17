# Product REST API - Quick Reference Card

## 📋 API Endpoints at a Glance

### CREATE Product
```
POST /v1/product
Content-Type: application/json

{
  "name": "Product Name",
  "description": "Description",
  "productCode": "PROD-001",
  "category": "Category",
  "type": "Type",
  "activeStatus": true,
  "quantity": 100
}

✅ Returns: 201 Created
❌ Errors: 400, 401, 403, 500
```

### GET Product
```
GET /v1/product/{id}

✅ Returns: 200 OK with full product details
❌ Errors: 400, 401, 403, 404, 500
```

### UPDATE Product (Full)
```
PUT /v1/product/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "quantity": 150
}

✅ Returns: 200 OK
❌ Errors: 400, 401, 403, 404, 500
```

### UPDATE Product (Partial)
```
PATCH /v1/product/{id}
Content-Type: application/json

{
  "quantity": 200
}

✅ Returns: 200 OK
❌ Errors: 400, 401, 403, 404, 500
```

### DELETE Product
```
DELETE /v1/product/{id}

✅ Returns: 204 No Content or 200 OK
❌ Errors: 400, 401, 403, 404, 500
```

---

## 🔐 Authentication

**All requests require:**
```
Authorization: Bearer <your_token>
```

---

## 📊 Common Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | Auto-generated | Product ID |
| name | String | Yes | Product name |
| description | String | No | Product description |
| productCode | String | No | Unique product code |
| category | String | No | Product category |
| type | String | No | Product type |
| activeStatus | Boolean | No | Active/Inactive flag |
| quantity | Integer | No | Available quantity |
| salesOwner | Object | No | Product owner |
| priceTags | Array | No | Pricing information |
| createdDate | Timestamp | Auto | Creation date |
| updatedDate | Timestamp | Auto | Last update date |

---

## 🛠️ cURL Command Templates

### Create
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"name":"Product","quantity":100}'
```

### Read
```bash
curl -X GET http://localhost:8080/v1/product/1 \
  -H "Authorization: Bearer TOKEN"
```

### Update (Full)
```bash
curl -X PUT http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"name":"Updated","quantity":200}'
```

### Update (Partial)
```bash
curl -X PATCH http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"quantity":200}'
```

### Delete
```bash
curl -X DELETE http://localhost:8080/v1/product/1 \
  -H "Authorization: Bearer TOKEN"
```

---

## 📈 HTTP Status Codes

| Code | Status | Meaning |
|------|--------|---------|
| 200 | ✅ | Success (GET, PUT, PATCH, DELETE) |
| 201 | ✅ | Created (POST) |
| 204 | ✅ | No Content (DELETE) |
| 400 | ❌ | Bad Request |
| 401 | ❌ | Unauthorized |
| 403 | ❌ | Forbidden |
| 404 | ❌ | Not Found |
| 500 | ❌ | Server Error |

---

## 🔍 Error Codes

| Error Code | HTTP | Description |
|-----------|------|-------------|
| PRODUCT_NOT_FOUND | 404 | Product doesn't exist |
| INVALID_REQUEST | 400 | Invalid request format |
| UNAUTHORIZED | 401 | Auth token missing/invalid |
| FORBIDDEN | 403 | Permission denied |
| VALIDATION_ERROR | 400 | Data validation failed |

---

## 📝 Request/Response Example

### Request
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{
    "name": "Laptop",
    "description": "Gaming laptop",
    "productCode": "LP-001",
    "category": "Electronics",
    "activeStatus": true,
    "quantity": 50
  }'
```

### Response (201 Created)
```json
{
  "id": 1001,
  "name": "Laptop",
  "description": "Gaming laptop",
  "productCode": "LP-001",
  "category": "Electronics",
  "activeStatus": true,
  "quantity": 50,
  "createdDate": "2026-07-23T14:30:00Z"
}
```

---

## ⚡ Quick Workflow

### 1️⃣ Create Product
```json
POST /v1/product
→ Get back: Product with ID
```

### 2️⃣ Retrieve Product
```json
GET /v1/product/{id}
→ Get back: Full product details
```

### 3️⃣ Update (Partial)
```json
PATCH /v1/product/{id}
→ Update specific fields
```

### 4️⃣ Check Status
```json
GET /v1/product/{id}
→ Verify changes
```

### 5️⃣ Delete if Needed
```json
DELETE /v1/product/{id}
→ Product removed
```

---

## 🎯 Common Use Cases

### Use Case 1: Create and Get
```bash
# Create
ID=$(curl -s -X POST http://localhost:8080/v1/product \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Product"}' | jq '.id')

# Get
curl -X GET http://localhost:8080/v1/product/$ID \
  -H "Authorization: Bearer TOKEN"
```

### Use Case 2: Update Quantity
```bash
curl -X PATCH http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"quantity": 100}'
```

### Use Case 3: Activate/Deactivate
```bash
curl -X PATCH http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"activeStatus": false}'
```

---

## 🚀 Tips & Tricks

### ✅ DO
- ✅ Use PATCH for updates (only changed fields)
- ✅ Always include Authorization header
- ✅ Validate data before sending
- ✅ Handle 404 errors gracefully
- ✅ Implement retry logic for 5xx errors
- ✅ Log all requests for debugging
- ✅ Use meaningful product codes

### ❌ DON'T
- ❌ Use PUT for partial updates
- ❌ Hardcode tokens in code
- ❌ Send invalid JSON
- ❌ Ignore error messages
- ❌ Use negative quantities
- ❌ Reuse product codes
- ❌ Expose sensitive data in logs

---

## 🔗 Related Resources

- **Full Documentation:** See `PRODUCT_API_DOCUMENTATION.md`
- **Usage Guide:** See `PRODUCT_API_USAGE_GUIDE.md`
- **Implementation:** `ProductRestManagementV1.java`
- **Source:** `/tlc-crm-server/core/src/main/java/com/tlc/crm/core/action/rest/`

---

## 📱 Quick Links

| Resource | Location |
|----------|----------|
| REST Handler | `ProductRestManagementV1.java` |
| Request Formatter | `ProductRestReqFormatter.java` |
| Converter | `ProductConverter.java` |
| Manager | `ProductManager.java` |
| Entity | `Product.java` |

---

## ⏱️ Response Times (Typical)

| Operation | Time |
|-----------|------|
| POST (Create) | 100-200ms |
| GET (Retrieve) | 50-100ms |
| PUT (Full Update) | 100-150ms |
| PATCH (Partial) | 100-150ms |
| DELETE | 100-150ms |

---

## 🎓 Learning Path

1. Read `PRODUCT_API_DOCUMENTATION.md` (comprehensive guide)
2. Review `PRODUCT_API_IMPLEMENTATION_SUMMARY.md` (overview)
3. Try examples from `PRODUCT_API_USAGE_GUIDE.md`
4. Use this Quick Reference for quick lookups
5. Explore source code in IDE

---

## 📞 Support

For issues:
1. Check HTTP status code
2. Review error message
3. Verify request format
4. Validate credentials
5. Check server logs
6. Review error documentation

---

## 🎯 Success Checklist

Before deploying:
- [ ] All endpoints tested
- [ ] Error handling implemented
- [ ] Authentication configured
- [ ] Permissions verified
- [ ] Database connectivity checked
- [ ] Logging configured
- [ ] Performance validated
- [ ] Documentation reviewed

---

**Last Updated:** July 23, 2026
**Quick Reference v1.0**

