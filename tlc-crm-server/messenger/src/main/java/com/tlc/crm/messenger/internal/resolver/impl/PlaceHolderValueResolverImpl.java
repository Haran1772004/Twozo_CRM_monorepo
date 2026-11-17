package com.tlc.crm.messenger.internal.resolver.impl;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivity;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.resolver.PlaceHolderValueResolver;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.iam.common.models.User;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Resolves the place holder values.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class PlaceHolderValueResolverImpl implements PlaceHolderValueResolver {

    private static final String YOU = "you";
    private static final String YOURSELF = "yourself";
    private static final String PLACEHOLDER_1 = "{{placeholder_1}}";
    private static final String PLACEHOLDER_2 = "{{placeholder_2}}";

    private final CrmUserManager crmUserManager;

    private PlaceHolderValueResolverImpl() {
        crmUserManager = CrmUserManager.getInstance();
    }

    private static class Instance {
        private static final PlaceHolderValueResolverImpl INSTANCE = new PlaceHolderValueResolverImpl();
    }

    /**
     * <p>
     * Gets {@link PlaceHolderValueResolverImpl}.
     * </p>
     *
     * @return {@link PlaceHolderValueResolverImpl}.
     */
    public static PlaceHolderValueResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the template by adding place holder values.
     * </p>
     *
     * @param template         {@link ConversationActivityTemplate}.
     * @param placeHolderValue {@link ConversationActivity.PlaceHolderValue}
     * @param userMap          Represents the user map
     * @return resolved message
     */
    public String resolve(final ConversationActivityTemplate template, final ConversationActivity.PlaceHolderValue placeHolderValue,
                          final Map<Long, User> userMap) {
        final JsonObject value = placeHolderValue.getValueAsJson();
        final long assignerId = value.getLong(MessengerFields.PLACEHOLDER_1);
        final long assigneeId = value.getLong(MessengerFields.PLACEHOLDER_2);
        final CrmUser loggedInUser = LoggedInUserInfo.getUser();
        final long orgId = loggedInUser.orgId();
        final Set<Long> userIds = new HashSet<>();

        userIds.add(assigneeId);
        userIds.add(assignerId);

        if (!crmUserManager.exists(orgId, userIds)) {
            return null;
        }

        final long currentUserId = loggedInUser.id();
        String message = ((assignerId == currentUserId) || (assigneeId == currentUserId)) ?
                template.getChatOwnerTemplate() : template.getOtherUserTemplate();

        if (message.contains(PLACEHOLDER_1)) {
            message = message.replace(PLACEHOLDER_1, resolveAssigner(assignerId, currentUserId, userMap));
        }

        if (message.contains(PLACEHOLDER_2)) {
            message = message.replace(PLACEHOLDER_2, resolveAssignee(assignerId, assigneeId, currentUserId, userMap));
        }

        if (assignerId == currentUserId) {
            return message.replaceFirst(".", message.substring(0,1).toUpperCase());
        } else {
            return message;
        }
    }

    /**
     * <p>
     * Resolves assigner value.
     * </p>
     *
     * @param assignerId    Assigner's id.
     * @param currentUserId Current user's id.
     * @param userMap       Represents the user map
     * @return assigner value.
     */
    private String resolveAssigner(final long assignerId, final long currentUserId, final Map<Long, User> userMap) {
        return Objects.equals(assignerId, currentUserId) ? YOU : userMap.get(assignerId).getFullName();
    }

    /**
     * <p>
     * Resolves assignee value.
     * </p>
     *
     * @param assignerId    Assigner's id.
     * @param assigneeId    Assignee's id
     * @param currentUserId Current user's id.
     * @return assignee value.
     */
    private String resolveAssignee(final long assignerId, final long assigneeId, final long currentUserId, final Map<Long, User> userMap) {
        if (assigneeId == currentUserId) {
            return assignerId == currentUserId ? YOURSELF : YOU;
        }
        return userMap.get(assigneeId).getFullName();
    }
}
