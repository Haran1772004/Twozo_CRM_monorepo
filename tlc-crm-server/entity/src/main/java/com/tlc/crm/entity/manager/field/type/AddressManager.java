package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.l10n.model.country.Country;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.fields.sql.resource.OMETADDRESS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Address} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class AddressManager extends AbstractConfigManager<Address> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddressManager.class);

    private static class Instance {
        private static final AddressManager INSTANCE = new AddressManager();
    }

    private AddressManager() {

    }

    /**
     * Gets the instance of {@link AddressManager}
     *
     * @return {@link AddressManager}
     */
    public static AddressManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETADDRESS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of address IDS
     * @return {@link Collection<Address>}
     */
    @Override
    public Collection<Address> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.ADDRESS_NOT_FOUND, "i18n.entity.field.type.address.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of Address IDs
     * @return
     */
    @Override
    public Collection<Address> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param address which contains the address data
     * @return {@link Address} which contains the address data
     */
    @Override
    public Address create(final Address address) {
        final DataContainer targetContainer = DataContainer.create();

        loadAddressIntoContainer(true, address, targetContainer);
        orgDataStore(address.orgId()).commitChanges(targetContainer);

        return address;
    }

    /**
     * <p>
     * Loads the address details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param address         An entity that holds the address attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadAddressIntoContainer(final boolean isCreateEvent, final Address address, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(address.orgId()).get(getTable(), List.of(address.id()));

        loadAddressIntoContainer(isCreateEvent, address, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the address details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param address         An entity that holds the address attributes
     * @param sourceContainer Holds the user defined and system field values of existing address
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadAddressIntoContainer(final boolean isCreateEvent, final Address address,
                                          final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table addressTable = getTable();
        final Long id = address.id();
        final Row addressRow = isCreateEvent ? new Row(addressTable) : sourceContainer.getRow(addressTable, id);

        if (!isCreateEvent && Objects.isNull(addressRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (addressRow.isNewRow()) {
            addressRow.setOrgId(address.orgId());
            address.setId(addressRow.getPKValue());
        }
        final Country country = address.getCountry();

        addressRow.set(OMETADDRESS.ADDRESS_LINE1, address.getAddressLine1());
        addressRow.set(OMETADDRESS.ADDRESS_LINE2, address.getAddressLine2());
        addressRow.set(OMETADDRESS.CITY, address.getCity());
        addressRow.set(OMETADDRESS.ZIPCODE, address.getZipcode());
        addressRow.set(OMETADDRESS.LATITUDE, address.getLatitude());
        addressRow.set(OMETADDRESS.LONGITUDE, address.getLongitude());
        addressRow.set(OMETADDRESS.STATE, address.getState());
        addressRow.set(OMETADDRESS.COUNTRY, Objects.isNull(country) ? null : country.getId());
        targetContainer.updateRow(addressRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     represents the Organisation ID
     * @param addresses represents the {@link Collection<Address>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Address> addresses) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = addresses.stream().map(Address::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        addresses.forEach(address -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadAddressIntoContainer(false, address, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(address.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Address update failed Org ID : {} ID : {} reason : {}", orgId, address.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed")));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(EntityErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link Address}
     * </p>
     *
     * @param row Address row to be converted
     * @return {@link Address}
     */
    public Address convertRowToModel(final Row row) {
        final Address address = new Address();
        final Integer country = row.get(OMETADDRESS.COUNTRY);

        address.setId(row.getPKValue());
        address.setOrgId(row.getOrgId());
        address.setCity(row.get(OMETADDRESS.CITY));
        address.setState(row.get(OMETADDRESS.STATE));
        address.setAddressLine1(row.get(OMETADDRESS.ADDRESS_LINE1));
        address.setAddressLine2(row.get(OMETADDRESS.ADDRESS_LINE2));
        address.setZipcode(row.get(OMETADDRESS.ZIPCODE));
        address.setLatitude(row.get(OMETADDRESS.LATITUDE));
        address.setLongitude(row.get(OMETADDRESS.LONGITUDE));

        if (Objects.nonNull(country)) {
            address.setCountry(Country.getCountryById(country));
        }

        return address;
    }

    /**
     * Converts partial row into {@link Address}
     *
     * @param addressRow Represents address row
     * @return {@link Address}
     */
    public Address convertRowToPartialModel(final Row addressRow) {
        final Address address = new Address();

        address.setId(addressRow.getPKValue());
        address.setOrgId(addressRow.getOrgId());
        address.setAddressLine1(addressRow.get(OMETADDRESS.ADDRESS_LINE1));
        address.setAddressLine2(addressRow.get(OMETADDRESS.ADDRESS_LINE2));

        return address;
    }
}