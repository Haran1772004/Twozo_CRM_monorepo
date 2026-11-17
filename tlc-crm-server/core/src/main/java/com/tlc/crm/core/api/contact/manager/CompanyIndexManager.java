package com.tlc.crm.core.api.contact.manager;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.ActivityIndexManager;
import com.tlc.crm.core.features.api.file.manager.FileIndexManager;
import com.tlc.crm.core.features.api.note.manager.NoteIndexManager;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.entity.search.fields.CommonSearchNestedFields;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;

import com.tlc.crm.core.api.contact.model.Company;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.service.WritableDocumentService;
import com.tlc.validator.TlcModel;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Company}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class CompanyIndexManager extends AbstractIndexManager<Company> {

    private static final Collection<String> RELATED_FIELDS = List.of(CommonSearchFields.NAME, CommonSearchFields.ADDRESS,
            CommonSearchFields.WEBSITE, CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.TERRITORY_ID,
            CommonSearchFields.OWNER_ID, CommonSearchFields.FOLLOWER_IDS);

    private final IndexDocumentLoader indexDocumentLoader;

    private static class Instance {
        private static final CompanyIndexManager INSTANCE = new CompanyIndexManager();
    }

    private CompanyIndexManager() {
        indexDocumentLoader = IndexDocumentLoader.get();
    }

    public static CompanyIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.COMPANY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(final Company retainedCompany, final Collection<Long> duplicateIds) {
        final Long orgId = retainedCompany.orgId();
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRemoveRequest = BulkWriteRequest.get(index);

        update(List.of(retainedCompany));
        duplicateIds.forEach(duplicateId -> bulkWriteRemoveRequest.addRemoveRequest(RemoveRequest.get(String.valueOf(duplicateId), index)));
        writableService().performBulkOperations(bulkWriteRemoveRequest);
        replaceDuplicateCompanyInRelatedIndexes(orgId, retainedCompany, duplicateIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Collection<Company> companies, final Action action) {
        final BulkWriteRequest contactWriteRequest = BulkWriteRequest.get(CommonSearchFields.CONTACT);
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

        final ContactIndexManager contactIndexManager = ContactIndexManager.getInstance();
        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> idField = List.of(CommonSearchFields.ID);
        final List<String> noteAndFileSelectedFields = List.of(CommonSearchFields.ENTITY_SOURCE, CommonSearchFields.ID,
                CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);

        for (final Company company : companies) {
            final Long companyId = company.id();
            final Long orgId = company.orgId();
            final Criteria companyIdCriteria = new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID);
            final FilterClause companySourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.COMPANY, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(companyIdCriteria);
            final FilterClause companyAppendedCondition = new FilterClause(companyIdCriteria).or(
                    new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_COMPANY_DOT_ID));
            final FilterClause otherSourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.COMPANY, Comparator.NOT_EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(companyAppendedCondition);

            switch (action) {
                case UPDATE -> {
                    final Map<String, Object> companyMap = constructEntity(orgId, company, Deal.class, CommonSearchFields.DEAL);
                    final Map<String, Object> partialCompanyMap = buildPartialCompany(companyMap);

                    contactIndexManager.buildCompanyUpdateRequests(orgId, companyMap, true,
                            new FilterClause(companyIdCriteria), idField).forEach(contactWriteRequest::addUpdateRequest);
                    dealIndexManager.buildCompanyModificationRequests(orgId, companyId, true, companyMap, partialCompanyMap).
                            forEach(dealWriteRequest::addUpdateRequest);
                    activityIndexManager.buildCompanyModificationRequests(orgId, company, true, companyMap).
                            forEach(activityWriteRequest::addUpdateRequest);
                    fileIndexManager.buildCompanyUpdateRequests(orgId, idField, companySourceFilter, companyMap).
                            forEach(fileWriteRequest::addUpdateRequest);
                    noteIndexManager.buildCompanyUpdateRequests(orgId, idField, companySourceFilter, companyMap).
                            forEach(noteWriteRequest::addUpdateRequest);
                    fileIndexManager.buildCompanyModificationRequests(orgId, companyId, true, noteAndFileSelectedFields,
                            otherSourceFilter, companyMap, partialCompanyMap).forEach(fileWriteRequest::addUpdateRequest);
                    noteIndexManager.buildCompanyModificationRequests(orgId, companyId, true, noteAndFileSelectedFields,
                            otherSourceFilter, companyMap, partialCompanyMap).forEach(noteWriteRequest::addUpdateRequest);
                }
                case DELETE -> {
                    contactIndexManager.buildCompanyUpdateRequests(orgId, null, false,
                            new FilterClause(companyIdCriteria), idField).forEach(contactWriteRequest::addUpdateRequest);
                    dealIndexManager.buildCompanyModificationRequests(orgId, companyId, false, null, null).
                            forEach(dealWriteRequest::addUpdateRequest);
                    activityIndexManager.buildCompanyModificationRequests(orgId, company, false, null).
                            forEach(activityWriteRequest::addUpdateRequest);
                    fileIndexManager.buildCompanyModificationRequests(orgId, companyId, false, noteAndFileSelectedFields,
                            otherSourceFilter, null, null).forEach(fileWriteRequest::addUpdateRequest);
                    noteIndexManager.buildCompanyModificationRequests(orgId, companyId, false, noteAndFileSelectedFields,
                            otherSourceFilter, null, null).forEach(noteWriteRequest::addUpdateRequest);
                    fileIndexManager.buildRemovalRequests(orgId, companySourceFilter, idField).
                            forEach(fileWriteRequest::addRemoveRequest);
                    noteIndexManager.buildRemovalRequests(orgId, companySourceFilter, idField).
                            forEach(noteWriteRequest::addRemoveRequest);
                }
            }
        }
        bulkActionPerformer.accept(contactWriteRequest);
        bulkActionPerformer.accept(dealWriteRequest);
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * <p>
     * Updates related fields.
     * </p>
     *
     * @param indexDoc       A map holding values from Company Index.
     * @param constructedDoc A map holding constructed values.
     */
    @Override
    protected void updateRelatedFields(@NotNull final Map<String, Object> indexDoc, @NotNull final Map<String, Object> constructedDoc) {

        for (final String relatedField : RELATED_FIELDS) {
            indexDoc.put(relatedField, constructedDoc.getOrDefault(relatedField, null));
        }
    }

    /**
     * <p>
     * Load follower ids in the doc.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className {@link TlcModel} name.
     * @param doc       Holds the entity data to be indexed.
     */
    protected void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIds(orgId, className, doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Long orgId, final Long companyId, final Collection<Long> followerIds) {
        final BulkWriteRequest contactWriteRequest = BulkWriteRequest.get(CommonSearchFields.CONTACT);
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

        final ContactIndexManager contactIndexManager = ContactIndexManager.getInstance();
        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> noteAndFileSelectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.ENTITY_SOURCE,
                CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);
        final List<String> selectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);

        final Criteria companyIdCriteria = new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID);
        final FilterClause companySourceFilter = new FilterClause(new Criteria(
                CommonSearchFields.COMPANY, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(companyIdCriteria);
        final FilterClause companyAppendedCondition = new FilterClause(companyIdCriteria).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_COMPANY_DOT_ID));
        final FilterClause otherSourceFilter = new FilterClause(new Criteria(
                CommonSearchFields.COMPANY, Comparator.NOT_EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(companyAppendedCondition);

        if (followerIds.isEmpty()) {
            contactIndexManager.buildCompanyFollowersRemovalRequests(orgId, new FilterClause(companyIdCriteria),
                    selectedFields).forEach(contactWriteRequest::addUpdateRequest);
            dealIndexManager.buildCompanyFollowersModificationRequests(orgId, companyId, false, followerIds).
                    forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildCompanyFollowersModificationRequests(orgId, companyId, false, followerIds).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildCompanyFollowersRemovalRequests(orgId, companyId, companySourceFilter, otherSourceFilter,
                    noteAndFileSelectedFields).forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildCompanyFollowersRemovalRequests(orgId, companyId, companySourceFilter, otherSourceFilter,
                    noteAndFileSelectedFields).forEach(noteWriteRequest::addUpdateRequest);
        } else {
            contactIndexManager.buildCompanyFollowersRequests(orgId, new FilterClause(companyIdCriteria),
                    selectedFields, followerIds).forEach(contactWriteRequest::addUpdateRequest);
            dealIndexManager.buildCompanyFollowersModificationRequests(orgId, companyId, true, followerIds).
                    forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildCompanyFollowersModificationRequests(orgId, companyId, true, followerIds).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildCompanyFollowersUpdateRequests(orgId, followerIds, companyId, noteAndFileSelectedFields,
                    companySourceFilter, otherSourceFilter).forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildCompanyFollowersUpdateRequests(orgId, followerIds, companyId, noteAndFileSelectedFields,
                    companySourceFilter, otherSourceFilter).forEach(noteWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(contactWriteRequest);
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
     * @param company {@link Company}.
     * @param doc     Holds the entity data to be indexed.
     */
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final Company company, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIdsForCompanyIndex(orgId, doc);
    }

    /**
     * <p>
     * Replaces duplicate company references with the retained company across related indexes.
     * </p>
     *
     * @param orgId           Organization's id.
     * @param retainedCompany Retained {@link Company}.
     * @param duplicateIds    Duplicate company ids.
     */
    private void replaceDuplicateCompanyInRelatedIndexes(final Long orgId, final Company retainedCompany,
                                                         final Collection<Long> duplicateIds) {
        final BulkWriteRequest contactWriteRequest = BulkWriteRequest.get(CommonSearchFields.CONTACT);
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
        final ContactIndexManager contactIndexManager = ContactIndexManager.getInstance();
        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> idField = List.of(CommonSearchFields.ID);
        final List<String> noteAndFileSelectedFields = List.of(CommonSearchFields.ENTITY_SOURCE, CommonSearchFields.ID,
                CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);
        final Map<String, Object> companyMap = constructEntity(orgId, retainedCompany, Deal.class, CommonSearchFields.DEAL);
        final Map<String, Object> partialCompanyMap = buildPartialCompany(companyMap);

        for (final Long duplicateId : duplicateIds) {
            final Criteria companyIdCriteria = new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID);
            final FilterClause companySourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.COMPANY, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(
                    new Criteria(duplicateId, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE_ID));
            final FilterClause companyAppendedCondition = new FilterClause(companyIdCriteria).or(
                    new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_COMPANY_DOT_ID)).or(
                    new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_COMPANY_DOT_ID));
            final FilterClause otherSourceFilter = new FilterClause(new Criteria(
                    CommonSearchFields.COMPANY, Comparator.NOT_EQUAL, CommonSearchFields.ENTITY_SOURCE)).and(companyAppendedCondition);

            contactIndexManager.buildCompanyUpdateRequests(orgId, companyMap, true,
                    new FilterClause(companyIdCriteria), idField).forEach(contactWriteRequest::addUpdateRequest);
            dealIndexManager.buildCompanyModificationRequests(orgId, duplicateId, true, companyMap, partialCompanyMap).
                    forEach(dealWriteRequest::addUpdateRequest);
            activityIndexManager.buildCompanyModificationRequests(orgId, duplicateId, retainedCompany, true, companyMap).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildCompanyUpdateRequests(orgId, idField, companySourceFilter, companyMap).
                    forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildCompanyUpdateRequests(orgId, idField, companySourceFilter, companyMap).
                    forEach(noteWriteRequest::addUpdateRequest);
            fileIndexManager.buildCompanyModificationRequests(orgId, duplicateId, true, noteAndFileSelectedFields,
                    otherSourceFilter, companyMap, partialCompanyMap).forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildCompanyModificationRequests(orgId, duplicateId, true, noteAndFileSelectedFields,
                    otherSourceFilter, companyMap, partialCompanyMap).forEach(noteWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(contactWriteRequest);
        bulkActionPerformer.accept(dealWriteRequest);
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * <p>
     * Builds a nested company snapshot including mapped permission fields from a full card map.
     * </p>
     *
     * @param companyMap Full company card from {@link #constructEntity}.
     * @return Nested snapshot for contact.company / deal.company patches.
     */
    private Map<String, Object> buildPartialCompany(final Map<String, Object> companyMap) {
        final Map<String, Object> partialCompanyMap = new HashMap<>();

        partialCompanyMap.put(CommonSearchFields.ID, companyMap.get(CommonSearchFields.ID));
        partialCompanyMap.put(CommonSearchFields.NAME, companyMap.get(CommonSearchFields.NAME));
        partialCompanyMap.put(CommonSearchFields.ORG_ID, companyMap.get(CommonSearchFields.ORG_ID));
        partialCompanyMap.put(CommonSearchFields.OWNER_ID, companyMap.get(CommonSearchFields.OWNER_ID));

        if (companyMap.containsKey(CommonSearchFields.FOLLOWER_IDS)) {
            partialCompanyMap.put(CommonSearchFields.FOLLOWER_IDS, companyMap.get(CommonSearchFields.FOLLOWER_IDS));
        }

        return partialCompanyMap;
    }
}