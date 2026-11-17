# Product REST API - Practical Usage Guide

## Quick Start Examples

This guide provides practical examples for using the Product REST API endpoints.

---

## 1. Create a Product

### Example 1: Create a Basic Product

**Request:**
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "name": "MacBook Pro",
    "description": "13-inch MacBook Pro with M1 chip",
    "productCode": "MBP-M1-2024",
    "category": "Laptops",
    "type": "Computer",
    "activeStatus": true,
    "quantity": 50
  }'
```

**Success Response (201 Created):**
```json
{
  "id": 1001,
  "name": "MacBook Pro",
  "description": "13-inch MacBook Pro with M1 chip",
  "productCode": "MBP-M1-2024",
  "category": "Laptops",
  "type": "Computer",
  "activeStatus": true,
  "quantity": 50,
  "createdBy": {
    "iamId": 123,
    "userName": "john.doe@company.com"
  },
  "createdDate": "2026-07-23T14:30:00Z",
  "updatedDate": "2026-07-23T14:30:00Z"
}
```

### Example 2: Create a Product with Pricing

**Request:**
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Latest iPhone with advanced camera",
    "productCode": "IPHONE-15-PRO",
    "category": "Mobile Phones",
    "type": "Smartphone",
    "activeStatus": true,
    "quantity": 200,
    "salesOwner": {
      "iamId": 123
    },
    "priceTags": [
      {
        "currency": 1,
        "price": 999.99,
        "tax": 89.99,
        "discount": 50.00
      }
    ]
  }'
```

**Success Response:**
```json
{
  "id": 1002,
  "name": "iPhone 15 Pro",
  "productCode": "IPHONE-15-PRO",
  "quantity": 200,
  "activeStatus": true,
  "priceTags": [
    {
      "id": 5001,
      "currency": 1,
      "price": 999.99,
      "tax": 89.99,
      "discount": 50.00
    }
  ],
  "createdDate": "2026-07-23T14:35:00Z"
}
```

### Example 3: Create Product - Error Cases

**Error: Missing Required Field**
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "description": "Product without name",
    "productCode": "PROD-NO-NAME"
  }'
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Field 'name' is required",
  "timestamp": "2026-07-23T14:40:00Z"
}
```

**Error: Authentication Failed**
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product"
  }'
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid",
  "timestamp": "2026-07-23T14:42:00Z"
}
```

---

## 2. Get a Product

### Example 1: Get Product by ID

**Request:**
```bash
curl -X GET http://localhost:8080/v1/product/1001 \
  -H "Authorization: Bearer ..."
```

**Success Response (200 OK):**
```json
{
  "id": 1001,
  "name": "MacBook Pro",
  "description": "13-inch MacBook Pro with M1 chip",
  "productCode": "MBP-M1-2024",
  "category": "Laptops",
  "type": "Computer",
  "activeStatus": true,
  "quantity": 50,
  "salesOwner": {
    "iamId": 123,
    "userName": "john.doe@company.com"
  },
  "variations": [
    {
      "id": 2001,
      "name": "Default",
      "priceTags": [
        {
          "id": 5001,
          "currency": 1,
          "currencyName": "USD",
          "price": 1299.99,
          "tax": 129.99,
          "discount": 100.00
        },
        {
          "id": 5002,
          "currency": 2,
          "currencyName": "EUR",
          "price": 1199.99,
          "tax": 119.99,
          "discount": 80.00
        }
      ]
    }
  ],
  "customFields": {
    "warranty": "2 years",
    "manufacturerCode": "MBP2024001"
  },
  "createdBy": {
    "iamId": 123,
    "userName": "john.doe@company.com"
  },
  "updatedBy": {
    "iamId": 123,
    "userName": "john.doe@company.com"
  },
  "createdDate": "2026-07-23T14:30:00Z",
  "updatedDate": "2026-07-23T14:30:00Z"
}
```

### Example 2: Get Non-existent Product

**Request:**
```bash
curl -X GET http://localhost:8080/v1/product/99999 \
  -H "Authorization: Bearer ..."
```

**Error Response (404 Not Found):**
```json
{
  "error": "Not Found",
  "message": "Product with ID 99999 not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-07-23T14:45:00Z"
}
```

### Example 3: Get Product - Invalid ID Format

**Request:**
```bash
curl -X GET http://localhost:8080/v1/product/abc \
  -H "Authorization: Bearer ..."
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Invalid product ID format. Expected numeric value",
  "timestamp": "2026-07-23T14:47:00Z"
}
```

---

## 3. Update a Product (Full Update with PUT)

### Example 1: Complete Product Update

**Request:**
```bash
curl -X PUT http://localhost:8080/v1/product/1001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "name": "MacBook Pro 14-inch",
    "description": "14-inch MacBook Pro with M1 Pro chip - Updated",
    "productCode": "MBP-M1-PRO-2024",
    "category": "Laptops",
    "type": "Computer",
    "activeStatus": true,
    "quantity": 75,
    "salesOwner": {
      "iamId": 125
    }
  }'
```

**Success Response (200 OK):**
```json
{
  "id": 1001,
  "name": "MacBook Pro 14-inch",
  "description": "14-inch MacBook Pro with M1 Pro chip - Updated",
  "productCode": "MBP-M1-PRO-2024",
  "category": "Laptops",
  "quantity": 75,
  "salesOwner": {
    "iamId": 125,
    "userName": "jane.smith@company.com"
  },
  "updatedBy": {
    "iamId": 123,
    "userName": "john.doe@company.com"
  },
  "updatedDate": "2026-07-23T15:00:00Z"
}
```

### Example 2: Update Only Key Fields

**Request:**
```bash
curl -X PUT http://localhost:8080/v1/product/1002 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "name": "iPhone 15 Pro Max",
    "quantity": 150,
    "activeStatus": false
  }'
```

**Success Response (200 OK):**
```json
{
  "id": 1002,
  "name": "iPhone 15 Pro Max",
  "quantity": 150,
  "activeStatus": false,
  "updatedDate": "2026-07-23T15:05:00Z"
}
```

### Example 3: Update - Validation Error

**Request:**
```bash
curl -X PUT http://localhost:8080/v1/product/1001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "name": "",
    "quantity": -10
  }'
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": [
    {
      "field": "name",
      "message": "Product name cannot be empty"
    },
    {
      "field": "quantity",
      "message": "Quantity must be greater than or equal to 0"
    }
  ],
  "timestamp": "2026-07-23T15:10:00Z"
}
```

---

## 4. Update a Product (Partial Update with PATCH)

### Example 1: Update Only Quantity

**Request:**
```bash
curl -X PATCH http://localhost:8080/v1/product/1001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "quantity": 120
  }'
```

**Success Response (200 OK):**
```json
{
  "id": 1001,
  "quantity": 120,
  "updatedDate": "2026-07-23T15:15:00Z",
  "message": "Product partially updated"
}
```

### Example 2: Update Status and Description

**Request:**
```bash
curl -X PATCH http://localhost:8080/v1/product/1002 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "activeStatus": true,
    "description": "Latest iPhone with advanced camera - Back in stock"
  }'
```

**Success Response (200 OK):**
```json
{
  "id": 1002,
  "activeStatus": true,
  "description": "Latest iPhone with advanced camera - Back in stock",
  "updatedDate": "2026-07-23T15:20:00Z"
}
```

### Example 3: PATCH Non-existent Product

**Request:**
```bash
curl -X PATCH http://localhost:8080/v1/product/99999 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "quantity": 100
  }'
```

**Error Response (404 Not Found):**
```json
{
  "error": "Not Found",
  "message": "Product with ID 99999 not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-07-23T15:25:00Z"
}
```

### Example 4: PATCH with Invalid Data

**Request:**
```bash
curl -X PATCH http://localhost:8080/v1/product/1001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ..." \
  -d '{
    "quantity": "invalid"
  }'
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Invalid data type",
  "validationErrors": [
    {
      "field": "quantity",
      "message": "Quantity must be a number",
      "receivedValue": "invalid"
    }
  ],
  "timestamp": "2026-07-23T15:30:00Z"
}
```

---

## 5. Delete a Product

### Example 1: Delete Product Successfully

**Request:**
```bash
curl -X DELETE http://localhost:8080/v1/product/1001 \
  -H "Authorization: Bearer ..."
```

**Success Response (204 No Content or 200 OK):**
```json
{
  "message": "Product deleted successfully",
  "deletedId": 1001,
  "timestamp": "2026-07-23T15:35:00Z"
}
```

### Example 2: Delete Non-existent Product

**Request:**
```bash
curl -X DELETE http://localhost:8080/v1/product/99999 \
  -H "Authorization: Bearer ..."
```

**Error Response (404 Not Found):**
```json
{
  "error": "Not Found",
  "message": "Product with ID 99999 not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-07-23T15:40:00Z"
}
```

### Example 3: Delete - Permission Denied

**Request:**
```bash
curl -X DELETE http://localhost:8080/v1/product/1002 \
  -H "Authorization: Bearer ..."
```

**Error Response (403 Forbidden):**
```json
{
  "error": "Forbidden",
  "message": "You do not have permission to delete this product",
  "timestamp": "2026-07-23T15:45:00Z"
}
```

---

## Common Response Patterns

### Success Response Pattern
```json
{
  "id": 1001,
  "name": "Product Name",
  "description": "Description",
  "...other fields...": "...",
  "createdDate": "2026-07-23T14:30:00Z",
  "updatedDate": "2026-07-23T14:30:00Z"
}
```

### Error Response Pattern
```json
{
  "error": "Error Type",
  "message": "Descriptive error message",
  "errorCode": "ERROR_CODE",
  "validationErrors": [
    {
      "field": "fieldName",
      "message": "Field-specific error message"
    }
  ],
  "timestamp": "2026-07-23T15:00:00Z"
}
```

---

## HTTP Status Codes Reference

| Code | Meaning | When It Occurs |
|------|---------|-------------------|
| 200 | OK | Successful GET, PUT, PATCH, or DELETE |
| 201 | Created | Product successfully created with POST |
| 204 | No Content | Successful DELETE (empty response) |
| 400 | Bad Request | Invalid request format or validation failed |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | User lacks permission for this operation |
| 404 | Not Found | Product ID doesn't exist |
| 500 | Server Error | Internal server error |

---

## Request Headers Required

```
Content-Type: application/json          (for POST, PUT, PATCH)
Authorization: Bearer <your_token>      (required for all endpoints)
Accept: application/json                (recommended)
Accept-Language: en-US                  (optional, for i18n)
```

## Response Headers

```
Content-Type: application/json
Date: <response-date>
X-Request-ID: <unique-request-id>
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: <reset-time>
```

---

## Python Usage Example

```python
import requests
import json

BASE_URL = "http://localhost:8080/v1/product"
TOKEN = "your_bearer_token"

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

# Create Product
def create_product(name, description, product_code):
    payload = {
        "name": name,
        "description": description,
        "productCode": product_code,
        "category": "Electronics",
        "type": "Computer",
        "activeStatus": True,
        "quantity": 100
    }
    response = requests.post(BASE_URL, json=payload, headers=headers)
    return response.json()

# Get Product
def get_product(product_id):
    response = requests.get(f"{BASE_URL}/{product_id}", headers=headers)
    return response.json()

# Update Product (Full)
def update_product(product_id, **kwargs):
    response = requests.put(f"{BASE_URL}/{product_id}", json=kwargs, headers=headers)
    return response.json()

# Partial Update Product
def patch_product(product_id, **kwargs):
    response = requests.patch(f"{BASE_URL}/{product_id}", json=kwargs, headers=headers)
    return response.json()

# Delete Product
def delete_product(product_id):
    response = requests.delete(f"{BASE_URL}/{product_id}", headers=headers)
    return response.status_code

# Usage
if __name__ == "__main__":
    # Create
    new_product = create_product("MacBook Pro", "Laptop", "MBP-001")
    product_id = new_product['id']
    print(f"Created product ID: {product_id}")
    
    # Get
    product = get_product(product_id)
    print(f"Product: {product['name']}")
    
    # Update
    updated = update_product(product_id, quantity=200, activeStatus=False)
    print(f"Updated: {updated}")
    
    # Partial Update
    patched = patch_product(product_id, quantity=150)
    print(f"Patched: {patched}")
    
    # Delete
    status = delete_product(product_id)
    print(f"Delete status: {status}")
```

---

## JavaScript/Node.js Usage Example

```javascript
const axios = require('axios');

const BASE_URL = 'http://localhost:8080/v1/product';
const TOKEN = 'your_bearer_token';

const headers = {
    'Authorization': `Bearer ${TOKEN}`,
    'Content-Type': 'application/json'
};

// Create Product
async function createProduct(name, description, productCode) {
    try {
        const payload = {
            name,
            description,
            productCode,
            category: 'Electronics',
            type: 'Computer',
            activeStatus: true,
            quantity: 100
        };
        const response = await axios.post(BASE_URL, payload, { headers });
        return response.data;
    } catch (error) {
        console.error('Create error:', error.response?.data);
    }
}

// Get Product
async function getProduct(productId) {
    try {
        const response = await axios.get(`${BASE_URL}/${productId}`, { headers });
        return response.data;
    } catch (error) {
        console.error('Get error:', error.response?.data);
    }
}

// Update Product (Full)
async function updateProduct(productId, data) {
    try {
        const response = await axios.put(`${BASE_URL}/${productId}`, data, { headers });
        return response.data;
    } catch (error) {
        console.error('Update error:', error.response?.data);
    }
}

// Partial Update
async function patchProduct(productId, data) {
    try {
        const response = await axios.patch(`${BASE_URL}/${productId}`, data, { headers });
        return response.data;
    } catch (error) {
        console.error('Patch error:', error.response?.data);
    }
}

// Delete Product
async function deleteProduct(productId) {
    try {
        const response = await axios.delete(`${BASE_URL}/${productId}`, { headers });
        return response.status;
    } catch (error) {
        console.error('Delete error:', error.response?.data);
    }
}

// Usage
(async () => {
    // Create
    const newProduct = await createProduct('MacBook Pro', 'Laptop', 'MBP-001');
    const productId = newProduct.id;
    console.log(`Created product ID: ${productId}`);
    
    // Get
    const product = await getProduct(productId);
    console.log(`Product: ${product.name}`);
    
    // Update
    const updated = await updateProduct(productId, { quantity: 200, activeStatus: false });
    console.log('Updated:', updated);
    
    // Patch
    const patched = await patchProduct(productId, { quantity: 150 });
    console.log('Patched:', patched);
    
    // Delete
    const status = await deleteProduct(productId);
    console.log(`Delete status: ${status}`);
})();
```

---

## Best Practices

1. **Always Include Authorization Header**
   - All requests must include valid Bearer token

2. **Use PATCH for Partial Updates**
   - Use PATCH when updating only specific fields
   - Use PUT when replacing entire resource

3. **Handle Errors Gracefully**
   - Check HTTP status codes
   - Parse error messages for debugging
   - Implement retry logic with exponential backoff

4. **Validate Data Before Sending**
   - Validate required fields
   - Check data types and formats
   - Ensure numeric fields are valid

5. **Use Request IDs for Tracing**
   - Include unique request IDs in logging
   - Use X-Request-ID header for correlation

6. **Cache Responses Appropriately**
   - Cache GET responses when appropriate
   - Invalidate cache after PUT/PATCH/DELETE

---

## Troubleshooting

### Issue: 401 Unauthorized
**Solution:**
- Verify token is included in Authorization header
- Check token format (Bearer <token>)
- Ensure token is not expired

### Issue: 403 Forbidden
**Solution:**
- Verify user has product permissions
- Check if Product source is enabled
- Verify user role/organization access

### Issue: 404 Not Found
**Solution:**
- Verify product ID is correct
- Check if product exists in database
- Verify organization ID matches

### Issue: 400 Bad Request
**Solution:**
- Validate JSON syntax
- Check field data types
- Ensure required fields are present
- Review validation error messages

---

**Last Updated:** July 23, 2026
**Version:** 1.0

