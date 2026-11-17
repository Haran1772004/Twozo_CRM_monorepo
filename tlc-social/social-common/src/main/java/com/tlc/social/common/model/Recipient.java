package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.validator.group.SocialValidatorGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * A model which represents the recipient
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public abstract class Recipient implements SocialEntity {

    @Getter(AccessLevel.NONE)
    private Long id;

    @NotNull(groups = SocialValidatorGroup.InboundMessageCreate.class, message = "i18n.social.chat.id.required")
    private Long chatId;

    @Valid
    private RecipientProfile recipientProfile;

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
