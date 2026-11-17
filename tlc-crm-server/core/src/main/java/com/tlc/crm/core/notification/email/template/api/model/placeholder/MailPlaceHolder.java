package com.tlc.crm.core.notification.email.template.api.model.placeholder;

import com.tlc.notification.model.template.PlaceHolder;

import java.util.Objects;

/**
 * An entity which extends the {@link PlaceHolder} to encapsulate fields related to Mail
 * inorder to process them.
 *
 * @author AswiniN
 * @version 1.0
 */
public class MailPlaceHolder extends PlaceHolder {

    private boolean isOwnerField;

    private boolean isSubjectPlaceHolder;

    public boolean isSubjectPlaceHolder() {
        return isSubjectPlaceHolder;
    }

    public void setSubjectPlaceHolder(final boolean isSubjectPlaceHolder) {
        this.isSubjectPlaceHolder = isSubjectPlaceHolder;
    }

    public boolean isOwnerField() {
        return isOwnerField;
    }

    public void setOwnerField(final boolean isOwnerField) {
        this.isOwnerField = isOwnerField;
    }

    /**
     * Refers the pattern of placeHolder
     *
     * @return {@link String} pattern
     */
    public String pattern() {
        return "span[data-info={fieldId: %s}]";
    }

    /**
     * <p>
     * Parses the PlaceHolder value to {@link Long}
     * </p>
     *
     * @return {@link Long} parsed value
     */
    public Long getLong() {
        final Object value = super.getValue();

        return Objects.nonNull(value) && value instanceof Number ? ((Number) value).longValue() : null;
    }
}