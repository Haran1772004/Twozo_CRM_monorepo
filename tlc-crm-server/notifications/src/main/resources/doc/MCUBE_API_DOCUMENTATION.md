# MCube Marketplace Action APIs

## Overview

This document covers the `executeAction` APIs handled by `MCubeExecutionHandler` for the MCube telephony marketplace app.

- **Handler**: `com.tlc.crm.notifications.marketplace.handler.MCubeExecutionHandler`
- **App key**: `mcube`
- **Request `type`**: `executeAction`

## API Endpoint

```
POST /api/marketplace/mgmt
Content-Type: application/json
```

**Authentication**: Required (Session-based CRM authentication)

## Common Request Envelope

```json
{
  "type": "executeAction",
  "data": {
    "id": 3001,
    "action": "authenticate",
    "actionData": {}
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | String | Yes | Must be `executeAction` |
| `data.id` | Long | Yes | Marketplace app installation id |
| `data.action` | String | Yes | Action to execute (see below) |
| `data.actionData` | Object | Depends | Action payload |

## Supported Actions

| Action | Purpose |
|---|---|
| `authenticate` | Link MCube credentials to org + app |
| `getAuthData` | Fetch stored MCube auth data |
| `syncUsers` | Sync multiple CRM users into MCube |
| `updateUser` | Update an already-linked MCube user |
| `syncUser` | Sync a single already-linked MCube user |
| `getUser` | Fetch one linked MCube user |
| `removeUser` | Remove one linked MCube user |
| `userList` | Fetch paginated linked MCube users |
| `removeAuthData` | Remove auth data and cleanup linked users |
| `getUsers` | Fetch CRM users eligible for MCube sync |

---

## 1) `authenticate`

Links MCube credentials using an authentication token.

### `actionData`

```json
{
  "authToken": "mcube-auth-token-xyz"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `authToken` | String | Yes | MCube authentication token |

### Success Response

```json
{
  "code": 0,
  "message": "MCube authentication successful"
}
```

### Known Failure Cases

- Auth token already linked for another installation
- MCube authentication failure
- Validation failure while storing auth data

---

## 2) `getAuthData`

Fetches stored MCube authentication metadata.

### `actionData`

No input required.

### Success Response

```json
{
  "code": 0,
  "data": {
    "authToken": "mcube-auth-token-xyz",
    "webhookUrl": "https://<crm-host>/<mcube-webhook-path>"
  }
}
```

### When Not Found

```json
{
  "code": 0,
  "message": "Authentication data not found"
}
```

---

## 3) `syncUsers`

Syncs multiple users that are not already linked to MCube.

### `actionData`

```json
{
  "list": [
    {
      "userId": 101
    },
    {
      "userId": 102
    }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `list` | Array | Yes | Users to sync |
| `list[].userId` | Long | Yes | CRM user id |

### Response (partial success supported)

```json
{
  "code": 0,
  "messages": [
    {
      "message": "2 users synced successfully",
      "status": "success"
    },
    {
      "message": "1 user sync failed",
      "status": "failure"
    }
  ]
}
```

### Notes

- Users already synced are skipped
- Mobile number must contain country code for sync
- If no users are eligible: returns `user already sync` message
- Role is auto-determined: Admin users get roleId=1, others get roleId=2

---

## 4) `updateUser`

Updates MCube details for an already-linked user.

### `actionData`

```json
{
  "userId": 101
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | Long | Yes | Linked CRM user id |

### Success Response

```json
{
  "code": 0,
  "message": "User update successful"
}
```

---

## 5) `syncUser`

Creates/syncs one already-linked user.

### `actionData`

```json
{
  "userId": 101
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | Long | Yes | CRM user id |

### Success Response

```json
{
  "code": 0,
  "message": "User sync successful"
}
```

### Common Failure Cases

- User not linked in local store
- User retrieval failure from IAM
- MCube API create failure

---

## 6) `getUser`

Fetches one linked MCube user.

### `actionData`

```json
{
  "userId": 101
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | Long | Yes | CRM user id |

### Success Response

```json
{
  "code": 0,
  "user": {
    "id": 101,
    "name": "Alex Smith"
  }
}
```

### Not Found

```json
{
  "code": 0,
  "message": "User not found"
}
```

---

## 7) `removeUser`

Removes one linked user from local mapping and MCube (if linked remotely).

### `actionData`

```json
{
  "userId": 101
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | Long | Yes | CRM user id to remove |

### Success Response

```json
{
  "code": 0,
  "message": "User removed successfully"
}
```

---

## 8) `userList`

Returns paginated linked MCube users.

### `actionData`

```json
{
  "start": 1,
  "limit": 20,
  "sort": {
    "field": "user",
    "type": "asc"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `start` | Integer | No | Page start (default: 1) |
| `limit` | Integer | No | Max records (default/max: 20) |
| `sort.field` | String | No | Sort field |
| `sort.type` | String | No | Sort type (`asc` or `desc`) |

### Success Response

```json
{
  "code": 0,
  "users": [
    {
      "user": {
        "id": 101,
        "name": "Alex Smith",
        "thumbnail": {}
      },
      "role": "Sales Rep",
      "syncStatus": "connected"
    }
  ],
  "totalRecords": 1,
  "hasMore": false
}
```

### Sync Status Values

| Status | Description |
|---|---|
| `connected` | User successfully synced with MCube |
| `failed` | User sync failed |

---

## 9) `removeAuthData`

Removes MCube auth configuration and disconnects all linked users.

### `actionData`

No input required.

### Success Response

```json
{
  "code": 0,
  "message": "Authentication removed successfully"
}
```

---

## 10) `getUsers`

Fetches eligible CRM users who are active, have phone numbers, and are not already synced to MCube.

### `actionData`

```json
{
  "start": 1,
  "limit": 20,
  "criteria": {
    "type": "name",
    "value": "alex"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `start` | Integer | No | Page start (default: 1) |
| `limit` | Integer | No | Max records (default/max: 20) |
| `criteria` | Object | No | Search filter |
| `criteria.type` | String | No | Search type |
| `criteria.value` | String | No | Search value |

### Success Response

```json
{
  "code": 0,
  "totalRecords": 24,
  "hasMore": true,
  "users": [
    {
      "id": 101,
      "name": "Alex Smith"
    }
  ]
}
```

---

## Install API

Installs the MCube marketplace app for the organization.

### Request

```json
{
  "type": "install",
  "data": {
    "id": 3001
  }
}
```

### Success Response

```json
{
  "code": 0,
  "type": "standalone",
  "message": "MCube app installed successfully"
}
```

---

## Uninstall API

Uninstalls the MCube marketplace app and removes all linked users.

### Request

```json
{
  "type": "uninstall",
  "data": {
    "id": 3001
  }
}
```

### Success Response

```json
{
  "code": 0,
  "message": "MCube app uninstalled successfully"
}
```

---

## Error Handling Notes

- API returns translated `message` fields for user-facing status
- Failures are returned through CRM error code wrappers
- Async actions (`syncUsers`, `syncUser`, `getUsers`) send response after callback completion
- Validation errors return HTTP 400 with error summary

## Error Codes

| Error Code | Description |
|---|---|
| `APP_ID_ALREADY_LINKED` | Auth token already linked to another installation |
| `AUTHENTICATION_FAILED` | MCube authentication failure |
| `USERS_SYNC_ACTION_FAILED` | Bulk user sync failed |
| `USER_SYNC_ACTION_FAILED` | Single user sync failed |
| `USER_DATA_VALIDATION_FAILED` | User data validation failed |
| `USER_RETRIEVAL_FAILED` | IAM user retrieval failed |
| `INSTALLATION_FAILED` | App installation failed |
| `UNINSTALLATION_FAILED` | App uninstallation failed |

---

## Configuration

The MCube integration requires the following configuration properties:

| Config Key | Description |
|------------|-------------|
| `notification.mcube.url.common` | MCube API base URL for common operations |
| `notification.mcube.url.config` | MCube configuration API URL |
| `notification.mcube.url.webhook` | Webhook URL for receiving MCube callbacks |

---

## Source of Truth

Implementation reference:

- `tlc-crm-server/notifications/src/main/java/com/tlc/crm/notifications/marketplace/handler/MCubeExecutionHandler.java`
- `tlc-crm-server/notifications/src/main/java/com/tlc/crm/notifications/fields/TelephoneFields.java`

