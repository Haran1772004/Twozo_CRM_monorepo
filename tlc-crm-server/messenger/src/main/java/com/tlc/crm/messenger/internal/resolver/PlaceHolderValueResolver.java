package com.tlc.crm.messenger.internal.resolver;

import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivity.PlaceHolderValue;
import com.tlc.crm.messenger.internal.resolver.impl.PlaceHolderValueResolverImpl;
import com.tlc.iam.common.models.User;

import java.util.Map;

/**
 * <p>
 * Resolves the place holder values.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface PlaceHolderValueResolver {

    /**
     * <p>
     * Gets {@link PlaceHolderValueResolver}.
     * </p>
     *
     * @return {@link PlaceHolderValueResolver}.
     */
    static PlaceHolderValueResolver getInstance() {
        return PlaceHolderValueResolverImpl.getInstance();
    }

    /**
     * <p>
     *     Resolves the template by adding place holder values.
     * </p>
     * @param template {@link ConversationActivityTemplate}.
     * @param placeHolderValue {@link PlaceHolderValue}
     * @return resolved message
     */
    String resolve(final ConversationActivityTemplate template, final PlaceHolderValue placeHolderValue, final Map<Long, User> userMap);
}
