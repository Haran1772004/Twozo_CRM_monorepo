package com.tlc.social.common.facebook.model;

import com.tlc.social.common.model.SocialPage;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * A model which represents the facebook page
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class FBPage extends SocialPage {

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
        final FBPage fbPage = (FBPage) object;

        return Objects.equals(getExternalPageId(), fbPage.getExternalPageId());
    }
}
