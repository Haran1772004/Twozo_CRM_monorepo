package com.tlc.crm.email.notification.email.sync.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.sync.SyncSettings;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.email.sync.internal.async.resolver.EmailSyncSettingResolver;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Listens for update row events related to {@link SyncSettings} and handles the
 * events triggered after processing each row.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see RowUpdateListener
 */
public class EmailSyncSettingListener implements RowUpdateListener {

    private static final Collection<String> FROM_ADDRESS_STATUS_COLUMNS = Set.of(
            OMEMSYNCEDEMAIL.SYNC_STATUS, OMEMSYNCEDEMAIL.CONNECTION_STATUS
    );

    private final EmailSyncManager emailSyncManager;
    private final EmailSyncSettingResolver emailSyncSettingResolver;

    private final static class Instance {
        private static final EmailSyncSettingListener INSTANCE = new EmailSyncSettingListener();
    }

    private EmailSyncSettingListener() {
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.emailSyncSettingResolver = EmailSyncSettingResolver.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link EmailSyncSettingListener}.
     * </p>
     *
     * @return {@link EmailSyncSettingListener}
     */
    public static EmailSyncSettingListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreUpdate(final Table table, final Set<String> columns) {
        return columns.stream().noneMatch(FROM_ADDRESS_STATUS_COLUMNS::contains);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param rows Refers the collection of email sync rows
     */
    @Override
    public void updateRows(final Collection<Row> rows) {

        if (Objects.isNull(rows) || rows.isEmpty()) {
            return;
        }
        final long orgId = resolveOrgId(rows);
        final Collection<Long> syncIds = new HashSet<>();
        final Map<Long, Collection<Long>> enableByUser = new HashMap<>();
        final Map<Long, Collection<Long>> disableByUser = new HashMap<>();

        for (final Row row : rows) {
            syncIds.add(row.getPKValue());
            final ConnectionStatus connectionStatus = ConnectionStatus.get(row.get(OMEMSYNCEDEMAIL.CONNECTION_STATUS));
            final SyncStatus syncStatus = SyncStatus.getStatus(row.get(OMEMSYNCEDEMAIL.SYNC_STATUS));
            final long userId = row.get(OMEMSYNCEDEMAIL.USER_ID);
            final long emailId = row.get(OMEMSYNCEDEMAIL.EMAIL_ID);

            if (!connectionStatus.isActive() || syncStatus == SyncStatus.FAILED || syncStatus.isStopped()) {
                addFromAddressAction(disableByUser, userId, emailId);
            } else if (syncStatus.isSynced()) {
                addFromAddressAction(enableByUser, userId, emailId);
            }
        }
        emailSyncManager.updateFromAddressStatus(orgId, enableByUser, disableByUser);
        emailSyncSettingResolver.remove(syncIds);
    }

    /**
     * <p>
     * Resolves the organization id of the given rows and ensures that every row
     * belongs to the same organization.
     * </p>
     *
     * @param rows Refers the collection of email sync rows
     * @return the organization id shared by all the rows
     * @throws ErrorCode if a row has no organization id or the rows belong to different organizations
     */
    private Long resolveOrgId(final Collection<Row> rows) {
        Long orgId = null;

        for (final Row row : rows) {
            final Long rowOrgId = row.getOrgId();

            if (Objects.isNull(rowOrgId) || (Objects.nonNull(orgId) && !orgId.equals(rowOrgId))) {
                throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_ADDRESS, "i18n.notifications.sync.address.invalid");
            }
            orgId = rowOrgId;
        }

        return orgId;
    }

    /**
     * <p>
     * Adds the email id under the given user id in the provided action map.
     * </p>
     *
     * @param actionsByUser Represents the email ids grouped by user id
     * @param userId        Represents the user id
     * @param emailId       Represents the email id
     */
    private void addFromAddressAction(final Map<Long, Collection<Long>> actionsByUser, final long userId, final long emailId) {
        actionsByUser.computeIfAbsent(userId, key -> new HashSet<>()).add(emailId);
    }
}