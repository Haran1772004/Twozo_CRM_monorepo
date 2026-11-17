package com.tlc.crm.notifications.api.call.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;

import com.tlc.crm.notifications.api.call.manager.impl.PhoneNumberManagerImpl;
import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.State;

import com.tlc.notification.model.call.LocationDetails;

import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing phone numbers within the CRM system.
 * <p>
 * This interface provides methods to:
 * <ul>
 *     <li>Purchase new phone numbers</li>
 *     <li>Update phone number details</li>
 *     <li>Delete single or multiple phone numbers</li>
 *     <li>Retrieve phone number details</li>
 *     <li>List phone numbers with pagination and sorting</li>
 *     <li>Check for phone number existence</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PhoneNumberManager {

    /**
     * Retrieves the singleton instance of {@link PhoneNumberManager}.
     *
     * @return The singleton instance of {@link PhoneNumberManager}.
     */
    static PhoneNumberManager getInstance() {
        return PhoneNumberManagerImpl.getInstance();
    }

    /**
     * Returns the table object for the phone number data.
     *
     * @return Table reference for OMNCPHONENUMBER.
     */
    Table getTable();

    /**
     * Purchases a new phone number.
     *
     * @param phoneNumber The {@link PhoneNumber} object containing purchase details.
     */
    void buyNumber(final PhoneNumber phoneNumber);

    /**
     * Updates the details of an existing phone number.
     *
     * @param phoneNumber The {@link PhoneNumber} object containing updated details.
     */
    void updateNumberDetails(final PhoneNumber phoneNumber);

    /**
     * Deletes a phone number by its ID.
     *
     * @param orgId The organization ID.
     * @param id    The unique ID of the phone number to delete.
     */
    void deleteNumber(final long orgId, final long id);

    /**
     * Deletes multiple phone numbers by their IDs.
     *
     * @param orgId The organization ID.
     * @param ids   A collection of phone number IDs to delete.
     */
    void deleteNumbers(final long orgId, final Collection<Long> ids);

    /**
     * Retrieves a phone number by number.
     *
     * @param number Represents the number.
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    Optional<PhoneNumber> getNumber(final String number);

    /**
     * Retrieves a phone number by number.
     *
     * @param orgId  Represents an org id
     * @param number Represents the number.
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    Optional<PhoneNumber> getNumber(final long orgId, final String number);

    /**
     * Retrieves a phone number by its ID.
     *
     * @param orgId The organization ID.
     * @param id    The unique ID of the phone number to retrieve.
     * @return The {@link PhoneNumber} object, or {@code null} if not found.
     */
    PhoneNumber getNumber(final long orgId, final long id);

    /**
     * Retrieves a collection of phone numbers with pagination and sorting.
     *
     * @param orgId       The organization ID.
     * @param searchText  The search keyword for filtering results (nullable).
     * @param limitConfig The pagination details, including start index and limit.
     * @param sortConfig  The sorting details, specifying sorting field and order.
     * @return A collection of {@link PhoneNumber} objects matching the criteria.
     */
    Collection<PhoneNumber> getNumbers(final long orgId, final String searchText,
                                       final LimitConfig limitConfig, final SortConfig sortConfig);

    /**
     * Retrieves the total count of phone numbers matching the given search criteria.
     *
     * @param orgId            The organization ID.
     * @param dropdownCriteria The criteria for filtering results.
     * @return The total count of matching phone numbers.
     */
    long getTotalNumbersCount(final long orgId, final DropdownCriteria dropdownCriteria, final TelephoneProvider telephoneProvider);

    /**
     * Retrieves a collection of phone numbers for the dropdown list with pagination.
     *
     * @param orgId            The organization ID.
     * @param dropdownCriteria The criteria for filtering results.
     * @param dropdownLimit    The pagination details, including start index and limit.
     * @return A collection of {@link PhoneNumber} objects matching the criteria.
     */
    Collection<PhoneNumber> getNumberDropdownList(final long orgId, DropdownCriteria dropdownCriteria,
                                                  final DropdownLimit dropdownLimit, final TelephoneProvider telephoneProvider);

    /**
     * Checks whether a phone number exists in the db.
     *
     * @param orgId The organization ID.
     * @param id    The unique ID of the phone number.
     * @return {@code true} if the phone number exists, otherwise {@code false}.
     */
    boolean exists(final long orgId, final long id);

    /**
     * Stores a collection of localities into the database.
     *
     * @param localities The collection of {@link Locality} objects to be stored.
     */
    void storeLocations(final Collection<Locality> localities);

    /**
     * Retrieves a list of countries based on filtering and pagination criteria.
     *
     * @param dropdownCriteria The filtering criteria for country names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link Country} objects matching the given criteria.
     */
    Collection<Country> getCountries(final DropdownCriteria dropdownCriteria, final DropdownLimit dropdownLimit);

    /**
     * Retrieves a list of states for a given country based on filtering and pagination criteria.
     *
     * @param countryId        The ID of the country to fetch states for.
     * @param dropdownCriteria The filtering criteria for state names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link State} objects matching the given criteria.
     */
    Collection<State> getStates(final long countryId, final DropdownCriteria dropdownCriteria, final DropdownLimit dropdownLimit);

    /**
     * Retrieves a list of localities for a given country and optional state based on filtering and pagination criteria.
     *
     * @param countryId        The ID of the country to fetch localities for.
     * @param stateId          The optional ID of the state to fetch localities for (can be {@code null}).
     * @param dropdownCriteria The filtering criteria for locality names.
     * @param dropdownLimit    The pagination details specifying the limit and offset.
     * @return A collection of {@link Locality} objects matching the given criteria.
     */
    Collection<Locality> getLocalities(final long countryId, final Long stateId, final DropdownCriteria dropdownCriteria,
                                       final DropdownLimit dropdownLimit);

    /**
     * Retrieves the total count of countries matching the given filtering criteria.
     *
     * @param dropdownCriteria The filtering criteria for country names.
     * @return The total count of matching countries.
     */
    long getTotalCountriesCount(final DropdownCriteria dropdownCriteria);

    /**
     * Retrieves the total count of states for a given country matching the given filtering criteria.
     *
     * @param countryId        The ID of the country to count states for.
     * @param dropdownCriteria The filtering criteria for state names.
     * @return The total count of matching states in the specified country.
     */
    long getTotalStatesCount(final long countryId, final DropdownCriteria dropdownCriteria);

    /**
     * Retrieves the total count of localities for a given country and optional state matching the given filtering criteria.
     *
     * @param countryId        The ID of the country to count localities for.
     * @param stateId          The optional ID of the state to count localities for (can be {@code null}).
     * @param dropdownCriteria The filtering criteria for locality names.
     * @return The total count of matching localities in the specified country and state.
     */
    long getTotalLocalitiesCount(final long countryId, final Long stateId, final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Retrieves the {@link Locality} by using {@link LocationDetails}.
     * </p>
     *
     * @param locationDetailsList Holds the details of the location.
     * @return Map of {@link LocationDetails} and {@link Locality}.
     */
    Map<LocationDetails, Locality> getLocations(final Collection<LocationDetails> locationDetailsList);

    /**
     * Converts a database row into a PhoneNumber model object.
     * Fetches additional location details from related tables.
     *
     * @param numberRow     The row containing phone number data.
     * @param dataContainer The container holding all related data.
     * @return A PhoneNumber object populated with data from the row.
     */
    PhoneNumber convertRowToModel(final Row numberRow, final DataContainer dataContainer);

    /**
     * <p>
     * Checks whether the phone number is already exists in the DB.
     * </p>
     *
     * @return A flag that indicates whether the number already exists or not.
     */
    boolean exists();

    /**
     * Stores the purchased phone number into the database.
     *
     * @param phoneNumber The {@link PhoneNumber} object containing purchase details.
     * @param numberRow   The number {@link Row} object.
     */
    void loadPhoneNumberIntoRow(final boolean isCreateEvent, final PhoneNumber phoneNumber, final Row numberRow);

    /**
     * <p>
     * Checks whether the organization has at least one purchased phone number from the default telephone provider
     * </p>
     *
     * @param orgId Represents an organization id
     * @return true if the organization has a purchased number;false otherwise
     */
    boolean hasPurchasedNumber(final long orgId);
}