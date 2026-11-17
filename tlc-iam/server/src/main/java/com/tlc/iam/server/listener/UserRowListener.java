package com.tlc.iam.server.listener;

import com.tlc.iam.common.models.User;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.iam.server.resolver.UserResolver;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.Objects;

/**
 * Listens to the create, update and delete operations performed on the user table
 *
 * @author Maharaja S
 * @version 1.0
 */
public class UserRowListener implements CompleteRowListener {

    private final UserResolver userResolver;
    private final UserManager userManager;

    private UserRowListener() {
        userResolver = UserResolver.getInstance();
        userManager = UserManager.getInstance();
    }

    private static final class Instance {
        private static final UserRowListener USER_ROW_LISTENER = new UserRowListener();
    }

    /**
     * <p>
     * Gets the instance of {@link  UserRowListener}
     * </p>
     *
     * @return the instance of {@link UserRowListener}
     */
    public static UserRowListener getInstance() {
        return Instance.USER_ROW_LISTENER;
    }

    /**
     * <p>
     * Listens to the create operation and adds the row data to the cache.
     * </p>
     *
     * @param rows Represents the collection of row data
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        addUsersToCache(rows);
    }

    /**
     * <p>
     * Listens to the delete operation and removes the row data from the cache.
     * </p>
     *
     * @param rows Represents the collection of row data
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                final User user = userManager.convertRowToUser(row);

                userResolver.remove(user.id());
            }
        }
    }

    /**
     * <p>
     * Listens to the update operation and adds the row data to the cache.
     * </p>
     *
     * @param rows Represents the collection of row data
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        addUsersToCache(rows);
    }

    /**
     * <p>
     * Converts each {@link Row} into {@link User} and adds it to the cache
     * </p>
     *
     * @param rows Represents the collection of user rows
     */
    private void addUsersToCache(final Collection<Row> rows) {
        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                final User user = userManager.convertRowToUser(row);

                userResolver.add(user);
            }
        }
    }
}