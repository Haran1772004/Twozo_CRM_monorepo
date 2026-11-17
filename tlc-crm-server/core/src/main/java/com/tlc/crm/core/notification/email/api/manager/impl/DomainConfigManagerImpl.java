package com.tlc.crm.core.notification.email.api.manager.impl;

import com.tlc.crm.core.notification.email.api.manager.DomainConfigManager;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMDOMAINCONFIG;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manager class responsible for CRUD operations on {@link DomainConfig},
 * including default sender and reply-to address resolution.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class DomainConfigManagerImpl implements DomainConfigManager {

    private static final String REPLY_TO = "replyTo";

    private final MailManager mailManager;

    private static class Instance {
        private static final DomainConfigManagerImpl INSTANCE = new DomainConfigManagerImpl();
    }

    private DomainConfigManagerImpl() {
        this.mailManager = MailManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DomainConfigManagerImpl} instance.
     * </p>
     *
     * @return {@link DomainConfigManager}
     */
    public static DomainConfigManager getInstance() {
        return DomainConfigManagerImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the given org.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @return the {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the domain config table.
     * </p>
     *
     * @return The {@link Table} containing domain config.
     */
    protected Table getTable() {
        return Table.get(OMEMDOMAINCONFIG.TABLE);
    }

    /**
     * <p>
     * Gets the email address table.
     * </p>
     *
     * @return The {@link Table} containing email address.
     */
    protected Table getEmailTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the from address table.
     * </p>
     *
     * @return The {@link Table} containing from address.
     */
    protected Table getFromAddressTable() {
        return Table.get(OMEMFROMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the reply-to address table alias.
     * </p>
     *
     * @return The aliased {@link Table} for reply-to address.
     */
    private Table getReplyToTable() {
        return Table.get(OMEMADDRESS.TABLE, REPLY_TO);
    }

    /**
     * <p>
     * Persists a new {@link DomainConfig} into the database.
     * </p>
     *
     * @param domainConfig The {@link DomainConfig} to be created.
     */
    @Override
    public void create(final DomainConfig domainConfig) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainConfigIntoContainer(domainConfig, null, targetContainer);

        orgDataStore(domainConfig.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates an existing {@link DomainConfig} in the database.
     * </p>
     *
     * @param domainConfig The {@link DomainConfig} to be updated.
     */
    @Override
    public void update(final DomainConfig domainConfig) {
        final DataContainer targetContainer = DataContainer.create();

        loadDomainConfigIntoContainer(domainConfig, getExistingContainer(domainConfig.orgId(), domainConfig.getDomainId()), targetContainer);

        orgDataStore(domainConfig.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Deletes a {@link DomainConfig} from the database by its domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID of the config to delete.
     */
    @Override
    public void delete(final Long orgId, final Long domainId) {
        final Table table = getTable();

        orgDataStore(orgId).delete(table, new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINCONFIG.DOMAIN_ID), domainId)));
    }

    /**
     * <p>
     * Retrieves a {@link DomainConfig} by its domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID.
     * @return An {@link Optional} containing the {@link DomainConfig} if found, or empty otherwise.
     */
    @Override
    public Optional<DomainConfig> get(final Long orgId, final Long domainId) {
        if (Objects.isNull(domainId)) {
            return Optional.empty();
        }
        final Table table = getTable();
        final DataContainer dataContainer = getExistingContainer(orgId, domainId);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINCONFIG.DOMAIN_ID), domainId));

        return Optional.ofNullable(convertRowToModel(dataContainer.getRow(table, whereClause), orgId, domainId, dataContainer));
    }

    /**
     * <p>
     * Checks whether a {@link DomainConfig} exists for the given domain ID.
     * </p>
     *
     * @param orgId    Represents the orgId.
     * @param domainId Represents the domain ID.
     * @return {@code true} if a config exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(final Long orgId, final Long domainId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINCONFIG.DOMAIN_ID), domainId)));

        return orgDataStore(orgId).get(countQuery) == 1;
    }

    /**
     * <p>
     * Retrieves the total number of domain configs for a given organization.
     * </p>
     *
     * @param orgId Represents the organization ID.
     * @return The total count of domain configs.
     */
    @Override
    public long getCount(final Long orgId) {
        final CountQuery countQuery = CountQuery.get(getTable());

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads domain config data into the target container for the given org and event type.
     * </p>
     *
     * @param orgId           Represents the orgId.
     * @param isCreateEvent   Represents whether this is a create event.
     * @param targetContainer Represents the target container.
     * @param domainConfig    The {@link DomainConfig} to load.
     */
    @Override
    public void loadDomainConfigIntoContainer(final Long orgId, final boolean isCreateEvent,
                                              final DataContainer targetContainer, final DomainConfig domainConfig) {
        loadDomainConfigIntoContainer(
                domainConfig,
                isCreateEvent ? null : getExistingContainer(orgId, domainConfig.getDomainId()),
                targetContainer
        );
    }

    /**
     * <p>
     * Converts a database {@link Row} into a {@link DomainConfig} model.
     * </p>
     *
     * @param row           The database row to convert.
     * @param orgId         The organization ID.
     * @param domainId      The domain ID.
     * @param dataContainer The {@link DataContainer} containing related rows.
     * @return A {@link DomainConfig} object, or {@code null} if the row is null.
     */
    private DomainConfig convertRowToModel(final Row row, final Long orgId, final Long domainId,
                                           final DataContainer dataContainer) {
        if (Objects.isNull(row)) {
            return null;
        }
        final DomainConfig domainConfig = new DomainConfig();
        final Table fromAddressTable = getFromAddressTable();
        final Table emailTable = getEmailTable();
        final Table replyToTable = getReplyToTable();

        domainConfig.setOrgId(orgId);
        domainConfig.setDomainId(domainId);
        domainConfig.setDefaultSenderName(row.get(OMEMDOMAINCONFIG.DEFAULT_SENDER_NAME));
        domainConfig.setIncludeDefaultFromAsInReplyTo(row.get(OMEMDOMAINCONFIG.INCLUDE_DEFAULT_FROM_AS_IN_REPLY_TO));
        final Long defaultSenderId = row.get(OMEMDOMAINCONFIG.DEFAULT_SENDER);

        if (Objects.nonNull(defaultSenderId)) {
            final Row fromAddressRow = dataContainer.getRow(fromAddressTable, defaultSenderId);

            if (Objects.nonNull(fromAddressRow)) {
                final Row emailAddressRow = dataContainer.getRow(emailTable, (Long) fromAddressRow.get(OMEMFROMADDRESS.EMAIL_ID));

                if (Objects.nonNull(emailAddressRow)) {
                    final FromAddressInfo fromAddressInfo = new FromAddressInfo(emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS));

                    fromAddressInfo.setId(defaultSenderId);

                    domainConfig.setDefaultFromAddress(fromAddressInfo);
                }
            }
        }
        final Long defaultReplyTo = row.get(OMEMDOMAINCONFIG.DEFAULT_REPLY_TO);

        if (Objects.nonNull(defaultReplyTo)) {
            final Row inReplyToRow = dataContainer.getRow(replyToTable, defaultReplyTo);

            if (Objects.nonNull(inReplyToRow)) {
                domainConfig.setDefaultReplyToAddress(new EmailAddress(inReplyToRow.getPKValue(), inReplyToRow.get(OMEMADDRESS.EMAIL_ADDRESS)));
            }
        }

        return domainConfig;
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} with all join data for a domain config.
     * </p>
     *
     * @param orgId    Represents the orgId.
     * @param domainId Represents the domain ID.
     * @return A {@link DataContainer} with domain config and related address data.
     */
    private DataContainer getExistingContainer(final Long orgId, final Long domainId) {
        return orgDataStore(orgId).get(getSelectQuery(domainId));
    }

    /**
     * <p>
     * Constructs a {@link SelectQuery} to retrieve a domain config.
     * </p>
     *
     * @param domainId Represents the domain ID to filter by.
     * @return A configured {@link SelectQuery}.
     */
    private SelectQuery getSelectQuery(final Long domainId) {
        final Table table = getTable();
        final Table replyToTable = getReplyToTable();
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailTable();

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(table.getColumn(OMEMDOMAINCONFIG.DEFAULT_REPLY_TO), replyToTable.getPKColumn(), leftJoin),
                        new JoinClause(table.getColumn(OMEMDOMAINCONFIG.DEFAULT_SENDER), fromAddressTable.getPKColumn(), leftJoin),
                        new JoinClause(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailAddressTable.getPKColumn(), leftJoin)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINCONFIG.DOMAIN_ID), domainId)));

        return selectQuery;
    }

    /**
     * <p>
     * Loads a {@link DomainConfig} into the target {@link DataContainer},
     * resolving the default sender and reply-to addresses as needed.
     * </p>
     *
     * @param domainConfig    The {@link DomainConfig} to load.
     * @param sourceContainer The source {@link DataContainer} with existing data, can be {@code null}.
     * @param targetContainer The target {@link DataContainer} to populate.
     */
    private void loadDomainConfigIntoContainer(final DomainConfig domainConfig, final DataContainer sourceContainer,
                                               final DataContainer targetContainer) {
        final Long orgId = domainConfig.orgId();
        final Long userId = domainConfig.getUserId();
        final Long domainId = domainConfig.getDomainId();

        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMDOMAINCONFIG.DOMAIN_ID), domainId));

        final Row targetRow = targetContainer.getRow(table, whereClause);
        final Row existingRow = Objects.isNull(targetRow)
                ? (Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(table, whereClause))
                : targetRow;
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;

        if (row.isNewRow()) {
            row.set(OMEMDOMAINCONFIG.USER_ID, domainConfig.getUserId());
            row.set(OMEMDOMAINCONFIG.DOMAIN_ID, domainConfig.getDomainId());
        }
        row.set(OMEMDOMAINCONFIG.DEFAULT_SENDER_NAME, domainConfig.getDefaultSenderName());
        final FromAddressInfo defaultSenderAddress = domainConfig.getDefaultFromAddress();

        if (Objects.isNull(defaultSenderAddress) || Objects.isNull(defaultSenderAddress.getEmailAddress())) {
            row.set(OMEMDOMAINCONFIG.DEFAULT_SENDER, null);
        } else {
            mailManager.loadFromAddressInfo(orgId, userId, defaultSenderAddress, FromAddressInfo.Type.CAMPAIGN, targetContainer);
            row.set(OMEMDOMAINCONFIG.DEFAULT_SENDER, defaultSenderAddress.id());
        }
        final EmailAddress defaultReplyToAddress = domainConfig.getDefaultReplyToAddress();

        if (Objects.isNull(defaultReplyToAddress) || Objects.isNull(defaultReplyToAddress.getEmailAddress())) {
            row.set(OMEMDOMAINCONFIG.DEFAULT_REPLY_TO, null);
        } else {
            loadDefaultReplyToAddress(orgId, userId, defaultReplyToAddress, targetContainer);
            row.set(OMEMDOMAINCONFIG.DEFAULT_REPLY_TO, defaultReplyToAddress.id());
        }
        row.set(OMEMDOMAINCONFIG.INCLUDE_DEFAULT_FROM_AS_IN_REPLY_TO, domainConfig.isIncludeDefaultFromAsInReplyTo());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Loads the default reply-to address into the target container,
     * creating a new email address row if one does not already exist.
     * </p>
     *
     * @param orgId                 Represents the organization id.
     * @param userId                Represents the user id.
     * @param defaultReplyToAddress Represents the default reply-to address.
     * @param targetContainer       Represents the target container.
     */
    private void loadDefaultReplyToAddress(final Long orgId, final Long userId,
                                           final EmailAddress defaultReplyToAddress,
                                           final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailTable();
        final String replyToEmailAddress = defaultReplyToAddress.getEmailAddress();
        final Long replyToEmailPkValue = mailManager.getEmailId(orgId, replyToEmailAddress);

        if (Objects.isNull(replyToEmailPkValue)) {
            final Row replyToEmailRow = targetContainer.getRow(emailAddressTable,
                    new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), replyToEmailAddress)));
            if (Objects.isNull(replyToEmailRow)) {
                mailManager.loadEmailAddress(orgId, userId, defaultReplyToAddress, targetContainer);
            } else {
                defaultReplyToAddress.setId(replyToEmailRow.getPKValue());
            }
        } else {
            mailManager.loadEmailAddress(orgId, userId, defaultReplyToAddress, targetContainer);
        }
    }
}