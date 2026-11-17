package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.builder.model.ContactBuilder;
import com.tlc.crm.core.features.internal.dataimport.builder.model.ModelBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.EntityLoader;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.crm.entity.service.OptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loader to load the {@link Contact} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 2.0
 */
public final class ContactLoader extends EntityLoader<Contact> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactLoader.class);

    private final ContactManager contactManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final ContactBuilder contactBuilder;
    private final ContactConverter contactConverter;
    private final FieldService fieldService;
    private final OptionService optionService;

    private ContactLoader() {
        super(Source.CONTACT);
        this.contactManager = ContactManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        this.contactBuilder = ContactBuilder.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
    }

    private static final class Instance {
        private static final ContactLoader CONTACT_LOADER = new ContactLoader();
    }

    /**
     * Gets the instance of {@link ContactLoader}
     *
     * @return {@link ContactLoader} instance
     */
    public static ContactLoader getInstance() {
        return Instance.CONTACT_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link Contact} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link Contact} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final Long orgId = dataHolder.orgId();
        final long importId = errorTracker.getImportId();
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Map<FieldInfo, Map<Object, SourceRecord>> indexedEntityRecords = sourceRecordContainer.indexData(source);
        final Map<Integer, Collection<Integer>> mergedIndexMap = sourceRecordContainer.getMergedIndicesWithinTheSheet(source);
        final Map<Integer, Contact> convertedContacts = new HashMap<>();

        loadIndicesInfo(sourceRecordContainer, dataHolder.sourceModelContainer());
        loadDistinctValues(dataHolder.orgId(), sourceRecordContainer, importId);

        if (!indexedEntityRecords.isEmpty()) {
            final Map<String, FieldInfo> contactFields = importFieldService.groupDefaultFieldsByAliasName(orgId, source);
            final Map<Object, SourceRecord> entityIdRecordMap = indexedEntityRecords.computeIfAbsent(contactFields.get(ContactFields.ID), idMap -> new HashMap<>());

            if (!entityIdRecordMap.isEmpty()) {
                LOGGER.info("Data import: Model loading - Contact models are updating, import ID: {}, org ID: {}", importId, orgId);
                final long modelUpdateStartTime = System.currentTimeMillis();
                loadUpdatableModels(dataHolder, convertedContacts, entityIdRecordMap, errorTracker);
                LOGGER.info("Data import: Model loading - Contact models are updated in {}ms, import ID: {}, org ID: {}", System.currentTimeMillis() - modelUpdateStartTime, importId, orgId);
            }
        }
        loadModels(dataHolder, mergedIndexMap, convertedContacts, errorTracker);
        loadRequiredFields(orgId, convertedContacts);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Contact} to be cloned
     * @return The cloned {@link Contact}
     */
    @Override
    protected Contact cloneModel(final long orgId, final Contact entity) {
        final Contact contact = super.cloneModel(orgId, entity);

        contact.setOrgId(orgId);
        contact.setId(entity.id());

        return contact;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceRecordContainer Represents the {@link SourceRecordContainer}
     * @param importField           Represents the {@link FieldInfo}
     * @param distinctValues        Contains a collection of distinct values used to fetch relevant values and map them to the corresponding JSON for the given field
     * @return {@link DistinctValueHolder} containing the main values of distinct values and a default value
     */
    @Override
    protected DistinctValueHolder getDistinctValueHolder(final long orgId, final SourceRecordContainer sourceRecordContainer,
                                                         final FieldInfo importField, final Collection<Object> distinctValues) {
        final String aliasName = importField.aliasName();

        return switch (aliasName) {
            case "lifeCycleStage" -> getLifeCycleStageValueHolder(orgId, distinctValues);
            case "lifeCycleStatus" -> getLifeCycleStatusValueHolder(orgId, distinctValues);
            default -> null;
        };
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the life cycle stage
     *
     * @param orgId          Represents the organization id.
     * @param distinctValues Contains the distinct values
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the life cycle stage
     */
    private DistinctValueHolder getLifeCycleStageValueHolder(final long orgId, final Collection<Object> distinctValues) {
        final Map<String, Long> lifeCycleStageIdsByNames = new HashMap<>();
        final long defaultLifeCycleStageId = lifeCycleStageManager.getDefaultStageId(orgId);

        if (!distinctValues.isEmpty()) {
            final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStages(orgId,
                    distinctValues.stream().map(String::valueOf).collect(Collectors.toSet()));

            for (final LifeCycleStage stage : lifeCycleStages) {
                lifeCycleStageIdsByNames.put(stage.getName().toLowerCase(), stage.id());
            }
        }
        return new DistinctValueHolder(Collections.unmodifiableMap(lifeCycleStageIdsByNames), defaultLifeCycleStageId);
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the life cycle status
     *
     * @param orgId          Represents the organization id.
     * @param distinctValues Contains the distinct values
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the life cycle status
     */
    private DistinctValueHolder getLifeCycleStatusValueHolder(final long orgId, final Collection<Object> distinctValues) {

        if (distinctValues.isEmpty()) {
            return null;
        }
        final long defaultLifeCycleStatusId = lifeCycleStatusManager.getDefaultStatusId(orgId, lifeCycleStageManager.getDefaultStageId(orgId));
        final Collection<LifeCycleStatus> lifeCycleStatuses = lifeCycleStatusManager.getLifeCycleStatuses(orgId,
                distinctValues.stream().map(String::valueOf).collect(Collectors.toSet()));
        final Map<String, Long> lifeCycleStatusIdsByNames = new HashMap<>();

        for (final LifeCycleStatus status : lifeCycleStatuses) {
            lifeCycleStatusIdsByNames.put(status.getName().toLowerCase(), status.id());
        }
        return new DistinctValueHolder(Collections.unmodifiableMap(lifeCycleStatusIdsByNames), defaultLifeCycleStatusId);
    }

    /**
     * Loads the required fields for the converted contacts
     *
     * @param orgId             Represents the organization ID
     * @param convertedContacts Which contains the converted contacts
     */
    private void loadRequiredFields(final Long orgId, final Map<Integer, Contact> convertedContacts) {
        LifeCycleStage defaultLifeCycleStage = null;
        LifeCycleStatus defaultLifeCycleStatus = null;
        Option defaultLostReason = null;
        boolean defaultLifeCycleStageExists = false;
        boolean defaultLifeCycleStatusExists = false;
        boolean defaultLostReasonExists = false;

        for (final Contact contact : convertedContacts.values()) {
            final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
            final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
            final Option lostReason = contact.getLostReason();

            if (Objects.isNull(lifeCycleStage)) {

                if (!defaultLifeCycleStageExists) {
                    defaultLifeCycleStage = lifeCycleStageManager.getDefaultStage(orgId);
                    defaultLifeCycleStageExists = true;
                }
                contact.setLifeCycleStage(defaultLifeCycleStage);
            }

            if (Objects.isNull(lifeCycleStatus)) {

                if (!defaultLifeCycleStatusExists) {
                    defaultLifeCycleStatus = lifeCycleStatusManager.getDefaultStatus(orgId, contact.getLifeCycleStage().id());
                    defaultLifeCycleStatusExists = true;
                }
                contact.setLifeCycleStatus(defaultLifeCycleStatus);
            }

            if (Objects.isNull(lostReason) && LifeCycleStatus.Label.CLOSED_LOST == contact.getLifeCycleStatus().getLabel()) {

                if (!defaultLostReasonExists) {
                    final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
                    final Optional<FieldInfo> optionalField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, lostReasonKey);

                    defaultLostReason = optionalField.map(lostReasonField -> optionService.getDefaultOption(orgId, lostReasonField)).orElseGet(() -> null);
                    defaultLostReasonExists = true;
                }
                contact.setLostReason(defaultLostReason);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ModelBuilder} of contact
     */
    @Override
    protected ModelBuilder<Contact> getModelBuilder() {
        return contactBuilder;
    }

    /**
     * Loads the mergeable data
     */
    @Override
    protected void loadMergeableData(final Contact existingContact, final Contact currentContact) {
        final Collection<Email> existingContactEmails = existingContact.getEmails();
        final Collection<Phone> existingContactPhones = existingContact.getPhones();
        final List<Phone> currentContactPhones = (List<Phone>) currentContact.getPhones();
        final List<Email> currentContactEmails = (List<Email>) currentContact.getEmails();

        if (Objects.nonNull(existingContactEmails)) {
            final Map<Email, Email> currentEmailMap = currentContactEmails.stream().collect(Collectors
                    .toMap(Function.identity(), Function.identity()));

            for (final Email existingEmail : existingContactEmails) {
                final Email currentEmail = currentEmailMap.get(existingEmail);

                if (Objects.nonNull(currentEmail)) {
                    currentEmail.setId(existingEmail.id());
                }
            }
        }

        if (Objects.nonNull(existingContactPhones)) {
            final Map<Phone, Phone> currentPhoneMap = currentContactPhones.stream().collect(Collectors
                    .toMap(Function.identity(), Function.identity()));

            for (final Phone existingPhone : existingContactPhones) {
                final Phone currentPhone = currentPhoneMap.get(existingPhone);

                if (Objects.nonNull(currentPhone)) {
                    currentPhone.setId(existingPhone.id());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ContactConverter}
     */
    @Override
    protected EntityConverter<Contact> getConverter() {
        return contactConverter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param id    Represents the ID
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @return The Optional {@link Contact}
     */
    @Override
    protected Optional<Contact> get(final long orgId, final long id, final SourceModelContainer sourceModelContainer) {
        final DataContainer dataContainer = sourceModelContainer.getImportDataContainerHolder().getRelatedDataContainer(source, id);
        final Table table = Table.get(MCOCONTACT.TABLE);
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        if (Objects.isNull(row)) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        return Optional.of(contactManager.convertRowToModel(orgId, row, false, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Refers the organization id.
     * @param contact Refers the model.
     */
    @Override
    protected void loadEntitySummary(final long orgId, final Contact contact) {
        contactManager.loadSummaryDetails(orgId, contact);
    }
}