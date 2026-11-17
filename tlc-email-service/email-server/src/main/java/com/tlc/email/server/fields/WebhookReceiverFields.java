package com.tlc.email.server.fields;

/**
 * Represents the fields related to email webhook receiver.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class WebhookReceiverFields {

    private WebhookReceiverFields() {
    }

    public static final String ID = "id";
    public static final String PAYLOAD = "payload";
    public static final String PROVIDER_ID = "providerId";
    public static final String MESSAGE = "message";
    public static final String DATA = "data";
    public static final String HISTORY_ID = "historyId";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String VALUE = "value";

    /* Outlook Fields */
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String CHANGE_TYPE = "changeType";
    public static final String ENCRYPTED_CONTENT = "encryptedContent";
    public static final String SUBSCRIPTION_EXPIRATION_DATE_TIME = "subscriptionExpirationDateTime";
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_DATA = "resourceData";
    public static final String TENANT_ID = "tenantId";
    public static final String ENCRYPTED_CERTIFICATE_ID = "encryptionCertificateId";
    public static final String DATA_KEY = "dataKey";
    public static final String ODATA_ID = "@odata.id";
    public static final String ODATA_TAG = "@odata.etag";
    public static final String ODATA_TYPE = "@odata.type";
    public static final String DATA_SIGNATURE = "dataSignature";
    public static final String VALIDATION_TOKEN = "validationToken";
}
