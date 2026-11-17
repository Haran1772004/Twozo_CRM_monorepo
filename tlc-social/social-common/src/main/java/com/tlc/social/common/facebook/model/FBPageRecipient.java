package com.tlc.social.common.facebook.model;

import com.tlc.social.common.model.Recipient;
import com.tlc.social.common.validator.group.SocialValidatorGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * A model which represents the facebook page recipient
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class FBPageRecipient extends Recipient {

    private Long pageRecipientId;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.facebook.page.scoped.id.required")
    private Long pageScopedId;

    @Valid
    private FBPage fbPage;
}
