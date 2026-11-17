package com.tlc.crm.common.iam.cache.internal.permission;

import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.EmailAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.FieldPermissionAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.GoalAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.LeadAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingListAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.PhoneAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.QuotationAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SocialChatAuthorizer;

import com.tlc.web.Action;

import java.lang.annotation.Annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>
 * Stores the authorizers into cache, which is registered in managements.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PermissionAuthorizerStoreImpl implements PermissionAuthorizerStore {

    private final Collection<Class<? extends Annotation>> annotations;
    private final Map<Action, Collection<Annotation>> actionAnnotations;

    private static final class Instance {
        private static final PermissionAuthorizerStoreImpl INSTANCE = new PermissionAuthorizerStoreImpl();
    }

    private PermissionAuthorizerStoreImpl() {
        annotations = new CopyOnWriteArraySet<>();
        actionAnnotations = new ConcurrentHashMap<>();

        annotations.addAll(Set.of(SourceAuthorizer.class, QuotaAuthorizer.class, GoalAuthorizer.class,
                EmailAuthorizer.class, PhoneAuthorizer.class, LimitAuthorizer.class, LeadAuthorizer.class,
                SettingsAuthorizer.class, FollowerAuthorizer.class, AnalyticsAuthorizer.class,
                SocialChatAuthorizer.class, FieldPermissionAuthorizer.class, MarketingListAuthorizer.class,
                QuotationAuthorizer.class, MarketingCampaignAuthorizer.class));
    }

    public static PermissionAuthorizerStoreImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Annotation> get(final Action action) {
        final Collection<Annotation> annotations = actionAnnotations.get(action);

        return Objects.isNull(annotations) ? Collections.emptySet() : annotations;
    }

    /**
     * <p>
     * Loads all the authorizers registered in the given {@link Action}.
     * </p>
     *
     * @param action {@link Action} which is manages the request.
     */
    @Override
    public void loadAuthorizers(final Action action) {
        final Class<? extends Action> actionClass = action.getClass();
        final Collection<Annotation> annotations = new HashSet<>();

        for (final Class<? extends Annotation> annotation : this.annotations) {
            final Annotation authorizer = actionClass.getAnnotation(annotation);

            if (Objects.nonNull(authorizer)) {
                annotations.add(authorizer);
            }
        }

        if (!annotations.isEmpty()) {
            actionAnnotations.put(action, annotations);
        }
    }

    /**
     * <p>
     * Clears all the stored annotations from the cache.
     * </p>
     */
    public void clearAnnotations() {
        actionAnnotations.clear();
    }
}
