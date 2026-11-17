package com.tlc.social.server.whatsapp.internal.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.social.common.whatsapp.model.WAPhoneRecipient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A model which represents the WhatsApp message
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAMessage extends Message {

    @NotNull(groups = {SocialValidatorGroup.InboundMessageCreate.class})
    @Size(max = 128, groups = {SocialValidatorGroup.InboundMessageCreate.class})
    private String externalMessageId;
    @Valid
    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class}, message = "i18n.social.whatsapp.business.recipient.required")
    private WAPhoneRecipient waPhoneRecipient;
    private Long templateId;
    private JsonObject paramData;

    public JsonObject getParamData() {
        return paramData;
    }

    public void setParamData(final JsonObject paramData) {
        this.paramData = paramData;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public WAPhoneRecipient getWaRecipient() {
        return waPhoneRecipient;
    }

    public void setWaRecipient(final WAPhoneRecipient waPhoneRecipient) {
        this.waPhoneRecipient = waPhoneRecipient;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(final String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }
}