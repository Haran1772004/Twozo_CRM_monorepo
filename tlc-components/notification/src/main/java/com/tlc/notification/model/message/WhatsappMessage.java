package com.tlc.notification.model.message;

import com.tlc.notification.model.Receiver;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.Notification;

import java.util.ArrayList;
import java.util.Objects;

/**
 * <p>
 * A model that represents the whatsapp message.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WhatsappMessage extends Notification<MobileNumber, MobileNumber> implements Receiver {

    private String serviceId;
    private String message;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void addRecipient(final MobileNumber mobileNumber) {
        if (Objects.isNull(super.recipients)) {
            super.recipients = new ArrayList<>();
        }
        super.recipients.add(mobileNumber);
    }
}
