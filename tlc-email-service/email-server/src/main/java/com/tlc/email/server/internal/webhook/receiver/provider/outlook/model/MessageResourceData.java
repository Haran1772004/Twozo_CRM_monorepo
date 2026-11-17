package com.tlc.email.server.internal.webhook.receiver.provider.outlook.model;

/**
 * Represents the message resource data.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class MessageResourceData {

    private String odataType;
    private String odataId;
    private String id;
    private String tag;

    public String getOdataType() {
        return odataType;
    }

    public void setOdataType(final String odataType) {
        this.odataType = odataType;
    }

    public String getOdataId() {
        return odataId;
    }

    public void setOdataId(final String odataId) {
        this.odataId = odataId;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }
}