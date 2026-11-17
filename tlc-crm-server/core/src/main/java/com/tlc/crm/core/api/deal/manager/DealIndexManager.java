package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.functions.TriConsumer;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.features.api.activity.manager.ActivityIndexManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.file.manager.FileIndexManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.note.manager.NoteIndexManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.index.service.EntityModificationRequestBuilder;
import com.tlc.crm.core.internal.index.service.IndexRequestLoader;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.core.internal.index.field.IndexEntityFieldModifier;
import com.tlc.crm.core.internal.index.field.IndexPermissionFieldModifier;
import com.tlc.crm.entity.search.fields.CommonSearchNestedFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.SearchService;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.WritableDocumentService;
import com.tlc.search.service.provider.SearchServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Deal}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class DealIndexManager extends AbstractIndexManager<Deal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealIndexManager.class);

    private static final Collection<String> RELATED_FIELDS = List.of(CommonSearchFields.NAME, CommonSearchFields.CONTACT,
            CommonSearchFields.COMPANY, DealFields.PIPELINE, DealFields.PIPELINE_STAGE, DealFields.DEAL_STATE,
            DealFields.DEAL_VALUE, DealFields.TOTAL_PRODUCT_COUNT, DealFields.EXPECTED_CLOSE_DATE, CommonSearchFields.OWNER_ID,
            CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.TERRITORY_ID, CommonSearchFields.FOLLOWER_IDS);

    private static final Class<ActivityHolder> ACTIVITY_HOLDER_CLASS = ActivityHolder.class;
    private static final Class<NoteHolder> NOTE_HOLDER_CLASS = NoteHolder.class;
    private static final Class<FileHolder> FILE_HOLDER_CLASS = FileHolder.class;

    private final IndexDocumentLoader indexDocumentLoader;
    private final IndexRequestLoader indexRequestLoader;
    private final IndexDataFetcher indexDataFetcher;
    private final EntityModificationRequestBuilder updateRequestService;
    private final IndexEntityFieldModifier entityFieldModifier;
    private final IndexPermissionFieldModifier permissionFieldModifier;

    private static class Instance {
        private static final DealIndexManager INSTANCE = new DealIndexManager();
    }

    private DealIndexManager() {
        indexDocumentLoader = IndexDocumentLoader.get();
        indexDataFetcher = IndexDataFetcher.get();
        indexRequestLoader = IndexRequestLoader.get();
        updateRequestService = EntityModificationRequestBuilder.getInstance();
        entityFieldModifier = IndexEntityFieldModifier.getInstance();
        permissionFieldModifier = IndexPermissionFieldModifier.getInstance();
    }

    /**
     * <p>
     * Builds the {@link DealIndexManager}.
     * </p>
     *
     * @return {@link DealIndexManager}.
     */
    public static DealIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.DEAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(final Deal retainedDeal, final Collection<Long> duplicateIds) {
        final Long orgId = retainedDeal.orgId();
        final Deal deal = indexDataFetcher.fetchDeal(orgId, retainedDeal.id());
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRemoveRequest = BulkWriteRequest.get(index);

        update(List.of(deal));
        duplicateIds.forEach(duplicateId -> bulkWriteRemoveRequest.addRemoveRequest(RemoveRequest.get(String.valueOf(duplicateId), index)));
        writableService().performBulkOperations(bulkWriteRemoveRequest);
        replaceDuplicateDealInRelatedIndexes(orgId, deal, duplicateIds);
    }

    /**
     * <p>
     * Builds requests to modify contact related fields.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param contactFilter  {@link FilterClause}.
     * @param isUpdate       Whether the action's update.
     * @param selectedFields Selected fields.
     * @param contactMap     Map holding contact card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactModificationRequests(final Long orgId, final FilterClause contactFilter,
                                                                      final boolean isUpdate, final List<String> selectedFields,
                                                                      final Map<String, Object> contactMap) {

        return updateRequestService.buildEntityModificationRequests(this.getSearchIndex().getName(), contactMap,
                isUpdate, CommonSearchFields.CONTACT, this.get(orgId, null, selectedFields, contactFilter));
    }

    /**
     * <p>
     * Builds requests to modify company related fields.
     * </p>
     *
     * @param orgId             Organization's id.
     * @param companyId         Company's id.
     * @param isUpdate          Whether the action's update.
     * @param companyMap        Map holding company card details.
     * @param partialCompanyMap Map holding partial details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyModificationRequests(final Long orgId, final Long companyId,
                                                                      final boolean isUpdate,
                                                                      final Map<String, Object> companyMap,
                                                                      final Map<String, Object> partialCompanyMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final FilterClause companyIdFilter = new FilterClause(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_COMPANY_DOT_ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT,
                CommonSearchFields.COMPANY), companyIdFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray dealArray = fetchedJson.getJsonArray(index);
            final int size = dealArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject dealJson = dealArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(dealJson.getString(CommonSearchFields.ID), index);

                if (isUpdate) {
                    entityFieldModifier.modifyEntity(companyId, CommonSearchFields.COMPANY, dealJson, companyMap, updateRequest);
                    entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.CONTACT, dealJson, partialCompanyMap,
                            updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                } else {
                    entityFieldModifier.removeEntity(companyId, CommonSearchFields.COMPANY, dealJson, updateRequest);
                    entityFieldModifier.removeCompanyFromEntity(
                            companyId, CommonSearchFields.CONTACT, dealJson, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                }
                updateRequest.setDoc(dealJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update contact followers.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param contactFilter  {@link FilterClause}.
     * @param selectedFields Selected fields.
     * @param followerIds    Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactFollowersUpdateRequests(final Long orgId, final FilterClause contactFilter,
                                                                         final List<String> selectedFields,
                                                                         final Collection<Long> followerIds) {

        return updateRequestService.buildFollowersUpdateRequests(this.getSearchIndex().getName(), this.get(
                orgId, null, selectedFields, contactFilter), CommonSearchFields.CONTACT, followerIds);
    }

    /**
     * <p>
     * Builds {@link UpsertRequest} in a collection to update followers field.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param companyId   Company's id.
     * @param followerIds Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersModificationRequests(final Long orgId, final Long companyId,
                                                                               final boolean isUpdate,
                                                                               final Collection<Long> followerIds) {
        final Collection<UpsertRequest> upsertRequests = new ArrayList<>();
        final FilterClause companyIdFilter = new FilterClause(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_COMPANY_DOT_ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.CONTACT,
                CommonSearchFields.ID, CommonSearchFields.COMPANY), companyIdFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray dealArray = fetchedJson.getJsonArray(index);
            final int size = dealArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject dealJson = dealArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(dealJson.getString(CommonSearchFields.ID), index);

                if (isUpdate) {
                    permissionFieldModifier.addFollowersToEntity(companyId, CommonSearchFields.COMPANY, dealJson, followerIds);
                    permissionFieldModifier.addCompanyFollowersToEntity(companyId, CommonSearchFields.CONTACT, dealJson, followerIds);
                } else {
                    permissionFieldModifier.removeFollowersFromEntity(companyId, CommonSearchFields.COMPANY, dealJson, updateRequest,
                            CommonSearchNestedFields.COMPANY_DOT_FOLLOWER_IDS);
                    permissionFieldModifier.removeCompanyFollowersFromEntity(companyId, CommonSearchFields.CONTACT, dealJson,
                            updateRequest, CommonSearchNestedFields.CONTACT_COMPANY_DOT_FOLLOWER_IDS);
                }
                updateRequest.setDoc(dealJson);
                upsertRequests.add(updateRequest);
            }
        }
        return upsertRequests;
    }

    /**
     * <p>
     * Builds requests to remove contact's followers.
     * </p>
     *
     * @param orgId          Organization's id.
     * @param contactFilter  {@link FilterClause}.
     * @param selectedFields Fields to be selected.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactFollowersRemovalRequests(final Long orgId, final FilterClause contactFilter,
                                                                          final List<String> selectedFields) {

        return updateRequestService.buildFollowersRemovalRequests(this.getSearchIndex().getName(),
                this.get(orgId, null, selectedFields, contactFilter), CommonSearchNestedFields.CONTACT_DOT_FOLLOWER_IDS);
    }

    /**
     * <p>
     * Updates the deal archive state
     * </p>
     *
     * @param dealId      Represents the deal id
     * @param isArchived  Represents the deal archive state
     */
    public void updateDealArchive(final Long orgId, final Long dealId, final boolean isArchived) {
        final String index = getSearchIndex().getName();
        final ReadableDocumentService readableDocumentService = readableService(orgId);

        if (readableDocumentService.exists(dealId.toString(), index)) {
            final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);
            final SearchService searchService = SearchServiceProvider.get();
            final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(dealId), index);

            updateRequest.setDoc(Map.of(CommonSearchFields.IS_ARCHIVED, isArchived));
            bulkWriteRequest.addUpdateRequest(updateRequest);
            searchService.getWritableService().performBulkOperations(bulkWriteRequest);
        } else {
            LOGGER.info("Common Search: Deal not indexed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Collection<Deal> deals, final Action action) {
        final WritableDocumentService writeService = writableService();
        final BulkWriteRequest contactBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.CONTACT);
        final BulkWriteRequest companyBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.COMPANY);
        final BulkWriteRequest noteBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest productBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.PRODUCT);
        final BulkWriteRequest activityBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest fileBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);

        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {

            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };
        final TriConsumer<Long, ProductDetailsHolder, BulkWriteRequest> productConsumer =
                (orgId, productDetailsHolder, bulkWriteRequest) -> {

            if (Objects.nonNull(productDetailsHolder)) {
                indexRequestLoader.loadProductIndexRequests(orgId, productDetailsHolder, bulkWriteRequest);
            }
        };
        final TriConsumer<Long, Company, BulkWriteRequest> companyConsumer = (orgId, company, bulkWriteRequest) -> {

            if (Objects.nonNull(company)) {
                indexRequestLoader.loadCompanyIndexRequest(orgId, company, bulkWriteRequest);
            }
        };
        final TriConsumer<Long, Contact, BulkWriteRequest> contactConsumer = (orgId, contact, bulkWriteRequest) -> {

            if (Objects.nonNull(contact)) {
                indexRequestLoader.loadContactIndexRequest(orgId, contact, bulkWriteRequest);
            }
        };
        final TriConsumer<Long, Collection<Participant>, BulkWriteRequest> participantsConsumer = (orgId, participants, bulkWriteRequest) -> {

            if (Objects.nonNull(participants)) {
                for (final Participant participant : participants) {
                    contactConsumer.accept(orgId, participant.getContact(), bulkWriteRequest);
                }
            }
        };
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> selectedFieldsForDelete = List.of(CommonSearchFields.ID);
        final List<String> noteAndFileSelectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.ENTITY_SOURCE,
                CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);

        switch (action) {
            case CREATE -> {
                for (final Deal deal : deals) {
                    final Long orgId = deal.orgId();

                    contactConsumer.accept(orgId, deal.getPrimaryContact(), contactBulkWriteRequest);
                    participantsConsumer.accept(orgId, deal.getParticipants(), contactBulkWriteRequest);
                    productConsumer.accept(orgId, deal.getProductDetailsHolder(), productBulkWriteRequest);
                    companyConsumer.accept(orgId, deal.getCompany(), companyBulkWriteRequest);
                }
            }
            case UPDATE -> {
                for (final Deal deal : deals) {
                    final Long dealId = deal.id();
                    final Long orgId = deal.orgId();
                    final Map<String, Object> dealMap = activityIndexManager.constructEntity(
                            orgId, deal, ACTIVITY_HOLDER_CLASS, CommonSearchFields.ACTIVITY);
                    final FilterClause dealIdFilter = new FilterClause(
                            new Criteria(dealId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_DOT_ID));

                    contactConsumer.accept(orgId, deal.getPrimaryContact(), contactBulkWriteRequest);
                    participantsConsumer.accept(orgId, deal.getParticipants(), contactBulkWriteRequest);
                    productConsumer.accept(orgId, deal.getProductDetailsHolder(), productBulkWriteRequest);
                    companyConsumer.accept(orgId, deal.getCompany(), companyBulkWriteRequest);
                    activityIndexManager.buildDealModificationRequests(orgId, dealIdFilter, true, dealMap).
                            forEach(activityBulkWriteRequest::addUpdateRequest);
                    fileIndexManager.buildDealUpdateRequests(orgId, deal, dealIdFilter, noteAndFileSelectedFields, dealMap).
                            forEach(fileBulkWriteRequest::addUpdateRequest);
                    noteIndexManager.buildDealUpdateRequests(orgId, deal, dealIdFilter, noteAndFileSelectedFields, dealMap).
                            forEach(noteBulkWriteRequest::addUpdateRequest);
                }
            }
            case DELETE -> {
                for (final Deal deal : deals) {
                    final Long dealId = deal.id();
                    final Long orgId = deal.orgId();
                    final FilterClause dealIdFilter = new FilterClause(
                            new Criteria(dealId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_DOT_ID));

                    activityIndexManager.buildDealModificationRequests(orgId, dealIdFilter, false, null).
                            forEach(activityBulkWriteRequest::addUpdateRequest);
                    fileIndexManager.buildRemovalRequests(orgId, dealIdFilter, selectedFieldsForDelete).
                            forEach(fileBulkWriteRequest::addRemoveRequest);
                    noteIndexManager.buildRemovalRequests(orgId, dealIdFilter, selectedFieldsForDelete).
                            forEach(noteBulkWriteRequest::addRemoveRequest);
                }
            }
            case PARTIAL_UPDATE -> {
                for (final Deal deal : deals) {
                    final Long orgId = deal.orgId();

                    getPartialUpdateRequests(orgId, deal, ACTIVITY_HOLDER_CLASS, activityIndexManager).
                            forEach(activityBulkWriteRequest::addUpdateRequest);
                    getPartialUpdateRequests(orgId, deal, NOTE_HOLDER_CLASS, noteIndexManager).
                            forEach(noteBulkWriteRequest::addUpdateRequest);
                    getPartialUpdateRequests(orgId, deal, FILE_HOLDER_CLASS, fileIndexManager).
                            forEach(fileBulkWriteRequest::addUpdateRequest);
                }
            }
        }
        bulkActionPerformer.accept(contactBulkWriteRequest);
        bulkActionPerformer.accept(companyBulkWriteRequest);
        bulkActionPerformer.accept(noteBulkWriteRequest);
        bulkActionPerformer.accept(activityBulkWriteRequest);
        bulkActionPerformer.accept(productBulkWriteRequest);
        bulkActionPerformer.accept(fileBulkWriteRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Long orgId, final Long dealId, final Collection<Long> followerIds) {
        final FilterClause dealIdFilter = new FilterClause(
                new Criteria(dealId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_DOT_ID));
        final BulkWriteRequest activityWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest noteWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest fileWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);
        final WritableDocumentService writeService = writableService();
        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {
            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };

        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();

        if (followerIds.isEmpty()) {
            final List<String> selectedFields = List.of(CommonSearchFields.ID);

            activityIndexManager.buildDealFollowersModificationRequests(orgId, false, dealIdFilter, followerIds).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildDealFollowersRemovalRequests(orgId, selectedFields, dealIdFilter).
                    forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildDealFollowersRemovalRequests(orgId, selectedFields, dealIdFilter).
                    forEach(noteWriteRequest::addUpdateRequest);
        } else {
            final List<String> selectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.DEAL);

            activityIndexManager.buildDealFollowersModificationRequests(orgId, true, dealIdFilter, followerIds).
                    forEach(activityWriteRequest::addUpdateRequest);
            fileIndexManager.buildDealFollowersUpdateRequests(orgId, followerIds, selectedFields, dealIdFilter).
                    forEach(fileWriteRequest::addUpdateRequest);
            noteIndexManager.buildDealFollowersUpdateRequests(orgId, followerIds, selectedFields, dealIdFilter).
                    forEach(noteWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(activityWriteRequest);
        bulkActionPerformer.accept(fileWriteRequest);
        bulkActionPerformer.accept(noteWriteRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValuesIntoModel(final Deal deal) {
        final Long orgId = deal.orgId();
        final Contact contact = deal.getPrimaryContact();
        final Company company = deal.getCompany();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final DealManager dealManager = DealManager.getInstance();
        final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

        if (Objects.nonNull(contact)) {
            deal.setPrimaryContact(indexDataFetcher.fetchContact(orgId, contact.id()));
        }

        if (Objects.nonNull(company)) {
            deal.setCompany(indexDataFetcher.fetchCompany(orgId, company.id()));
        }

        if (Objects.nonNull(pipeline) && Objects.isNull(pipeline.getName())) {
            deal.setPipeline(indexDataFetcher.fetchPipeline(orgId, pipeline.id()));
        }

        if (Objects.nonNull(pipelineStage) && Objects.isNull(pipelineStage.getName())) {
            deal.setPipelineStage(indexDataFetcher.fetchPipelineStage(orgId, pipelineStage.id()));
        }

        if (Objects.isNull(productDetailsHolder)) {
            deal.setTotalProductCount(dealManager.getProductCount(orgId, deal.id()));
        } else {
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            deal.setTotalProductCount(Objects.isNull(productDetails) ? NumericalConstant.ZERO : productDetails.size());
        }
    }

    /**
     * <p>
     * Load follower ids in the doc.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className Entity model's class name.
     * @param doc       Holds the entity data to be indexed.
     */
    protected void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIds(orgId, className, doc);
    }

    /**
     * <p>
     * Updates related fields.
     * </p>
     *
     * @param indexDoc       A map holding values from Deal Index.
     * @param constructedDoc A map holding constructed values.
     */
    @Override
    protected void updateRelatedFields(final Map<String, Object> indexDoc, final Map<String, Object> constructedDoc) {

        for (final String relatedField : RELATED_FIELDS) {
            indexDoc.put(relatedField, constructedDoc.getOrDefault(relatedField, null));
        }
        indexDoc.values().removeIf(Objects::isNull);
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param deal {@link Deal}.
     * @param doc  Holds the entity data to be indexed.
     */
    @Override
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final Deal deal, final Map<String, Object> doc) {
        indexDocumentLoader.loadFollowerIdsForDealIndex(orgId, doc);
    }

    /**
     * <p>
     * Builds partial update requests.
     * </p>
     *
     * @param deal         Represents {@link Deal}.
     * @param parentClass  Parent's class.
     * @param indexManager Represents {@link AbstractIndexManager}.
     * @return {@link UpsertRequest} in a collection.
     */
    private Collection<UpsertRequest> getPartialUpdateRequests(final Long orgId, final Deal deal, final Class<?> parentClass,
                                                               final AbstractIndexManager<?> indexManager) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final FilterClause filter = new FilterClause(new Criteria(deal.id(), Comparator.EQUAL, CommonSearchFields.DEAL_DOT_ID));
        final JsonObject fetchedJson = indexManager.get(deal.orgId(), null, null, filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String indexName = indexManager.getSearchIndex().getName();
            final JsonArray entities = fetchedJson.getJsonArray(indexName);
            final Map<String, Object> doc = constructEntity(orgId, deal, parentClass, indexName);
            final JsonObject dealJson = Json.object();

            dealJson.put(CommonSearchFields.DEAL, doc);

            for (int index = NumericalConstant.ZERO; index < entities.size(); index++) {
                final JsonObject entity = entities.getJsonObject(index);
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(entity.getLong(CommonSearchFields.ID)), indexName);

                updateRequest.setDoc(dealJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Replaces duplicate deal references with the retained deal across related indexes.
     * </p>
     *
     * @param orgId        Organization's id.
     * @param retainedDeal Retained {@link Deal}.
     * @param duplicateIds Duplicate deal ids.
     */
    private void replaceDuplicateDealInRelatedIndexes(final Long orgId, final Deal retainedDeal,
                                                      final Collection<Long> duplicateIds) {
        final BulkWriteRequest activityBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.ACTIVITY);
        final BulkWriteRequest noteBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.NOTE);
        final BulkWriteRequest fileBulkWriteRequest = BulkWriteRequest.get(CommonSearchFields.FILE);
        final WritableDocumentService writeService = writableService();
        final Consumer<BulkWriteRequest> bulkActionPerformer = (bulkWriteRequest) -> {

            if (bulkWriteRequest.hasRequests()) {
                writeService.performBulkOperations(bulkWriteRequest);
            }
        };
        final ActivityIndexManager activityIndexManager = ActivityIndexManager.getInstance();
        final NoteIndexManager noteIndexManager = NoteIndexManager.getInstance();
        final FileIndexManager fileIndexManager = FileIndexManager.getInstance();
        final List<String> noteAndFileSelectedFields = List.of(CommonSearchFields.ID, CommonSearchFields.ENTITY_SOURCE,
                CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);
        final Map<String, Object> dealMap = activityIndexManager.constructEntity(
                orgId, retainedDeal, ACTIVITY_HOLDER_CLASS, CommonSearchFields.ACTIVITY);

        for (final Long duplicateId : duplicateIds) {
            final FilterClause dealIdFilter = new FilterClause(
                    new Criteria(duplicateId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_DOT_ID));

            activityIndexManager.buildDealModificationRequests(orgId, dealIdFilter, true, dealMap).
                    forEach(activityBulkWriteRequest::addUpdateRequest);
            fileIndexManager.buildDealUpdateRequests(orgId, retainedDeal, dealIdFilter, noteAndFileSelectedFields, dealMap).
                    forEach(fileBulkWriteRequest::addUpdateRequest);
            noteIndexManager.buildDealUpdateRequests(orgId, retainedDeal, dealIdFilter, noteAndFileSelectedFields, dealMap).
                    forEach(noteBulkWriteRequest::addUpdateRequest);
        }
        bulkActionPerformer.accept(activityBulkWriteRequest);
        bulkActionPerformer.accept(fileBulkWriteRequest);
        bulkActionPerformer.accept(noteBulkWriteRequest);
    }
}