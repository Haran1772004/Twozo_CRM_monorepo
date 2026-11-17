package com.tlc.social.common.whatsapp.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.SocialEntity;
import com.tlc.social.common.validator.group.SocialValidatorGroup;

import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * A model which represents WhatsApp Business phone number
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WABusinessPhoneNumber implements SocialEntity {

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.whatsapp.phone.number.id.required")
    private Long id;
    @NotNull(groups = {Group.Create.class, SocialValidatorGroup.OutboundMessageCreate.class}, message = "i18n.social.whatsapp.phone.number.external.id.required")
    private Long externalPhoneNumberId;
    private Long waBusinessAccountId;
    @NotNull(groups = {Group.Create.class}, message = "i18n.social.whatsapp.phone.display.name.required")
    private String displayPhoneNumber;
    @Valid
    @NotNull(groups = {Group.Create.class}, message = "i18n.social.whatsapp.business.channel.required")
    private Channel channel;
    private String encodedPIN;
    private boolean isRegistered;
    private Long templateCount;

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getExternalPhoneNumberId() {
        return externalPhoneNumberId;
    }

    public void setExternalPhoneNumberId(final Long externalPhoneNumberId) {
        this.externalPhoneNumberId = externalPhoneNumberId;
    }

    public Long getWaBusinessAccountId() {
        return waBusinessAccountId;
    }

    public void setWaBusinessAccountId(final Long waBusinessAccountId) {
        this.waBusinessAccountId = waBusinessAccountId;
    }

    public String getDisplayPhoneNumber() {
        return displayPhoneNumber;
    }

    public void setDisplayPhoneNumber(final String displayPhoneNumber) {
        this.displayPhoneNumber = displayPhoneNumber;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(final boolean registered) {
        isRegistered = registered;
    }

    public String getEncodedPIN() {
        return encodedPIN;
    }

    public void setEncodedPIN(final String encodedPIN) {
        this.encodedPIN = encodedPIN;
    }

    public Long getTemplateCount() {
        return templateCount;
    }

    public void setTemplateCount(final Long templateCount) {
        this.templateCount = templateCount;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(SocialErrorCodes.ORG_ID_NOT_SUPPORTED, "i18n.social.model.org.id.not.supported");
    }

    @Override
    public Object identity() {
        return null;
    }
}