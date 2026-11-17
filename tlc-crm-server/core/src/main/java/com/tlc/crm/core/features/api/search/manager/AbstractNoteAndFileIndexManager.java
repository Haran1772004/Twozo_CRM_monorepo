package com.tlc.crm.core.features.api.search.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.internal.index.field.IndexEntityFieldModifier;
import com.tlc.crm.core.internal.index.field.IndexPermissionFieldModifier;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.entity.search.fields.CommonSearchNestedFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.internal.index.service.EntityModificationRequestBuilder;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages all things pertaining to note and file indexes.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public abstract class AbstractNoteAndFileIndexManager<T extends TlcModel> extends AbstractIndexManager<T> {

    protected final IndexDataFetcher indexDataFetcher;
    protected final IndexDocumentLoader indexDocumentLoader;

    private final EntityModificationRequestBuilder entityModificationRequestBuilder;
    private final IndexEntityFieldModifier entityFieldModifier;
    private final IndexPermissionFieldModifier permissionFieldModifier;

    protected AbstractNoteAndFileIndexManager() {
        entityModificationRequestBuilder = EntityModificationRequestBuilder.getInstance();
        indexDataFetcher = IndexDataFetcher.get();
        entityFieldModifier = IndexEntityFieldModifier.getInstance();
        permissionFieldModifier = IndexPermissionFieldModifier.getInstance();
        indexDocumentLoader = IndexDocumentLoader.get();
    }

    /**
     * <p>
     * Builds requests for deal source as it is a direct source and can't be dependent.
     * </p>
     *
     * @param orgId      Organization's id.
     * @param deal       {@link Deal}.
     * @param dealFilter {@link FilterClause}.
     * @param dealMap    Map holding deal card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildDealUpdateRequests(final Long orgId, final Deal deal, final FilterClause dealFilter,
                                                             final List<String> selectedFields, final Map<String, Object> dealMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, selectedFields, dealFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();
            final Company company = deal.getCompany();
            final Contact contact = deal.getPrimaryContact();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAndFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                if (Objects.isNull(company)) {
                    if (noteAndFileJson.containsKey(CommonSearchFields.COMPANY)) {
                        noteAndFileJson.remove(CommonSearchFields.COMPANY);
                        updateRequest.addFieldToRemove(CommonSearchFields.COMPANY);
                    }
                } else {
                    modifyEntityField(CommonSearchFields.COMPANY, noteAndFileJson, updateRequest,
                            constructEntity(orgId, company, Deal.class, CommonSearchFields.DEAL));
                }

                if (Objects.isNull(contact)) {
                    if (noteAndFileJson.containsKey(CommonSearchFields.CONTACT)) {
                        noteAndFileJson.remove(CommonSearchFields.CONTACT);
                        updateRequest.addFieldToRemove(CommonSearchFields.CONTACT);
                    }
                } else {
                    modifyEntityField(CommonSearchFields.CONTACT, noteAndFileJson, updateRequest,
                            constructEntity(orgId, contact, Deal.class, CommonSearchFields.DEAL));
                }
                setOwnerIdAndTerritoryId(deal.getOwnerId(), (Long) dealMap.get(CommonSearchFields.TERRITORY_ID),
                        updateRequest, noteAndFileJson);
                modifyEntityField(CommonSearchFields.DEAL, noteAndFileJson, updateRequest, dealMap);

                if (Objects.equals(CommonSearchFields.DEAL, noteAndFileJson.getString(CommonSearchFields.ENTITY_SOURCE))) {
                    setEntitySourceFields(orgId, CommonSearchFields.DEAL, noteAndFileJson, updateRequest, dealMap);
                }
                updateRequest.setDoc(noteAndFileJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update contact fields with direct and other sources filter.
     * </p>
     *
     * @param orgId               Organization's id.
     * @param contactSourceFilter Contact source filter.
     * @param dealSourceFilter    Deal source filter.
     * @param contactMap          Map holding contact details.
     * @param partialContactMap   Map holding partial contact details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactUpdateRequests(final Long orgId, final Company company,
                                                                final FilterClause contactSourceFilter,
                                                                final FilterClause dealSourceFilter,
                                                                final Map<String, Object> contactMap,
                                                                final Map<String, Object> partialContactMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject contactSourceJson = this.get(orgId, null, List.of(CommonSearchFields.ID, CommonSearchFields.COMPANY,
                CommonSearchFields.ENTITY_SOURCE, CommonSearchFields.CONTACT), contactSourceFilter);

        if (!Json.isEmpty(contactSourceJson)) {
            final JsonArray fetchedArray = contactSourceJson.getJsonArray(index);
            final int size = fetchedArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = fetchedArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                modifyEntityField(CommonSearchFields.CONTACT, noteAndFileJson, updateRequest, contactMap);
                setOwnerIdAndTerritoryId((Long) contactMap.get(CommonSearchFields.OWNER_ID),
                        (Long) contactMap.get(CommonSearchFields.TERRITORY_ID), updateRequest, noteAndFileJson);
                setEntitySourceFields(orgId, CommonSearchFields.CONTACT, noteAndFileJson, updateRequest, contactMap);

                if (Objects.isNull(company) && noteAndFileJson.containsKey(CommonSearchFields.COMPANY)) {
                    noteAndFileJson.remove(CommonSearchFields.COMPANY);
                    updateRequest.addFieldToRemove(CommonSearchFields.COMPANY);
                } else {
                    loadCompanyIntoDoc(orgId, company, noteAndFileJson, updateRequest);
                }
                updateRequest.setDoc(noteAndFileJson);
                updateRequests.add(updateRequest);
            }
        }
        final JsonObject dealSourceJson = this.get(orgId, null, List.of(CommonSearchFields.DEAL,
                CommonSearchFields.ID, CommonSearchFields.CONTACT), dealSourceFilter);

        if (!Json.isEmpty(dealSourceJson)) {
            final Long contactId = (Long) contactMap.get(CommonSearchFields.ID);
            final JsonArray fetchedArray = dealSourceJson.getJsonArray(index);
            final int size = fetchedArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = fetchedArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                entityFieldModifier.modifyEntity(contactId, CommonSearchFields.CONTACT, noteAndFileJson, contactMap, updateRequest);
                entityFieldModifier.modifyEntityContact(contactId, CommonSearchFields.CONTACT, noteAndFileJson, partialContactMap,
                        updateRequest, CommonSearchNestedFields.DEAL_DOT_CONTACT);
                updateRequest.setDoc(noteAndFileJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update company fields with direct source filter.
     * </p>
     *
     * @param orgId               Organization's id.
     * @param selectedFields      Selected fields.
     * @param companySourceFilter {@link FilterClause}.
     * @param companyMap          Map holding company card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyUpdateRequests(final Long orgId, final List<String> selectedFields,
                                                                final FilterClause companySourceFilter,
                                                                final Map<String, Object> companyMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, selectedFields, companySourceFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAndFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                setOwnerIdAndTerritoryId((Long) companyMap.get(CommonSearchFields.OWNER_ID),
                        (Long) companyMap.get(CommonSearchFields.TERRITORY_ID), updateRequest, noteAndFileJson);
                modifyEntityField(CommonSearchFields.COMPANY, noteAndFileJson, updateRequest, companyMap);
                setEntitySourceFields(orgId, CommonSearchFields.COMPANY, noteAndFileJson, updateRequest, companyMap);
                updateRequest.setDoc(noteAndFileJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update company fields with other source filter.
     * </p>
     *
     * @param orgId             Organization's id.
     * @param companyId         Company's id.
     * @param isUpdate          Whether the action's update.
     * @param otherSourceFilter {@link FilterClause}.
     * @param companyMap        Map holding company card details.
     * @param partialCompanyMap Map holding partial details.
     */
    public Collection<UpsertRequest> buildCompanyModificationRequests(final Long orgId, final Long companyId,
                                                                      final boolean isUpdate, final List<String> selectedFields,
                                                                      final FilterClause otherSourceFilter,
                                                                      final Map<String, Object> companyMap,
                                                                      final Map<String, Object> partialCompanyMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, selectedFields, otherSourceFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAndFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                modifyCompanyFields(isUpdate, companyId, noteAndFileJson, companyMap, partialCompanyMap, updateRequest);
                updateRequest.setDoc(noteAndFileJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests that removes note/file based on the id filter (direct source).
     * </p>
     *
     * @param orgId          Organization's id.
     * @param idFilter       {@link FilterClause}.
     * @param selectedFields Fields that are selected.
     * @return {@link RemoveRequest} in a collection.
     */
    public Collection<RemoveRequest> buildRemovalRequests(final Long orgId, final FilterClause idFilter,
                                                          final List<String> selectedFields) {
        final Collection<RemoveRequest> removeRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, selectedFields, idFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                removeRequests.add(RemoveRequest.get(noteAndFileArray.getJsonObject(count).getString(CommonSearchFields.ID), index));
            }
        }
        return removeRequests;
    }

    /**
     * <p>
     * Builds requests to remove contact fields only for deal source filter.
     * </p>
     *
     * @param orgId      Organization's id.
     * @param contactId  Contact's id.
     * @param dealFilter {@link FilterClause}.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactRemovalRequests(final Long orgId, final Long contactId,
                                                                 final FilterClause dealFilter) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject dealSourceJson = this.get(orgId, null, List.of(CommonSearchFields.DEAL,
                CommonSearchFields.ID, CommonSearchFields.CONTACT), dealFilter);

        if (!Json.isEmpty(dealSourceJson)) {
            final JsonArray noteAndFileArray = dealSourceJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAndFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                entityFieldModifier.removeEntity(contactId, CommonSearchFields.CONTACT, noteAndFileJson, updateRequest);
                entityFieldModifier.removeContactFromEntity(contactId, CommonSearchFields.DEAL, noteAndFileJson, updateRequest,
                        CommonSearchNestedFields.DEAL_DOT_CONTACT);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update deal followers field (direct source)
     * </p>
     *
     * @param orgId            Organization's id.
     * @param dealSourceFilter {@link FilterClause}.
     * @param selectedFields   Selected fields.
     * @param followerIds      Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildDealFollowersUpdateRequests(final Long orgId, final Collection<Long> followerIds,
                                                                      final List<String> selectedFields,
                                                                      final FilterClause dealSourceFilter) {

        return buildFollowersUpdateRequests(orgId, dealSourceFilter, CommonSearchFields.DEAL, selectedFields, followerIds);
    }

    /**
     * <p>
     * Builds requests to update contact followers field for both direct and other sources filters.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param selectedFields     Selected fields for get request.
     * @param followerIds        Follower ids.
     * @param directSourceFilter Pertains to direct source of note/file.
     * @param otherSourcesFilter Pertains to direct source of note/file.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactFollowersUpdateRequests(final Long orgId, final Collection<Long> followerIds,
                                                                         final List<String> selectedFields,
                                                                         final FilterClause directSourceFilter,
                                                                         final FilterClause otherSourcesFilter) {
        final String index = this.getSearchIndex().getName();
        final Collection<UpsertRequest> followersUpdateRequests = buildFollowersUpdateRequests(
                orgId, directSourceFilter, CommonSearchFields.CONTACT, selectedFields, followerIds);
        final JsonObject otherSourcesJson = this.get(orgId, null, selectedFields, otherSourcesFilter);

        followersUpdateRequests.addAll(entityModificationRequestBuilder.buildFollowersUpdateRequests(
                index, otherSourcesJson, CommonSearchFields.CONTACT, followerIds));

        return followersUpdateRequests;
    }

    /**
     * <p>
     * Builds requests to update company followers field for both direct and other sources filters.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param selectedFields     Selected fields for get request.
     * @param followerIds        Follower ids.
     * @param directSourceFilter Pertains to direct source of note/file.
     * @param otherSourcesFilter Pertains to direct source of note/file.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersUpdateRequests(final Long orgId, final Collection<Long> followerIds,
                                                                         final Long companyId, final List<String> selectedFields,
                                                                         final FilterClause directSourceFilter,
                                                                         final FilterClause otherSourcesFilter) {
        final String index = this.getSearchIndex().getName();
        final Collection<UpsertRequest> followersUpdateRequests = buildFollowersUpdateRequests(
                orgId, directSourceFilter, CommonSearchFields.COMPANY, selectedFields, followerIds);
        final JsonObject otherSourcesJson = this.get(orgId, null, selectedFields, otherSourcesFilter);

        if (!Json.isEmpty(otherSourcesJson)) {
            final JsonArray noteAnFileArray = otherSourcesJson.getJsonArray(index);
            final int size = noteAnFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAnFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                permissionFieldModifier.addFollowersToEntity(companyId, CommonSearchFields.COMPANY, noteAndFileJson, followerIds);
                permissionFieldModifier.addCompanyFollowersToEntity(companyId, CommonSearchFields.CONTACT, noteAndFileJson, followerIds);
                updateRequest.setDoc(noteAndFileJson);
                followersUpdateRequests.add(updateRequest);
            }
        }

        return followersUpdateRequests;
    }

    /**
     * <p>
     * Builds requests to update followers field (direct source).
     * </p>
     *
     * @param orgId              Organization's id.
     * @param selectedFields     Selected fields for get request.
     * @param directSourceFilter Pertains to direct source of note/file.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildDealFollowersRemovalRequests(final Long orgId, final List<String> selectedFields,
                                                                       final FilterClause directSourceFilter) {

        return buildFollowersRemovalRequestsForDirectSource(orgId, this.getSearchIndex().getName(),
                selectedFields, directSourceFilter, CommonSearchNestedFields.DEAL_DOT_FOLLOWERS_ID);
    }

    /**
     * <p>
     * Builds requests to remove contact's followers for both direct and other sources filters.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param directSourceFilter {@link FilterClause}.
     * @param otherSourceFilter  {@link FilterClause}.
     * @param selectedFields     Fields that are selected.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactFollowersRemovalRequests(final Long orgId, final FilterClause directSourceFilter,
                                                                          final FilterClause otherSourceFilter,
                                                                          final List<String> selectedFields) {
        final String index = this.getSearchIndex().getName();
        final Collection<UpsertRequest> followersRemovalRequests = buildFollowersRemovalRequestsForDirectSource(
                orgId, index, selectedFields, directSourceFilter, CommonSearchNestedFields.CONTACT_DOT_FOLLOWER_IDS);

        followersRemovalRequests.addAll(entityModificationRequestBuilder.buildFollowersRemovalRequests(index,
                this.get(orgId, null, selectedFields, otherSourceFilter), CommonSearchNestedFields.CONTACT_DOT_FOLLOWER_IDS));

        return followersRemovalRequests;
    }

    /**
     * <p>
     * Builds requests to remove company's followers for both direct and other sources filters.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param companyId          Company's id.
     * @param directSourceFilter {@link FilterClause}.
     * @param otherSourcesFilter {@link FilterClause}.
     * @param selectedFields     Fields that are selected.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersRemovalRequests(final Long orgId, final Long companyId,
                                                                          final FilterClause directSourceFilter,
                                                                          final FilterClause otherSourcesFilter,
                                                                          final List<String> selectedFields) {
        final String index = this.getSearchIndex().getName();
        final Collection<UpsertRequest> followersRemovalRequests = buildFollowersRemovalRequestsForDirectSource(
                orgId, index, selectedFields, directSourceFilter, CommonSearchNestedFields.COMPANY_DOT_FOLLOWER_IDS);
        final JsonObject otherSourcesJson = this.get(orgId, null, selectedFields, otherSourcesFilter);

        if (!Json.isEmpty(otherSourcesJson)) {
            final JsonArray noteAnFileArray = otherSourcesJson.getJsonArray(index);
            final int size = noteAnFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject noteAndFileJson = noteAnFileArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);

                permissionFieldModifier.removeFollowersFromEntity(companyId, CommonSearchFields.COMPANY, noteAndFileJson, updateRequest,
                        CommonSearchNestedFields.COMPANY_DOT_FOLLOWER_IDS);
                permissionFieldModifier.removeCompanyFollowersFromEntity(companyId, CommonSearchFields.CONTACT, noteAndFileJson,
                        updateRequest, CommonSearchNestedFields.CONTACT_COMPANY_DOT_FOLLOWER_IDS);
                followersRemovalRequests.add(updateRequest);
            }
        }

        return followersRemovalRequests;
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
     * <p>
     * Builds requests to remove followers for direct source filter.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param index              Index name.
     * @param directSourceFilter {@link FilterClause}.
     * @param followersField     Name of the follower field.
     * @return {@link UpsertRequest} in a collection.
     */
    private Collection<UpsertRequest> buildFollowersRemovalRequestsForDirectSource(final Long orgId, final String index,
                                                                                   final List<String> selectedFields,
                                                                                   final FilterClause directSourceFilter,
                                                                                   final String followersField) {

        return entityModificationRequestBuilder.buildFollowersRemovalRequests(index, this.get(
                orgId, null, selectedFields, directSourceFilter), CommonSearchFields.FOLLOWER_IDS, followersField);
    }

    /**
     * <p>
     * Builds requests to update followers for direct source filter.
     * </p>
     *
     * @param orgId              Organization's id.
     * @param directSourceFilter {@link FilterClause}.
     * @param selectedFields     Selected fields.
     * @param followerIds        Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    private Collection<UpsertRequest> buildFollowersUpdateRequests(final Long orgId, final FilterClause directSourceFilter,
                                                                   final String entity, final List<String> selectedFields,
                                                                   final Collection<Long> followerIds) {
        final Collection<UpsertRequest> upsertRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, selectedFields, directSourceFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                upsertRequests.add(buildFollowerIdsForDirectSource(index, entity, followerIds, noteAndFileArray.getJsonObject(count)));
            }
        }
        return upsertRequests;
    }

    /**
     * <p>
     * Builds {@link UpsertRequest} for direct source.
     * </p>
     *
     * @param index           Index name.
     * @param entity          Entity's name.
     * @param noteAndFileJson Note/file json.
     * @param followerIds     Follower ids.
     * @return {@link UpsertRequest}.
     */
    private UpsertRequest buildFollowerIdsForDirectSource(final String index, final String entity,
                                                          final Collection<Long> followerIds,
                                                          final JsonObject noteAndFileJson) {
        final UpsertRequest upsertRequest = UpsertRequest.get(noteAndFileJson.getString(CommonSearchFields.ID), index);
        final JsonObject entityJson = noteAndFileJson.getJsonObject(entity);

        noteAndFileJson.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
        entityJson.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
        noteAndFileJson.put(entity, entityJson);
        upsertRequest.setDoc(noteAndFileJson);

        return upsertRequest;
    }

    /**
     * <p>
     * Modifies contact's company & company fields.
     * </p>
     *
     * @param isUpdate          whether action's update.
     * @param companyId         A company's id.
     * @param noteAndFileJson   Fetched json doc.
     * @param companyMap        Map holding required company details.
     * @param partialCompanyMap Map holding required partial company details.
     * @param updateRequest     {@link UpsertRequest}.
     */
    private void modifyCompanyFields(final boolean isUpdate, final Long companyId, final JsonObject noteAndFileJson,
                                     final Map<String, Object> companyMap, final Map<String, Object> partialCompanyMap,
                                     final UpsertRequest updateRequest) {
        final String source = noteAndFileJson.getString(CommonSearchFields.ENTITY_SOURCE);

        if (isUpdate) {
            entityFieldModifier.modifyEntity(companyId, CommonSearchFields.COMPANY, noteAndFileJson, companyMap, updateRequest);

            switch (source) {
                case CommonSearchFields.CONTACT -> entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.CONTACT,
                        noteAndFileJson, partialCompanyMap, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                case CommonSearchFields.DEAL -> {
                    entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.CONTACT, noteAndFileJson,
                            partialCompanyMap, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                    entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.DEAL, noteAndFileJson,
                            partialCompanyMap, updateRequest, CommonSearchNestedFields.DEAL_DOT_COMPANY);
                }
            }
        } else {
            entityFieldModifier.removeEntity(companyId, CommonSearchFields.COMPANY, noteAndFileJson, updateRequest);

            switch (source) {
                case CommonSearchFields.CONTACT -> entityFieldModifier.removeCompanyFromEntity(
                        companyId, CommonSearchFields.CONTACT, noteAndFileJson, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                case CommonSearchFields.DEAL -> {
                    entityFieldModifier.removeCompanyFromEntity(
                            companyId, CommonSearchFields.CONTACT, noteAndFileJson, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
                    entityFieldModifier.removeCompanyFromEntity(
                            companyId, CommonSearchFields.DEAL, noteAndFileJson, updateRequest, CommonSearchNestedFields.DEAL_DOT_COMPANY);
                }
            }
        }
    }

    /**
     * <p>
     * Loads company into the document.
     * </p>.
     *
     * @param company         {@link Company}.
     * @param noteAndFileJson Note/File json.
     * @param updateRequest   {@link UpsertRequest}.
     */
    private void loadCompanyIntoDoc(final Long orgId, final Company company, final JsonObject noteAndFileJson,
                                    final UpsertRequest updateRequest) {
        if (Objects.nonNull(company)) {
            noteAndFileJson.put(CommonSearchFields.COMPANY, constructEntity(orgId, company, NoteHolder.class, CommonSearchFields.NOTE));
            updateRequest.addFieldToRemove(CommonSearchFields.COMPANY);
        }
    }

    /**
     * <p>
     * Sets owner and territory id.
     * </p>
     *
     * @param ownerId         Owner's id.
     * @param territoryId     {@link UpsertRequest}.
     * @param updateRequest   {@link UpsertRequest}.
     * @param noteAndFileJson Note/File json.
     */
    private void setOwnerIdAndTerritoryId(final Long ownerId, final Long territoryId, final UpsertRequest updateRequest,
                                          final JsonObject noteAndFileJson) {

        if (Objects.isNull(territoryId)) {
            updateRequest.addFieldToRemove(CommonSearchFields.TERRITORY_ID);
        } else {
            noteAndFileJson.put(CommonSearchFields.TERRITORY_ID, territoryId);
        }
        noteAndFileJson.put(CommonSearchFields.OWNER_ID, ownerId);
    }

    /**
     * <p>
     * Modifies entity field.
     * </p>
     *
     * @param entityKey       Key name.
     * @param noteAndFileJson Note/file json.
     * @param updateRequest   {@link UpsertRequest}.
     * @param entityMap       Entity details map.
     */
    private void modifyEntityField(final String entityKey, final JsonObject noteAndFileJson, final UpsertRequest updateRequest,
                                   final Map<String, Object> entityMap) {
        noteAndFileJson.put(entityKey, entityMap);
        updateRequest.addFieldToRemove(entityKey);
    }

    /**
     * <p>
     * Sets entity source id and name from the retained entity map, and syncs source-owned follower ids.
     * </p>
     *
     * @param orgId           Organization's id.
     * @param source          Entity source name.
     * @param noteAndFileJson Doc about to be indexed.
     * @param updateRequest   {@link UpsertRequest}.
     * @param entityMap       Retained entity details.
     */
    private void setEntitySourceFields(final Long orgId, final String source, final JsonObject noteAndFileJson,
                                       final UpsertRequest updateRequest, final Map<String, Object> entityMap) {
        final Map<String, Object> mutableDoc = new HashMap<>();

        noteAndFileJson.put(CommonSearchFields.ENTITY_SOURCE_ID, entityMap.get(CommonSearchFields.ID));
        noteAndFileJson.put(CommonSearchFields.ENTITY_SOURCE_NAME, entityMap.get(CommonSearchFields.NAME));
        updateRequest.addFieldToRemove(CommonSearchFields.ENTITY_SOURCE_ID);
        updateRequest.addFieldToRemove(CommonSearchFields.ENTITY_SOURCE_NAME);
        noteAndFileJson.forEach(mutableDoc::put);
        indexDocumentLoader.loadFollowerIdsForSourceOwnedDoc(orgId, source, mutableDoc);

        if (mutableDoc.containsKey(CommonSearchFields.FOLLOWER_IDS)) {
            noteAndFileJson.put(CommonSearchFields.FOLLOWER_IDS, mutableDoc.get(CommonSearchFields.FOLLOWER_IDS));
        }
        updateRequest.addFieldToRemove(CommonSearchFields.FOLLOWER_IDS);
    }
}