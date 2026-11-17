package com.tlc.crm.core.notification.email.org.api;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmRole;

import com.tlc.crm.email.notification.AbstractNotificationManager;

import com.tlc.crm.security.sql.resource.OMCRMROLE;

import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.MEMORGANIZATIONEMAILROLE;
import com.tlc.crm.email.notification.sql.resource.OMEMORGANIZATIONEMAIL;

import com.tlc.crm.core.notification.email.org.status.OrgEmailErrorCodes;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.org.api.model.OrgEmail;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link OrgEmail} related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class OrgEmailManager extends AbstractNotificationManager<OrgEmail> {

    private final MailManager mailManager;

    private static class Instance {
        private static final OrgEmailManager INSTANCE = new OrgEmailManager();
    }

    private OrgEmailManager() {
        mailManager = MailManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link OrgEmailManager} Instance
     * </p>
     *
     * @return {@link OrgEmailManager}
     */
    public static OrgEmailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of organization email
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMORGANIZATIONEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the org mail role table
     * </p>
     *
     * @return the {@link Table} of organization email
     */
    public Table getOrgMailRoleTable() {
        return Table.get(MEMORGANIZATIONEMAILROLE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link OrgEmail}
     */
    @Override
    public OrgEmail create(final OrgEmail orgEmail) {
        final DataContainer targetContainer = DataContainer.create();

        loadOrgEmailIntoContainer(true, orgEmail, targetContainer);
        orgDataStore(orgEmail.orgId()).commitChanges(targetContainer);

        return orgEmail;
    }

    /**
     * Deletes the entity by orgId.
     *
     * @param orgId       Represents an organization id.
     * @param userId      Represents the user id
     * @param orgEmailIds Collection of {@link OrgEmail} ids.
     */
    @Override
    public void delete(final long orgId, final long userId, final Collection<Long> orgEmailIds) {
        final Table orgEmailTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(orgEmailTable.getPKColumn(), orgEmailIds))
                .and(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.USER_ID), userId));
        final Long emailAddressId = mailManager.getEmailAddressPkValue(orgId, whereClause, orgEmailTable);
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        final DeleteQuery deleteQuery = DeleteQuery.get(orgEmailTable);

        deleteQuery.setWhereClause(whereClause);
        deleteQueries.add(deleteQuery);

        if (Objects.nonNull(emailAddressId)) {
            final Table fromAddressTable = mailManager.getFromAddressTable();
            final DeleteQuery fromAddressDeleteQuery = DeleteQuery.get(fromAddressTable);

            fromAddressDeleteQuery.setWhereClause(new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId))
                    .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailAddressId))
                    .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), FromAddressInfo.Type.ORG_EMAIL.getId())));
            deleteQueries.add(fromAddressDeleteQuery);
        }

        orgDataStore(orgId).delete(deleteQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @param ids   Represents the org email ids
     * @return Collection of {@link OrgEmail}
     */
    @Override
    public Collection<OrgEmail> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = getDataContainer(orgId, ids);
        final Stream<Row> rows = Objects.nonNull(dataContainer) ? dataContainer.getRows(getTable()) : null;

        return rows.filter(Objects::nonNull).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id
     * @param orgEmails Collection of {@link OrgEmail}
     * @return  The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<OrgEmail> orgEmails) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Collection<Long> ids = orgEmails.stream().map(OrgEmail::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = getDataContainer(orgId, ids);

        orgEmails.forEach(orgEmail -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadOrgEmailIntoContainer(false, orgEmail, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(orgEmail.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Org Email update failed Org ID : {} ID : {} reason : {}", orgId, orgEmail.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(OrgEmailErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, orgEmail.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(OrgEmailErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * <p>
     * Gets the {@link OrgEmail} list for specific organization id
     * </p>
     *
     * @param orgId Represents the organization id
     * @param start Represents the lower bound of org email to be fetched
     * @param limit Represents the upper bound of org email to be fetched
     * @return Collection of {@link OrgEmail}
     */
    public Collection<OrgEmail> getOrgEmailList(final Long orgId, final int start, final int limit) {
        final Table orgEmailTable = getTable();
        final Column pkColumn = orgEmailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(orgEmailTable);
        final SelectQuery innerQuery = SelectQuery.get(orgEmailTable);

        buildSelectQueryForOrgEmail(selectQuery);
        innerQuery.addSelectClause(pkColumn);
        innerQuery.setLimitClause(new LimitClause(start, limit));
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.ORG_ID), orgId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.isEmpty() ? null : dataContainer.getRows(orgEmailTable);

        return Objects.isNull(rows) ? Collections.emptyList() : rows.map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Checks the existence of org email address
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param emailId Represents the org email addresses
     * @return {@link Boolean} status of email address existence
     */
    public boolean checkEmailAddressExists(final Long orgId, final String emailId) {
        final Table emailTable = mailManager.getEmailAddressTable();
        final CountQuery countQuery = CountQuery.get(emailTable);

        countQuery.addJoinClause(new JoinClause(emailTable.getPKColumn(), getTable().getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the verification of emails
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param emailId The emailId to be verified
     */
    public void updateVerificationStatus(final long orgId, final long userId, final Long emailId) {

        if (Objects.nonNull(emailId)) {
            final Table orgEmailTable = getTable();
            final Table emailAddressTable = mailManager.getEmailAddressTable();
            final SelectQuery selectQuery = SelectQuery.get(orgEmailTable);

            selectQuery.addSelectClause(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS));
            selectQuery.addSelectClause(orgEmailTable);
            selectQuery.addJoinClause(new JoinClause(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID), emailAddressTable.getPKColumn()));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID), emailId))
                    .and(Criteria.eq(orgEmailTable.getColumn(CoreEmailFields.USER_ID.name()), userId)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {
                final DataContainer targetContainer = DataContainer.create();
                final Row emailAddressRow = dataContainer.getRow(emailAddressTable);
                final Row orgEmailRow = dataContainer.getRow(orgEmailTable);

                orgEmailRow.set(OMEMORGANIZATIONEMAIL.EMAIL_ID, orgEmailRow.get(OMEMORGANIZATIONEMAIL.EMAIL_ID));
                orgEmailRow.set(OMEMORGANIZATIONEMAIL.NAME, orgEmailRow.get(OMEMORGANIZATIONEMAIL.NAME));
                orgEmailRow.set(OMEMORGANIZATIONEMAIL.IS_VERIFIED, true);
                targetContainer.updateRow(orgEmailRow);

                if (Objects.nonNull(emailAddressRow)) {
                    mailManager.loadFromAddressIntoContainer(orgId, userId, FromAddressInfo.Type.ORG_EMAIL,
                            emailAddressRow.getPKValue(), targetContainer);
                }

                orgDataStore(orgId).commitChanges(targetContainer);
            }
        }
    }

    /**
     * <p>
     * Fetches the count of available org emails
     * </p>
     *
     * @param orgId Represents the organization id
     * @return {@link Long}
     */
    public Long getOrgEmailCount(final Long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * <p>
     * Checks the existence of org email name
     * </p>
     *
     * @param orgId Represents the organization id
     * @param name  Represents the org email name
     * @return {@link Boolean} status of name existence
     */
    public boolean checkDisplayNameExists(long orgId, final String name) {
        final Table orgEmailTable = getTable();
        final CountQuery countQuery = CountQuery.get(orgEmailTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.NAME), name)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Loads the org email details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgEmail        An entity that holds org email related attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadOrgEmailIntoContainer(final boolean isCreateEvent, final OrgEmail orgEmail, final DataContainer targetContainer) {
        final Table orgEmailTable = getTable();
        final EmailAddress emailAddress = orgEmail.getAddress();
        final long orgId = orgEmail.orgId();

        mailManager.loadEmailAddress(orgId, orgEmail.getUserId(), emailAddress, targetContainer);
        final DataContainer dataContainer = orgDataStore(orgId).get(orgEmailTable,
                new WhereClause(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID), emailAddress.id())));

        loadOrgEmailIntoContainer(isCreateEvent, orgEmail, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the org email information into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgEmail        An entity that holds org email related attributes
     * @param entityContainer Holds the user defined values of existing org mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadOrgEmailIntoContainer(final boolean isCreateEvent, final OrgEmail orgEmail, final DataContainer entityContainer,
                                           final DataContainer targetContainer) {
        final Table orgEmailTable = getTable();
        final Long id = orgEmail.id();
        final long orgId = orgEmail.orgId();
        final Row orgEmailRow = isCreateEvent ? new Row(orgEmailTable) : entityContainer.getRow(orgEmailTable, id);

        if (!isCreateEvent && Objects.isNull(orgEmailRow)) {
            throw ErrorCode.get(OrgEmailErrorCodes.ORG_EMAIL_NOT_FOUND, String.format("orgId %d Id %d", orgId, id));
        }

        if (orgEmailRow.isNewRow()) {
            orgEmail.setId(orgEmailRow.getPKValue());
            orgEmailRow.setOrgId(orgId);
            orgEmailRow.set(OMEMORGANIZATIONEMAIL.EMAIL_ID, orgEmail.getAddress().id());
        }
        orgEmailRow.set(OMEMORGANIZATIONEMAIL.NAME, orgEmail.getName());
        orgEmailRow.set(OMEMORGANIZATIONEMAIL.USER_ID, orgEmail.getUserId());

        loadRoleIntoContainer(isCreateEvent, orgEmail, entityContainer, targetContainer);
        targetContainer.updateRow(orgEmailRow);

        if (!orgEmailRow.isNewRow()) {
            removeUnmappedRoleIds(id, orgEmail.getCrmRoles().stream().map(CrmRole::id).collect(Collectors.toSet()), entityContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Removes the unmapped role ids in the database
     * </p>
     *
     * @param orgMailId       Represents the organization mail id
     * @param roleIds         Represents the collection of role ids
     * @param entityContainer Holds the user defined values of existing org mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void removeUnmappedRoleIds(final Long orgMailId, final Collection<Long> roleIds,
                                       final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table orgMailRoleTable = getOrgMailRoleTable();
        final Stream<Row> orgMailRoleRows = entityContainer.getRows(orgMailRoleTable,
                new WhereClause(Criteria.eq(orgMailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ORG_EMAIL_ID), orgMailId))
                        .and(Criteria.notIn(orgMailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ROLE_ID), roleIds)));

        orgMailRoleRows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Loads the role information into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgEmail        An entity that holds org email related attributes
     * @param entityContainer Holds the user defined values of existing org mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadRoleIntoContainer(final boolean isCreateEvent, final OrgEmail orgEmail,
                                       final DataContainer entityContainer, final DataContainer targetContainer) {
        orgEmail.getCrmRoles().forEach(crmUserRole -> {
            final long orgEmailId = orgEmail.id();
            final Table orgMailRoleTable = getOrgMailRoleTable();
            final long roleId = crmUserRole.id();
            final Row orgMailRoleRow;

            if (isCreateEvent) {
                orgMailRoleRow = new Row(orgMailRoleTable);
            } else {
                final Row oldRow = entityContainer.getRow(orgMailRoleTable,
                        new WhereClause(Criteria.eq(orgMailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ORG_EMAIL_ID), orgEmailId))
                                .and(Criteria.eq(orgMailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ROLE_ID), roleId)));
                orgMailRoleRow = Objects.isNull(oldRow) ? new Row(orgMailRoleTable) : oldRow;
            }

            if (orgMailRoleRow.isNewRow()) {
                orgMailRoleRow.set(MEMORGANIZATIONEMAILROLE.ORG_EMAIL_ID, orgEmailId);
                orgMailRoleRow.set(MEMORGANIZATIONEMAILROLE.ROLE_ID, roleId);
                targetContainer.updateRow(orgMailRoleRow);
            }
        });
    }

    /**
     * Gets the data container for org email.
     *
     * @param orgId Represents the organization id
     * @param ids   Collection of ids
     * @return {@link DataContainer}
     */
    private DataContainer getDataContainer(final long orgId, final Collection<Long> ids) {
        final Table orgEmailTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(orgEmailTable);

        buildSelectQueryForOrgEmail(selectQuery);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(orgEmailTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Converts the obtained row into {@link OrgEmail}
     * </p>
     *
     * @param row Represents the org email row
     * @return The {@link OrgEmail}
     */
    private OrgEmail convertRowToModel(final Row row, final DataContainer dataContainer) {
        final OrgEmail orgEmail = new OrgEmail();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table orgMailRoleTable = getOrgMailRoleTable();
        final Table roleTable = Table.get(OMCRMROLE.TABLE);
        final Collection<CrmRole> crmRoles = new ArrayList<>();
        final Column roleTablePKColumn = roleTable.getPKColumn();
        final long orgEmailPKValue = row.getPKValue();
        final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(),
                row.get(OMEMORGANIZATIONEMAIL.EMAIL_ID))));
        final Stream<Row> orgMailRoleRows = dataContainer.getRows(orgMailRoleTable, new WhereClause(Criteria.eq(orgMailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ORG_EMAIL_ID), orgEmailPKValue)));

        if (Objects.nonNull(emailRow)) {
            final EmailAddress address = new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

            address.setId(emailRow.getPKValue());
            orgEmail.setAddress(address);
        }
        orgEmail.setId(orgEmailPKValue);
        orgEmail.setOrgId(emailRow.getOrgId());
        orgEmail.setName(row.get(OMEMORGANIZATIONEMAIL.NAME));
        orgEmail.setVerified(row.get(OMEMORGANIZATIONEMAIL.IS_VERIFIED));

        if (Objects.nonNull(orgMailRoleRows)) {
            orgMailRoleRows.forEach(orgMailRoleRow -> {
                final CrmRole crmRole = new CrmRole();
                final Row roleRow = dataContainer.getRow(roleTable, new WhereClause(Criteria.eq(roleTablePKColumn,
                        orgMailRoleRow.get(MEMORGANIZATIONEMAILROLE.ROLE_ID))));

                crmRole.setId(roleRow.getPKValue());
                crmRole.setName(roleRow.get(OMCRMROLE.NAME));

                crmRoles.add(crmRole);
            });
        }
        orgEmail.setCrmRoles(crmRoles);

        return orgEmail;
    }

    /**
     * <p>
     * Constructs the select query to fetch the org emails
     * </p>
     *
     * @param selectQuery The {@link SelectQuery}.
     */
    private void buildSelectQueryForOrgEmail(final SelectQuery selectQuery) {
        final Table orgEmailTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table orgEmailRoleTable = getOrgMailRoleTable();
        final Table roleTable = Table.get(OMCRMROLE.TABLE);
        final Column emailIdColumn = orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID);
        final Column orgEmailIdColumn = orgEmailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ORG_EMAIL_ID);
        final Column roleIdColumn = orgEmailRoleTable.getColumn(MEMORGANIZATIONEMAILROLE.ROLE_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(emailIdColumn, emailTable.getPKColumn()),
                new JoinClause(orgEmailTable.getPKColumn(), orgEmailIdColumn), new JoinClause(roleIdColumn, roleTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailIdColumn,
                orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.NAME), orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.IS_VERIFIED),
                orgEmailIdColumn, roleIdColumn, roleTable.getColumn(OMCRMROLE.NAME), emailTable.getColumn(OMEMADDRESS.ORG_ID)));
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link OrgEmail} details for a given organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param emailIds which holds the {@link Collection} of email ids
     * @return A map containing email address as keys and corresponding organization email display name as values
     */
    public Collection<OrgEmail> getOrgEmails(final long orgId, final Collection<Long> emailIds) {
        final Table orgEmailTable = getTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Column emailAddressPkColumn = emailAddressTable.getPKColumn();
        final Column orgEmailIdColumn = orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(orgEmailTable);

        selectQuery.addSelectClause(List.of(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.NAME), orgEmailIdColumn,
                emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS)));
        selectQuery.addJoinClause(new JoinClause(orgEmailIdColumn, emailAddressPkColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.IS_VERIFIED),
                true)).and(Criteria.in(orgEmailTable.getColumn(OMEMORGANIZATIONEMAIL.EMAIL_ID), emailIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<OrgEmail> orgEmails = new ArrayList<>();

        if (!dataContainer.isEmpty()) {
            dataContainer.getRows(orgEmailTable).forEach(row -> {
                final long emailId = row.get(OMEMORGANIZATIONEMAIL.EMAIL_ID);
                final Row emailRow = dataContainer.getRow(emailAddressTable, new WhereClause(Criteria.eq(emailAddressPkColumn, emailId)));

                if (Objects.nonNull(emailRow)) {
                    final OrgEmail orgEmail = new OrgEmail();

                    orgEmail.setAddress(new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS)));
                    orgEmail.setName(row.get(OMEMORGANIZATIONEMAIL.NAME));
                    orgEmails.add(orgEmail);
                }
            });
        }

        return orgEmails;
    }

    /**
     * <p>
     * Fetches the email id of the provided email address
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param emailId Represents the user provided email address
     * @return {@link Long}
     */
    public Long getEmailId(final Long orgId, final String emailId) {
        final Table emailTable = mailManager.getEmailAddressTable();

        return isEmailIdExists(orgId, emailId) ? orgDataStore(orgId).get(emailTable,
                new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailId)),
                List.of(emailTable.getPKColumn())).getRow(emailTable).getPKValue() : null;
    }

    /**
     * <p>
     * Checks the existence of email address provided
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param emailAddress Represents the email address
     * @return {@link Boolean} existence of email address
     */
    private boolean isEmailIdExists(final Long orgId, final String emailAddress) {
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final CountQuery countQuery = CountQuery.get(emailAddressTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}