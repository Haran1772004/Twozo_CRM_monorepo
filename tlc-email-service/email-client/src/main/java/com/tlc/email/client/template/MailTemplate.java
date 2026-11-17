package com.tlc.email.client.template;

import com.tlc.notification.model.template.Template;
import com.tlc.notification.model.template.PlaceHolder;

import java.util.Collection;

/**
 * <p>
 * An entity that represents the mail template
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class MailTemplate implements Template {

    private Collection<PlaceHolder> placeHolders;
    private boolean isContentHasPlaceHolders;
    private boolean isSubjectHasPlaceHolders;

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Collection} of {@link PlaceHolder}
     */
    @Override
    public Collection<PlaceHolder> placeHolders() {
        return placeHolders;
    }

    public void setPlaceHolders(final Collection<PlaceHolder> placeHolders) {
        this.placeHolders = placeHolders;
    }

    public boolean isContentHasPlaceHolders() {
        return isContentHasPlaceHolders;
    }

    protected void setContentHasPlaceHolders(final boolean contentHasPlaceHolders) {
        isContentHasPlaceHolders = contentHasPlaceHolders;
    }

    public boolean isSubjectHasPlaceHolders() {
        return isSubjectHasPlaceHolders;
    }

    protected void setSubjectHasPlaceHolders(final boolean subjectHasPlaceHolders) {
        isSubjectHasPlaceHolders = subjectHasPlaceHolders;
    }

    public boolean hasPlaceHolders() {
        return this.isSubjectHasPlaceHolders || this.isContentHasPlaceHolders;
    }
}