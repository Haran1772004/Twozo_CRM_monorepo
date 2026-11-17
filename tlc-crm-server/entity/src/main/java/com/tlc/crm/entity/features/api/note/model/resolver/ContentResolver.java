package com.tlc.crm.entity.features.api.note.model.resolver;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.web.HttpUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for resolving user mentions in HTML content.
 *
 * <p>This class takes an HTML string with anchor tags containing a custom "user-id" attribute,
 * matches those user IDs against provided {@link CrmUser} objects, and replaces the anchor tag
 * content with the user's full name. It also updates the href attribute to a fully qualified URL.
 *
 * <p>Used to render HTML content with enriched user identity information.</p>
 *
 * Example Input:
 * {@code <a user-id="123" href="/user/">@placeholder</a>}
 *
 * Example Output:
 * {@code <a user-id="123" href="https://domain.com/user/123">John Doe</a>}
 *
 * @author Sathish
 * @version 1.0
 */
public final class ContentResolver {

    private final HtmlDocument content;

    /**
     * Constructs a new ContentResolver for the given raw HTML string.
     *
     * @param content HTML content potentially containing user mentions
     */
    public ContentResolver(final String content) {
        final HtmlParser htmlParser = HtmlParser.getInstance();
        this.content = htmlParser.parse(content);
    }

    /**
     * Resolves user mentions by replacing anchor tag text with full user names and
     * updating the href with an absolute URL.
     *
     * @param mentionedUsers the collection of {@link CrmUser} objects that may be referenced in the content
     * @return a modified HTML string with user-friendly names and URLs
     */
    public String resolveMentions(final Collection<CrmUser> mentionedUsers) {
        final HtmlElements aTagElements = content.select(NoteFields.A_TAG.getName());
        final Map<Long, CrmUser> userMap = mentionedUsers.stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

        for (final HtmlElement aTagElement : aTagElements.getElements()) {
            final String userIdString = aTagElement.attribute(NoteFields.USER_ID_TAG.getName());

            if (Objects.nonNull(userIdString) && !userIdString.isBlank()) {
                final long userId = NumberUtils.parseNumber(userIdString).longValue();
                final CrmUser user = userMap.get(userId);
                final String userName = user.getFullName();

                if (Objects.nonNull(userName)) {
                    aTagElement.attribute(NoteFields.HREF_TAG.getName(), HttpUtil.buildAbsoluteUrl(aTagElement.attribute(NoteFields.HREF_TAG.getName())));
                    aTagElement.text(String.format("%s%s", "@", userName));
                }
            }
        }
        return content.html();
    }
}