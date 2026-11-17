package com.tlc.crm.core.notification.email.iam.service;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.notification.email.api.manager.addon.bcc.SmartBccManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.email.notification.sql.resource.OMEMAutoForwardingAddress;
import com.tlc.crm.email.notification.sql.resource.OMEMSMARTBCCADDRESS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.service.EmailAddOnService;
import com.tlc.email.common.service.SmartBccService;
import com.tlc.email.common.service.TeamInboxService;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Service for backfilling email addon initialization for existing organizations.
 * This is a temporary service used to initialize Smart BCC and Team Inbox for organizations
 * that were created before the AddonInitializer was implemented.
 * </p>
 *
 * @author GitHub Copilot
 * @version 1.0
 */
public class EmailAddonBackfillService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAddonBackfillService.class);

    private final EmailAddOnService smartBccService;
    private final SmartBccManager smartBccManager;
    private final EmailAddOnService teamInboxService;
    private final TeamInboxManager teamInboxManager;
    private final UserService iamUserService;

    private static class Instance {
        private static final EmailAddonBackfillService INSTANCE = new EmailAddonBackfillService();
    }

    public static EmailAddonBackfillService getInstance() {
        return Instance.INSTANCE;
    }

    private EmailAddonBackfillService() {
        this.smartBccService = SmartBccService.getInstance();
        this.smartBccManager = SmartBccManager.getInstance();
        this.teamInboxService = TeamInboxService.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.iamUserService = UserService.getInstance();
    }

    /**
     * <p>
     * Initializes email addons (Smart BCC and Team Inbox) for a single organization.
     * This method is synchronous and waits for the initialization to complete.
     * Only initializes addons that don't already exist for the organization.
     * </p>
     *
     * @param crmOrg  The organization to initialize
     * @param crmUser The admin user for the organization
     * @return true if initialization succeeded, false otherwise
     */
    public boolean initializeAddonsForOrg(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();

        LOGGER.debug("Addon Backfill: Starting email addons initialization for Org ID: {}, User ID: {}", orgId, userId);

        try {
            final boolean smartBccExists = isSmartBccInitialized(orgId);
            final boolean teamInboxExists = isTeamInboxInitialized(orgId);

            if (smartBccExists && teamInboxExists) {
                LOGGER.debug("Addon Backfill: Both Smart BCC and Team Inbox already initialized for Org ID: {}, skipping", orgId);
                return true;
            }

            LOGGER.debug("Addon Backfill: Smart BCC exists: {}, Team Inbox exists: {} for Org ID: {}",
                    smartBccExists, teamInboxExists, orgId);

            if (!smartBccExists) {
                smartBccManager.loadParentAddressesIntoContainer(List.of(smartBccService.getAccount()));
            }
            if (!teamInboxExists) {
                teamInboxManager.loadParentAddressesIntoContainer(List.of(teamInboxService.getAccount()));
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicInteger successCount = new AtomicInteger(0);

            final EventCallback<Collection<User>> userFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        if (!smartBccExists && initializeSmartBccWithUsers(orgId, userId, users)) {
                            successCount.incrementAndGet();
                        } else if (smartBccExists) {
                            LOGGER.debug("Addon Backfill: Smart BCC already exists for Org ID: {}, skipping initialization", orgId);
                            successCount.incrementAndGet();
                        }

                        if (!teamInboxExists && initializeTeamInboxWithUsers(orgId, userId, users)) {
                            successCount.incrementAndGet();
                        } else if (teamInboxExists) {
                            LOGGER.debug("Addon Backfill: Team Inbox already exists for Org ID: {}, skipping initialization", orgId);
                            successCount.incrementAndGet();
                        }

                        LOGGER.debug("Addon Backfill: Completed email addons initialization for Org ID: {}, User ID: {}, Success count: {}",
                                orgId, userId, successCount.get());
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    try {
                        LOGGER.error("Addon Backfill: User fetch failed for Org ID: {}, Reason: {}",
                                orgId, throwable.getMessage(), throwable);
                    } finally {
                        latch.countDown();
                    }
                }
            };
            iamUserService.fetchUsers(crmOrg.getIamOrgId(), List.of(crmUser.getIamId()), userFetchCallBack);
            final boolean completed = latch.await(30, TimeUnit.SECONDS);

            if (!completed) {
                LOGGER.error("Addon Backfill: Timeout waiting for initialization. Org ID: {}", orgId);
                return false;
            }
            final boolean success = successCount.get() > 0;

            if (success) {
                LOGGER.debug("Addon Backfill: Successfully initialized {} addon(s) for Org ID: {}", successCount.get(), orgId);
            } else {
                LOGGER.warn("Addon Backfill: No addons were initialized for Org ID: {}", orgId);
            }

            return success;
        } catch (final Throwable throwable) {
            LOGGER.error("Addon Backfill: Failed to initialize addons for Org ID: {}, Reason: {}",
                    orgId, throwable.getMessage(), throwable);
            return false;
        }
    }

    /**
     * <p>
     * Checks if Smart BCC addon is already initialized for the given organization.
     * </p>
     *
     * @param orgId Organization ID
     * @return true if Smart BCC is already initialized, false otherwise
     */
    private boolean isSmartBccInitialized(final Long orgId) {
        try {
            final Table smartBccTable = Table.get(OMEMSMARTBCCADDRESS.TABLE);
            final SelectQuery selectQuery = SelectQuery.get(smartBccTable);

            selectQuery.addSelectClause(smartBccTable.getColumn(OMEMSMARTBCCADDRESS.ORG_ID));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(smartBccTable.getColumn(OMEMSMARTBCCADDRESS.ORG_ID), orgId)));
            selectQuery.setLimitClause(new LimitClause(1, 1));

            final WritableDataStore dataStore = DataStoreProvider.getWritableDataStore();
            final Row row = dataStore.get(selectQuery).getRow(smartBccTable);

            return row != null;
        } catch (final Throwable throwable) {
            LOGGER.error("Addon Backfill: Error checking Smart BCC initialization for Org ID: {}, assuming not initialized. Error: {}",
                    orgId, throwable.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Checks if Team Inbox addon is already initialized for the given organization.
     * </p>
     *
     * @param orgId Organization ID
     * @return true if Team Inbox is already initialized, false otherwise
     */
    private boolean isTeamInboxInitialized(final Long orgId) {
        try {
            final Table teamInboxTable = Table.get(OMEMAutoForwardingAddress.TABLE);
            final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

            selectQuery.addSelectClause(teamInboxTable.getColumn(OMEMAutoForwardingAddress.ORG_ID));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMAutoForwardingAddress.ORG_ID), orgId)));
            selectQuery.setLimitClause(new LimitClause(1, 1));

            final WritableDataStore dataStore = DataStoreProvider.getWritableDataStore();
            final Row row = dataStore.get(selectQuery).getRow(teamInboxTable);

            return row != null;
        } catch (final Throwable throwable) {
            LOGGER.error("Addon Backfill: Error checking Team Inbox initialization for Org ID: {}, assuming not initialized. Error: {}",
                    orgId, throwable.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Initializes the Smart BCC add-on with the fetched users.
     * </p>
     *
     * @param orgId  Organization ID
     * @param userId User ID
     * @param users  Collection of IAM users
     * @return true if initialization succeeded, false otherwise
     */
    private boolean initializeSmartBccWithUsers(final Long orgId, final Long userId, final Collection<User> users) {
        final String serviceName = ServiceType.SMART_BCC.getName();

        LOGGER.debug("Addon Backfill: {} initialization started. Org ID: {}, User ID: {}", serviceName, orgId, userId);

        try {
            final Row parentAddress = smartBccManager.getParentAddress();

            smartBccManager.loadEmailAddress(
                    orgId, userId, users,
                    new EmailAddress(smartBccService.generateSharedMailBoxAddress()),
                    parentAddress.getPKValue()
            );
            LOGGER.debug("Addon Backfill: {} (alias) address creation succeeded. Org ID: {}, User ID: {}",
                    serviceName, orgId, userId);
            return true;
        } catch (final Throwable throwable) {
            LOGGER.error("Addon Backfill: {} address creation failed. Org ID: {}, Reason: {}",
                    serviceName, orgId, throwable.getMessage(), throwable);
            return false;
        }
    }

    /**
     * <p>
     * Initializes the Team Inbox add-on with the fetched users.
     * </p>
     *
     * @param orgId  Organization ID
     * @param userId User ID
     * @param users  Collection of IAM users
     * @return true if initialization succeeded, false otherwise
     */
    private boolean initializeTeamInboxWithUsers(final Long orgId, final Long userId, final Collection<User> users) {
        final String serviceName = ServiceType.TEAM_INBOX.getName();

        LOGGER.debug("Addon Backfill: {} initialization started. Org ID: {}, User ID: {}", serviceName, orgId, userId);

        try {
            final Row parentAddress = teamInboxManager.getParentAddress();

            teamInboxManager.loadEmailAddress(
                    orgId, userId, users,
                    new EmailAddress(teamInboxService.generateSharedMailBoxAddress()),
                    parentAddress.getPKValue()
            );
            LOGGER.debug("Addon Backfill: {} (alias) address creation succeeded. Org ID: {}, User ID: {}",
                    serviceName, orgId, userId);
            return true;
        } catch (final Throwable throwable) {
            LOGGER.error("Addon Backfill: {} address creation failed. Org ID: {}, Reason: {}",
                    serviceName, orgId, throwable.getMessage(), throwable);
            return false;
        }
    }
}