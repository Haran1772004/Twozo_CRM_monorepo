package com.tlc.crm.core.list;

import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.ListPreviewRetriever;
import com.tlc.crm.entity.list.model.ListPreviewResponse;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.l10n.model.country.Country;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the abstraction for {@link ListPreviewRetriever}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public abstract class AbstractListPreviewRetriever implements ListPreviewRetriever {

    /**
     * <p>
     * Fetches the required contact related preview details for the related source list action
     * </p>
     *
     * @param source        Represents the {@link Source}
     * @param entityIds     Represents the collection of entity ids
     * @param dataContainer Represents the {@link DataContainer} contains the entity related data
     * @return The optional {@link ListPreviewResponse}
     */
    protected final Optional<ListPreviewResponse> fetchContactRelatedListPreview(final Source source,
                                                                                 final Collection<Long> entityIds, final DataContainer dataContainer) {
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Table contactTable = Table.get(MCOCONTACT.TABLE);
        final Table participantTable = Table.get(MDOPARTICIPANT.TABLE);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Map<String, Map<String, Object>> entityDataMap = new HashMap<>();
        final boolean isDealSource = Source.DEAL.equals(source);

        for (final Long entityId : entityIds) {
            final Row contactRow;
            final Collection<Row> participantRows;

            if (isDealSource) {
                final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(), entityId)));
                contactRow = Objects.nonNull(dealRow) ? dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPkColumn,
                        dealRow.get(MDODEAL.CONTACT_ID)))) : null;
                final Collection<Long> contactIds = dataContainer.getRows(participantTable,
                                new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), entityId))).toList().stream()
                        .map(row -> (Long) row.get(MDOPARTICIPANT.CONTACT_ID)).collect(Collectors.toCollection(ArrayList::new));

                if (Objects.nonNull(contactRow)) {
                    contactIds.remove(contactRow.getPKValue());
                }
                participantRows = contactIds.isEmpty() ? null : dataContainer.getRows(contactTable, new WhereClause(Criteria.in(contactPkColumn,
                        contactIds))).toList();
            } else {
                contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPkColumn, entityId)));
                participantRows = null;
            }

            if (Objects.nonNull(contactRow)) {
                final Map<String, Object> entityFieldsMap = entityDataMap.computeIfAbsent(String.valueOf(entityId), dataMap -> new HashMap<>());

                mapContactDetails(contactRow, entityFieldsMap, dataContainer);

                if (Objects.nonNull(participantRows) && !participantRows.isEmpty()) {
                    final Collection<Map<String, Object>> relatedContacts = new ArrayList<>();

                    for (final Row participantRow : participantRows) {
                        final Map<String, Object> participants = new HashMap<>();

                        mapContactDetails(participantRow, participants, dataContainer);

                        relatedContacts.add(participants);
                    }

                    entityFieldsMap.put(DealFields.RELATED_CONTACTS, relatedContacts);
                }
            }

        }

        return Optional.of(new ListPreviewResponse(entityDataMap));
    }

    /**
     * Populates the given entityFieldsMap with contact information extracted from the provided contactRow.
     * Also fetches and includes the contact's primary email details from the DataContainer.
     *
     * @param contactRow      the row containing contact data
     * @param entityFieldsMap the map to be populated with contact details
     * @param dataContainer   the data container used to fetch additional related data (e.g., email)
     */
    private void mapContactDetails(final Row contactRow, final Map<String, Object> entityFieldsMap, final DataContainer dataContainer) {
        final Table emailTable = Table.get(OMETEMAIL.TABLE);
        final Table phoneTable = Table.get(OMETPHONE.TABLE);
        final Table contactTable = Table.get(MCOCONTACT.TABLE);
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column emailTypeColumn = emailTable.getColumn(OMETEMAIL.TYPE);
        final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Column numberColumn = phoneTable.getColumn(OMETPHONE.NUMBER);
        final Column countryIdColumn = phoneTable.getColumn(OMETPHONE.COUNTRY_ID);
        final String idKey = ContactFields.ID;
        final String numberKey = ContactFields.NUMBER;
        final String primaryPhoneKey = ContactFields.PRIMARY_PHONE;
        final String firstName = contactRow.get(firstNameColumn);
        final String lastName = contactRow.get(lastNameColumn);
        final String fullName = Objects.isNull(lastName) ? firstName : String.join(" ", firstName, lastName);

        entityFieldsMap.put(idKey, contactRow.getPKValue());
        entityFieldsMap.put(ContactFields.NAME, fullName);
        final Long emailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);
        final Long phoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

        if (Objects.nonNull(phoneId)) {
            final Row phoneRow = dataContainer.getRow(phoneTable, phoneId);

            if (Objects.nonNull(phoneRow)) {
                final Integer countryId = phoneRow.get(countryIdColumn);
                final Map<String, Object> phoneDataMap = new HashMap<>();
                final Phone phone = new Phone();

                phone.setNumber(phoneRow.get(numberColumn));

                if (Objects.nonNull(countryId)) {
                    phone.setCountry(Country.getCountryById(countryId));
                }
                phoneDataMap.put(idKey, phoneId);
                phoneDataMap.put(numberKey, phone.getNumberWithCode());

                entityFieldsMap.put(primaryPhoneKey, phoneDataMap);
            }
        }

        if (Objects.nonNull(emailId)) {
            final Row emailRow = dataContainer.getRow(emailTable, emailId);

            if (Objects.nonNull(emailRow)) {
                final Integer emailTypeId = emailRow.get(emailTypeColumn);
                final Map<String, Object> emailDataMap = new HashMap<>();

                emailDataMap.put(idKey, emailId);
                emailDataMap.put(ContactFields.TYPE, Email.Type.valueOf(emailTypeId));
                emailDataMap.put(ContactFields.VALUE, emailRow.get(emailValueColumn));

                entityFieldsMap.put(ContactFields.PRIMARY_EMAIL, emailDataMap);
            }
        }
    }
}