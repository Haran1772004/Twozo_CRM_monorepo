package com.tlc.crm.notifications.api.call.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.RecordType;
import com.tlc.crm.notifications.api.call.model.State;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.sql.resource.MNCCOUNTRY;
import com.tlc.crm.notifications.sql.resource.MNCLOCALITY;
import com.tlc.crm.notifications.sql.resource.MNCSTATE;
import com.tlc.crm.notifications.sql.resource.OMNCPHONENUMBER;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Implementation of PhoneNumberManager responsible for managing phone numbers,
 * including purchasing, updating, deleting, and retrieving numbers.
 *
 * This class follows the Singleton design pattern and interacts with
 * the database and external services (e.g., VoiceCallService) to manage
 * phone number records.
 */
public class PhoneNumberManagerImpl implements PhoneNumberManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhoneNumberManagerImpl.class);
    private static final String EMPTY_SPACE = " ";
    private static final String EMPTY_STRING = "";

    private final VoiceCallService voiceCallService;
    private final AdminDataStore adminDataStore;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes dependencies using service providers.
     */
    private PhoneNumberManagerImpl() {
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
        adminDataStore = DataStoreProvider.getAdminDataStore();
    }

    /**
     * Singleton instance holder to ensure thread-safe lazy initialization.
     */
    private static final class InstanceHolder {
        private static final PhoneNumberManagerImpl PHONE_NUMBER_MANAGER = new PhoneNumberManagerImpl();
    }

    /**
     * Provides the singleton instance of PhoneNumberManagerImpl.
     *
     * @return Singleton instance of PhoneNumberManagerImpl.
     */
    public static PhoneNumberManagerImpl getInstance() {
        return InstanceHolder.PHONE_NUMBER_MANAGER;
    }

    /**
     * Returns the table object for the phone number data.
     *
     * @return Table reference for OMNCPHONENUMBER.
     */
    @Override
    public Table getTable() {
        return Table.get(OMNCPHONENUMBER.TABLE);
    }

    /**
     * Returns the table object for country data.
     *
     * @return Table reference for MNCCOUNTRY.
     */
    private Table getCountryTable() {
        return Table.get(MNCCOUNTRY.TABLE);
    }

    /**
     * Returns the table object for state data.
     *
     * @return Table reference for MNCSTATE.
     */
    private Table getStateTable() {
        return Table.get(MNCSTATE.TABLE);
    }

    /**
     * Returns the table object for locality data.
     *
     * @return Table reference for MNCLOCALITY.
     */
    private Table getLocalityTable() {
        return Table.get(MNCLOCALITY.TABLE);
    }

    /**
     * Retrieves an organization-specific data store.
     *
     * @param orgId Organization ID.
     * @return OrgDataStore instance.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Retrieves an organization-specific secure data store.
     *
     * @param orgId Organization ID.
     * @return Secure OrgDataStore instance.
     */
    private OrgDataStore secureOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Purchases a phone number and stores it in the database.
     *
     * @param phoneNumber PhoneNumber object containing details of the number to be purchased.
     */
    @Override
    public void buyNumber(final PhoneNumber phoneNumber) {
        final Long orgId = phoneNumber.orgId();
        final MobileNumber mobileNumber = buyMobileNumber(phoneNumber);
        final DataContainer targetContainer = DataContainer.create();
        final Row numberRow = new Row(getTable());

        loadPhoneNumberIntoRow(true, phoneNumber, numberRow);
        numberRow.set(OMNCPHONENUMBER.SERVICE_ID, mobileNumber.getServiceId());
        targetContainer.addNewRow(numberRow);

        try {
            secureOrgDataStore(orgId).commitChanges(targetContainer);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unable to store the purchased number into DB");
            throw ErrorCode.get(NotificationErrorCodes.FAILED_TO_STORE_PURCHASED_NUMBER, "i18n.notifications.phone.number.create.failed");
        }
    }

    /**
     * Updates phone number details in the database and external service.
     *
     * @param phoneNumber PhoneNumber object with updated details.
     */
    @Override
    public void updateNumberDetails(final PhoneNumber phoneNumber) {
        final Long orgId = phoneNumber.orgId();
        final Table numberTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(numberTable.getPKColumn(), phoneNumber.id()));
        final DataContainer sourceContainer = secureOrgDataStore(orgId).get(numberTable, whereClause);
        final DataContainer targetContainer = DataContainer.create();
        final Row numberRow = sourceContainer.getRow(numberTable);
        final MobileNumber mobileNumber = new MobileNumber(phoneNumber.getNumber());

        mobileNumber.setServiceId(numberRow.get(OMNCPHONENUMBER.SERVICE_ID));
        mobileNumber.setFriendlyName(phoneNumber.getDisplayName());
        voiceCallService.updateMobileNumberName(mobileNumber);

        loadPhoneNumberIntoRow(false, phoneNumber, numberRow);
        targetContainer.updateRow(numberRow);

        try {
            secureOrgDataStore(orgId).commitChanges(targetContainer);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unable to update the phone number in DB");
            throw ErrorCode.get(NotificationErrorCodes.FAILED_TO_UPDATE_NUMBER, "i18n.notifications.phone.number.update.failed");
        }
    }

    /**
     * Deletes a single phone number by ID.
     *
     * @param orgId Organization ID.
     * @param id Phone number ID.
     */
    @Override
    public void deleteNumber(final long orgId, final long id) {
        deleteNumbers(orgId, Set.of(id));
    }

    /**
     * Deletes multiple phone numbers by IDs.
     *
     * @param orgId Organization ID.
     * @param ids Collection of phone number IDs to delete.
     */
    @Override
    public void deleteNumbers(final long orgId, final Collection<Long> ids) {
        final Table numberTable = getTable();
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(numberTable, ids);

        try {
            secureOrgDataStore(orgId).delete(numberTable, ids);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unable to delete the phone number from DB");
            throw ErrorCode.get(NotificationErrorCodes.FAILED_TO_DELETE_NUMBER, "i18n.notifications.phone.number.delete.failed");
        }
        dataContainer.getRows(numberTable).forEach(numberRow -> {
            final MobileNumber mobileNumber = new MobileNumber(numberRow.get(OMNCPHONENUMBER.NUMBER));

            mobileNumber.setServiceId(numberRow.get(OMNCPHONENUMBER.SERVICE_ID));
            voiceCallService.deleteNumber(mobileNumber);
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param number Represents the number.
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    @Override
    public Optional<PhoneNumber> getNumber(final String number) {
        return getNumber(number, adminDataStore);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an org id
     * @param number Represents the number.
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    @Override
    public Optional<PhoneNumber> getNumber(final long orgId, final String number) {
        return getNumber(number, orgDataStore(orgId));
    }

    /**
     * Retrieves a phone number by ID.
     *
     * @param orgId Organization ID.
     * @param id Phone number ID.
     * @return PhoneNumber object.
     */
    @Override
    public PhoneNumber getNumber(final long orgId, final long id) {
        final Table numberTable = getTable();
        final Table countryTable = getCountryTable();
        final Table stateTable = getStateTable();
        final Table localityTable = getLocalityTable();
        final SelectQuery selectQuery = SelectQuery.get(numberTable);

        selectQuery.addJoinClause(new JoinClause(numberTable.getColumn(OMNCPHONENUMBER.LOCATION_ID), localityTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(localityTable.getColumn(MNCLOCALITY.STATE_ID), stateTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(localityTable.getColumn(MNCLOCALITY.COUNTRY_ID), countryTable.getPKColumn()));
        selectQuery.setWhereClause(buildDefaultProviderCriteria().and(Criteria.eq(numberTable.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row numberRow = dataContainer.getRow(numberTable);

        return convertRowToModel(numberRow, dataContainer);
    }

    /**
     * Retrieves a collection of phone numbers based on search criteria.
     *
     * @param orgId Organization ID.
     * @param searchText Search text for filtering.
     * @param limitConfig Pagination details.
     * @param sortConfig Sorting details.
     * @return Collection of PhoneNumber objects.
     */
    @Override
    public Collection<PhoneNumber> getNumbers(final long orgId, final String searchText, final LimitConfig limitConfig,
                                              final SortConfig sortConfig) {
        final Table numberTable = getTable();
        final Table countryTable = getCountryTable();
        final Table stateTable = getStateTable();
        final Table localityTable = getLocalityTable();
        final SelectQuery selectQuery = SelectQuery.get(numberTable);
        final WhereClause providerCriteria = buildDefaultProviderCriteria();

        selectQuery.addJoinClause(new JoinClause(numberTable.getColumn(OMNCPHONENUMBER.LOCATION_ID), localityTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(localityTable.getColumn(MNCLOCALITY.STATE_ID), stateTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(localityTable.getColumn(MNCLOCALITY.COUNTRY_ID), countryTable.getPKColumn()));
        selectQuery.addOrderByClause(new OrderByClause(getSortColumn(sortConfig.fieldName()), sortConfig.type().getOrderType()));
        selectQuery.setLimitClause(new LimitClause(limitConfig.start(), limitConfig.limit()));

        selectQuery.setWhereClause(Objects.isNull(searchText) ? providerCriteria : WhereClauseUtil.connect(providerCriteria, new WhereClause(Criteria.contains(numberTable.getColumn(OMNCPHONENUMBER.DISPLAY_NAME), searchText))
                .or(Criteria.contains(numberTable.getColumn(OMNCPHONENUMBER.NUMBER), searchText)), Connector.AND));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Stream<Row> numberRows = dataContainer.getRows(numberTable);

        return numberRows.map(numberRow -> convertRowToModel(numberRow, dataContainer)).toList();
    }

    /**
     * Retrieves the total count of phone numbers in an organization that match the given search text.
     *
     * @param orgId            The ID of the organization.
     * @param dropdownCriteria The search keyword to filter phone numbers.
     * @return The total count of matching phone numbers.
     */
    @Override
    public long getTotalNumbersCount(final long orgId, final DropdownCriteria dropdownCriteria, final TelephoneProvider telephoneProvider) {
        final Table numberTable = getTable();
        final CountQuery countQuery = CountQuery.get(numberTable);
        final WhereClause providerCriteria = new WhereClause(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), true))
                .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.PROVIDER), telephoneProvider.getId()));

        countQuery.setWhereClause(providerCriteria.and(dropdownCriteria.build(numberTable.getColumn(OMNCPHONENUMBER.DISPLAY_NAME)))
                .or(dropdownCriteria.build(numberTable.getColumn(OMNCPHONENUMBER.NUMBER))));
        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves a collection of phone numbers for the dropdown list with pagination.
     *
     * @param orgId            The organization ID.
     * @param dropdownCriteria The criteria for filtering results.
     * @param dropdownLimit    The pagination details, including start index and limit.
     * @return A collection of {@link PhoneNumber} objects matching the criteria.
     */
    @Override
    public Collection<PhoneNumber> getNumberDropdownList(final long orgId, final DropdownCriteria dropdownCriteria,
                                                         final DropdownLimit dropdownLimit, final TelephoneProvider telephoneProvider) {
        final Table numberTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(numberTable);
        final WhereClause providerCriteria = new WhereClause(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), true))
                .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.PROVIDER), telephoneProvider.getId()));

        selectQuery.addOrderByClause(new OrderByClause(numberTable.getPKColumn()));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        selectQuery.setWhereClause(providerCriteria.and(new WhereClause(dropdownCriteria.build(numberTable.getColumn(OMNCPHONENUMBER.DISPLAY_NAME)))
                .or(dropdownCriteria.build(numberTable.getColumn(OMNCPHONENUMBER.NUMBER)))));

        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Stream<Row> numberRows = dataContainer.getRows(numberTable);

        return numberRows.map(numberRow -> convertRowToModel(numberRow, dataContainer)).toList();
    }

    /**
     * Checks if a phone number exists.
     *
     * @param orgId Organization ID.
     * @param id Phone number ID.
     * @return true if exists, false otherwise.
     */
    @Override
    public boolean exists(final long orgId, final long id) {
        final Table numberTable = getTable();
        final CountQuery countQuery = CountQuery.get(numberTable);

        countQuery.setWhereClause(buildDefaultProviderCriteria().and(Criteria.eq(numberTable.getPKColumn(), id)));
        return 0 < secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Stores a collection of locations (countries, states, and localities) into the database.
     *
     * @param localities The collection of {@link Locality} objects to be stored.
     */
    @Override
    public void storeLocations(final Collection<Locality> localities) {
        final Collection<String> countryNames = new HashSet<>();
        final Collection<String> stateNames = new HashSet<>();
        final Collection<String> localityNames = new HashSet<>();

        for (final Locality locality : localities) {
            final String countryName = locality.getCountry().getName();
            final String stateName = locality.getState().getName();
            final String localityName = locality.getName();

            if (Objects.nonNull(countryName)) {
                countryNames.add(countryName);
            }

            if (Objects.nonNull(stateName)) {
                stateNames.add(stateName);
            }
            if (Objects.nonNull(localityName)) {
                localityNames.add(localityName);
            }
        }
        final Table countryTable = getCountryTable();
        final Table stateTable = getStateTable();
        final Table localityTable = getLocalityTable();
        final Column countryNameColumn = countryTable.getColumn(MNCCOUNTRY.NAME);
        final Column stateNameColumn = stateTable.getColumn(MNCSTATE.NAME);
        final Column countryIdColumn = stateTable.getColumn(MNCSTATE.COUNTRY_ID);
        final Column localityNameColumn = localityTable.getColumn(MNCLOCALITY.NAME);
        final Column stateIdColumn = localityTable.getColumn(MNCLOCALITY.STATE_ID);
        final DataContainer dataContainer = fetchLocationContainer(countryNames, stateNames, localityNames);
        final DataContainer targetContainer = DataContainer.create();
        dataContainer.indexRows(countryNameColumn);
        dataContainer.indexRows(stateNameColumn);
        dataContainer.indexRows(localityNameColumn);

        for (final Locality locality : localities) {
            final Country country = locality.getCountry();
            final State state = locality.getState();
            final String countryName = Objects.isNull(country) ? null : country.getName();
            final String stateName = Objects.isNull(state) ? null : state.getName();
            final String localityName = locality.getName();

            if (Objects.isNull(countryName)) {
                continue;
            }
            final Row countryRow = getRow(dataContainer, countryTable, countryNameColumn, countryName, null, targetContainer);

            loadCountryRowIntoContainer(country, countryRow, targetContainer);

            if (Objects.nonNull(stateName)) {
                final Row stateRow = getRow(dataContainer, stateTable, stateNameColumn, stateName, Criteria.eq(countryIdColumn, countryRow.getPKValue()), targetContainer);

                loadStateRowIntoContainer(state, stateRow, targetContainer);

                if (Objects.nonNull(localityName)) {
                    final Row localityRow = getRow(dataContainer, localityTable, localityNameColumn, localityName, Criteria.eq(stateIdColumn, stateRow.getPKValue()), targetContainer);

                    loadLocalityRowIntoContainer(locality, localityRow, targetContainer);
                }
            }
        }
        adminDataStore.commitChanges(targetContainer);
    }

    /**
     * Retrieves a list of countries based on filtering, sorting, and pagination criteria.
     *
     * @param dropdownCriteria The filtering criteria for country names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link Country} objects matching the given criteria.
     */
    @Override
    public Collection<Country> getCountries(final DropdownCriteria dropdownCriteria, final DropdownLimit dropdownLimit) {
        final Table countryTable = getCountryTable();
        final SelectQuery selectQuery = SelectQuery.get(countryTable);

        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(countryTable.getColumn(MNCCOUNTRY.NAME))));
        selectQuery.addOrderByClause(new OrderByClause(countryTable.getColumn(MNCCOUNTRY.NAME)));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());

        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> countryRows = dataContainer.getRows(countryTable);

        return countryRows.map(this::convertRowToCountry).toList();
    }

    /**
     * Retrieves a list of states for a given country based on filtering and pagination criteria.
     *
     * @param countryId        The ID of the country to fetch states for.
     * @param dropdownCriteria The filtering criteria for state names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link State} objects matching the given criteria.
     */
    @Override
    public Collection<State> getStates(final long countryId, final DropdownCriteria dropdownCriteria,
                                       final DropdownLimit dropdownLimit) {
        final Table stateTable = getStateTable();
        final Column countryIdColumn = stateTable.getColumn(MNCSTATE.COUNTRY_ID);
        final SelectQuery selectQuery = SelectQuery.get(stateTable);

        selectQuery.addJoinClause(new JoinClause(countryIdColumn, getCountryTable().getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(countryIdColumn, countryId))
                .and(dropdownCriteria.build(stateTable.getColumn(MNCSTATE.NAME))));
        selectQuery.addOrderByClause(new OrderByClause(stateTable.getColumn(MNCSTATE.NAME)));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());

        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> stateRows = dataContainer.getRows(stateTable);

        return stateRows.map(stateRow -> convertRowToState(stateRow, dataContainer)).toList();
    }

    /**
     * Retrieves a list of localities for a given country and optional state based on filtering, sorting, and pagination criteria.
     *
     * @param countryId        The ID of the country to fetch localities for.
     * @param stateId          The optional ID of the state to fetch localities for (can be {@code null}).
     * @param dropdownCriteria The filtering criteria for locality names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link Locality} objects matching the given criteria.
     */
    @Override
    public Collection<Locality> getLocalities(final long countryId, final Long stateId, final DropdownCriteria dropdownCriteria,
                                              final DropdownLimit dropdownLimit) {
        final Table localityTable = getLocalityTable();
        final Column countryIdColumn = localityTable.getColumn(MNCLOCALITY.COUNTRY_ID);
        final Column stateIdColumn = localityTable.getColumn(MNCLOCALITY.STATE_ID);
        final SelectQuery selectQuery = SelectQuery.get(localityTable);
        final WhereClause countryCriteria = new WhereClause(Criteria.eq(countryIdColumn, countryId));
        final WhereClause locationCriteria = Objects.isNull(stateId) ? countryCriteria
                : countryCriteria.and(Criteria.eq(stateIdColumn, stateId));

        selectQuery.addJoinClause(new JoinClause(countryIdColumn, getCountryTable().getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(stateIdColumn, getStateTable().getPKColumn()));
        selectQuery.setWhereClause(locationCriteria.and(dropdownCriteria.build(localityTable.getColumn(MNCLOCALITY.NAME))));
        selectQuery.addOrderByClause(new OrderByClause(localityTable.getColumn(MNCLOCALITY.NAME)));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());

        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> localityRows = dataContainer.getRows(localityTable);

        return localityRows.map(localityRow -> convertRowToLocality(localityRow, dataContainer)).toList();
    }

    /**
     * Retrieves the total count of countries matching the given filtering criteria.
     *
     * @param dropdownCriteria The filtering criteria for country names.
     * @return The total count of matching countries.
     */
    @Override
    public long getTotalCountriesCount(final DropdownCriteria dropdownCriteria) {
        final Table countryTable = getCountryTable();
        final CountQuery countQuery = CountQuery.get(countryTable);

        countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(countryTable.getColumn(MNCCOUNTRY.NAME))));
        return adminDataStore.get(countQuery);
    }

    /**
     * Retrieves the total count of states for a given country matching the given filtering criteria.
     *
     * @param countryId        The ID of the country to count states for.
     * @param dropdownCriteria The filtering criteria for state names.
     * @return The total count of matching states in the specified country.
     */
    @Override
    public long getTotalStatesCount(final long countryId, final DropdownCriteria dropdownCriteria) {
        final Table stateTable = getStateTable();
        final CountQuery countQuery = CountQuery.get(stateTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(stateTable.getColumn(MNCSTATE.COUNTRY_ID), countryId))
                .and(dropdownCriteria.build(stateTable.getColumn(MNCSTATE.NAME))));
        return adminDataStore.get(countQuery);
    }

    /**
     * Retrieves the total count of localities for a given country and optional state matching the given filtering criteria.
     *
     * @param countryId        The ID of the country to count localities for.
     * @param stateId          The optional ID of the state to count localities for (can be {@code null}).
     * @param dropdownCriteria The filtering criteria for locality names.
     * @return The total count of matching localities in the specified country and state.
     */
    @Override
    public long getTotalLocalitiesCount(final long countryId, final Long stateId, final DropdownCriteria dropdownCriteria) {
        final Table localityTable = getLocalityTable();
        final CountQuery countQuery = CountQuery.get(localityTable);
        final WhereClause countryCriteria = new WhereClause(Criteria.eq(localityTable.getColumn(MNCLOCALITY.COUNTRY_ID), countryId));
        final WhereClause locationCriteria = Objects.isNull(stateId) ? countryCriteria
                : countryCriteria.and(Criteria.eq(localityTable.getColumn(MNCLOCALITY.STATE_ID), stateId));

        countQuery.setWhereClause(locationCriteria.and(dropdownCriteria.build(localityTable.getColumn(MNCLOCALITY.NAME))));
        return adminDataStore.get(countQuery);
    }

    /**
     * Retrieves a map of {@link LocationDetails} to {@link Locality}, creating missing records if necessary.
     *
     * @param locationDetailsList A collection of {@link LocationDetails} objects to be retrieved or created.
     * @return A map of {@link LocationDetails} to corresponding {@link Locality} objects.
     */
    @Override
    public Map<LocationDetails, Locality> getLocations(final Collection<LocationDetails> locationDetailsList) {
        final Map<LocationDetails, Locality> localityMap = new HashMap<>();
        final Table countryTable = getCountryTable();
        final Table stateTable = getStateTable();
        final Table localityTable = getLocalityTable();
        final Column countryNameColumn = countryTable.getColumn(MNCCOUNTRY.NAME);
        final Column stateNameColumn = stateTable.getColumn(MNCSTATE.NAME);
        final Column countryIdColumn = stateTable.getColumn(MNCSTATE.COUNTRY_ID);
        final Column localityNameColumn = localityTable.getColumn(MNCLOCALITY.NAME);
        final Column stateIdColumn = localityTable.getColumn(MNCLOCALITY.STATE_ID);
        final DataContainer dataContainer = fetchLocationContainer(locationDetailsList);
        final DataContainer targetContainer = DataContainer.create();

        dataContainer.indexRows(countryNameColumn);
        dataContainer.indexRows(stateNameColumn);
        dataContainer.indexRows(localityNameColumn);

        for (final LocationDetails locationDetails : locationDetailsList) {
            final String countryCode = locationDetails.getCountryCode();
            final Country country = new Country(locationDetails.getCountry(), countryCode);
            final State state = new State(locationDetails.getState());
            final Locality locality = new Locality(locationDetails.getLocality());

            state.setCountry(country);
            locality.setState(state);
            final Row countryRow = getRow(dataContainer, countryTable, countryNameColumn, country.getName(), null, targetContainer);
            final Row stateRow = getRow(dataContainer, stateTable, stateNameColumn, state.getName(),
                    Criteria.eq(countryIdColumn, countryRow.getPKValue()), targetContainer);
            final Row localityRow = getRow(dataContainer, localityTable, localityNameColumn, locality.getName(),
                    Criteria.eq(stateIdColumn, stateRow.getPKValue()), targetContainer);

            loadCountryRowIntoContainer(country, countryRow, targetContainer);
            loadStateRowIntoContainer(state, stateRow, targetContainer);
            loadLocalityRowIntoContainer(locality, localityRow, targetContainer);
            localityMap.put(locationDetails, locality);
        }
        adminDataStore.commitChanges(targetContainer);
        return localityMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param numberRow     The row containing phone number data.
     * @param dataContainer The container holding all related data.
     * @return A PhoneNumber object populated with data from the row.
     */
    @Override
    public PhoneNumber convertRowToModel(final Row numberRow, final DataContainer dataContainer) {
        final PhoneNumber phoneNumber = new PhoneNumber();
        final Short recordType = numberRow.get(OMNCPHONENUMBER.RECORD_TYPE);
        final Row localityRow = dataContainer.getRow(getLocalityTable(), (Long) numberRow.get(OMNCPHONENUMBER.LOCATION_ID));

        phoneNumber.setId(numberRow.getPKValue());
        phoneNumber.setOrgId(numberRow.getOrgId());
        phoneNumber.setDisplayName(numberRow.get(OMNCPHONENUMBER.DISPLAY_NAME));
        phoneNumber.setNumber(numberRow.get(OMNCPHONENUMBER.NUMBER));

        if (Objects.nonNull(recordType)) {
            phoneNumber.setRecordType(RecordType.get(recordType));
        }

        if (Objects.nonNull(localityRow)) {
            phoneNumber.setLocality(convertRowToLocality(localityRow, dataContainer));
        }

        return phoneNumber;
    }

    /**
     * {@inheritDoc}
     *
     * @return A flag that indicates whether the number already exists or not.
     */
    @Override
    public boolean exists() {
        return 0 < adminDataStore.get(CountQuery.get(getTable()));
    }

    /**
     * Stores the purchased phone number into the database.
     *
     * @param phoneNumber The {@link PhoneNumber} object containing purchase details.
     * @param numberRow   The number {@link Row} object.
     */
    @Override
    public void loadPhoneNumberIntoRow(final boolean isCreateEvent, final PhoneNumber phoneNumber, final Row numberRow) {
        if (isCreateEvent) {
            final Locality locality = phoneNumber.getLocality();

            numberRow.setOrgId(phoneNumber.orgId());
            numberRow.set(OMNCPHONENUMBER.NUMBER, phoneNumber.getNumber().replace(EMPTY_SPACE, EMPTY_STRING));

            if (Objects.nonNull(locality)) {
                numberRow.set(OMNCPHONENUMBER.LOCATION_ID, locality.getId());
            }
        }
        phoneNumber.setId(numberRow.getPKValue());
        numberRow.set(OMNCPHONENUMBER.IS_ACTIVE, phoneNumber.isActive());
        numberRow.set(OMNCPHONENUMBER.DISPLAY_NAME, phoneNumber.getDisplayName());
        numberRow.set(OMNCPHONENUMBER.RECORD_TYPE, phoneNumber.getRecordType().getId());
        numberRow.set(OMNCPHONENUMBER.PROVIDER, phoneNumber.getTelephoneProvider().getId());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return true if the organization has a purchased number;false otherwise
     */
    @Override
    public boolean hasPurchasedNumber(final long orgId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(buildDefaultProviderCriteria());
        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * Retrieves an existing row or builds a new row for a given table and column.
     *
     * @param dataContainer   The existing data container.
     * @param table           The table to fetch or insert into.
     * @param column          The column used for searching.
     * @param value           The value to search for.
     * @param criteria        The criteria to fetch the row.
     * @param targetContainer The container to store new rows.
     * @return The existing or newly created row.
     */
    private Row getRow(final DataContainer dataContainer, final Table table, final Column column,
                       final String value, final Criteria criteria, final DataContainer targetContainer) {
        return Optional.ofNullable(dataContainer.getIndexedRow(column, value))
                .orElseGet(() -> {
                    final WhereClause whereClause = new WhereClause(Criteria.eq(column, value));
                    final Row row = targetContainer.getRow(table, Objects.isNull(criteria) ? whereClause : whereClause.and(criteria));

                    return Objects.isNull(row) ? new Row(table) : row;
                });
    }

    /**
     * <p>
     * Loads a locality's data into the target container if it is a new row.
     * </p>
     *
     * @param locality       The {@link Locality} object to be stored.
     * @param localityRow    The {@link Row} representing the locality data.
     * @param targetContainer The {@link DataContainer} where the row will be added.
     */
    private void loadLocalityRowIntoContainer(final Locality locality, final Row localityRow, final DataContainer targetContainer) {
        if (localityRow.isNewRow()) {
            localityRow.set(MNCLOCALITY.NAME, locality.getName());
            localityRow.set(MNCLOCALITY.COUNTRY_ID, locality.getCountry().getId());
            localityRow.set(MNCLOCALITY.STATE_ID, locality.getState().getId());
            targetContainer.addNewRow(localityRow);
        }
        locality.setId(localityRow.getPKValue());
    }

    /**
     * <p>
     * Loads a state’s data into the target container if it is a new row.
     * </p>
     *
     * @param state           The {@link State} object to be stored.
     * @param stateRow        The {@link Row} representing the state data.
     * @param targetContainer The {@link DataContainer} where the row will be added.
     */
    private void loadStateRowIntoContainer(final State state, final Row stateRow, final DataContainer targetContainer) {
        if (stateRow.isNewRow()) {
            stateRow.set(MNCSTATE.NAME, state.getName());
            stateRow.set(MNCSTATE.COUNTRY_ID, state.getCountry().getId());
            targetContainer.addNewRow(stateRow);
        }
        state.setId(stateRow.getPKValue());
    }

    /**
     * <p>
     * Loads a country's data into the target container if it is a new row.
     * </p>
     *
     * @param country         The {@link Country} object to be stored.
     * @param countryRow      The {@link Row} representing the country data.
     * @param targetContainer The {@link DataContainer} where the row will be added.
     */
    private void loadCountryRowIntoContainer(final Country country, final Row countryRow, final DataContainer targetContainer) {
        if (countryRow.isNewRow()) {
            countryRow.set(MNCCOUNTRY.CODE, country.getCode());
            countryRow.set(MNCCOUNTRY.NAME, country.getName());
            targetContainer.addNewRow(countryRow);
        }
        country.setId(countryRow.getPKValue());
    }

    /**
     * Converts a database row into a {@link Locality} object.
     *
     * @param localityRow    The {@link Row} representing the locality.
     * @param dataContainer  The {@link DataContainer} holding related state and country data.
     * @return A {@link Locality} object populated with relevant details.
     */
    private Locality convertRowToLocality(final Row localityRow, final DataContainer dataContainer) {
        final Locality locality = new Locality();
        final Row stateRow = dataContainer.getRow(getStateTable(), (long) localityRow.get(MNCLOCALITY.STATE_ID));

        locality.setId(localityRow.getPKValue());
        locality.setName(localityRow.get(MNCLOCALITY.NAME));
        locality.setState(convertRowToState(stateRow, dataContainer));
        return locality;
    }

    /**
     * Converts a database row into a {@link State} object.
     *
     * @param stateRow      The {@link Row} representing the state.
     * @param dataContainer The {@link DataContainer} holding related country data.
     * @return A {@link State} object populated with relevant details.
     */
    private State convertRowToState(final Row stateRow, final DataContainer dataContainer) {
        final State state = new State();
        final Row countryRow = dataContainer.getRow(getCountryTable(), (long) stateRow.get(MNCSTATE.COUNTRY_ID));

        state.setId(stateRow.getPKValue());
        state.setName(stateRow.get(MNCSTATE.NAME));
        state.setCountry(convertRowToCountry(countryRow));
        return state;
    }

    /**
     * Converts a database row into a {@link Country} object.
     *
     * @param countryRow The {@link Row} representing the country.
     * @return A {@link Country} object populated with relevant details.
     */
    private Country convertRowToCountry(final Row countryRow) {
        final Country country = new Country();

        country.setId(countryRow.getPKValue());
        country.setName(countryRow.get(MNCCOUNTRY.NAME));
        country.setCode(countryRow.get(MNCCOUNTRY.CODE));
        return country;
    }


    /**
     * Retrieves the database column associated with the given sort field name.
     * If the field name is not recognized, defaults to the display name column.
     *
     * @param fieldName     The name of the field to sort by (e.g., "number", "displayName").
     * @return The corresponding database column.
     */
    private Column getSortColumn(final String fieldName) {
        final Table numberTable = getTable();

        return switch (fieldName) {
            case "number" -> numberTable.getColumn(OMNCPHONENUMBER.NUMBER);
            default -> numberTable.getColumn(OMNCPHONENUMBER.DISPLAY_NAME);
        };
    }

    /**
     * Fetches location data for a given collection of {@link LocationDetails}.
     * <p>
     * Extracts country, state, and locality names from the provided list of location details
     * and delegates the retrieval to {@link #fetchLocationContainer(Collection, Collection, Collection)}.
     * </p>
     *
     * @param locationDetailsList The collection of {@link LocationDetails} objects containing country, state, and locality names.
     * @return A {@link DataContainer} containing the matching location records.
     */
    private DataContainer fetchLocationContainer(final Collection<LocationDetails> locationDetailsList) {
        final Collection<String> countries = new HashSet<>();
        final Collection<String> states = new HashSet<>();
        final Collection<String> localities = new HashSet<>();

        for (final LocationDetails locationDetails : locationDetailsList) {
            final String stateName = locationDetails.getState();
            final String localityName = locationDetails.getLocality();

            countries.add(locationDetails.getCountry());
            states.add(stateName);

            if (Objects.isNull(localityName)) {
                localities.add(stateName);
            } else {
                localities.add(localityName);
            }
        }
        return fetchLocationContainer(countries, states, localities);
    }

    /**
     * Fetches location data for the given country, state, and locality names.
     * <p>
     * Executes a database query to retrieve existing location records matching the given names.
     * The result includes countries, states, and localities that exist in the database.
     * </p>
     *
     * @param countries  The collection of country names to fetch.
     * @param states     The collection of state names to fetch.
     * @param localities The collection of locality names to fetch.
     * @return A {@link DataContainer} containing the matching location records.
     */
    private DataContainer fetchLocationContainer(final Collection<String> countries, final Collection<String> states,
                                                 final Collection<String> localities) {
        final Table countryTable = getCountryTable();
        final Table stateTable = getStateTable();
        final Table localityTable = getLocalityTable();

        final SelectQuery selectQuery = SelectQuery.get(countryTable);
        WhereClause whereClause = null;

        selectQuery.addJoinClause(new JoinClause(countryTable.getPKColumn(), stateTable.getColumn(MNCSTATE.COUNTRY_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(stateTable.getPKColumn(), localityTable.getColumn(MNCLOCALITY.STATE_ID), JoinClause.JoinType.LEFT));

        if (!countries.isEmpty()) {
            whereClause = new WhereClause(Criteria.in(countryTable.getColumn(MNCCOUNTRY.NAME), countries));
        }

        if (!states.isEmpty()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.in(stateTable.getColumn(MNCSTATE.NAME), states)), Connector.OR);
        }

        if (!localities.isEmpty()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.in(localityTable.getColumn(MNCLOCALITY.NAME), localities)), Connector.OR);
        }
        selectQuery.setWhereClause(whereClause);

        return adminDataStore.get(selectQuery);
    }

    /**
     * Purchases a mobile number via the Voice Call Service.
     *
     * @param phoneNumber The {@link PhoneNumber} object containing purchase details.
     * @return The purchased {@link MobileNumber}.
     */
    private MobileNumber buyMobileNumber(final PhoneNumber phoneNumber) {
        final MobileNumber mobileNumber = new MobileNumber(phoneNumber.getNumber());

        mobileNumber.setFriendlyName(phoneNumber.getDisplayName());
        voiceCallService.buyMobileNumber(mobileNumber);
        return mobileNumber;
    }

    /**
     * <p>
     * Fetches a {@link PhoneNumber} record based on the provided number.
     * </p>
     *
     * @param number            Represents the number.
     * @param readableDataStore Represents the {@link ReadableDataStore}
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    private Optional<PhoneNumber> getNumber(final String number, final ReadableDataStore readableDataStore) {
        final Table numberTable = getTable();
        final DataContainer dataContainer = readableDataStore.get(numberTable,
                new WhereClause(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.NUMBER), number.replace(EMPTY_SPACE, EMPTY_STRING)))
                        .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), true)));

        return Optional.ofNullable(dataContainer.getRow(numberTable)).map(row -> convertRowToModel(row, dataContainer));
    }

    /**
     * <p>
     * Builds the default provider criteria used for filtering phone numbers
     * </p>
     *
     * @return a {@link WhereClause} combining the active check and default provider condition
     */
    private WhereClause buildDefaultProviderCriteria() {
        final Table numberTable = getTable();

        return new WhereClause(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), true))
                .and(Criteria.eq(numberTable.getColumn(OMNCPHONENUMBER.PROVIDER), TelephoneProvider.getDefaultProvider().getId()));
    }
}
