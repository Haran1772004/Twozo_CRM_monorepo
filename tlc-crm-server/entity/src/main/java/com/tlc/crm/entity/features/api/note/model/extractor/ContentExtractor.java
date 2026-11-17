package com.tlc.crm.entity.features.api.note.model.extractor;

import com.tlc.commons.parser.html.HtmlDocument;

import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.common.util.NumberUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Utility class for parsing HTML content and extracting user mentions based on custom HTML tag attributes.
 *
 * Specifically, it looks for <a> tags that contain a "user-id" attribute and converts those into {@link CrmUser} objects.
 * This is useful in collaborative content or comment systems where users are mentioned in rich text editors.
 *
 * <p>Example HTML input:
 * <pre>{@code
 *     <p>This is a comment mentioning <a user-id="123">John Doe</a></p>
 * }</pre>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ContentExtractor {

    private static final UserService USER_SERVICE;

    private final long orgId;
    private final HtmlDocument content;

    static {
        USER_SERVICE = UserService.getInstance();
    }

    /**
     * Constructs a ContentExtractor from raw HTML content and org ID context.
     *
     * @param orgId   the organization ID (used for user lookup)
     * @param content the raw HTML string to be parsed
     */
    public ContentExtractor(final long orgId, final String content) {
        final HtmlParser htmlParser = HtmlParser.getInstance();

        this.orgId = orgId;
        this.content = htmlParser.parse(content);
    }

    /**
     * Parses the content and extracts all mentioned users by inspecting anchor tags
     * that include the custom "user-id" attribute.
     *
     * @return a collection of {@link CrmUser} objects mentioned in the content
     */
    public Collection<CrmUser> extractMentions() {
        final Collection<Long> userIds = new HashSet<>();
        final HtmlElements aTagElements = content.select(NoteFields.A_TAG.getName());

        for (final HtmlElement aTagElement : aTagElements.getElements()) {
            final String userIdString = aTagElement.attribute(NoteFields.USER_ID_TAG.getName());

            if (Objects.nonNull(userIdString) && !userIdString.isBlank()) {
                final long userId = NumberUtils.parseNumber(userIdString).longValue();

                userIds.add(userId);
            }
        }
        return USER_SERVICE.getUsers(orgId, userIds);
    }
}