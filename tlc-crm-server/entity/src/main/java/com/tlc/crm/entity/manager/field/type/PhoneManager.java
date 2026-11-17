package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.l10n.model.country.Country;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.Phone.Type;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Phone} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class PhoneManager extends AbstractConfigManager<Phone> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhoneManager.class);

    private static class Instance {
        private static final PhoneManager INSTANCE = new PhoneManager();
    }

    private PhoneManager() {

    }

    /**
     * Gets the instance of {@link PhoneManager}
     *
     * @return {@link PhoneManager}
     */
    public static PhoneManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETPHONE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of phone IDS
     * @return {@link Collection<Phone>}
     */
    @Override
    public Collection<Phone> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.PHONE_NUMBER_NOT_FOUND, "i18n.entity.field.phone.number.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of phone IDs
     * @return
     */
    @Override
    public Collection<Phone> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param phone which contains the phone data
     * @return {@link Phone} which contains the phone data
     */
    @Override
    public Phone create(final Phone phone) {
        final DataContainer targetContainer = DataContainer.create();

        loadPhoneIntoContainer(true, phone, targetContainer);
        orgDataStore(phone.orgId()).commitChanges(targetContainer);

        return phone;
    }

    /**
     * <p>
     * Loads the phone details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param phone           An entity that holds the phone attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPhoneIntoContainer(final boolean isCreateEvent, final Phone phone, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(phone.orgId()).get(getTable(), List.of(phone.id()));

        loadPhoneIntoContainer(isCreateEvent, phone, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the phone details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param phone           An entity that holds the phone attributes
     * @param sourceContainer Holds the user defined and system field values of existing phone
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPhoneIntoContainer(final boolean isCreateEvent, final Phone phone,
                                       final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table phoneTable = getTable();
        final Long orgId = phone.orgId();
        final WhereClause phoneCriteria = new WhereClause(Criteria.eq(phoneTable.getColumn(OMETPHONE.NUMBER), phone.getNumber()));
        final Optional<Row> optionalRow = Optional.ofNullable(isCreateEvent ? null : sourceContainer.getRow(phoneTable, phoneCriteria))
                .or(() -> Optional.ofNullable(orgDataStore(orgId).get(phoneTable, phoneCriteria).getRow(phoneTable)));

        final Row phoneRow = isCreateEvent && optionalRow.isEmpty() ? new Row(phoneTable) : optionalRow.orElseGet(() -> new Row(phoneTable));
        final Country country = phone.getCountry();

        if (phoneRow.isNewRow()) {
            phoneRow.setOrgId(phone.orgId());
        }
        phoneRow.set(OMETPHONE.TYPE, phone.getType().getId());
        phoneRow.set(OMETPHONE.NUMBER, phone.getNumber());
        phone.setId(phoneRow.getPKValue());

        if (Objects.nonNull(country)) {
            phoneRow.set(OMETPHONE.COUNTRY_ID, country.getId());
        }
        targetContainer.updateRow(phoneRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  represents the Organisation ID
     * @param phones represents the {@link Collection<Phone>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Phone> phones) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = phones.stream().map(Phone::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        phones.forEach(phone -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadPhoneIntoContainer(false, phone, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(phone.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Phone update failed Org ID : {} ID : {} reason : {}", orgId, phone.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed")));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link Phone}
     * </p>
     *
     * @param row Phone row to be converted
     * @return {@link Phone}
     */
    public Phone convertRowToModel(final Row row) {
        final Phone phone = new Phone();
        final Integer country = row.get(OMETPHONE.COUNTRY_ID);
        final Integer type = row.get(OMETPHONE.TYPE);

        phone.setId(row.getPKValue());
        phone.setOrgId(row.getOrgId());
        phone.setNumber(row.get(OMETPHONE.NUMBER));

        if (Objects.nonNull(country)) {
            phone.setCountry(Country.getCountryById(country));
        }

        if (Objects.nonNull(type)) {
            phone.setType(Type.valueOf(type));
        }

        return phone;
    }

    /**
     * <p>
     * Check {@link Phone} exists using orgId, phoneNumber
     * </p>
     *
     * @param orgId         represents the Organisation ID
     * @param phoneNumber   represents the phone number
     * @return {@link Collection<Phone>}
     */
    public boolean exists(final Long orgId, final String phoneNumber) {
        final Table phoneTable = getTable();
        final CountQuery countQuery = CountQuery.get(phoneTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(phoneTable.getColumn(OMETPHONE.NUMBER), phoneNumber)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Get {@link Phone} using orgId, phoneNumber
     * </p>
     *
     * @param orgId         represents the Organisation ID
     * @param phoneNumber   represents the phone number
     * @return {@link Collection<Phone>}
     */
    public Phone get(final Long orgId, final String phoneNumber) {
        final Table phoneTable = getTable();
        final WhereClause phoneCriteria = new WhereClause(Criteria.eq(phoneTable.getColumn(OMETPHONE.NUMBER), phoneNumber));
        final Row row = orgDataStore(orgId).get(phoneTable, phoneCriteria).getRow(phoneTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(EntityErrorCodes.PHONE_NUMBER_NOT_FOUND, "i18n.entity.field.phone.number.not.found");
        }

        return convertRowToModel(row);
    }
}