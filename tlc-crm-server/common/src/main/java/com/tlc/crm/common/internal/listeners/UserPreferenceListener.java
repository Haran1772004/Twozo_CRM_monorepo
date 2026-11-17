package com.tlc.crm.common.internal.listeners;

import com.tlc.crm.common.cache.UserPreferenceHolderCache;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.sql.resource.OMCRMPREFERENCE;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Listener that listens to DML operations performed on the {@link OMCRMPREFERENCE}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class UserPreferenceListener implements CompleteRowListener {

    private final UserPreferenceHolderCache userPreferenceHolderCache;
    private final UserPreferenceManager userPreferenceManager;

    private UserPreferenceListener() {
        userPreferenceHolderCache = UserPreferenceHolderCache.get();
        userPreferenceManager = UserPreferenceManager.getInstance();
    }

    private static final class Instance {
        private static final UserPreferenceListener USER_PREFERENCE_HOLDER_LISTENER = new UserPreferenceListener();
    }

    /**
     * <p>
     * Gets the instance of {@link  UserPreferenceListener}
     * </p>
     *
     * @return the instance of {@link UserPreferenceListener}
     */
    public static UserPreferenceListener getInstance() {
        return Instance.USER_PREFERENCE_HOLDER_LISTENER;
    }

    /**
     * <p>
     * Listens to rows addition, converts and adds them to the cache.
     * </p>
     *
     * @param rows Collection of {@link Row}.
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        modifyCache(rows, true);
    }

    /**
     * <p>
     * Listens to rows deletion and remove them to the cache.
     * </p>
     *
     * @param rows Collection of {@link Row}.
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        modifyCache(rows, false);
    }

    /**
     * <p>
     * Listens to rows update, converts and adds them to the cache.
     * </p>
     *
     * @param rows Collection of {@link Row}.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        modifyCache(rows, false);
    }

    /**
     * <p>
     * Adds {@link UserPreferenceHolder}.
     * </p>
     *
     * @param rows Collection of {@link Row}.
     */
    private void modifyCache(final Collection<Row> rows, final boolean isCreateEvent) {

        for (final Row row : rows) {
            final Long orgId = row.getOrgId();
            final Long preferenceId = row.getPKValue();
            final Long userId = userPreferenceManager.getUserId(orgId, preferenceId);

            if (Objects.nonNull(userId)) {

                if (isCreateEvent) {
                    userPreferenceHolderCache.add(orgId, userPreferenceManager.convertRowToModel(userId, row));
                } else {
                    userPreferenceHolderCache.remove(orgId, userId);
                }
            }
        }
    }
}
