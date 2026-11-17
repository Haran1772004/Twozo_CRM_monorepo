package com.tlc.social.server.instagram.internal.model;

import com.tlc.social.common.instagram.model.InstagramPageRecipient;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Attributes pertaining to instagram's message.
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
public class InstagramMessage extends Message {

    @NotNull(groups = {SocialValidatorGroup.InboundMessageCreate.class})
    private String externalMessageId;

    @Valid
    private InstagramPageRecipient instagramPageRecipient;
}
