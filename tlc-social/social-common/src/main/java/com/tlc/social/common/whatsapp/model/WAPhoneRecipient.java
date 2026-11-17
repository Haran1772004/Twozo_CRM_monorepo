package com.tlc.social.common.whatsapp.model;

import com.tlc.social.common.model.Recipient;
import com.tlc.social.common.validator.group.SocialValidatorGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * A model which represents the WhatsApp phone recipient
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAPhoneRecipient extends Recipient {

    private Long waPhoneRecipientId;
    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.whatsapp.recipient.id.required")
    private Long externalRecipientId;
    @Valid
    @NotNull(groups = {SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.whatsapp.business.phone.number.required")
    private WABusinessPhoneNumber waBusinessPhoneNumber;

    public Long getWaPhoneRecipientId() {
        return waPhoneRecipientId;
    }

    public void setWaPhoneRecipientId(final Long waPhoneRecipientId) {
        this.waPhoneRecipientId = waPhoneRecipientId;
    }

    public Long getExternalRecipientId() {
        return externalRecipientId;
    }

    public void setExternalRecipientId(final Long externalRecipientId) {
        this.externalRecipientId = externalRecipientId;
    }

    public WABusinessPhoneNumber getWaBusinessPhoneNumber() {
        return waBusinessPhoneNumber;
    }

    public void setWaBusinessPhoneNumber(final WABusinessPhoneNumber waBusinessPhoneNumber) {
        this.waBusinessPhoneNumber = waBusinessPhoneNumber;
    }
}