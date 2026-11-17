# Quotation Management API Documentation

## Base URL

```
/api/quote/mgmt
```

## Overview

The Quotation Management API provides endpoints for creating, retrieving, updating, drafting, deleting, cloning, and bulk operations on quotations. All requests are sent as `POST` with a JSON body containing a `type` field that determines the operation.

---

## 1. Create Quotation

Creates a new quotation by resolving deal data (contacts, products, seller info) into a quote template.

### Request

```json
{
    "type": "create",
    "data": {
        "dealId": 151
    }
}
```

| Field         | Type   | Required | Description                                   |
|---------------|--------|----------|-----------------------------------------------|
| `type`        | String | Yes      | Must be `"create"`                            |
| `data.dealId` | Long   | Yes      | The ID of the deal to generate the quote from |

### Response

```json
{
    "code": 0,
    "data": {
        "quoteJson": {
            "name": "CRM Deal",
            "settings": {
                "language": {
                    "name": "English (United States)",
                    "id": 9
                },
                "locale": "US",
                "currency": {
                    "code": "USD",
                    "id": 150
                },
                "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/1"
            },
            "formAttributes": [
                {
                    "name": "Header",
                    "type": "HEADER",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The name given to the quote for easy identification and reference.",
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "Quote",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Quote reference",
                            "type": "QUOTE_REF",
                            "info": "A unique reference number associated with the quote for tracking purposes.",
                            "label": {
                                "name": "Label",
                                "value": "Quote ref.",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "20260415-151",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Issue date",
                            "type": "DATE",
                            "info": "The date on which the quote is officially issued or published.",
                            "label": {
                                "name": "Label",
                                "value": "Issue date",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1776256244499,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Expiration date",
                            "type": "DATE",
                            "info": "The date until which the quote remains valid for acceptance.",
                            "label": {
                                "name": "Label",
                                "value": "Expires",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1784118644499,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Currency",
                            "type": "CURRENCY",
                            "info": "The currency in which all quote values and totals are calculated.",
                            "label": {
                                "name": "Label",
                                "value": "Currency",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "USD",
                                    "type": "CURRENCY",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ],
                    "logo": [
                        {
                            "name": "Show Company Name",
                            "type": "COMPANY_NAME_TOGGLE",
                            "value": "TLC",
                            "info": "Toggle to show or hide the registered company name in the header.",
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Logo",
                            "type": "ATTACHMENT",
                            "value": "",
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Parties",
                    "type": "PARTIES",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "sellerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the seller in the quote.",
                            "values": [
                                {
                                    "value": "Seller",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "SELLER_COMPANY",
                            "info": "The company information of the seller, derived from the logged-in account.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "TLC",
                                    "type": "ORGANISATION",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "company": {
                                "name": "TLC"
                            },
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "SELLER_CONTACT",
                            "info": "The seller's contact information, based on the logged-in user.",
                            "label": {
                                "name": "Label",
                                "value": "Contact",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Sathish Kumar",
                                    "type": "USER",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Email",
                                    "value": "sathish@twozo.io",
                                    "type": "USER",
                                    "position": "SUB_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "contact": {
                                "firstName": "Sathish",
                                "lastName": "Kumar",
                                "phone": "+919788971797",
                                "email": "sathish@twozo.io"
                            },
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        }
                    ],
                    "buyerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the buyer in the quote.",
                            "values": [
                                {
                                    "value": "Buyer",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "BUYER_COMPANY",
                            "info": "The company information associated with the buyer in this quote.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Twozo",
                                    "type": "COMPANY",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "showDeleteIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "BUYER_CONTACTS",
                            "info": "The contact information associated with the buyer in this quote.",
                            "placeHolder": "{{PARTY_BUYER_CONTACTS}}",
                            "label": {
                                "name": "Label",
                                "value": "Contacts",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": true
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                },
                {
                    "name": "Summary",
                    "type": "SUMMARY",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "summaryDetails": [
                        {
                            "name": "Effective date",
                            "type": "EFFECTIVE_DATE",
                            "info": "The date from which the terms of the quote become applicable.",
                            "label": {
                                "name": "Label",
                                "value": "Effective date",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Effective",
                                    "value": 1,
                                    "type": "EFFECTIVE_DATE",
                                    "hasInlineEditOption": true
                                },
                                {
                                    "name": "Effective",
                                    "value": "On Agreement",
                                    "type": "EFFECTIVE_DATE",
                                    "position": "VALUE",
                                    "isHiddenOnForm": true,
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Term length",
                            "type": "TERM_LENGTH",
                            "info": "The duration for which the quote terms remain in effect, applicable to subscription-based products.",
                            "label": {
                                "name": "Label",
                                "value": "Term length",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false,
                                    "isFormField": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total Discount",
                            "type": "DISCOUNT",
                            "info": "The overall discount applied to the quote amount.",
                            "format": "Show as amount",
                            "label": {
                                "name": "Label",
                                "value": "Total Discount",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "valueAsAmount": 275.0,
                                    "valueAsPercentage": "10.96%",
                                    "valueAsPercentageAndAmount": "275.0 (10.96%)",
                                    "currency": "$",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total contract value",
                            "type": "TOTAL_VALUE",
                            "info": "The overall value of the contract calculated over its full duration.",
                            "label": {
                                "name": "Label",
                                "value": "Total contract value",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 2346.25,
                                    "currency": "$",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ]
                },
                {
                    "name": "Line Items",
                    "type": "LINE_ITEMS",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "items": [
                        {
                            "name": "Line items",
                            "displayName": "Line items (2)",
                            "type": "PRODUCTS",
                            "info": "Add your products or services to your quote template.",
                            "placeHolder": "{{LINE_ITEMS}}",
                            "values": [
                                {
                                    "name": "CRM",
                                    "quantity": 10,
                                    "discount": 10.0,
                                    "unitPrice": 200.0,
                                    "termTotal": 1800.0,
                                    "currency": "$",
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Pharma CRM",
                                    "quantity": 5,
                                    "discount": 15.0,
                                    "unitPrice": 100.0,
                                    "termTotal": 425.0,
                                    "currency": "$",
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Cycle",
                                    "quantity": 1,
                                    "discount": 0.0,
                                    "unitPrice": 10.0,
                                    "termTotal": 10.0,
                                    "currency": "$",
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ],
                    "totals": {
                        "name": "Totals",
                        "info": "Turn on the totals section to show sub-total, discounts, taxes, amount due on the effective date, and total contract value on your quote.",
                        "isEnabled": false,
                        "canMakeDisable": true,
                        "attributes": [
                            {
                                "name": "Subtotal",
                                "type": "SUBTOTAL",
                                "info": "The total value of all line items before any discounts or taxes are applied.",
                                "format": "Simple subtotal amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Subtotal",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 2235.0,
                                        "valueWithBreakdown": "$2,235.00/one time",
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Discount included in the subtotal",
                                "type": "DISCOUNT",
                                "info": "The total discount applied across line items, already reflected within the sub-total.",
                                "format": "Show as amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Discount included in the subtotal",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "valueAsAmount": 275.0,
                                        "valueAsPercentage": "10.96%",
                                        "valueAsPercentageAndAmount": "275.0 (10.96%)",
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Taxes",
                                "type": "TAX",
                                "info": "The total tax amount applied to the quote. If no tax rates are configured for line items, the tax amount will not be displayed.",
                                "label": {
                                    "name": "Label",
                                    "value": "Taxes",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 111.25,
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Due on effective date",
                                "type": "DUE",
                                "info": "Indicates that the payment is expected on the effective date of the quote.",
                                "description": {
                                    "name": "Description",
                                    "value": "The total amount due on the effective date, not including any payments starting at a later date.",
                                    "type": "TEXT",
                                    "position": "DESCRIPTION",
                                    "hasInlineEditOption": true
                                },
                                "label": {
                                    "name": "Label",
                                    "value": "Due on effective date",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 2346.25,
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Total contract value (TCV)",
                                "type": "TOTAL_VALUE",
                                "info": "The overall value of the contract calculated over its full duration.",
                                "label": {
                                    "name": "Label",
                                    "value": "Total contract value",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 2346.25,
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            }
                        ]
                    }
                },
                {
                    "name": "Payments",
                    "type": "PAYMENT",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "label": {
                                "name": "Label",
                                "value": "Payments",
                                "type": "TEXT",
                                "position": "HEADER",
                                "hasInlineEditOption": true
                            },
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Billing",
                            "type": "BILLING",
                            "info": "Defines how charges are applied within the quote.",
                            "label": {
                                "name": "Label",
                                "value": "Customer must agree to the following payment terms:",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Payment Terms",
                                    "info": "The number of days from the invoice date to complete the full payment.",
                                    "value": 1,
                                    "type": "PAYMENT_TERMS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Acceptance",
                    "type": "ACCEPTANCE",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "description": {
                                "name": "Description",
                                "value": "This section is designated for the authorized representatives from both parties involved in this agreement. By signing below, the signatories confirm their acceptance of the terms and conditions outlined in this document.",
                                "type": "TEXT",
                                "position": "DESCRIPTION",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "Acceptance",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Acceptance method",
                            "type": "ACC_METHOD",
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Subtitle",
                            "type": "BUYER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the buyer,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Buyer contacts required to sign",
                            "type": "BUYER_CONTACTS",
                            "placeHolder": "{{ACCEPTANCE_BUYER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        },
                        {
                            "name": "Subtitle",
                            "type": "SELLER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the seller,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Countersigners",
                            "type": "SELLER_CONTACTS",
                            "info": "People in your company required to sign in this quote.",
                            "placeHolder": "{{ACCEPTANCE_SELLER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish Kumar",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "USER",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                }
            ]
        },
        "id": 1,
        "message": "Quotation created successfully"
    }
}
```

| Field            | Type    | Description                                  |
|------------------|---------|----------------------------------------------|
| `code`           | Integer | `0` on success, non-zero on failure          |
| `data.id`        | Long    | The ID of the newly created quotation        |
| `data.quoteJson` | Object  | The fully resolved quote JSON with deal data |
| `data.message`   | String  | Success message                              |

### Error Response

```json
{
    "code": -1,
    "data": {
        "errorCode": "QUOTATION_CREATE_FAILED",
        "errorSummary": {
            "error": { "code": "QUOTATION_CREATE_FAILED", "key": "DATA_VALIDATION_FAILED" },
            "errors": [ ... ]
        }
    }
}
```

---

## 2. Get Quotation

Retrieves a single quotation by its ID.

### Request

```json
{
    "type": "get",
    "data": {
        "id": 1
    }
}
```

| Field    | Type   | Required | Description      |
|----------|--------|----------|------------------|
| `type`   | String | Yes      | Must be `"get"`  |
| `data.id`| Long   | Yes      | The quotation ID |

### Response

```json
{
    "code": 0,
    "data": {
        "quotation": {
            "id": 1,
            "name": "CRM Deal",
            "number": "QT-001",
            "status": "DRAFT",
            "amount": 2346.25,
            "currency": "USD",
            "dealId": 151,
            "owner": 1001,
            "createdBy": 1001,
            "createdTime": 1776256244499,
            "expirationTime": 1784118644499,
            "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/1",
            "quoteJson": {
                "name": "CRM Deal",
                "settings": {
                    "language": {
                        "name": "English (United States)",
                        "id": 9
                    },
                    "locale": "US",
                    "currency": {
                        "code": "USD",
                        "id": 150
                    },
                    "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/1"
                },
                "formAttributes": [
                    {
                        "name": "Header",
                        "type": "HEADER",
                        "isEnabled": true,
                        "canMakeHidden": true,
                        "details": [
                            {
                                "name": "Title",
                                "type": "TITLE",
                                "info": "The name given to the quote for easy identification and reference.",
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": "Quote",
                                        "type": "TEXT",
                                        "position": "HEADER",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Quote reference",
                                "type": "QUOTE_REF",
                                "info": "A unique reference number associated with the quote for tracking purposes.",
                                "label": {
                                    "name": "Label",
                                    "value": "Quote ref.",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": "20260415-151",
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Issue date",
                                "type": "DATE",
                                "info": "The date on which the quote is officially issued or published.",
                                "label": {
                                    "name": "Label",
                                    "value": "Issue date",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 1776256244499,
                                        "type": "DATE",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Expiration date",
                                "type": "DATE",
                                "info": "The date until which the quote remains valid for acceptance.",
                                "label": {
                                    "name": "Label",
                                    "value": "Expires",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 1784118644499,
                                        "type": "DATE",
                                        "position": "VALUE",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Currency",
                                "type": "CURRENCY",
                                "info": "The currency in which all quote values and totals are calculated.",
                                "label": {
                                    "name": "Label",
                                    "value": "Currency",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": "USD",
                                        "type": "CURRENCY",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            }
                        ],
                        "logo": [
                            {
                                "name": "Show Company Name",
                                "type": "COMPANY_NAME_TOGGLE",
                                "value": "TLC",
                                "info": "Toggle to show or hide the registered company name in the header.",
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Logo",
                                "type": "ATTACHMENT",
                                "value": "",
                                "isEnabled": true,
                                "canMakeDisable": false
                            }
                        ]
                    },
                    {
                        "name": "Parties",
                        "type": "PARTIES",
                        "isEnabled": true,
                        "canMakeHidden": false,
                        "sellerDetails": [
                            {
                                "name": "Title",
                                "type": "TITLE",
                                "info": "The label used to identify the seller in the quote.",
                                "values": [
                                    {
                                        "value": "Seller",
                                        "type": "TEXT",
                                        "position": "HEADER",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Company Details",
                                "type": "SELLER_COMPANY",
                                "info": "The company information of the seller, derived from the logged-in account.",
                                "values": [
                                    {
                                        "name": "Name",
                                        "value": "TLC",
                                        "type": "ORGANISATION",
                                        "position": "SUB_HEADER",
                                        "hasInlineEditOption": false
                                    },
                                    {
                                        "name": "Address",
                                        "value": "",
                                        "type": "ADDRESS",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "company": {
                                    "name": "TLC"
                                },
                                "isEnabled": true,
                                "showEditIcon": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Contact Details",
                                "type": "SELLER_CONTACT",
                                "info": "The seller's contact information, based on the logged-in user.",
                                "label": {
                                    "name": "Label",
                                    "value": "Contact",
                                    "type": "TEXT",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Name",
                                        "value": "Sathish Kumar",
                                        "type": "USER",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    },
                                    {
                                        "name": "Email",
                                        "value": "sathish@twozo.io",
                                        "type": "USER",
                                        "position": "SUB_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "contact": {
                                    "firstName": "Sathish",
                                    "lastName": "Kumar",
                                    "phone": "+919788971797",
                                    "email": "sathish@twozo.io"
                                },
                                "isEnabled": true,
                                "showEditIcon": true,
                                "canMakeDisable": false
                            }
                        ],
                        "buyerDetails": [
                            {
                                "name": "Title",
                                "type": "TITLE",
                                "info": "The label used to identify the buyer in the quote.",
                                "values": [
                                    {
                                        "value": "Buyer",
                                        "type": "TEXT",
                                        "position": "HEADER",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Company Details",
                                "type": "BUYER_COMPANY",
                                "info": "The company information associated with the buyer in this quote.",
                                "values": [
                                    {
                                        "name": "Name",
                                        "value": "Twozo",
                                        "type": "COMPANY",
                                        "position": "SUB_HEADER",
                                        "hasInlineEditOption": false
                                    },
                                    {
                                        "name": "Address",
                                        "value": "",
                                        "type": "ADDRESS",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "showEditIcon": true,
                                "showDeleteIcon": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Contact Details",
                                "type": "BUYER_CONTACTS",
                                "info": "The contact information associated with the buyer in this quote.",
                                "placeHolder": "{{PARTY_BUYER_CONTACTS}}",
                                "label": {
                                    "name": "Label",
                                    "value": "Contacts",
                                    "type": "TEXT",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Sathish",
                                        "email": "sathish@twozo.io",
                                        "icon": "",
                                        "type": "CONTACT",
                                        "position": "VALUE",
                                        "isEnabled": true,
                                        "isQuoteSigner": true
                                    },
                                    {
                                        "name": "Sanjai",
                                        "email": "sanjai@twozo.io",
                                        "icon": "",
                                        "type": "CONTACT",
                                        "position": "VALUE",
                                        "isEnabled": true,
                                        "isQuoteSigner": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false,
                                "hasArrayOfValues": true
                            }
                        ]
                    },
                    {
                        "name": "Summary",
                        "type": "SUMMARY",
                        "isEnabled": true,
                        "canMakeHidden": true,
                        "summaryDetails": [
                            {
                                "name": "Effective date",
                                "type": "EFFECTIVE_DATE",
                                "info": "The date from which the terms of the quote become applicable.",
                                "label": {
                                    "name": "Label",
                                    "value": "Effective date",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Effective",
                                        "value": 1,
                                        "type": "EFFECTIVE_DATE",
                                        "hasInlineEditOption": true
                                    },
                                    {
                                        "name": "Effective",
                                        "value": "On Agreement",
                                        "type": "EFFECTIVE_DATE",
                                        "position": "VALUE",
                                        "isHiddenOnForm": true,
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Term length",
                                "type": "TERM_LENGTH",
                                "info": "The duration for which the quote terms remain in effect, applicable to subscription-based products.",
                                "label": {
                                    "name": "Label",
                                    "value": "Term length",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "value": "",
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false,
                                        "isFormField": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Total Discount",
                                "type": "DISCOUNT",
                                "info": "The overall discount applied to the quote amount.",
                                "format": "Show as amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Total Discount",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "valueAsAmount": 275.0,
                                        "valueAsPercentage": "10.96%",
                                        "valueAsPercentageAndAmount": "275.0 (10.96%)",
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Total contract value",
                                "type": "TOTAL_VALUE",
                                "info": "The overall value of the contract calculated over its full duration.",
                                "label": {
                                    "name": "Label",
                                    "value": "Total contract value",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_HEADER",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 2346.25,
                                        "currency": "$",
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            }
                        ]
                    },
                    {
                        "name": "Line Items",
                        "type": "LINE_ITEMS",
                        "isEnabled": true,
                        "canMakeHidden": false,
                        "items": [
                            {
                                "name": "Line items",
                                "displayName": "Line items (2)",
                                "type": "PRODUCTS",
                                "info": "Add your products or services to your quote template.",
                                "placeHolder": "{{LINE_ITEMS}}",
                                "values": [
                                    {
                                        "name": "CRM",
                                        "quantity": 10,
                                        "discount": 10.0,
                                        "unitPrice": 200.0,
                                        "termTotal": 1800.0,
                                        "currency": "$",
                                        "type": "PRODUCT",
                                        "position": "TABLE_VALUE",
                                        "hasInlineEditOption": false
                                    },
                                    {
                                        "name": "Pharma CRM",
                                        "quantity": 5,
                                        "discount": 15.0,
                                        "unitPrice": 100.0,
                                        "termTotal": 425.0,
                                        "currency": "$",
                                        "type": "PRODUCT",
                                        "position": "TABLE_VALUE",
                                        "hasInlineEditOption": false
                                    },
                                    {
                                        "name": "Cycle",
                                        "quantity": 1,
                                        "discount": 0.0,
                                        "unitPrice": 10.0,
                                        "termTotal": 10.0,
                                        "currency": "$",
                                        "type": "PRODUCT",
                                        "position": "TABLE_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false,
                                "hasArrayOfValues": true
                            }
                        ],
                        "totals": {
                            "name": "Totals",
                            "info": "Turn on the totals section to show sub-total, discounts, taxes, amount due on the effective date, and total contract value on your quote.",
                            "isEnabled": false,
                            "canMakeDisable": true,
                            "attributes": [
                                {
                                    "name": "Subtotal",
                                    "type": "SUBTOTAL",
                                    "info": "The total value of all line items before any discounts or taxes are applied.",
                                    "format": "Simple subtotal amount",
                                    "label": {
                                        "name": "Label",
                                        "value": "Subtotal",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_KEY",
                                        "hasInlineEditOption": true
                                    },
                                    "values": [
                                        {
                                            "name": "Value",
                                            "value": 2235.0,
                                            "valueWithBreakdown": "$2,235.00/one time",
                                            "currency": "$",
                                            "type": "TEXT",
                                            "position": "BOLD_TEXT_VALUE",
                                            "hasInlineEditOption": false
                                        }
                                    ],
                                    "isEnabled": true,
                                    "canMakeDisable": false
                                },
                                {
                                    "name": "Discount included in the subtotal",
                                    "type": "DISCOUNT",
                                    "info": "The total discount applied across line items, already reflected within the sub-total.",
                                    "format": "Show as amount",
                                    "label": {
                                        "name": "Label",
                                        "value": "Discount included in the subtotal",
                                        "type": "TEXT",
                                        "position": "KEY",
                                        "hasInlineEditOption": true
                                    },
                                    "values": [
                                        {
                                            "name": "Value",
                                            "valueAsAmount": 275.0,
                                            "valueAsPercentage": "10.96%",
                                            "valueAsPercentageAndAmount": "275.0 (10.96%)",
                                            "currency": "$",
                                            "type": "TEXT",
                                            "position": "VALUE",
                                            "hasInlineEditOption": false
                                        }
                                    ],
                                    "isEnabled": true,
                                    "canMakeDisable": true
                                },
                                {
                                    "name": "Taxes",
                                    "type": "TAX",
                                    "info": "The total tax amount applied to the quote. If no tax rates are configured for line items, the tax amount will not be displayed.",
                                    "label": {
                                        "name": "Label",
                                        "value": "Taxes",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_KEY",
                                        "hasInlineEditOption": true
                                    },
                                    "values": [
                                        {
                                            "name": "Value",
                                            "value": 111.25,
                                            "currency": "$",
                                            "type": "TEXT",
                                            "position": "BOLD_TEXT_VALUE",
                                            "hasInlineEditOption": false
                                        }
                                    ],
                                    "isEnabled": true,
                                    "canMakeDisable": true
                                },
                                {
                                    "name": "Due on effective date",
                                    "type": "DUE",
                                    "info": "Indicates that the payment is expected on the effective date of the quote.",
                                    "description": {
                                        "name": "Description",
                                        "value": "The total amount due on the effective date, not including any payments starting at a later date.",
                                        "type": "TEXT",
                                        "position": "DESCRIPTION",
                                        "hasInlineEditOption": true
                                    },
                                    "label": {
                                        "name": "Label",
                                        "value": "Due on effective date",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_KEY",
                                        "hasInlineEditOption": true
                                    },
                                    "values": [
                                        {
                                            "name": "Value",
                                            "value": 2346.25,
                                            "currency": "$",
                                            "type": "TEXT",
                                            "position": "BOLD_TEXT_VALUE",
                                            "hasInlineEditOption": false
                                        }
                                    ],
                                    "isEnabled": true,
                                    "canMakeDisable": true
                                },
                                {
                                    "name": "Total contract value (TCV)",
                                    "type": "TOTAL_VALUE",
                                    "info": "The overall value of the contract calculated over its full duration.",
                                    "label": {
                                        "name": "Label",
                                        "value": "Total contract value",
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_KEY",
                                        "hasInlineEditOption": true
                                    },
                                    "values": [
                                        {
                                            "name": "Value",
                                            "value": 2346.25,
                                            "currency": "$",
                                            "type": "TEXT",
                                            "position": "BOLD_TEXT_VALUE",
                                            "hasInlineEditOption": false
                                        }
                                    ],
                                    "isEnabled": true,
                                    "canMakeDisable": true
                                }
                            ]
                        }
                    },
                    {
                        "name": "Payments",
                        "type": "PAYMENT",
                        "isEnabled": true,
                        "canMakeHidden": true,
                        "details": [
                            {
                                "name": "Title",
                                "type": "TITLE",
                                "label": {
                                    "name": "Label",
                                    "value": "Payments",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                },
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Billing",
                                "type": "BILLING",
                                "info": "Defines how charges are applied within the quote.",
                                "label": {
                                    "name": "Label",
                                    "value": "Customer must agree to the following payment terms:",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Payment Terms",
                                        "info": "The number of days from the invoice date to complete the full payment.",
                                        "value": 1,
                                        "type": "PAYMENT_TERMS",
                                        "position": "VALUE",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            }
                        ]
                    },
                    {
                        "name": "Acceptance",
                        "type": "ACCEPTANCE",
                        "isEnabled": true,
                        "canMakeHidden": true,
                        "details": [
                            {
                                "name": "Title",
                                "type": "TITLE",
                                "description": {
                                    "name": "Description",
                                    "value": "This section is designated for the authorized representatives from both parties involved in this agreement. By signing below, the signatories confirm their acceptance of the terms and conditions outlined in this document.",
                                    "type": "TEXT",
                                    "position": "DESCRIPTION",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "value": "Acceptance",
                                        "type": "TEXT",
                                        "position": "HEADER",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Acceptance method",
                                "type": "ACC_METHOD",
                                "values": [
                                    {
                                        "value": "",
                                        "type": "TEXT",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Subtitle",
                                "type": "BUYER_SUB_TITLE",
                                "values": [
                                    {
                                        "value": "On behalf of the buyer,",
                                        "type": "TEXT",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Buyer contacts required to sign",
                                "type": "BUYER_CONTACTS",
                                "placeHolder": "{{ACCEPTANCE_BUYER_CONTACTS}}",
                                "values": [
                                    {
                                        "name": "Sathish",
                                        "email": "sathish@twozo.io",
                                        "icon": "",
                                        "type": "CONTACT",
                                        "isEnabled": true,
                                        "position": "BOLD_TEXT_VALUE"
                                    },
                                    {
                                        "name": "Sanjai",
                                        "email": "sanjai@twozo.io",
                                        "icon": "",
                                        "type": "CONTACT",
                                        "isEnabled": true,
                                        "position": "BOLD_TEXT_VALUE"
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false,
                                "hasArrayOfValues": true
                            },
                            {
                                "name": "Subtitle",
                                "type": "SELLER_SUB_TITLE",
                                "values": [
                                    {
                                        "value": "On behalf of the seller,",
                                        "type": "TEXT",
                                        "hasInlineEditOption": true
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Countersigners",
                                "type": "SELLER_CONTACTS",
                                "info": "People in your company required to sign in this quote.",
                                "placeHolder": "{{ACCEPTANCE_SELLER_CONTACTS}}",
                                "values": [
                                    {
                                        "name": "Sathish Kumar",
                                        "email": "sathish@twozo.io",
                                        "icon": "",
                                        "type": "USER",
                                        "isEnabled": true,
                                        "position": "BOLD_TEXT_VALUE"
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false,
                                "hasArrayOfValues": true
                            }
                        ]
                    }
                ]
            }
        }
    }
}
```

---

## 3. Draft Quotation

Saves a quotation in `DRAFT` status. The full `quoteJson` is provided by the client (after user edits) and persisted as-is.

### Request

```json
{
    "type": "draft",
    "data": {
        "id": 1,
        "quoteJson": {
            "name": "CRM Deal Quotation",
            "settings": {
                "language": {
                    "name": "English (United States)",
                    "id": 9
                },
                "locale": "US",
                "currency": {
                    "code": "USD",
                    "id": 150
                },
                "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/651"
            },
            "formAttributes": [
                {
                    "name": "Header",
                    "type": "HEADER",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The name given to the quote for easy identification and reference.",
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "Quotation",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Quote reference",
                            "type": "QUOTE_REF",
                            "info": "A unique reference number associated with the quote for tracking purposes.",
                            "label": {
                                "name": "Label",
                                "value": "Quote Reference",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "20260407-54",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Issue date",
                            "type": "DATE",
                            "info": "The date on which the quote is officially issued or published.",
                            "label": {
                                "name": "Label",
                                "value": "Issued date",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1775560292732,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Expiration date",
                            "type": "DATE",
                            "info": "The date until which the quote remains valid for acceptance.",
                            "label": {
                                "name": "Label",
                                "value": "Expiry Date",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1793422692732,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Currency",
                            "type": "CURRENCY",
                            "info": "The currency in which all quote values and totals are calculated.",
                            "label": {
                                "name": "Label",
                                "value": "Currency",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "INR",
                                    "type": "CURRENCY",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ],
                    "logo": [
                        {
                            "name": "Show Company Name",
                            "type": "COMPANY_NAME_TOGGLE",
                            "value": "TLC",
                            "info": "Toggle to show or hide the registered company name in the header.",
                            "isEnabled": false,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Logo",
                            "type": "ATTACHMENT",
                            "value": "",
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Parties",
                    "type": "PARTIES",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "sellerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the seller in the quote.",
                            "values": [
                                {
                                    "value": "Seller Details",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "SELLER_COMPANY",
                            "info": "The company information of the seller, derived from the logged-in account.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "TLC",
                                    "type": "ORGANISATION",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "SELLER_CONTACT",
                            "info": "The seller's contact information, based on the logged-in user.",
                            "label": {
                                "name": "Label",
                                "value": "Contact Details",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Sathish Kumar",
                                    "type": "USER",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Email",
                                    "value": "sathish@twozo.io",
                                    "type": "USER",
                                    "position": "SUB_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        }
                    ],
                    "buyerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the buyer in the quote.",
                            "values": [
                                {
                                    "value": "Buyer Details",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "BUYER_COMPANY",
                            "info": "The company information associated with the buyer in this quote.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Twozo",
                                    "type": "COMPANY",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "Twozo Tech, SB Road, Tirunelveli, Tamil Nadu, 627007, india",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "showDeleteIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "BUYER_CONTACTS",
                            "info": "The contact information associated with the buyer in this quote.",
                            "placeHolder": "{{PARTY_BUYER_CONTACTS}}",
                            "label": {
                                "name": "Label",
                                "value": "Contacts",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": true
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                },
                {
                    "name": "Summary",
                    "type": "SUMMARY",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "summaryDetails": [
                        {
                            "name": "Effective date",
                            "type": "EFFECTIVE_DATE",
                            "info": "The date from which the terms of the quote become applicable.",
                            "label": {
                                "name": "Label",
                                "value": "Effective date",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Effective",
                                    "value": "1",
                                    "type": "EFFECTIVE_DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Term length",
                            "type": "TERM_LENGTH",
                            "info": "The duration for which the quote terms remain in effect, applicable to subscription-based products.",
                            "label": {
                                "name": "Label",
                                "value": "Term length",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false,
                                    "isFormField": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total Discount",
                            "type": "DISCOUNT",
                            "info": "The overall discount applied to the quote amount.",
                            "format": "Show as amount",
                            "label": {
                                "name": "Label",
                                "value": "Total Discount",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 19.0,
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total contract value",
                            "type": "TOTAL_VALUE",
                            "info": "The overall value of the contract calculated over its full duration.",
                            "label": {
                                "name": "Label",
                                "value": "Total contract value",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 388.62,
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ]
                },
                {
                    "name": "Line Items",
                    "type": "LINE_ITEMS",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "items": [
                        {
                            "name": "Line items",
                            "displayName": "Line items (2)",
                            "type": "PRODUCTS",
                            "info": "Add your products or services to your quote template.",
                            "placeHolder": "{{LINE_ITEMS}}",
                            "values": [
                                {
                                    "name": "CRM",
                                    "quantity": 3,
                                    "discount": 5.0,
                                    "unitPrice": 100.0,
                                    "termTotal": 285.0,
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Pharma CRM",
                                    "quantity": 2,
                                    "discount": 4.0,
                                    "unitPrice": 50.0,
                                    "termTotal": 96.0,
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ],
                    "totals": {
                        "name": "Totals",
                        "info": "Turn on the totals section to show sub-total, discounts, taxes, amount due on the effective date, and total contract value on your quote.",
                        "isEnabled": false,
                        "canMakeDisable": true,
                        "attributes": [
                            {
                                "name": "Subtotal",
                                "type": "SUBTOTAL",
                                "info": "The total value of all line items before any discounts or taxes are applied.",
                                "format": "Simple subtotal amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Subtotal",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 381.0,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Discount included in the subtotal",
                                "type": "DISCOUNT",
                                "info": "The total discount applied across line items, already reflected within the sub-total.",
                                "format": "Show as amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Discount included in the subtotal",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 19.0,
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Taxes",
                                "type": "TAX",
                                "info": "The total tax amount applied to the quote. If no tax rates are configured for line items, the tax amount will not be displayed.",
                                "label": {
                                    "name": "Label",
                                    "value": "Taxes",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 7.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Due on effective date",
                                "type": "DUE",
                                "info": "Indicates that the payment is expected on the effective date of the quote.",
                                "description": {
                                    "name": "Description",
                                    "value": "The total amount due on the effective date, not including any payments starting at a later date.",
                                    "type": "TEXT",
                                    "position": "DESCRIPTION",
                                    "hasInlineEditOption": true
                                },
                                "label": {
                                    "name": "Label",
                                    "value": "Due on effective date",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 388.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Total contract value (TCV)",
                                "type": "TOTAL_VALUE",
                                "info": "The overall value of the contract calculated over its full duration.",
                                "label": {
                                    "name": "Label",
                                    "value": "Total contract value",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 388.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            }
                        ]
                    }
                },
                {
                    "name": "Payments",
                    "type": "PAYMENT",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "label": {
                                "name": "Label",
                                "value": "Payments",
                                "type": "TEXT",
                                "position": "HEADER",
                                "hasInlineEditOption": true
                            },
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Billing",
                            "type": "BILLING",
                            "info": "Defines how charges are applied within the quote.",
                            "label": {
                                "name": "Label",
                                "value": "Customer must agree to the following payment terms:",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Payment Terms",
                                    "info": "The number of days from the invoice date to complete the full payment.",
                                    "value": "1",
                                    "type": "PAYMENT_TERMS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Acceptance",
                    "type": "ACCEPTANCE",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "description": {
                                "name": "Description",
                                "value": "This section is designated for the authorized representatives from both parties involved in this agreement. By signing below, the signatories confirm their acceptance of the terms and conditions outlined in this document.",
                                "type": "TEXT",
                                "position": "DESCRIPTION",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "Acceptance",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Acceptance method",
                            "type": "ACC_METHOD",
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Subtitle",
                            "type": "BUYER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the buyer,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Buyer contacts required to sign",
                            "type": "BUYER_CONTACTS",
                            "placeHolder": "{{ACCEPTANCE_BUYER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        },
                        {
                            "name": "Subtitle",
                            "type": "SELLER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the seller,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Countersigners",
                            "type": "SELLER_CONTACTS",
                            "info": "People in your company required to sign in this quote.",
                            "placeHolder": "{{ACCEPTANCE_SELLER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish Kumar",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "USER",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                }
            ]
        }
    }
}
```

| Field           | Type   | Required | Description                             |
|-----------------|--------|----------|-----------------------------------------|
| `type`          | String | Yes      | Must be `"draft"`                       |
| `data.id`       | Long   | Yes      | The existing quotation ID               |
| `data.quoteJson`| Object | Yes      | The complete quote JSON with user edits |

### Response

```json
{
    "code": 0,
    "data": {
        "message": "Quotation drafted successfully"
    }
}
```

---

## 4. Update Quotation

Updates an existing quotation with the provided quote JSON.

### Request

```json
{
    "type": "update",
    "data": {
        "id": 1,
        "quoteJson": {
            "name": "CRM Deal Quotation",
            "settings": {
                "language": {
                    "name": "English (United States)",
                    "id": 9
                },
                "locale": "US",
                "currency": {
                    "code": "USD",
                    "id": 150
                },
                "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/651"
            },
            "formAttributes": [
                {
                    "name": "Header",
                    "type": "HEADER",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The name given to the quote for easy identification and reference.",
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "Quotation",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Quote reference",
                            "type": "QUOTE_REF",
                            "info": "A unique reference number associated with the quote for tracking purposes.",
                            "label": {
                                "name": "Label",
                                "value": "Quote Reference",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "20260407-54",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Issue date",
                            "type": "DATE",
                            "info": "The date on which the quote is officially issued or published.",
                            "label": {
                                "name": "Label",
                                "value": "Issued date",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1775560292732,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Expiration date",
                            "type": "DATE",
                            "info": "The date until which the quote remains valid for acceptance.",
                            "label": {
                                "name": "Label",
                                "value": "Expiry Date",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 1793422692732,
                                    "type": "DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Currency",
                            "type": "CURRENCY",
                            "info": "The currency in which all quote values and totals are calculated.",
                            "label": {
                                "name": "Label",
                                "value": "Currency",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": "INR",
                                    "type": "CURRENCY",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ],
                    "logo": [
                        {
                            "name": "Show Company Name",
                            "type": "COMPANY_NAME_TOGGLE",
                            "value": "TLC",
                            "info": "Toggle to show or hide the registered company name in the header.",
                            "isEnabled": false,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Logo",
                            "type": "ATTACHMENT",
                            "value": "",
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Parties",
                    "type": "PARTIES",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "sellerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the seller in the quote.",
                            "values": [
                                {
                                    "value": "Seller Details",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "SELLER_COMPANY",
                            "info": "The company information of the seller, derived from the logged-in account.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "TLC",
                                    "type": "ORGANISATION",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "SELLER_CONTACT",
                            "info": "The seller's contact information, based on the logged-in user.",
                            "label": {
                                "name": "Label",
                                "value": "Contact Details",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Sathish Kumar",
                                    "type": "USER",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Email",
                                    "value": "sathish@twozo.io",
                                    "type": "USER",
                                    "position": "SUB_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "canMakeDisable": false
                        }
                    ],
                    "buyerDetails": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "info": "The label used to identify the buyer in the quote.",
                            "values": [
                                {
                                    "value": "Buyer Details",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Company Details",
                            "type": "BUYER_COMPANY",
                            "info": "The company information associated with the buyer in this quote.",
                            "values": [
                                {
                                    "name": "Name",
                                    "value": "Twozo",
                                    "type": "COMPANY",
                                    "position": "SUB_HEADER",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Address",
                                    "value": "Twozo Tech, SB Road, Tirunelveli, Tamil Nadu, 627007, india",
                                    "type": "ADDRESS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "showEditIcon": true,
                            "showDeleteIcon": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Contact Details",
                            "type": "BUYER_CONTACTS",
                            "info": "The contact information associated with the buyer in this quote.",
                            "placeHolder": "{{PARTY_BUYER_CONTACTS}}",
                            "label": {
                                "name": "Label",
                                "value": "Contacts",
                                "type": "TEXT",
                                "position": "SUB_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": true
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "position": "VALUE",
                                    "isEnabled": true,
                                    "isQuoteSigner": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                },
                {
                    "name": "Summary",
                    "type": "SUMMARY",
                    "isEnabled": true,
                    "canMakeHidden": true,
                    "summaryDetails": [
                        {
                            "name": "Effective date",
                            "type": "EFFECTIVE_DATE",
                            "info": "The date from which the terms of the quote become applicable.",
                            "label": {
                                "name": "Label",
                                "value": "Effective date",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Effective",
                                    "value": "1",
                                    "type": "EFFECTIVE_DATE",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Term length",
                            "type": "TERM_LENGTH",
                            "info": "The duration for which the quote terms remain in effect, applicable to subscription-based products.",
                            "label": {
                                "name": "Label",
                                "value": "Term length",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false,
                                    "isFormField": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total Discount",
                            "type": "DISCOUNT",
                            "info": "The overall discount applied to the quote amount.",
                            "format": "Show as amount",
                            "label": {
                                "name": "Label",
                                "value": "Total Discount Value",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 19.0,
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        },
                        {
                            "name": "Total contract value",
                            "type": "TOTAL_VALUE",
                            "info": "The overall value of the contract calculated over its full duration.",
                            "label": {
                                "name": "Label",
                                "value": "Total contract value",
                                "type": "TEXT",
                                "position": "BOLD_TEXT_HEADER",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Value",
                                    "value": 388.62,
                                    "type": "TEXT",
                                    "position": "VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": true
                        }
                    ]
                },
                {
                    "name": "Line Items",
                    "type": "LINE_ITEMS",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "items": [
                        {
                            "name": "Line items",
                            "displayName": "Line items (2)",
                            "type": "PRODUCTS",
                            "info": "Add your products or services to your quote template.",
                            "placeHolder": "{{LINE_ITEMS}}",
                            "values": [
                                {
                                    "name": "CRM",
                                    "quantity": 3,
                                    "discount": 5.0,
                                    "unitPrice": 100.0,
                                    "termTotal": 285.0,
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                },
                                {
                                    "name": "Pharma CRM",
                                    "quantity": 2,
                                    "discount": 4.0,
                                    "unitPrice": 50.0,
                                    "termTotal": 96.0,
                                    "type": "PRODUCT",
                                    "position": "TABLE_VALUE",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ],
                    "totals": {
                        "name": "Totals",
                        "info": "Turn on the totals section to show sub-total, discounts, taxes, amount due on the effective date, and total contract value on your quote.",
                        "isEnabled": false,
                        "canMakeDisable": true,
                        "attributes": [
                            {
                                "name": "Subtotal",
                                "type": "SUBTOTAL",
                                "info": "The total value of all line items before any discounts or taxes are applied.",
                                "format": "Simple subtotal amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Subtotal",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 381.0,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": false
                            },
                            {
                                "name": "Discount included in the subtotal",
                                "type": "DISCOUNT",
                                "info": "The total discount applied across line items, already reflected within the sub-total.",
                                "format": "Show as amount",
                                "label": {
                                    "name": "Label",
                                    "value": "Discount included in the subtotal",
                                    "type": "TEXT",
                                    "position": "KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 19.0,
                                        "type": "TEXT",
                                        "position": "VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Taxes",
                                "type": "TAX",
                                "info": "The total tax amount applied to the quote. If no tax rates are configured for line items, the tax amount will not be displayed.",
                                "label": {
                                    "name": "Label",
                                    "value": "Taxes",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 7.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Due on effective date",
                                "type": "DUE",
                                "info": "Indicates that the payment is expected on the effective date of the quote.",
                                "description": {
                                    "name": "Description",
                                    "value": "The total amount due on the effective date, not including any payments starting at a later date.",
                                    "type": "TEXT",
                                    "position": "DESCRIPTION",
                                    "hasInlineEditOption": true
                                },
                                "label": {
                                    "name": "Label",
                                    "value": "Due on effective date",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 388.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            },
                            {
                                "name": "Total contract value (TCV)",
                                "type": "TOTAL_VALUE",
                                "info": "The overall value of the contract calculated over its full duration.",
                                "label": {
                                    "name": "Label",
                                    "value": "Total contract value",
                                    "type": "TEXT",
                                    "position": "BOLD_TEXT_KEY",
                                    "hasInlineEditOption": true
                                },
                                "values": [
                                    {
                                        "name": "Value",
                                        "value": 388.62,
                                        "type": "TEXT",
                                        "position": "BOLD_TEXT_VALUE",
                                        "hasInlineEditOption": false
                                    }
                                ],
                                "isEnabled": true,
                                "canMakeDisable": true
                            }
                        ]
                    }
                },
                {
                    "name": "Payments",
                    "type": "PAYMENT",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "label": {
                                "name": "Label",
                                "value": "Payments",
                                "type": "TEXT",
                                "position": "HEADER",
                                "hasInlineEditOption": true
                            },
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Billing",
                            "type": "BILLING",
                            "info": "Defines how charges are applied within the quote.",
                            "label": {
                                "name": "Label",
                                "value": "Customer must agree to the following payment terms:",
                                "type": "TEXT",
                                "position": "KEY",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "name": "Payment Terms",
                                    "info": "The number of days from the invoice date to complete the full payment.",
                                    "value": "1",
                                    "type": "PAYMENT_TERMS",
                                    "position": "VALUE",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        }
                    ]
                },
                {
                    "name": "Acceptance",
                    "type": "ACCEPTANCE",
                    "isEnabled": true,
                    "canMakeHidden": false,
                    "details": [
                        {
                            "name": "Title",
                            "type": "TITLE",
                            "description": {
                                "name": "Description",
                                "value": "This section is designated for the authorized representatives from both parties involved in this agreement. By signing below, the signatories confirm their acceptance of the terms and conditions outlined in this document.",
                                "type": "TEXT",
                                "position": "DESCRIPTION",
                                "hasInlineEditOption": true
                            },
                            "values": [
                                {
                                    "value": "Acceptance",
                                    "type": "TEXT",
                                    "position": "HEADER",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Acceptance method",
                            "type": "ACC_METHOD",
                            "values": [
                                {
                                    "value": "",
                                    "type": "TEXT",
                                    "hasInlineEditOption": false
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Subtitle",
                            "type": "BUYER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the buyer,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Buyer contacts required to sign",
                            "type": "BUYER_CONTACTS",
                            "placeHolder": "{{ACCEPTANCE_BUYER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                },
                                {
                                    "name": "Sanjai",
                                    "email": "sanjai@twozo.io",
                                    "icon": "",
                                    "type": "CONTACT",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        },
                        {
                            "name": "Subtitle",
                            "type": "SELLER_SUB_TITLE",
                            "values": [
                                {
                                    "value": "On behalf of the seller,",
                                    "type": "TEXT",
                                    "hasInlineEditOption": true
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false
                        },
                        {
                            "name": "Countersigners",
                            "type": "SELLER_CONTACTS",
                            "info": "People in your company required to sign in this quote.",
                            "placeHolder": "{{ACCEPTANCE_SELLER_CONTACTS}}",
                            "values": [
                                {
                                    "name": "Sathish Kumar",
                                    "email": "sathish@twozo.io",
                                    "icon": "",
                                    "type": "USER",
                                    "isEnabled": true,
                                    "position": "BOLD_TEXT_VALUE"
                                }
                            ],
                            "isEnabled": true,
                            "canMakeDisable": false,
                            "hasArrayOfValues": true
                        }
                    ]
                }
            ]
        }
    }
}
```

| Field           | Type   | Required | Description                          |
|-----------------|--------|----------|--------------------------------------|
| `type`          | String | Yes      | Must be `"update"`                   |
| `data.id`       | Long   | Yes      | The existing quotation ID            |
| `data.quoteJson`| Object | Yes      | The complete quote JSON with updates |

### Response

```json
{
    "code": 0,
    "data": {
        "message": "Quotation updated successfully"
    }
}
```

---

## 5. Delete Quotation

Deletes a quotation by its ID.

### Request

```json
{
    "type": "delete",
    "data": {
        "id": 1
    }
}
```

| Field    | Type   | Required | Description       |
|----------|--------|----------|-------------------|
| `type`   | String | Yes      | Must be `"delete"`|
| `data.id`| Long   | Yes      | The quotation ID  |

### Response

```json
{
    "code": 0,
    "data": {
        "message": "Quotation deleted successfully"
    }
}
```

---

## 6. Clone Quotation

Creates a copy of an existing quotation with a new ID, new UUID, and `DRAFT` status.

### Request

```json
{
    "type": "clone",
    "data": {
        "id": 1
    }
}
```

| Field    | Type   | Required | Description                      |
|----------|--------|----------|----------------------------------|
| `type`   | String | Yes      | Must be `"clone"`                |
| `data.id`| Long   | Yes      | The ID of the quotation to clone |

### Response

```json
{
    "code": 0,
    "data": {
        "id": 2,
        "name": "CRM Deal",
        "message": "Quotation cloned successfully"
    }
}
```

---

## 7. Bulk Create Quotations

Creates multiple quotations in a single transaction.

### Request

```json
{
    "type": "bCreate",
    "data": {
        "quotations": [
            { "dealId": 151 },
            { "dealId": 152 }
        ]
    }
}
```

| Field             | Type   | Required | Description                     |
|-------------------|--------|----------|---------------------------------|
| `type`            | String | Yes      | Must be `"bCreate"`             |
| `data.quotations` | Array  | Yes      | Array of quotation data objects |

### Response

```json
{
    "code": 0,
    "data": {
        "createdRecords": 2,
        "quotations": [1, 2],
        "message": "Quotation created successfully"
    }
}
```

---

## 8. Bulk Update Quotations

Updates multiple quotations in a single transaction.

### Request

```json
{
    "type": "bUpdate",
    "data": {
        "quotations": [
            {
                "id": 1,
                "quoteJson": {
                    "name": "CRM Deal Quotation Updated",
                    "settings": {
                        "language": { "name": "English (United States)", "id": 9 },
                        "locale": "US",
                        "currency": { "code": "USD", "id": 150 },
                        "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/1"
                    },
                    "formAttributes": []
                }
            },
            {
                "id": 2,
                "quoteJson": {
                    "name": "Another Quotation Updated",
                    "settings": {
                        "language": { "name": "English (United States)", "id": 9 },
                        "locale": "US",
                        "currency": { "code": "INR", "id": 101 },
                        "quoteUrl": "http://margert-dozier-carlton.ngrok-free.dev/api/quotation/download/2"
                    },
                    "formAttributes": []
                }
            }
        ]
    }
}
```

| Field             | Type   | Required | Description                     |
|-------------------|--------|----------|---------------------------------|
| `type`            | String | Yes      | Must be `"bUpdate"`             |
| `data.quotations` | Array  | Yes      | Array of quotation data objects |

### Response

```json
{
    "code": 0,
    "data": {
        "updatedRecords": 2,
        "quotations": [1, 2],
        "message": "Quotation updated successfully"
    }
}
```


---

## 9. List Quotations

Retrieves a paginated list of quotations with optional field selection, sorting, filtering, and structure fetching.

### Base URL

```
/api/quote/list
```

### Request — Fetch Data with Default Fields

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": false,
    "fetchData": true,
    "limit": {
        "start": 0,
        "count": 25
    }
}
```

### Request — Fetch Structure and Data

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": true,
    "fetchData": true,
    "limit": {
        "start": 0,
        "count": 25
    }
}
```

### Request — Fetch Data with Custom Fields, Sorting, and Filtering

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": false,
    "fetchData": true,
    "fields": [
        { "field": "name", "isEnabled": true },
        { "field": "amount", "isEnabled": true },
        { "field": "status", "isEnabled": true },
        { "field": "deal", "isEnabled": true },
        { "field": "number", "isEnabled": true }
    ],
    "sort": [
        { "field": "createdAt", "type": "desc" }
    ],
    "limit": {
        "start": 0,
        "count": 25
    }
}
```

### Request — Fetch Data with Filter ID

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": false,
    "fetchData": true,
    "filterId": 101,
    "limit": {
        "start": 0,
        "count": 25
    }
}
```

### Request — Fetch Structure Only (No Data)

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": true,
    "fetchData": false
}
```

### Request — Fetch Data with Empty Fields (Mandatory Fields Only)

```json
{
    "entityId": 151,
    "source": "deal",
    "fetchStruct": false,
    "fetchData": true,
    "fields": [],
    "limit": {
        "start": 0,
        "count": 25
    }
}
```

### Request Key Descriptions

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `entityId` | The ID of the parent entity (e.g., deal ID) to scope the quotation list | NUMBER | Always | |
| `source` | The source entity type that the quotations belong to (e.g., "deal") | STRING | Always | |
| `fetchStruct` | Whether to include the column structure metadata in the response | BOOLEAN | Optional | Defaults to false if not provided |
| `fetchData` | Whether to include the data rows in the response | BOOLEAN | Optional | Defaults to true if not provided |
| `fields` | Array of field objects specifying which columns to select and display | ARRAY | Optional | If absent, default fields are used; if empty array, only mandatory fields are selected |
| `fields[].field` | The field name to select (e.g., "name", "amount", "status") | STRING | Always | |
| `fields[].isEnabled` | Whether the field is enabled for display | BOOLEAN | Optional | Defaults to true if not provided |
| `fields[].subFields` | Array of sub-field objects to select within a composite field | ARRAY | Optional | Present when the field has sub-fields to configure |
| `fields[].subFields[].field` | The sub-field name to select | STRING | Always | |
| `sort` | Array of sort criteria objects | ARRAY | Optional | If absent or empty, default sort order is applied and any saved sort is removed |
| `sort[].field` | The field name to sort by (e.g., "createdAt") | STRING | Always | |
| `sort[].type` | Sort direction: "asc" for ascending, "desc" for descending | STRING | Always | |
| `limit` | Pagination configuration object | OBJECT | Optional | If absent, all matching records are returned |
| `limit.start` | Zero-based offset for pagination | NUMBER | Always | |
| `limit.count` | Maximum number of records to return per page | NUMBER | Always | |
| `filterId` | ID of a saved filter to apply | NUMBER | Optional | Present when applying a previously saved filter |
| `criteria` | Array of inline filter criteria objects | ARRAY | Optional | Present when applying ad-hoc filter conditions |
| `criteria[].field` | The field name to filter on | STRING | Always | |
| `criteria[].comparator` | The comparison operator (e.g., "is", "contains", "gt", "lt") | STRING | Always | |
| `criteria[].value` | The value to compare against | ANY | Always | |
| `additionalCriteria` | Array of additional filter criteria applied alongside other filters | ARRAY | Optional | Present when extra filtering is needed on top of existing filters |
| `filterCriteria` | Custom filter criteria object for advanced filtering | OBJECT | Optional | Present when custom criteria are needed |
| `ownerId` | Filter records by a specific owner user ID | NUMBER | Optional | Present when filtering by record owner |

### Response — Data with Structure

```json
{
    "code": 0,
    "data": {
        "table": {
            "structure": {
                "columns": [
                    {
                        "name": "name",
                        "displayName": "Name",
                        "search": true,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "TEXT",
                        "isMandatoryField": true,
                        "linkType": null,
                        "isClickable": true
                    },
                    {
                        "name": "amount",
                        "displayName": "Amount",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "DOUBLE",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "status",
                        "displayName": "Status",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "TEXT",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "currency",
                        "displayName": "Currency",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "TEXT",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "owner",
                        "displayName": "Owner",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "USER",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "createdBy",
                        "displayName": "Created by",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "USER",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "createdAt",
                        "displayName": "Created at",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "sort": "desc",
                        "fieldType": "DATE_TIME",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "expiryTime",
                        "displayName": "Expiration date",
                        "search": false,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "DATE_TIME",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "deal",
                        "displayName": "Deal",
                        "search": false,
                        "hidden": false,
                        "isSortable": false,
                        "fieldType": "LOOKUP",
                        "isMandatoryField": false,
                        "linkType": null
                    },
                    {
                        "name": "number",
                        "displayName": "Quote number",
                        "search": true,
                        "hidden": false,
                        "isSortable": true,
                        "fieldType": "TEXT",
                        "isMandatoryField": false,
                        "linkType": null
                    }
                ],
                "primary": "name"
            },
            "totalRecords": 15,
            "rows": [
                {
                    "id": 1,
                    "name": "CRM Deal",
                    "amount": 2346.25,
                    "status": "DRAFT",
                    "currency": "USD",
                    "owner": {
                        "id": 1001,
                        "name": "Sathish Kumar",
                        "thumbnail": {
                            "name": "profile",
                            "size": 12345,
                            "extension": "png",
                            "url": "http://example.com/profile.png"
                        }
                    },
                    "createdBy": {
                        "id": 1001,
                        "name": "Sathish Kumar"
                    },
                    "createdAt": 1776256244499,
                    "expiryTime": 1784118644499,
                    "deal": 151,
                    "number": "QT-001"
                },
                {
                    "id": 2,
                    "name": "Pharma Deal Quotation",
                    "amount": 388.62,
                    "status": "DRAFT",
                    "currency": "INR",
                    "owner": {
                        "id": 1001,
                        "name": "Sathish Kumar"
                    },
                    "createdBy": {
                        "id": 1001,
                        "name": "Sathish Kumar"
                    },
                    "createdAt": 1775560292732,
                    "expiryTime": 1793422692732,
                    "deal": 152,
                    "number": "QT-002"
                }
            ]
        }
    }
}
```

### Response — Data Only (No Structure)

```json
{
    "code": 0,
    "data": {
        "table": {
            "totalRecords": 15,
            "rows": [
                {
                    "id": 1,
                    "name": "CRM Deal",
                    "amount": 2346.25,
                    "status": "DRAFT",
                    "currency": "USD",
                    "owner": {
                        "id": 1001,
                        "name": "Sathish Kumar"
                    },
                    "createdBy": {
                        "id": 1001,
                        "name": "Sathish Kumar"
                    },
                    "createdAt": 1776256244499,
                    "expiryTime": 1784118644499,
                    "deal": 151,
                    "number": "QT-001"
                }
            ]
        }
    }
}
```

### Response Key Descriptions

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `table` | The top-level container for the list response | OBJECT | Always | |
| `table.structure` | Column structure metadata for the list view | OBJECT | Optional | Present only when `fetchStruct` is true in the request |
| `table.structure.columns` | Array of column definition objects | ARRAY | Always | |
| `table.structure.primary` | The primary field name used as the main display column | STRING | Always | |
| `table.totalRecords` | Total number of records matching the query (before pagination) | NUMBER | Optional | Present when data is fetched |
| `table.rows` | Array of data row objects | ARRAY | Optional | Present when data is fetched |

### Structure Column Key Descriptions

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The internal field name used in requests (e.g., "name", "amount") | STRING | Always | |
| `displayName` | The user-facing display name for the column | STRING | Always | |
| `search` | Whether the column supports text search | BOOLEAN | Always | |
| `hidden` | Whether the column is hidden by default | BOOLEAN | Always | |
| `isSortable` | Whether the column supports sorting | BOOLEAN | Always | |
| `sort` | The current sort direction applied to this column ("asc" or "desc") | STRING | Optional | Present only when a sort is active on this column |
| `fieldType` | The data type of the column (e.g., "TEXT", "DOUBLE", "DATE_TIME", "USER", "LOOKUP") | STRING | Always | |
| `isMandatoryField` | Whether the column is mandatory and always included | BOOLEAN | Always | |
| `linkType` | The type of link associated with the column (e.g., for navigation) | STRING/NULL | Always | |
| `isClickable` | Whether the column value is clickable for navigation | BOOLEAN | Optional | Present when the column supports click interaction |
| `id` | The field ID from the field configuration | NUMBER | Optional | Present when the field has a persistent configuration |
| `config` | The field's preset configuration | OBJECT | Optional | Present when the field has a persistent configuration |
| `subTypes` | Array of sub-type definitions for composite fields | ARRAY | Optional | Present when the field has sub-fields |

### Data Row Key Descriptions

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `id` | The unique quotation record ID | NUMBER | Always | |
| `name` | The quotation name | STRING | Optional | Present when the `name` field is selected |
| `amount` | The quotation monetary amount | NUMBER | Optional | Present when the `amount` field is selected |
| `status` | The quotation status (DRAFT, PUBLISHED, ACCEPTED, REJECTED, EXPIRED) | STRING | Optional | Present when the `status` field is selected |
| `currency` | The currency code (e.g., "USD", "INR") | STRING | Optional | Present when the `currency` field is selected |
| `owner` | The quotation owner user object | OBJECT | Optional | Present when the `owner` field is selected |
| `owner.id` | The CRM user ID of the owner | NUMBER | Always | |
| `owner.name` | The display name of the owner | STRING | Always | |
| `owner.thumbnail` | The owner's profile picture details | OBJECT | Optional | Present when the owner has a profile picture |
| `owner.thumbnail.name` | The image type name | STRING | Always | |
| `owner.thumbnail.size` | The image file size in bytes | NUMBER | Always | |
| `owner.thumbnail.extension` | The image file extension (e.g., "png", "jpg") | STRING | Always | |
| `owner.thumbnail.url` | The URL to the profile picture | STRING | Always | |
| `createdBy` | The user who created the quotation | OBJECT | Optional | Present when the `createdBy` field is selected |
| `createdBy.id` | The CRM user ID of the creator | NUMBER | Always | |
| `createdBy.name` | The display name of the creator | STRING | Always | |
| `createdBy.thumbnail` | The creator's profile picture details | OBJECT | Optional | Present when the creator has a profile picture |
| `createdAt` | The creation timestamp in milliseconds | NUMBER | Optional | Present when the `createdAt` field is selected |
| `expiryTime` | The expiration timestamp in milliseconds | NUMBER | Optional | Present when the `expiryTime` field is selected |
| `deal` | The associated deal ID | NUMBER | Optional | Present when the `deal` field is selected |
| `number` | The quotation number (e.g., "QT-001") | STRING | Optional | Present when the `number` field is selected |
| `partialData` | Preview data for fields not fully loaded in the list | OBJECT | Optional | Present when preview data is available for the record |

### Available Quotation List Fields

| Field Name | Description | Field Type | Sortable | Searchable |
|------------|-------------|------------|----------|------------|
| `name` | Quotation name | TEXT | Yes | Yes |
| `amount` | Quotation monetary amount | DOUBLE | Yes | No |
| `status` | Quotation status | TEXT | Yes | No |
| `currency` | Currency code | TEXT | Yes | No |
| `owner` | Record owner | USER | Yes | No |
| `createdBy` | Record creator | USER | Yes | No |
| `createdAt` | Creation timestamp | DATE_TIME | Yes | No |
| `expiryTime` | Expiration timestamp | DATE_TIME | Yes | No |
| `deal` | Associated deal | LOOKUP | No | No |
| `number` | Quotation number | TEXT | Yes | Yes |

---

## Quote JSON Key Descriptions

### Top-Level Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The name of the quotation, derived from the deal name | STRING | Always | |
| `settings` | Contains language, locale, currency, and quote URL configurations | OBJECT | Always | |
| `formAttributes` | Array of form sections that make up the quote structure | ARRAY | Always | |

### Settings Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `language` | Language configuration for the quote | OBJECT | Always | |
| `language.name` | Display name of the language (e.g., "English (United States)") | STRING | Always | |
| `language.id` | Unique identifier of the language | NUMBER | Always | |
| `locale` | Locale code for regional formatting (e.g., "US") | STRING | Always | |
| `currency` | Currency configuration for all monetary values in the quote | OBJECT | Always | |
| `currency.code` | ISO currency code (e.g., "USD", "INR") | STRING | Always | |
| `currency.id` | Unique identifier of the currency | NUMBER | Always | |
| `quoteUrl` | Public URL for downloading or viewing the published quote | STRING | Always | |

### Form Attribute Section Keys (Common)

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name of the form section | STRING | Always | |
| `type` | Section type identifier (HEADER, PARTIES, SUMMARY, LINE_ITEMS, PAYMENT, ACCEPTANCE) | STRING | Always | |
| `isEnabled` | Whether the section is currently enabled and visible | BOOLEAN | Always | |
| `canMakeHidden` | Whether the user is allowed to hide this entire section | BOOLEAN | Always | |

### Common Detail/Value Keys

These keys appear across multiple detail items and value objects within sections.

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name of the detail item or value entry | STRING | Optional | Present when the item has a named label |
| `type` | Classifies the purpose of the item (e.g., TITLE, QUOTE_REF, DATE, CURRENCY, TEXT, PRODUCT, CONTACT, USER) | STRING | Always | |
| `value` | The resolved value of the field; can be string, number, boolean, or object depending on context | ANY | Optional | Present when the item carries a data value |
| `position` | Determines where the field is rendered in the UI layout | STRING | Optional | Present in value objects that have a specific UI placement |
| `isEnabled` | Whether the detail item or value entry is currently active | BOOLEAN | Optional | Present in toggleable items and contact entries |
| `canMakeDisable` | Whether the user is allowed to toggle this item's visibility | BOOLEAN | Optional | Present in detail items that support toggling |
| `hasInlineEditOption` | Whether the field value can be edited inline on the quote form | BOOLEAN | Optional | Present in value objects that support inline editing |
| `hasArrayOfValues` | Indicates that the values array is dynamically sized based on data | BOOLEAN | Optional | Present when values array contains multiple dynamic entries (contacts, products) |
| `info` | Tooltip or descriptive text explaining the purpose of the field | STRING | Optional | Present in detail items that have contextual help |
| `label` | Label object associated with the field | OBJECT | Optional | Present in detail items that have a separate label display |
| `values` | Array of value objects that hold the actual data for the field | ARRAY | Optional | Present in detail items that carry data values |
| `placeHolder` | Placeholder key used for array-of-values expansion (e.g., `{{PARTY_BUYER_CONTACTS}}`) | STRING | Optional | Present only in detail items with `hasArrayOfValues: true` |
| `description` | Descriptive content; can be a plain string or an object with name, value, type, position, and hasInlineEditOption | STRING/OBJECT | Optional | Present in detail items that have explanatory text |
| `format` | Display format for the field (e.g., "Show as amount", "Simple subtotal amount") | STRING | Optional | Present in discount and subtotal detail items |
| `currency` | Currency symbol used for monetary display (e.g., "$") | STRING | Optional | Present in value objects that represent monetary amounts |

### Label Object Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name of the label (typically "Label") | STRING | Always | |
| `value` | The label text displayed to the user | STRING | Always | |
| `type` | Always "TEXT" for label objects | STRING | Always | |
| `position` | UI position of the label (e.g., KEY, SUB_HEADER, BOLD_TEXT_HEADER, BOLD_TEXT_KEY) | STRING | Always | |
| `hasInlineEditOption` | Whether the label text can be edited inline | BOOLEAN | Always | |

### HEADER Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `details` | Array of header detail items (title, quote reference, dates, currency) | ARRAY | Always | |
| `logo` | Array of logo-related items (company name toggle and logo attachment) | ARRAY | Always | |

#### Header Detail Item — TITLE (type: "TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name of the detail item ("Title") | STRING | Always | |
| `type` | Always "TITLE" | STRING | Always | |
| `info` | Tooltip describing the title field purpose | STRING | Always | |
| `values` | Array containing the title value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The quote title text (e.g., "Quote", "Quotation") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "HEADER" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the title can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the title is visible on the quote | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle the title visibility | BOOLEAN | Always | |

#### Header Detail Item — Quote Reference (type: "QUOTE_REF")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name of the detail item ("Quote reference") | STRING | Always | |
| `type` | Always "QUOTE_REF" | STRING | Always | |
| `info` | Tooltip describing the quote reference field purpose | STRING | Always | |
| `label` | Label object for the quote reference key display | OBJECT | Always | |
| `values` | Array containing the quote reference value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The quote reference string (format: YYYYMMDD-{dealId}) | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the reference can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the quote reference is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Header Detail Item — Issue Date / Expiration Date (type: "DATE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Issue date" or "Expiration date") | STRING | Always | |
| `type` | Always "DATE" | STRING | Always | |
| `info` | Tooltip describing the date field purpose | STRING | Always | |
| `label` | Label object for the date key display | OBJECT | Always | |
| `values` | Array containing the date value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | Unix timestamp in milliseconds | NUMBER | Always | |
| `values[].type` | Always "DATE" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the date can be edited inline (false for issue date, true for expiration date) | BOOLEAN | Always | |
| `isEnabled` | Whether the date field is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Header Detail Item — Currency (type: "CURRENCY")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Currency") | STRING | Always | |
| `type` | Always "CURRENCY" | STRING | Always | |
| `info` | Tooltip describing the currency field purpose | STRING | Always | |
| `label` | Label object for the currency key display | OBJECT | Always | |
| `values` | Array containing the currency value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The currency code string (e.g., "USD", "INR") | STRING | Always | |
| `values[].type` | Always "CURRENCY" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the currency can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the currency field is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Header Logo Item — Company Name Toggle (type: "COMPANY_NAME_TOGGLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Show Company Name") | STRING | Always | |
| `type` | Always "COMPANY_NAME_TOGGLE" | STRING | Always | |
| `value` | The company name string | STRING | Always | |
| `info` | Tooltip describing the toggle purpose | STRING | Always | |
| `isEnabled` | Whether the company name is currently shown | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle this (true) | BOOLEAN | Always | |

#### Header Logo Item — Logo Attachment (type: "ATTACHMENT")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Logo") | STRING | Always | |
| `type` | Always "ATTACHMENT" | STRING | Always | |
| `value` | URL of the uploaded logo image; empty string if not set | STRING | Always | |
| `isEnabled` | Whether the logo is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle the logo (false) | BOOLEAN | Always | |

### PARTIES Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `sellerDetails` | Array of seller-related detail items (title, company, contact) | ARRAY | Always | |
| `buyerDetails` | Array of buyer-related detail items (title, company, contacts) | ARRAY | Always | |

#### Seller Detail Item — Title (type: "TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Title") | STRING | Always | |
| `type` | Always "TITLE" | STRING | Always | |
| `info` | Tooltip describing the seller title field | STRING | Always | |
| `values` | Array containing the title value object | ARRAY | Always | |
| `values[].value` | The seller section title text (e.g., "Seller", "Seller Details") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "HEADER" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the title can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the title is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Seller Detail Item — Company (type: "SELLER_COMPANY")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Company Details") | STRING | Always | |
| `type` | Always "SELLER_COMPANY" | STRING | Always | |
| `info` | Tooltip describing the seller company field | STRING | Always | |
| `values` | Array of value objects (company name and address) | ARRAY | Always | |
| `values[0].name` | Label ("Name") | STRING | Always | |
| `values[0].value` | The seller organisation name | STRING | Always | |
| `values[0].type` | Always "ORGANISATION" | STRING | Always | |
| `values[0].position` | Always "SUB_HEADER" | STRING | Always | |
| `values[0].hasInlineEditOption` | Whether the name can be edited inline (false) | BOOLEAN | Always | |
| `values[1].name` | Label ("Address") | STRING | Always | |
| `values[1].value` | The seller company address; empty string if not available | STRING | Always | |
| `values[1].type` | Always "ADDRESS" | STRING | Always | |
| `values[1].position` | Always "VALUE" | STRING | Always | |
| `values[1].hasInlineEditOption` | Whether the address can be edited inline (false) | BOOLEAN | Always | |
| `company` | Object containing the full seller company details | OBJECT | Optional | Present when seller company data is loaded from IAM |
| `company.name` | The seller company/organisation name | STRING | Optional | Present when company object is available |
| `isEnabled` | Whether the company details are visible | BOOLEAN | Always | |
| `showEditIcon` | Whether the edit icon is shown for this item | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Seller Detail Item — Contact (type: "SELLER_CONTACT")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Contact Details") | STRING | Always | |
| `type` | Always "SELLER_CONTACT" | STRING | Always | |
| `info` | Tooltip describing the seller contact field | STRING | Always | |
| `label` | Label object for the contact section header | OBJECT | Always | |
| `values` | Array of value objects (contact name and email) | ARRAY | Always | |
| `values[0].name` | Label ("Name") | STRING | Always | |
| `values[0].value` | The seller contact's full name | STRING | Always | |
| `values[0].type` | Always "USER" | STRING | Always | |
| `values[0].position` | Always "VALUE" | STRING | Always | |
| `values[0].hasInlineEditOption` | Whether the name can be edited inline (false) | BOOLEAN | Always | |
| `values[1].name` | Label ("Email") | STRING | Always | |
| `values[1].value` | The seller contact's email address | STRING | Always | |
| `values[1].type` | Always "USER" | STRING | Always | |
| `values[1].position` | Always "SUB_VALUE" | STRING | Always | |
| `values[1].hasInlineEditOption` | Whether the email can be edited inline (false) | BOOLEAN | Always | |
| `contact` | Object containing the full seller contact details | OBJECT | Optional | Present when seller contact data is loaded from IAM |
| `contact.firstName` | Seller contact's first name | STRING | Optional | Present when contact object is available |
| `contact.lastName` | Seller contact's last name | STRING | Optional | Present when contact object is available |
| `contact.phone` | Seller contact's phone number | STRING | Optional | Present when contact object is available |
| `contact.email` | Seller contact's email address | STRING | Optional | Present when contact object is available |
| `isEnabled` | Whether the contact details are visible | BOOLEAN | Always | |
| `showEditIcon` | Whether the edit icon is shown for this item | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Buyer Detail Item — Title (type: "TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Title") | STRING | Always | |
| `type` | Always "TITLE" | STRING | Always | |
| `info` | Tooltip describing the buyer title field | STRING | Always | |
| `values` | Array containing the title value object | ARRAY | Always | |
| `values[].value` | The buyer section title text (e.g., "Buyer", "Buyer Details") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "HEADER" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the title can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the title is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Buyer Detail Item — Company (type: "BUYER_COMPANY")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Company Details") | STRING | Always | |
| `type` | Always "BUYER_COMPANY" | STRING | Always | |
| `info` | Tooltip describing the buyer company field | STRING | Always | |
| `values` | Array of value objects (company name and address) | ARRAY | Always | |
| `values[0].name` | Label ("Name") | STRING | Always | |
| `values[0].value` | The buyer company name | STRING | Always | |
| `values[0].type` | Always "COMPANY" | STRING | Always | |
| `values[0].position` | Always "SUB_HEADER" | STRING | Always | |
| `values[0].hasInlineEditOption` | Whether the name can be edited inline (false) | BOOLEAN | Always | |
| `values[1].name` | Label ("Address") | STRING | Always | |
| `values[1].value` | The buyer company address; empty string if not available | STRING | Always | |
| `values[1].type` | Always "ADDRESS" | STRING | Always | |
| `values[1].position` | Always "VALUE" | STRING | Always | |
| `values[1].hasInlineEditOption` | Whether the address can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the company details are visible | BOOLEAN | Always | |
| `showEditIcon` | Whether the edit icon is shown for this item | BOOLEAN | Always | |
| `showDeleteIcon` | Whether the delete icon is shown for this item | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Buyer Detail Item — Contacts (type: "BUYER_CONTACTS")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Contact Details") | STRING | Always | |
| `type` | Always "BUYER_CONTACTS" | STRING | Always | |
| `info` | Tooltip describing the buyer contacts field | STRING | Always | |
| `placeHolder` | Placeholder key for dynamic array expansion (`{{PARTY_BUYER_CONTACTS}}`) | STRING | Always | |
| `label` | Label object for the contacts section header | OBJECT | Always | |
| `values` | Dynamic array of buyer contact value objects | ARRAY | Always | |
| `isEnabled` | Whether the contacts section is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |
| `hasArrayOfValues` | Always true; indicates values array is dynamically sized | BOOLEAN | Always | |

#### Buyer Contact Value Object Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The contact's display name | STRING | Always | |
| `email` | The contact's email address | STRING | Always | |
| `icon` | URL to the contact's profile picture; empty string if not available | STRING | Always | |
| `type` | Always "CONTACT" for buyer contacts | STRING | Always | |
| `position` | UI position for rendering (e.g., "VALUE") | STRING | Always | |
| `isEnabled` | Whether this contact is active in the quote | BOOLEAN | Always | |
| `isQuoteSigner` | Whether this contact is designated as a quote signer | BOOLEAN | Always | |

### SUMMARY Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `summaryDetails` | Array of summary detail items (effective date, term length, discount, total value) | ARRAY | Always | |

#### Summary Detail Item — Effective Date (type: "EFFECTIVE_DATE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Effective date") | STRING | Always | |
| `type` | Always "EFFECTIVE_DATE" | STRING | Always | |
| `info` | Tooltip describing the effective date field | STRING | Always | |
| `label` | Label object for the effective date key display | OBJECT | Always | |
| `values` | Array of effective date value objects | ARRAY | Always | |
| `values[0].name` | Label for the form value ("Effective") | STRING | Always | |
| `values[0].value` | The effective date option ID (e.g., 1) | NUMBER | Always | |
| `values[0].type` | Always "EFFECTIVE_DATE" | STRING | Always | |
| `values[0].hasInlineEditOption` | Whether the option can be edited inline (true) | BOOLEAN | Always | |
| `values[1].name` | Label for the display value ("Effective") | STRING | Optional | Present when a display-only value is needed |
| `values[1].value` | Display text for the effective date (e.g., "On Agreement") | STRING | Optional | Present when a display-only value is needed |
| `values[1].type` | Always "EFFECTIVE_DATE" | STRING | Optional | Present when a display-only value is needed |
| `values[1].position` | Always "VALUE" | STRING | Optional | Present when a display-only value is needed |
| `values[1].isHiddenOnForm` | Whether this value is hidden on the form (true) | BOOLEAN | Optional | Present when a display-only value is needed |
| `values[1].hasInlineEditOption` | Whether it can be edited inline (true) | BOOLEAN | Optional | Present when a display-only value is needed |
| `isEnabled` | Whether the effective date is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Summary Detail Item — Term Length (type: "TERM_LENGTH")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Term length") | STRING | Always | |
| `type` | Always "TERM_LENGTH" | STRING | Always | |
| `info` | Tooltip describing the term length field | STRING | Always | |
| `label` | Label object for the term length key display | OBJECT | Always | |
| `values` | Array containing the term length value object | ARRAY | Always | |
| `values[].value` | The term length text; empty string if not applicable | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the term can be edited inline (false) | BOOLEAN | Always | |
| `values[].isFormField` | Whether this is a user-editable form field (false for auto-calculated) | BOOLEAN | Always | |
| `isEnabled` | Whether the term length is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Summary Detail Item — Discount (type: "DISCOUNT")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Total Discount") | STRING | Always | |
| `type` | Always "DISCOUNT" | STRING | Always | |
| `info` | Tooltip describing the discount field | STRING | Always | |
| `format` | Display format (e.g., "Show as amount") | STRING | Always | |
| `label` | Label object for the discount key display | OBJECT | Always | |
| `values` | Array containing the discount value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].valueAsAmount` | The discount as a flat monetary amount | NUMBER | Optional | Present when format includes amount display |
| `values[].valueAsPercentage` | The discount as a percentage string (e.g., "10.96%") | STRING | Optional | Present when format includes percentage display |
| `values[].valueAsPercentageAndAmount` | Combined display (e.g., "275.0 (10.96%)") | STRING | Optional | Present when format includes combined display |
| `values[].value` | The discount amount (used in draft/update when client sends back data) | NUMBER | Optional | Present when discount is sent as a simple value |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when monetary values are included |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the discount can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the discount is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Summary Detail Item — Total Contract Value (type: "TOTAL_VALUE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Total contract value") | STRING | Always | |
| `type` | Always "TOTAL_VALUE" | STRING | Always | |
| `info` | Tooltip describing the total value field | STRING | Always | |
| `label` | Label object for the total value key display | OBJECT | Always | |
| `values` | Array containing the total value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The total contract value as a number | NUMBER | Always | |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when monetary values are included |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the value can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the total value is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

### LINE_ITEMS Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `items` | Array of line item groups (typically one group of type PRODUCTS) | ARRAY | Always | |
| `totals` | Totals subsection with subtotal, discount, taxes, due amount, and TCV | OBJECT | Always | |

#### Line Item Group Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Label for the group ("Line items") | STRING | Always | |
| `displayName` | Display label with item count (e.g., "Line items (2)") | STRING | Always | |
| `type` | Always "PRODUCTS" for product line items | STRING | Always | |
| `info` | Tooltip describing the line items group | STRING | Always | |
| `placeHolder` | Placeholder key for dynamic array expansion (`{{LINE_ITEMS}}`) | STRING | Always | |
| `values` | Dynamic array of product value objects | ARRAY | Always | |
| `isEnabled` | Whether the line items group is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |
| `hasArrayOfValues` | Always true; indicates values array is dynamically sized | BOOLEAN | Always | |

#### Product Value Object Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The product or service name | STRING | Always | |
| `quantity` | The quantity ordered | NUMBER | Always | |
| `discount` | The discount amount applied to this line item | NUMBER | Always | |
| `unitPrice` | The price per unit before discount | NUMBER | Always | |
| `termTotal` | The total for this line item after discount | NUMBER | Always | |
| `currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when the create API resolves the value |
| `type` | Always "PRODUCT" | STRING | Always | |
| `position` | Always "TABLE_VALUE" | STRING | Always | |
| `hasInlineEditOption` | Whether the product can be edited inline (false) | BOOLEAN | Always | |

#### Totals Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Totals") | STRING | Always | |
| `info` | Tooltip describing the totals section purpose | STRING | Always | |
| `isEnabled` | Whether the totals section is visible on the quote | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle the totals section visibility | BOOLEAN | Always | |
| `attributes` | Array of total attribute items (subtotal, discount, tax, due, TCV) | ARRAY | Always | |

#### Totals Attribute Item — Subtotal (type: "SUBTOTAL")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Subtotal") | STRING | Always | |
| `type` | Always "SUBTOTAL" | STRING | Always | |
| `info` | Tooltip describing the subtotal field | STRING | Always | |
| `format` | Display format (e.g., "Simple subtotal amount") | STRING | Always | |
| `label` | Label object for the subtotal key display | OBJECT | Always | |
| `values` | Array containing the subtotal value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The subtotal monetary amount | NUMBER | Always | |
| `values[].valueWithBreakdown` | Formatted string with currency and billing frequency (e.g., "$2,235.00/one time") | STRING | Optional | Present when the create API resolves the value with breakdown |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when the create API resolves the value |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the subtotal can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the subtotal is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Totals Attribute Item — Discount (type: "DISCOUNT")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Discount included in the subtotal") | STRING | Always | |
| `type` | Always "DISCOUNT" | STRING | Always | |
| `info` | Tooltip describing the discount field | STRING | Always | |
| `format` | Display format (e.g., "Show as amount") | STRING | Always | |
| `label` | Label object for the discount key display | OBJECT | Always | |
| `values` | Array containing the discount value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].valueAsAmount` | The discount as a flat monetary amount | NUMBER | Optional | Present when format includes amount display |
| `values[].valueAsPercentage` | The discount as a percentage string (e.g., "10.96%") | STRING | Optional | Present when format includes percentage display |
| `values[].valueAsPercentageAndAmount` | Combined display (e.g., "275.0 (10.96%)") | STRING | Optional | Present when format includes combined display |
| `values[].value` | The discount amount (used in draft/update) | NUMBER | Optional | Present when discount is sent as a simple value |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when monetary values are included |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the discount can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the discount line is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Totals Attribute Item — Taxes (type: "TAX")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Taxes") | STRING | Always | |
| `type` | Always "TAX" | STRING | Always | |
| `info` | Tooltip describing the tax field | STRING | Always | |
| `label` | Label object for the tax key display | OBJECT | Always | |
| `values` | Array containing the tax value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The total tax amount | NUMBER | Always | |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when the create API resolves the value |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the tax can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the tax line is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Totals Attribute Item — Due on Effective Date (type: "DUE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Due on effective date") | STRING | Always | |
| `type` | Always "DUE" | STRING | Always | |
| `info` | Tooltip describing the due amount field | STRING | Always | |
| `description` | Editable description object with explanatory text about the due amount | OBJECT | Always | |
| `description.name` | Label ("Description") | STRING | Always | |
| `description.value` | The description text | STRING | Always | |
| `description.type` | Always "TEXT" | STRING | Always | |
| `description.position` | Always "DESCRIPTION" | STRING | Always | |
| `description.hasInlineEditOption` | Whether the description can be edited inline (true) | BOOLEAN | Always | |
| `label` | Label object for the due amount key display | OBJECT | Always | |
| `values` | Array containing the due amount value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The total amount due on the effective date | NUMBER | Always | |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when the create API resolves the value |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the value can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the due amount is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

#### Totals Attribute Item — Total Contract Value (type: "TOTAL_VALUE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Total contract value (TCV)") | STRING | Always | |
| `type` | Always "TOTAL_VALUE" | STRING | Always | |
| `info` | Tooltip describing the TCV field | STRING | Always | |
| `label` | Label object for the TCV key display | OBJECT | Always | |
| `values` | Array containing the TCV value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Value") | STRING | Always | |
| `values[].value` | The total contract value | NUMBER | Always | |
| `values[].currency` | Currency symbol (e.g., "$") | STRING | Optional | Present when the create API resolves the value |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the value can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the TCV is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility | BOOLEAN | Always | |

### PAYMENT Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `details` | Array of payment detail items (title, billing) | ARRAY | Always | |

#### Payment Detail Item — Title (type: "TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Title") | STRING | Always | |
| `type` | Always "TITLE" | STRING | Always | |
| `label` | Label object containing the section title text (e.g., "Payments") | OBJECT | Always | |
| `isEnabled` | Whether the title is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Payment Detail Item — Billing (type: "BILLING")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Billing") | STRING | Always | |
| `type` | Always "BILLING" | STRING | Always | |
| `info` | Tooltip describing the billing field | STRING | Always | |
| `label` | Label object for the payment terms statement | OBJECT | Always | |
| `values` | Array containing the payment terms value object | ARRAY | Always | |
| `values[].name` | Label for the value ("Payment Terms") | STRING | Always | |
| `values[].info` | Tooltip describing the payment terms option | STRING | Always | |
| `values[].value` | The payment terms option ID or value (e.g., 1) | NUMBER/STRING | Always | |
| `values[].type` | Always "PAYMENT_TERMS" | STRING | Always | |
| `values[].position` | Always "VALUE" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the payment terms can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the billing item is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

### ACCEPTANCE Section Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `details` | Array of acceptance detail items (title, method, subtitles, buyer contacts, seller contacts) | ARRAY | Always | |

#### Acceptance Detail Item — Title (type: "TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Title") | STRING | Always | |
| `type` | Always "TITLE" | STRING | Always | |
| `description` | Editable description object containing the agreement text | OBJECT | Always | |
| `description.name` | Label ("Description") | STRING | Always | |
| `description.value` | The agreement description text | STRING | Always | |
| `description.type` | Always "TEXT" | STRING | Always | |
| `description.position` | Always "DESCRIPTION" | STRING | Always | |
| `description.hasInlineEditOption` | Whether the description can be edited inline (true) | BOOLEAN | Always | |
| `values` | Array containing the title value object | ARRAY | Always | |
| `values[].value` | The acceptance section title text (e.g., "Acceptance") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].position` | Always "HEADER" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the title can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the title is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Acceptance Detail Item — Acceptance Method (type: "ACC_METHOD")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Acceptance method") | STRING | Always | |
| `type` | Always "ACC_METHOD" | STRING | Always | |
| `values` | Array containing the acceptance method value object | ARRAY | Always | |
| `values[].value` | The acceptance method value; empty string by default | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the method can be edited inline (false) | BOOLEAN | Always | |
| `isEnabled` | Whether the acceptance method is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Acceptance Detail Item — Buyer Subtitle (type: "BUYER_SUB_TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Subtitle") | STRING | Always | |
| `type` | Always "BUYER_SUB_TITLE" | STRING | Always | |
| `values` | Array containing the subtitle value object | ARRAY | Always | |
| `values[].value` | The subtitle text (e.g., "On behalf of the buyer,") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the subtitle can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the subtitle is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Acceptance Detail Item — Buyer Contacts (type: "BUYER_CONTACTS")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Buyer contacts required to sign") | STRING | Always | |
| `type` | Always "BUYER_CONTACTS" | STRING | Always | |
| `placeHolder` | Placeholder key for dynamic array expansion (`{{ACCEPTANCE_BUYER_CONTACTS}}`) | STRING | Always | |
| `values` | Dynamic array of buyer contact signer objects | ARRAY | Always | |
| `isEnabled` | Whether the buyer contacts section is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |
| `hasArrayOfValues` | Always true; indicates values array is dynamically sized | BOOLEAN | Always | |

#### Acceptance Buyer Contact Value Object Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The buyer signer's display name | STRING | Always | |
| `email` | The buyer signer's email address | STRING | Always | |
| `icon` | URL to the signer's profile picture; empty string if not available | STRING | Always | |
| `type` | Always "CONTACT" | STRING | Always | |
| `isEnabled` | Whether this signer is active | BOOLEAN | Always | |
| `position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |

#### Acceptance Detail Item — Seller Subtitle (type: "SELLER_SUB_TITLE")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Subtitle") | STRING | Always | |
| `type` | Always "SELLER_SUB_TITLE" | STRING | Always | |
| `values` | Array containing the subtitle value object | ARRAY | Always | |
| `values[].value` | The subtitle text (e.g., "On behalf of the seller,") | STRING | Always | |
| `values[].type` | Always "TEXT" | STRING | Always | |
| `values[].hasInlineEditOption` | Whether the subtitle can be edited inline (true) | BOOLEAN | Always | |
| `isEnabled` | Whether the subtitle is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |

#### Acceptance Detail Item — Seller Contacts (type: "SELLER_CONTACTS")

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | Display name ("Countersigners") | STRING | Always | |
| `type` | Always "SELLER_CONTACTS" | STRING | Always | |
| `info` | Tooltip describing the seller contacts field | STRING | Always | |
| `placeHolder` | Placeholder key for dynamic array expansion (`{{ACCEPTANCE_SELLER_CONTACTS}}`) | STRING | Always | |
| `values` | Dynamic array of seller countersigner objects | ARRAY | Always | |
| `isEnabled` | Whether the seller contacts section is visible | BOOLEAN | Always | |
| `canMakeDisable` | Whether the user can toggle visibility (false) | BOOLEAN | Always | |
| `hasArrayOfValues` | Always true; indicates values array is dynamically sized | BOOLEAN | Always | |

#### Acceptance Seller Contact Value Object Keys

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `name` | The seller countersigner's display name | STRING | Always | |
| `email` | The seller countersigner's email address | STRING | Always | |
| `icon` | URL to the countersigner's profile picture; empty string if not available | STRING | Always | |
| `type` | Always "USER" | STRING | Always | |
| `isEnabled` | Whether this countersigner is active | BOOLEAN | Always | |
| `position` | Always "BOLD_TEXT_VALUE" | STRING | Always | |

### Position Values Reference

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `HEADER` | Main section header text | STRING | Optional | Used in title value objects |
| `SUB_HEADER` | Sub-section header text | STRING | Optional | Used in label objects for contacts and company sections |
| `KEY` | Label or key text in a key-value pair | STRING | Optional | Used in label objects for quote reference, dates, currency |
| `VALUE` | The value text in a key-value pair | STRING | Optional | Used in most value objects |
| `SUB_VALUE` | Secondary value beneath the main value | STRING | Optional | Used in seller contact email |
| `BOLD_TEXT_HEADER` | Bold header text for summary-style fields | STRING | Optional | Used in summary label objects |
| `BOLD_TEXT_KEY` | Bold key text in totals key-value pairs | STRING | Optional | Used in totals attribute label objects |
| `BOLD_TEXT_VALUE` | Bold value text in totals key-value pairs | STRING | Optional | Used in totals attribute value objects and acceptance contacts |
| `TABLE_VALUE` | Value displayed in a table row | STRING | Optional | Used in product line item value objects |
| `DESCRIPTION` | Descriptive paragraph text below a section | STRING | Optional | Used in description objects for acceptance and due amount |

### Type Values Reference

| Key | Description | Type | Key Presence | Key Presence Case |
|-----|-------------|------|--------------|-------------------|
| `TEXT` | Plain text value | STRING | Optional | Used across general text fields |
| `DATE` | Unix timestamp in milliseconds | STRING | Optional | Used in issue date and expiration date |
| `currency` | Currency code string (e.g., "USD") | STRING | Optional | Used in header currency field |
| `ORGANISATION` | Organisation/company name for the seller | STRING | Optional | Used in seller company values |
| `ADDRESS` | Physical address string | STRING | Optional | Used in company detail values |
| `USER` | IAM user name or email | STRING | Optional | Used in seller contact values and seller countersigners |
| `COMPANY` | CRM company name for the buyer | STRING | Optional | Used in buyer company values |
| `CONTACT` | CRM contact entry | STRING | Optional | Used in buyer contact values and buyer acceptance contacts |
| `PRODUCT` | Product/service line item | STRING | Optional | Used in line item product values |
| `EFFECTIVE_DATE` | Effective date option (ID or display string) | STRING | Optional | Used in summary effective date values |
| `PAYMENT_TERMS` | Payment terms option value | STRING | Optional | Used in payment billing values |


---

## Validation

All create and update operations validate the quotation model before persisting. Validation groups used:

| Operation   | Validation Group |
|-------------|------------------|
| Create      | `Group.Create`   |
| Draft       | `Group.Update`   |
| Update      | `Group.Update`   |
| Delete      | `Group.Delete`   |
| Bulk Create | `Group.Create`   |
| Bulk Update | `Group.Update`   |

### Validation Constraints

| Field               | Constraint     |
|---------------------|----------------|
| `name`              | Max length 255 |
| `uuid`              | Not null       |
| `quoteJson`         | Not null       |
| `quoteJsonAsString` | Not null       |
| `amount`            | Not null       |
| `owner`             | Not null       |
| `createdBy`         | Not null       |
| `updatedBy`         | Not null       |
| `expirationTime`    | Not null       |

### Validation Error Response

```json
{
    "code": -1,
    "data": {
        "errorCode": "QUOTATION_CREATE_FAILED",
        "errorSummary": {
            "error": {
                "code": "QUOTATION_CREATE_FAILED",
                "key": "DATA_VALIDATION_FAILED"
            },
            "errors": [
                {
                    "field": "name",
                    "message": "must not be null"
                }
            ]
        }
    }
}
```

---

## Quotation Statuses

| Status      | Description                                      |
|-------------|--------------------------------------------------|
| `DRAFT`     | Quotation is saved but not finalized             |
| `PUBLISHED` | Quotation is finalized and shared with the buyer |
| `ACCEPTED`  | Buyer has accepted the quotation                 |
| `REJECTED`  | Buyer has rejected the quotation                 |
| `EXPIRED`   | Quotation has passed its expiration date         |

---

## Array-of-Values Sections

Certain sections in the quote JSON use `"hasArrayOfValues": true` to indicate that the `values` array is dynamically sized based on data:

| Section                     | Placeholder                 | Description           |
|-----------------------------|-----------------------------|-----------------------|
| Buyer Contacts (Parties)    | `PARTY_BUYER_CONTACTS`      | Deal contacts         |
| Line Items                  | `LINE_ITEMS`                | Deal products         |
| Buyer Contacts (Acceptance) | `ACCEPTANCE_BUYER_CONTACTS` | Buyer signatories     |
| Seller Contacts (Acceptance)| `ACCEPTANCE_SELLER_CONTACTS`| Seller countersigners |

---

## Notes

- The `create` API only requires `dealId`. The server fetches all deal data (name, value, products, contacts, company, sales owner) and resolves placeholders in the quote template automatically.
- The `draft` and `update` APIs require the full `quoteJson` as the client sends back the edited version.
- The `create` API may respond asynchronously when fetching sales owner details from the IAM microservice.
- All monetary values are returned as numbers (e.g., `2346.25`), with a separate `currency` field for the symbol (e.g., `"$"`).
- Discount values may include `valueAsAmount`, `valueAsPercentage`, and `valueAsPercentageAndAmount` representations.
- Subtotal values may include `valueWithBreakdown` (e.g., `"$2,235.00/one time"`).

