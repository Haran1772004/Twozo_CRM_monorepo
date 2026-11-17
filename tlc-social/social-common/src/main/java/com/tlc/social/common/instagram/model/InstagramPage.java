package com.tlc.social.common.instagram.model;

import com.tlc.social.common.model.SocialPage;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <p>
 * Model pertaining to instagram related fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
public class InstagramPage extends SocialPage {

    private Long fbPageExternalId;

    /**
     * {@inheritDoc}
     *
     * @param object Represents the object to be compared with
     * @return true if the object equals with current object, false otherwise
     */
    @Override
    public boolean equals(final Object object) {

        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }

        return Objects.equals(getExternalPageId(), ((InstagramPage) object).getExternalPageId());
    }
}
