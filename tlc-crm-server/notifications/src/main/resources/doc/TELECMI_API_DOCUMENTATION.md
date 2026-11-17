# TeleCMI Marketplace Action APIs

## Overview

This document covers the `executeAction` APIs handled by `TeleCMIExecutionHandler` for the TeleCMI marketplace app.

- Handler: `com.tlc.crm.notifications.marketplace.handler.TeleCMIExecutionHandler`
- App key: `telecmi`
- Request `type`: `executeAction`

## Common Request Envelope

```json
{
  "type": "executeAction",
  "data": {
    "id": 2001,
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
| `authenticate` | Link TeleCMI credentials to org + app |
| `getAuthData` | Fetch stored TeleCMI auth data |
| `syncUsers` | Sync multiple CRM users into TeleCMI |
| `updateUser` | Update an already-linked TeleCMI user |
| `syncUser` | Sync a single already-linked TeleCMI user |
| `getUser` | Fetch one linked TeleCMI user |
| `removeUser` | Remove one linked TeleCMI user |
| `userList` | Fetch paginated linked TeleCMI users |
| `removeAuthData` | Remove auth data and cleanup linked users |
| `getUsers` | Fetch CRM users eligible for TeleCMI sync |

---

## 1) `authenticate`

Links TeleCMI app credentials.

### `actionData`

```json
{
  "appId": 123456,
  "secret": "telecmi-secret"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `appId` | Long | Yes | TeleCMI app id |
| `secret` | String | Yes | TeleCMI secret |

### Success Response (example)

```json
{
  "message": "TeleCMI authentication successful"
}
```

### Known Failure Cases

- App id already linked for another installation.
- TeleCMI authentication failure.
- Validation failure while storing auth data.

---

## 2) `getAuthData`

Fetches stored TeleCMI authentication metadata.

### `actionData`

No input required.

### Success Response (example)

```json
{
  "data": {
    "appId": 123456,
    "secret": "telecmi-secret",
    "webhookUrl": "https://<crm-host>/<telecmi-webhook-path>"
  }
}
```

### When Not Found

```json
{
  "message": "Authentication data not found"
}
```

---

## 3) `syncUsers`

Syncs multiple users that are not already linked.

### `actionData`

```json
{
  "list": [
    {
      "userId": 101,
      "password": "pass-101"
    },
    {
      "userId": 102,
      "password": "pass-102"
    }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `list` | Array | Yes | Users to sync |
| `list[].userId` | Long | Yes | CRM user id |
| `list[].password` | String | Yes | TeleCMI password |

### Response (partial success supported)

```json
{
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

- Users already synced are skipped.
- Mobile number must contain country code for sync.
- If no users are eligible: returns `user already sync` message.

---

## 4) `updateUser`

Updates TeleCMI details for an already-linked user.

### `actionData`

```json
{
  "userId": 101,
  "password": "new-password"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | Long | Yes | Linked CRM user id |
| `password` | String | No | Updated password |

### Success Response

```json
{
  "message": "User update successful"
}
```

---

## 5) `syncUser`

Creates/syncs one already-linked user using next available extension.

### `actionData`

```json
{
  "userId": 101,
  "password": "password"
}
```

### Success Response

```json
{
  "message": "User sync successful"
}
```

### Common Failure Cases

- User not linked in local store.
- User retrieval failure from IAM.
- TeleCMI API create failure.

---

## 6) `getUser`

Fetches one linked TeleCMI user.

### `actionData`

```json
{
  "userId": 101
}
```

### Success Response (example)

```json
{
  "user": {
    "id": 101,
    "name": "Alex",
    "password": "******"
  }
}
```

### Not Found

```json
{
  "message": "User not found"
}
```

---

## 7) `removeUser`

Removes one linked user from local mapping and TeleCMI (if linked remotely).

### `actionData`

```json
{
  "userId": 101
}
```

### Success Response

```json
{
  "message": "User removed successfully"
}
```

---

## 8) `userList`

Returns paginated linked TeleCMI users.

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
| `sort.type` | String | No | Sort type |

### Success Response (example)

```json
{
  "users": [
    {
      "user": {
        "id": 101,
        "name": "Alex",
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

---

## 9) `removeAuthData`

Removes TeleCMI auth configuration and disconnects linked users.

### `actionData`

No input required.

### Success Response

```json
{
  "message": "Authentication removed successfully"
}
```

---

## 10) `getUsers`

Fetches eligible CRM users who are active, have phone numbers, and are not already synced to TeleCMI.

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

### Success Response (example)

```json
{
  "totalRecords": 24,
  "hasMore": true,
  "users": [
    {
      "id": 101,
      "name": "Alex"
    }
  ]
}
```

---

## Error Handling Notes

- API returns translated `message` fields for user-facing status.
- Failures are returned through CRM error code wrappers (for example user retrieval or TeleCMI sync failures).
- Async actions (`syncUsers`, `syncUser`, `getUsers`) send response after callback completion.

## Source of Truth

Implementation reference:

- `tlc-crm-server/notifications/src/main/java/com/tlc/crm/notifications/marketplace/handler/TeleCMIExecutionHandler.java`
- `tlc-crm-server/notifications/src/main/java/com/tlc/crm/notifications/fields/TelephoneFields.java`

