# Product REST API Documentation

## Overview
The Product REST API provides comprehensive CRUD (Create, Read, Update, Delete) operations for managing products in the TLC CRM system. The API is implemented in the `ProductRestManagementV1` class and follows RESTful conventions with JSON request/response payloads.

## Base URL
```
/v1/product
```

## Authentication
All endpoints require authentication. The API uses the CRM security context with organization ID and user ID validation.

## API Endpoints

### 1. Create Product
**Endpoint:** `POST /v1/product`

**Description:** Create a new product

**Request Body:**
```json
{
  "name": "Product Name",
  "description": "Product Description",
  "productCode": "PROD-001",
  "category": "Category Name",
  "type": "Product Type",
  "activeStatus": true,
  "quantity": 100,
  "salesOwner": {
    "iamId": 12345
  },
  "priceTags": [
    {
      "currency": 1,
      "price": 99.99,
      "tax": 10.00,
      "discount": 5.00
    }
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Product Name",
  "description": "Product Description",
  "productCode": "PROD-001",
  "category": "Category Name",
  "type": "Product Type",
  "activeStatus": true,
  "quantity": 100,
  "createdBy": {
    "iamId": 12345,
    "userName": "user@example.com"
  },
  "createdDate": "2026-07-23T10:30:00Z",
  "updatedDate": "2026-07-23T10:30:00Z"
}
```

**HTTP Status Codes:**
- `201 Created` - Product created successfully
- `400 Bad Request` - Invalid request payload
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Permission denied
- `500 Internal Server Error` - Server error

---

### 2. Get Product by ID
**Endpoint:** `GET /v1/product/{id}`

**Description:** Retrieve a product by its ID

**Path Parameters:**
- `id` (required): The product ID

**Request Example:**
```
GET /v1/product/1
```

**Response:**
```json
{
  "id": 1,
  "name": "Product Name",
  "description": "Product Description",
  "productCode": "PROD-001",
  "category": "Category Name",
  "type": "Product Type",
  "activeStatus": true,
  "quantity": 100,
  "salesOwner": {
    "iamId": 12345,
    "userName": "user@example.com"
  },
  "variations": [
    {
      "id": 1,
      "name": "Variation Name",
      "priceTags": [
        {
          "id": 1,
          "currency": 1,
          "price": 99.99,
          "tax": 10.00,
          "discount": 5.00
        }
      ]
    }
  ],
  "createdBy": {
    "iamId": 12345,
    "userName": "user@example.com"
  },
  "updatedBy": {
    "iamId": 12345,
    "userName": "user@example.com"
  },
  "createdDate": "2026-07-23T10:30:00Z",
  "updatedDate": "2026-07-23T10:30:00Z"
}
```

**HTTP Status Codes:**
- `200 OK` - Product retrieved successfully
- `400 Bad Request` - Invalid ID format
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Permission denied
- `404 Not Found` - Product not found
- `500 Internal Server Error` - Server error

---

### 3. Update Product (Full Update)
**Endpoint:** `PUT /v1/product/{id}`

**Description:** Update an entire product (all fields will be replaced)

**Path Parameters:**
- `id` (required): The product ID

**Request Body:**
```json
{
  "name": "Updated Product Name",
  "description": "Updated Description",
  "productCode": "PROD-001-UPD",
  "category": "Updated Category",
  "type": "Updated Type",
  "activeStatus": true,
  "quantity": 150,
  "salesOwner": {
    "iamId": 12345
  },
  "priceTags": [
    {
      "currency": 1,
      "price": 149.99,
      "tax": 15.00,
      "discount": 10.00
    }
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Updated Product Name",
  "description": "Updated Description",
  "productCode": "PROD-001-UPD",
  "category": "Updated Category",
  "type": "Updated Type",
  "activeStatus": true,
  "quantity": 150,
  "updatedDate": "2026-07-23T11:45:00Z"
}
```

**HTTP Status Codes:**
- `200 OK` - Product updated successfully
- `400 Bad Request` - Invalid request payload
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Permission denied
- `404 Not Found` - Product not found
- `500 Internal Server Error` - Server error

---

### 4. Update Product (Partial Update)
**Endpoint:** `PATCH /v1/product/{id}`

**Description:** Update specific fields of a product (only provided fields will be updated)

**Path Parameters:**
- `id` (required): The product ID

**Request Body:**
```json
{
  "name": "Updated Product Name",
  "quantity": 200
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Updated Product Name",
  "quantity": 200,
  "updatedDate": "2026-07-23T12:00:00Z"
}
```

**HTTP Status Codes:**
- `200 OK` - Product partially updated successfully
- `400 Bad Request` - Invalid request payload
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Permission denied
- `404 Not Found` - Product not found
- `500 Internal Server Error` - Server error

---

### 5. Delete Product
**Endpoint:** `DELETE /v1/product/{id}`

**Description:** Delete a product by its ID

**Path Parameters:**
- `id` (required): The product ID

**Request Example:**
```
DELETE /v1/product/1
```

**Response:**
```json
{
  "message": "Product deleted successfully"
}
```

**HTTP Status Codes:**
- `200 OK` / `204 No Content` - Product deleted successfully
- `400 Bad Request` - Invalid ID format
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Permission denied
- `404 Not Found` - Product not found
- `500 Internal Server Error` - Server error

---

## Implementation Details

### File Location
- **REST API Handler:** `/home/thalaimalai_pandiyan/Desktop/may/tlc-crm-server/core/src/main/java/com/tlc/crm/core/action/rest/ProductRestManagementV1.java`

### Key Classes
- **ProductRestManagementV1:** Main REST API handler class extending `CrmSecureRestAction<Product>`
- **ProductRestReqFormatter:** Handles request formatting and field mapping
- **ProductConverter:** Converts between JSON and Product domain objects
- **ProductManager:** Manages product business logic and persistence
- **ConverterConfigBuilder:** Builds configuration for converters

### Architecture Pattern
The implementation follows a layered architecture:
```
REST Controller (ProductRestManagementV1)
        ↓
Request Formatter (ProductRestReqFormatter)
        ↓
Converter (ProductConverter)
        ↓
Manager (ProductManager)
        ↓
Database/Persistence Layer
```

### Request Flow
1. HTTP request received at `/v1/product` endpoint
2. `@ApiRoute` annotation routes to appropriate method (GET, POST, PUT, PATCH, DELETE)
3. `ProductRestReqFormatter.format()` processes the request JSON
4. `ProductConverter.convert()` converts JSON to Product domain object
5. Manager methods (create, update, get, delete) execute business logic
6. Response is converted back to JSON and returned to client

### Error Handling
- Invalid product IDs are validated and return 404 if not found
- Invalid request payloads return 400 Bad Request
- Authentication failures return 401 Unauthorized
- Permission denied scenarios return 403 Forbidden
- Server errors are caught and return 500 Internal Server Error

### Security
- All endpoints are secured with `@SourceAuthorizer` and `CrmSecureRestAction` base class
- Organization ID and User ID are validated from the security context
- All operations include audit trails (createdBy, updatedBy, timestamps)

## Request/Response Format

### Request Headers
```
Content-Type: application/json
Authorization: Bearer <token>
X-Org-Id: <organization-id>
X-User-Id: <user-id>
```

### Response Headers
```
Content-Type: application/json
X-Request-ID: <unique-request-id>
Date: <response-date>
```

## Field Mapping
Common product fields supported:
- `id` - Product ID (auto-generated)
- `name` - Product name (required)
- `description` - Product description
- `productCode` - Unique product code
- `category` - Product category
- `type` - Product type
- `activeStatus` - Whether product is active
- `quantity` - Available quantity
- `salesOwner` - Owner of the product
- `priceTags` - Pricing information with currency, price, tax, discount
- `variations` - Product variations
- `createdBy` - User who created the product
- `updatedBy` - User who last updated the product
- `createdDate` - Creation timestamp
- `updatedDate` - Last update timestamp

## Testing

### Create a Product
```bash
curl -X POST http://localhost:8080/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "productCode": "TEST-001",
    "category": "Test Category",
    "type": "Test Type",
    "activeStatus": true,
    "quantity": 50
  }'
```

### Get a Product
```bash
curl -X GET http://localhost:8080/v1/product/1 \
  -H "Authorization: Bearer <token>"
```

### Update a Product (Full)
```bash
curl -X PUT http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "Updated Test Product",
    "quantity": 75
  }'
```

### Partial Update a Product
```bash
curl -X PATCH http://localhost:8080/v1/product/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "quantity": 100
  }'
```

### Delete a Product
```bash
curl -X DELETE http://localhost:8080/v1/product/1 \
  -H "Authorization: Bearer <token>"
```

## Additional Notes

### Pagination & Filtering
The current implementation supports single product retrieval. For list operations, refer to the `ProductManagement` class which handles bulk operations through the `/product/mgmt` endpoint.

### Price Tags Management
Price tags can be managed through dedicated endpoints in `ProductManagement`:
- `POST /product/mgmt` with type: "updatePriceTags"
- `GET /product/mgmt` with type: "getProductPriceTags"
- `DELETE /product/mgmt` with type: "deletePriceTag"

### Variations
Product variations are managed through:
- Variation entity model (`Variation.java`)
- Variation manager (`VariationManager.java`)
- Variation converter (`VariationConverter.java`)

## Troubleshooting

### Common Issues

**Issue: 404 Product Not Found**
- Ensure the product ID is valid and exists in the database
- Verify the organization ID matches the product's organization

**Issue: 400 Bad Request**
- Validate JSON syntax in request body
- Ensure all required fields are provided
- Check field data types match expected types

**Issue: 403 Permission Denied**
- Verify user has permission to access products
- Check source settings (Product source must be enabled)
- Verify user has appropriate role/permissions

**Issue: 500 Internal Server Error**
- Check server logs for detailed error information
- Verify database connectivity
- Ensure all required services are running

## Related Documentation

- Product Entity Model: `Product.java`
- Product Manager: `ProductManager.java`
- Product Converter: `ProductConverter.java`
- CRM Configuration Pattern: `EntityConfigAction.java`
- REST Security: `CrmSecureRestAction.java`

---

**Last Updated:** July 23, 2026
**Version:** 1.0
**Status:** Complete ✅

