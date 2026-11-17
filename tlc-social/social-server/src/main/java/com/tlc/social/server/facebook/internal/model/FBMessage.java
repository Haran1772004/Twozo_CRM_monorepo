package com.tlc.social.server.facebook.internal.model;

import com.tlc.social.common.facebook.model.FBPageRecipient;
import com.tlc.social.common.model.Message;

import com.tlc.social.common.validator.group.SocialValidatorGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * A model which represents the facebook message
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class FBMessage extends Message {

    @NotNull(groups = {SocialValidatorGroup.InboundMessageCreate.class})
    private String externalMessageId;

    @Valid
    private FBPageRecipient fbPageRecipient;
}
