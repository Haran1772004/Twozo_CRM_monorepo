package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.DraftEmail.DraftAction;
import com.tlc.email.common.model.type.Email;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;

/**
 * <p>
 * Represents the drafted mail.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EntityDraftEmail extends EntityOutboundEmail {

    private DraftAction draftAction;

    public boolean isDraft() {
        return true;
    }

    public void setDraftAction(final DraftAction draftAction) {
        this.draftAction = draftAction;
    }

    public DraftAction getDraftAction() {
        return draftAction;
    }

    /**
     * <p>
     * Checks whether the mail instance is scheduled or ordinary email
     * </p>
     *
     * @return false.
     */
    public Boolean isScheduled() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return DRAFT;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Email}
     */
    @Override
    public Email getEmail() {
        final DraftEmail email = new DraftEmail();

        email.setDraftAction(this.draftAction);

        return email;
    }
}
