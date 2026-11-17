package com.tlc.social.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.validator.group.SocialValidatorGroup;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <p>
 * Pertains to fields related to a page.
 * </p>
 *
 * @author Ajay.
 * @version 1.0/.
 */
@Getter
@Setter
public class SocialPage implements SocialEntity {

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {SocialValidatorGroup.OutboundMessageCreate.class, SocialValidatorGroup.InboundMessageCreate.class}, message = "i18n.social.facebook.page.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, SocialValidatorGroup.OutboundMessageCreate.class}, message = "i18n.social.facebook.external.id.required")
    private Long externalPageId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.social.facebook.page.name.required")
    private String name;

    @Valid()
    private Channel channel;

    /**
     * {@inheritDoc}
     *
     * @return the id
     */
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

    /**
     * {@inheritDoc}
     *
     * @return the hash value of this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(externalPageId);
    }
}