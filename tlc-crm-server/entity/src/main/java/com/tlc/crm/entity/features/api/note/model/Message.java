package com.tlc.crm.entity.features.api.note.model;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.features.api.note.model.extractor.ContentExtractor;
import com.tlc.crm.entity.features.api.note.model.resolver.ContentResolver;
import com.tlc.crm.entity.validator.internal.MessageValidator;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Model which represents the {@link Message}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@MessageValidator(groups = {Group.Create.class, Group.Update.class})
public abstract class Message implements TlcModel {

    private static final short MIN_LENGTH = 0;

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.note.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @SearchField(name = "name", readMethodName = "getContentBody")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.note.invalid.content")
    private String content;
    private CrmUser createdBy;
    private CrmUser updatedBy;
    private DateTime createdTime;
    private DateTime updatedTime;
    private boolean isPinned;
    private boolean isContentTruncated;
    private Collection<CrmUser> mentionedUsers;

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getContent() {
        final Collection<CrmUser> mentionedUsers = getMentionedUsers();

        if (Objects.isNull(content) || Objects.isNull(mentionedUsers) || mentionedUsers.isEmpty()) {
            return content;
        }
        final ContentResolver contentResolver = new ContentResolver(content);

        return contentResolver.resolveMentions(mentionedUsers);
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(final boolean pinned) {
        isPinned = pinned;
    }

    public boolean isContentTruncated() {
        return isContentTruncated;
    }

    public void setContentTruncated(final boolean contentTruncated) {
        isContentTruncated = contentTruncated;
    }

    public Collection<CrmUser> getMentionedUsers() {
        if (Objects.isNull(mentionedUsers) && Objects.nonNull(content)) {
            final ContentExtractor contentExtractor = new ContentExtractor(orgId, content);

            mentionedUsers = contentExtractor.extractMentions();
        }
        return mentionedUsers;
    }

    public void setMentionedUsers(final Collection<CrmUser> mentionedUsers) {
        this.mentionedUsers = mentionedUsers;
    }

    /**
     * Retrieves the note trimmed to the specified length.
     *
     * @param maxLength Refers the max length of the trimmed note.
     * @return The trimmed note.
     */
    public String getTrimmedContent(final int maxLength) {

        if (Objects.isNull(content)) {
            return null;
        }
        final HtmlParser htmlParser = HtmlParser.getInstance();
        final String plainTextContent = htmlParser.isHtml(content) ? htmlParser.parse(content).text() : content;

        if (plainTextContent.length() > maxLength) {
            return plainTextContent.substring(MIN_LENGTH, maxLength);
        }
        return plainTextContent;
    }

    /**
     * <p>
     * Gets the body of the content.
     * </p>
     *
     * @return body of the content.
     */
    public String getContentBody() {
        final HtmlParser htmlParser = HtmlParser.getInstance();

        return htmlParser.isHtml(content) ? htmlParser.parse(content).text() : content;
    }

    @Override
    public Object identity() {
        return null;
    }
}