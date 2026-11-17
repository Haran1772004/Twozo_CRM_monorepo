package com.tlc.email.common.model;

import com.tlc.email.common.model.type.Email;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A model that represents thread which encapsulates mail
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class Thread implements EmailModel {

    private boolean hasActiveMessages;
    private boolean hasDrafts;
    private boolean isArchived;
    private boolean isStarred;
    private boolean hasAttachments;
    private Long id;
    private Long conversationCount;
    private Collection<Email> emails;

    /**
     * {@inheritDoc}
     *
     * @return id
     */
    @Override
    public Long id() {
        return id;
    }

    public boolean hasActiveMessages() {
        return hasActiveMessages;
    }

    public void setHasActiveMessages(final boolean hasActiveMessages) {
        this.hasActiveMessages = hasActiveMessages;
    }

    public boolean hasDrafts() {
        return hasDrafts;
    }

    public void setHasDrafts(final boolean hasDrafts) {
        this.hasDrafts = hasDrafts;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(final boolean archived) {
        isArchived = archived;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(final boolean starred) {
        isStarred = starred;
    }

    public boolean hasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(final boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getConversationCount() {
        return conversationCount;
    }

    public void setConversationCount(final Long conversationCount) {
        this.conversationCount = conversationCount;
    }

    public Collection<Email> getEmails() {
        return emails;
    }

    public void setEmails(final Collection<Email> emails) {
        this.emails = emails;
    }

    public Optional<Email> getComposeEmail() {
        return Objects.isNull(emails) ? Optional.empty() : emails.stream().findFirst();
    }
}