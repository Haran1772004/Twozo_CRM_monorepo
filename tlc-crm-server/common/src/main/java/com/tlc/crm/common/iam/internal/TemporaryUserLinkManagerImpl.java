package com.tlc.crm.common.iam.internal;

import com.tlc.crm.common.iam.api.manager.TemporaryUserLinkManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.link.user.ForgotPasswordLink;
import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.models.link.user.UserInvitationLink;
import com.tlc.crm.common.sql.resource.OMCRMTRANSIENTUSERLINKDETAILS;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Optional;

/**
 * <p>
 * A {@link TemporaryUserLinkManagerImpl} that provides the various methods for the {@link TemporaryUserLinkManager}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public final class TemporaryUserLinkManagerImpl implements TemporaryUserLinkManager {

    private final UserService userService;

    private TemporaryUserLinkManagerImpl() {
        this.userService = UserService.getInstance();
    }

    private static final class Instance {
        private static final TemporaryUserLinkManagerImpl TEMPORARY_USER_LINK_MANAGER = new TemporaryUserLinkManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link TemporaryUserLink}
     * </p>
     *
     * @return {@link TemporaryUserLinkManagerImpl}
     */
    public static TemporaryUserLinkManagerImpl getInstance() {
        return Instance.TEMPORARY_USER_LINK_MANAGER;
    }

    /**
     * <p>
     * Gets the org data store
     * </P>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the temporary user link table.
     * </p>
     *
     * @return The {@link Table} of temporary user link table
     */
    private Table getTemporaryUserLinkDetailsTable() {
        return Table.get(OMCRMTRANSIENTUSERLINKDETAILS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param temporaryUserLink Represents the {@link TemporaryUserLinkManager} details
     */
    @Override
    public void save(final TemporaryUserLink temporaryUserLink) {
        final long orgId = temporaryUserLink.orgId();

        getOrgDataStore(orgId).addRow(buildTemporaryUserLinkRow(orgId, temporaryUserLink));
    }

    /**
     * <p>
     * Loads the temporary user link details into data container
     * </p>
     *
     * @param temporaryUserLink Represents the {@link TemporaryUserLink} details
     * @param targetContainer  Represents the target {@link DataContainer} to load the temporary user link details
     */
    public void loadTemporaryLinkIntoContainer(final TemporaryUserLink temporaryUserLink,
                                               final DataContainer targetContainer) {
        targetContainer.updateRow(buildTemporaryUserLinkRow(temporaryUserLink.orgId(), temporaryUserLink));
    }

    /**
     * <p>
     * Converts the {@link TemporaryUserLink} details into {@link Row}
     * </p>
     *
     * @param temporaryUserLink Represents the {@link TemporaryUserLink} details
     * @return The {@link Row} of temporary user link details
     */
    public Row buildTemporaryUserLinkRow(final long orgId, final TemporaryUserLink temporaryUserLink) {
        final Row row = new Row(getTemporaryUserLinkDetailsTable());

        row.setOrgId(orgId);
        row.set(OMCRMTRANSIENTUSERLINKDETAILS.USER_ID, temporaryUserLink.getUser().id());
        row.set(OMCRMTRANSIENTUSERLINKDETAILS.EXPIRATION_TIME, temporaryUserLink.getExpireTime());
        row.set(OMCRMTRANSIENTUSERLINKDETAILS.CATEGORY, temporaryUserLink.getCategory().getId());
        temporaryUserLink.setId(row.getPKValue());

        return row;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param expiryLinkId Represents the link expiry id
     * @return The {@link TemporaryUserLink} details
     */
    @Override
    public Optional<TemporaryUserLink> get(final long orgId, final long expiryLinkId) {
        final Table table = getTemporaryUserLinkDetailsTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), expiryLinkId)));

        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Optional.ofNullable(row).map(this::convertRowToModel);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link  TemporaryUserLink} details
     */
    @Override
    public Optional<TemporaryUserLink> get(final long orgId, final long userId, final TemporaryUserLink.Category category) {
        final Table table = getTemporaryUserLinkDetailsTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCRMTRANSIENTUSERLINKDETAILS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCRMTRANSIENTUSERLINKDETAILS.CATEGORY), category.getId())));
        selectQuery.setLimitClause(new LimitClause(1));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn(), OrderByClause.OrderType.DESCENDING));

        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Optional.ofNullable(row).map(this::convertRowToModel);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param expiryLinkId Represents the expiry link id
     */
    @Override
    public void delete(final long orgId, final long expiryLinkId) {
        final Table table = getTemporaryUserLinkDetailsTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), expiryLinkId)));

        getOrgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Converts user link expiry log row into {@link TemporaryUserLink} details
     * </p>
     *
     * @param row Represents the {@link TemporaryUserLink} row
     * @return The {@link TemporaryUserLink} details
     */
    private TemporaryUserLink convertRowToModel(final Row row) {
        final TemporaryUserLink.Category category = TemporaryUserLink.Category.get(row.get(OMCRMTRANSIENTUSERLINKDETAILS.CATEGORY));
        final TemporaryUserLink temporaryUserLink = switch (category) {
            case INVITE -> new UserInvitationLink();
            case FORGOT_PASSWORD -> new ForgotPasswordLink();
        };
        final Long orgId = row.getOrgId();

        temporaryUserLink.setUser(userService.getUser(orgId, row.get(OMCRMTRANSIENTUSERLINKDETAILS.USER_ID)));
        temporaryUserLink.setOrgId(orgId);
        temporaryUserLink.setExpireTime(row.get(OMCRMTRANSIENTUSERLINKDETAILS.EXPIRATION_TIME));
        temporaryUserLink.setId(row.getPKValue());
        temporaryUserLink.setCategory(category);

        return temporaryUserLink;
    }
}
