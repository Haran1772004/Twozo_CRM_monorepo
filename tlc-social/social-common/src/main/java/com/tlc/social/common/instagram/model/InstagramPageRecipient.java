package com.tlc.social.common.instagram.model;

import com.tlc.social.common.model.Recipient;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Model pertaining to instagram page's recipient.
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class InstagramPageRecipient extends Recipient {

    private Long pageRecipientId;

    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.facebook.page.scoped.id.required")
    private Long pageScopedId;

    @Valid
    private InstagramPage instagramPage;
}
