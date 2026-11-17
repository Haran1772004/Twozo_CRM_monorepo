package com.tlc.crm.core.features.action.call.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.action.call.manager.FavouriteContactManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.action.call.model.FavouriteContact;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.OMNCFAVOURITECONTACT;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Manages persistence and retrieval of {@link FavouriteContact}
 *
 * @author kishore J
 * @version 1.0
 */
public final class FavouriteContactManagerImpl extends AbstractConfigManager<FavouriteContact> implements FavouriteContactManager {

    private final ContactManager contactManager;
    private final PhoneManager phoneManager;

    private FavouriteContactManagerImpl() {
        this.contactManager = ContactManager.getInstance();
        this.phoneManager = PhoneManager.getInstance();
    }

    private static class InstanceHolder {
        private static final FavouriteContactManagerImpl INSTANCE = new FavouriteContactManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FavouriteContactManagerImpl}.
     * </p>
     *
     * @return The instance of {@link FavouriteContactManagerImpl}.
     */
    public static FavouriteContactManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table} backing the favourite contact entity.
     */
    @Override
    public Table getTable() {
        return Table.get(OMNCFAVOURITECONTACT.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organisation id.
     * @param favouriteContacts Holds the {@link FavouriteContact} to be updated.
     * @return The {@link ProcessOutcome} of the update operation.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<FavouriteContact> favouriteContacts) {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Collection of favourite contact ids to be fetched.
     * @return Collection of {@link FavouriteContact}.
     */
    @Override
    public Collection<FavouriteContact> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table)
                .map(row -> {
                    final FavouriteContact favouriteContact = new FavouriteContact();

                    favouriteContact.setOrgId(orgId);
                    favouriteContact.setUserId(row.get(OMNCFAVOURITECONTACT.USER_ID));
                    favouriteContact.setId(row.getPKValue());

                    return favouriteContact;
                })
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Collection of favourite contact ids to be partially fetched.
     * @return Collection of partially loaded {@link FavouriteContact}.
     */
    @Override
    public Collection<FavouriteContact> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * Creates a favourite contact entry for the given user and contact, if it does not
     * already exist.
     *
     * @param favouriteContact The {@link FavouriteContact} to be persisted.
     * @return The persisted {@link FavouriteContact} (with id populated when newly inserted).
     */
    @Override
    public FavouriteContact create(final FavouriteContact favouriteContact) {
        final Long orgId = favouriteContact.orgId();
        final Long userId = favouriteContact.getUserId();
        final Long contactId = favouriteContact.getContact().id();
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMNCFAVOURITECONTACT.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMNCFAVOURITECONTACT.CONTACT_ID), contactId)));

        if (orgDataStore.get(countQuery) > NumericalConstant.ZERO) {
            return favouriteContact;
        }
        final DataContainer targetContainer = DataContainer.create();
        final Row row = new Row(table);

        row.setOrgId(orgId);
        row.set(OMNCFAVOURITECONTACT.USER_ID, userId);
        row.set(OMNCFAVOURITECONTACT.CONTACT_ID, contactId);
        favouriteContact.setId(row.getPKValue());

        targetContainer.addNewRow(row);
        orgDataStore.commitChanges(targetContainer);

        return favouriteContact;
    }

    /**
     * Returns the total number of favourite contacts marked by the given user.
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return The count of favourite contacts for the user.
     */
    @Override
    public long getTotalRecords(final long orgId, final long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMNCFAVOURITECONTACT.USER_ID), userId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Fetches the user's favourite contacts in a single database hit by joining the
     * {@code OMCOFavouriteContact} table with the {@code MCOContact} table.
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id whose favourites are to be loaded.
     * @param limitConfig Optional pagination configuration; when {@code null} no limit is applied.
     * @return Collection of {@link FavouriteContact} populated with basic contact details.
     */
    @Override
    public Collection<FavouriteContact> get(final long orgId, final long userId, final LimitConfig limitConfig) {
        final Table table = getTable();
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.FIRST_NAME));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.LAST_NAME));
        selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE));
        selectQuery.addSelectClause(phoneTable.getColumn(OMETPHONE.NUMBER));
        selectQuery.addSelectClause(phoneTable.getColumn(OMETPHONE.COUNTRY_ID));
        selectQuery.addJoinClause(new JoinClause(table.getColumn(OMNCFAVOURITECONTACT.CONTACT_ID),
                contactTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                phoneTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMNCFAVOURITECONTACT.USER_ID), userId)));

        if (Objects.nonNull(limitConfig)) {
            selectQuery.setLimitClause(new LimitClause(limitConfig.start(), limitConfig.limit()));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table)
                .map(row -> {
                    final Row contactRow = dataContainer.getRow(contactTable, (Long) row.get(OMNCFAVOURITECONTACT.CONTACT_ID));
                    final FavouriteContact favouriteContact = new FavouriteContact();

                    if (Objects.nonNull(contactRow)) {
                        final Contact contact = new Contact();

                        contact.setId(contactRow.getPKValue());
                        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                        final Long phoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

                        if (Objects.nonNull(phoneId)) {
                            final Row phoneRow = dataContainer.getRow(phoneTable, phoneId);

                            if (Objects.nonNull(phoneRow)) {
                                contact.setPrimaryPhone(phoneManager.convertRowToModel(phoneRow));
                            }
                        }
                        favouriteContact.setContact(contact);
                    }
                    favouriteContact.setUserId(userId);
                    favouriteContact.setOrgId(orgId);
                    favouriteContact.setId(row.getPKValue());

                    return favouriteContact;
                })
                .toList();
    }
}
