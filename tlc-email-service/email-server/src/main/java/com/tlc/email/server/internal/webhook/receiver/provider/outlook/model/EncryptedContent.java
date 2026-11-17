package com.tlc.email.server.internal.webhook.receiver.provider.outlook.model;

/**
 * Represents the encrypted content.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class EncryptedContent {

    private String key;
    private String data;
    private String signature;
    private String encryptionCertificateId;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(final String signature) {
        this.signature = signature;
    }

    public String getEncryptionCertificateId() {
        return encryptionCertificateId;
    }

    public void setEncryptionCertificateId(final String encryptionCertificateId) {
        this.encryptionCertificateId = encryptionCertificateId;
    }
}