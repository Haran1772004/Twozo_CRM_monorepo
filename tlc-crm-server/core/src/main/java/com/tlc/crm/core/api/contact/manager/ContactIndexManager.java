package com.tlc.crm.core.api.contact.manager;

import com.tlc.commons.functions.TriConsumer;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.ActivityIndexManager;
import com.tlc.crm.core.features.api.file.manager.FileIndexManager;
import com.tlc.crm.core.features.api.note.manager.NoteIndexManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.search.fields.CommonSearchNestedFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.core.internal.index.service.IndexRequestLoader;
import com.tlc.crm.core.internal.index.service.EntityModificationRequestBuilder;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.service.WritableDocumentService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Contact}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ContactIndexManager extends AbstractIndexManager<Contact> {

    private static final Collection<String> RELATED_FIELDS = List.of(ContactFields.NAME, ContactFields.EMAILS,
            ContactFields.PHONES, ContactFields.ADDRESS, ContactFields.COMPANY, CommonSearchFields.CUSTOM_FIELDS,
            CommonSearchFields.TERRITORY_ID, CommonSearchFields.OWNER_ID, CommonSearchFields.FOLLOWER_IDS,
            CommonSearchFields.LINKEDIN, CommonSearchFields.FACEBOOK);

    private final IndexDocumentLoader indexDocumentLoader;
    private final IndexRequestLoader indexRequestLoader;
    private final IndexDataFetcher indexDataFetcher;
    private final EntityModificationRequestBuilder updateRequestService;

    private static class Instance {
        private static final ContactIndexManager INSTANCE = new ContactIndexManager();
    }

    private ContactIndexManager() {
        indexDocumentLoader = IndexDocumentLoader.get();
        indexDataFetcher = IndexDataFetcher.get();
        indexRequestLoader = IndexRequestLoader.get();
        updateRequestService = EntityModificationRequestBuilder.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactIndexManager}.
     * </p>
     *
     * @return {@link ContactIndexManager}.
     */
    public static ContactIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.CONTACT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(final Contact retainedContact, final Collection<Long> duplicateIds) {
        final Long orgId = retainedContact.orgId();
        final Contact contact = indexDataFetcher.fetchContact(orgId, retainedContact.id());
        final Company company = contact.getCompany();

        if (Objects.nonNull(company) && Objects.nonNull(company.id())) {
            contact.setCompany(indexDataFetcher.fetchCompany(orgId, company.id()));
        }
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRemoveRequest = BulkWriteRequest.get(index);

        update(List.of(contact));
        duplicateIds.forEach(duplicateId -> bulkWriteRemoveRequest.addRemoveRequest(RemoveRequest.get(String.valueOf(duplicateId), index)));
        writableService().performBulkOperations(bulkWriteRemoveRequest);
        replaceDuplicateContactInRelatedIndexes(orgId, contact, duplicateIds);
    }

    /**
     * <p>
     * Builds requests to modify company field.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param companyMap     Map holding contact card details.
     * @param companyFilter  {@link FilterClause}.
     * @param selectedFields Fields to be selected.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyUpdateRequests(final Long orgId, final Map<String, Object> companyMap,
                                                                final boolean isUpdate, final FilterClause companyFilter,
                                                                final List<String> selectedFields) {

        return updateRequestService.buildEntityModificationRequests(this.getSearchIndex().getName(), companyMap,
                isUpdate, CommonSearchFields.COMPANY, this.get(orgId, null, selectedFields, companyFilter));
    }

    /**
     * <p>
     * Builds requests to update company followers.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param contactFilter  {@link FilterClause}.
     * @param selectedFields Fields to be selected.
     * @param followerIds    Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersRequests(final Long orgId, final FilterClause contactFilter,
                                                                   final List<String> selectedFields,
                                                                   final Collection<Long> followerIds) {

        return updateRequestService.buildFollowersUpdateRequests(this.getSearchIndex().getName(), this.get(
                orgId, null, selectedFields, contactFilter), CommonSearchFields.COMPANY, followerIds);
    }

    /**
     * <p>
     * Builds requests to remove company followers.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param contactFilter  {@link FilterClause}.
     * @param selectedFields Fields to be selected.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersRemovalRequests(final Long orgId, final FilterClause contactFilter,
                                                                          final List<String> selectedFields) {
        return updateRequestService.buildFollowersRemovalRequests(this.getSearchIndex().getName(), this.get(
                orgId, null, selectedFields, contactFilter), CommonSearchNestedFields.COMPANY_DOT_FOLLOWER_IDS);
    }

    /**
     * <p>
     * Updates related fields.
     * </p>
     *
     * @param indexDoc       A map holding values from Contact Index.
     * @param constructedDoc A map holding constructed values.
     */
    @Override
    protected void updateRelatedFields(final Map<String, Object> indexDoc, final Map<String, Object> constructedDoc) {

        for (final String relatedField : RELATED_FIELDS) {
            indexDoc.put(relatedField, constructedDoc.getOrDefault(relatedField, null));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValuesIntoModel(final Contact contact) {
        final Company company = contact.getCompany();

        if (Objects.nonNull(company)) {
            contact.setCompany(indexDataFetcher.fetchCompany(contact.orgId(), company.id()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Collection<Contact> contacts, final Action action) {
        final BulkWriteRequest companyWriteRequest = BulkWriteRequest.get(CommonSearchFields.COMPANY);
        final BulkWriteRequest dealWriteRequest = BulkWriteRequest.get(CommonSearchFields.DEAL);
        final BulkWriteRequest activityWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest noteWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest fileWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);
        final WritableDocumentService writeService = writableService();
        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {
            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };
        final TriConsumer<Long, Company, BulkWriteRequest> companyConsumer = (orgId, company, bulkWriteRequest) -> {

            if (Objects.nonNull(company)) {
                indexRequestLoader.loadCompanyIndexRequest(orgId, company, bulkWriteRequest);
            }
        };

        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> selectedFieldsForUpdate = List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT);
        final List<String> selectedFieldsForDelete = List.of(CommonSearchFields.ID);

        for (final Contact contact : contacts) {
            final Long contactId = contact.id();
            final Long orgId = contact.orgId();
            final Criteria contactIdCriteria = new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_DOT_ID);
            final FilterClause contactSourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.CONTACT, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(contactIdCriteria);
            final FilterClause contactConditionAppendedFilter = new FilterClause(contactIdCriteria).or(
                    new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_CONTACT_DOT_ID));
            final FilterClause dealSourceFilter = new FilterClause(new Criteria(CommonSearchFields.DEAL,
                    Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(contactConditionAppendedFilter);

            switch (action) {
                case CREATE -> companyConsumer.accept(contact.orgId(), contact.getCompany(), companyWriteRequest);
                case UPDATE -> {
                    final Map<String, Object> contactMap = constructEntity(orgId, contact, Deal.class, CommonSearchFields.DEAL);
                    final Map<String, Object> partialContactMap = buildPartialContact(contactMap);
                    final Company company = contact.getCompany();

                    dealIndexManager.buildContactModificationRequests(orgId, new FilterClause(contactIdCriteria), true,
                            selectedFieldsForUpdate, contactMap).forEach(dealWriteRequest::addUpdateRequest);
                    activityIndexManager.buildContactModificationRequests(orgId, contact, true, contactMap).
                            forEach(activityWriteRequest::addUpdateRequest);
                    fileIndexManager.buildContactUpdateRequests(orgId, company, contactSourceFilter, dealSourceFilter,
                            contactMap, partialContactMap).forEach(fileWriteRequest::addUpdateRequest);
                    noteIndexManager.buildContactUpdateRequests(orgId, company, contactSourceFilter, dealSourceFilter,
                            contactMap, partialContactMap).forEach(noteWriteRequest::addUpdateRequest);
                }
                case DELETE -> {
                    dealIndexManager.buildContactModificationRequests(orgId, new FilterClause(contactIdCriteria), false,
                            selectedFieldsForDelete, null).forEach(dealWriteRequest::addUpdateRequest);
                    activityIndexManager.buildContactModificationRequests(orgId, contact, false, null).
                            forEach(activityWriteRequest::addUpdateRequest);
                    fileIndexManager.buildContactRemovalRequests(orgId, contactId, dealSourceFilter).
                            forEach(fileWriteRequest::addUpdateRequest);
                    noteIndexManager.buildContactRemovalRequests(orgId, contactId, dealSourceFilter).
                            forEach(noteWriteRequest::addUpdateRequest);
                    fileIndexManager.buildRemovalRequests(orgId, contactSourceFilter, selectedFieldsForDelete).
                            forEach(fileWriteRequest::addRemoveRequest);
                    noteIndexManager.buildRemovalRequests(orgId, contactSourceFilter, selectedFieldsForDelete).
                            forEach(noteWriteRequest::addRemoveRequest);
                }
            }
        }

        bulkActionPerformer.accept(companyWriteRequest);
        bulkActionPerformer.accept(dealWriteRequest);
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Long orgId, final Long contactId, final Collection<Long> followerIds) {
        final BulkWriteRequest dealWriteRequest = BulkWriteRequest.get(CommonSearchFields.DEAL);
        final BulkWriteRequest activityWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest noteWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest fileWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);
        final WritableDocumentService writeService = writableService();
        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {
            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };

        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();

        final Criteria contactIdCriteria = new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_DOT_ID);
        final FilterClause contactSourceFilter = getSourceFilter(CommonSearchFields.CONTACT).and(contactIdCriteria);
        final FilterClause dealSourceFilter = getSourceFilter(CommonSearchFields.DEAL).and(contactIdCriteria);

        if (followerIds.isEmpty()) {
            final List<String> selectedFields = List.of(CommonSearchFields.ID);

            dealIndexManager.buildContactFollowersRemovalRequests(orgId, new FilterClause(contactIdCriteria), selectedFields)
                    .forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildContactFollowersModificationRequests(orgId, contactId, false, null).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildContactFollowersRemovalRequests(orgId, contactSourceFilter, dealSourceFilter, selectedFields).
                    forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildContactFollowersRemovalRequests(orgId, contactSourceFilter, dealSourceFilter, selectedFields).
                    forEach(noteWriteRequest::addUpdateRequest);
        } else {
            final List<String> selectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT);

            dealIndexManager.buildContactFollowersUpdateRequests(orgId, new FilterClause(contactIdCriteria), selectedFields, followerIds)
                    .forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildContactFollowersModificationRequests(orgId, contactId, true, followerIds).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildContactFollowersUpdateRequests(orgId, followerIds, selectedFields,
                    contactSourceFilter, dealSourceFilter).forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildContactFollowersUpdateRequests(orgId, followerIds, selectedFields,
                    contactSourceFilter, dealSourceFilter).forEach(noteWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(dealWriteRequest);
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final Contact contact, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIdsForContactIndex(orgId, doc);
    }

    /**
     * <p>
     * Load follower ids in the doc.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className Clazz name.
     * @param doc       Holds the entity data to be indexed.
     */
    protected void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIds(orgId, className, doc);
    }

    /**
     * <p>
     * Gets source filter clause.
     * </p>
     *
     * @param source Source name.
     * @return {@link FilterClause}.
     */
    private FilterClause getSourceFilter(final String source) {
        return new FilterClause(new Criteria(source, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE));
    }

    /**
     * <p>
     * Replaces duplicate contact references with the retained contact across related indexes.
     * </p>
     *
     * @param orgId           Organization's id.
     * @param retainedContact Retained {@link Contact}.
     * @param duplicateIds    Duplicate contact ids.
     */
    private void replaceDuplicateContactInRelatedIndexes(final Long orgId, final Contact retainedContact,
                                                         final Collection<Long> duplicateIds) {
        final BulkWriteRequest dealWriteRequest = BulkWriteRequest.get(CommonSearchFields.DEAL);
        final BulkWriteRequest activityWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest noteWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest fileWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);
        final WritableDocumentService writeService = writableService();
        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {

            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };
        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> selectedFieldsForUpdate = List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT);
        final Map<String, Object> contactMap = constructEntity(orgId, retainedContact, Deal.class, CommonSearchFields.DEAL);
        final Map<String, Object> partialContactMap = buildPartialContact(contactMap);
        final Company company = retainedContact.getCompany();

        for (final Long duplicateId : duplicateIds) {
            final Criteria contactIdCriteria = new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_DOT_ID);
            final FilterClause contactSourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.CONTACT, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(contactIdCriteria);
            final FilterClause contactConditionAppendedFilter = new FilterClause(contactIdCriteria).or(
                    new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_CONTACT_DOT_ID));
            final FilterClause dealSourceFilter = new FilterClause(new Criteria(CommonSearchFields.DEAL,
                    Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(contactConditionAppendedFilter);

            dealIndexManager.buildContactModificationRequests(orgId, new FilterClause(contactIdCriteria), true,
                    selectedFieldsForUpdate, contactMap).forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildContactModificationRequests(orgId, duplicateId, retainedContact, true, contactMap).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildContactUpdateRequests(orgId, company, contactSourceFilter, dealSourceFilter,
                    contactMap, partialContactMap).forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildContactUpdateRequests(orgId, company, contactSourceFilter, dealSourceFilter,
                    contactMap, partialContactMap).forEach(noteWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(dealWriteRequest);
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * <p>
     * Builds a nested contact snapshot including mapped permission fields from a full card map.
     * </p>
     *
     * @param contactMap Full contact card from {@link #constructEntity}.
     * @return Nested snapshot for deal.contact patches.
     */
    private Map<String, Object> buildPartialContact(final Map<String, Object> contactMap) {
        final Map<String, Object> partialContactMap = new HashMap<>();

        partialContactMap.put(CommonSearchFields.ID, contactMap.get(CommonSearchFields.ID));
        partialContactMap.put(CommonSearchFields.NAME, contactMap.get(CommonSearchFields.NAME));
        partialContactMap.put(CommonSearchFields.ORG_ID, contactMap.get(CommonSearchFields.ORG_ID));
        partialContactMap.put(CommonSearchFields.OWNER_ID, contactMap.get(CommonSearchFields.OWNER_ID));

        if (contactMap.containsKey(CommonSearchFields.FOLLOWER_IDS)) {
            partialContactMap.put(CommonSearchFields.FOLLOWER_IDS, contactMap.get(CommonSearchFields.FOLLOWER_IDS));
        }

        return partialContactMap;
    }
}