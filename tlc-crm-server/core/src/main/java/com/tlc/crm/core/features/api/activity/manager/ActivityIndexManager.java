package com.tlc.crm.core.features.api.activity.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.internal.index.field.IndexEntityFieldModifier;
import com.tlc.crm.core.internal.index.field.IndexPermissionFieldModifier;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.search.fields.CommonSearchNestedFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.Optional;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Activity}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ActivityIndexManager extends AbstractIndexManager<ActivityHolder> {

    private static final int ZERO = 0;

    private final IndexDataFetcher indexDataFetcher;
    private final IndexEntityFieldModifier entityFieldModifier;
    private final IndexPermissionFieldModifier permissionFieldModifier;

    private static class Instance {
        private static final ActivityIndexManager INSTANCE = new ActivityIndexManager();
    }

    private ActivityIndexManager() {
        indexDataFetcher = IndexDataFetcher.get();
        entityFieldModifier = IndexEntityFieldModifier.getInstance();
        permissionFieldModifier = IndexPermissionFieldModifier.getInstance();
    }

    /**
     * <p>
     * Builds the {@link ActivityIndexManager}.
     * </p>
     *
     * @return {@link ActivityIndexManager}.
     */
    public static ActivityIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.ACTIVITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadAdditionalFieldsIntoDoc(final Long orgId, final ActivityHolder activityHolder, final Map<String, Object> doc) {
        final Activity activity = activityHolder.getActivity();
        final ActivityType activityType = activity.getType();
        final Map<String, Object> activityTypeMap = (Map<String, Object>) doc.get(CommonSearchFields.ACTIVITY_TYPE);

        if (Objects.nonNull(activityTypeMap)) {
            final Map<String, Object> iconDetails = new HashMap<>();
            final Icon activityIcon;
            final Optional<Icon> optionalIcon = Optional.ofNullable(activityType.getActivityIcon());

            if (optionalIcon.isPresent()) {
                activityIcon = optionalIcon.get();
            } else {
                final Long iconId = SourceActivityManager.getInstance().getIconId(orgId, activity.id());

                activityIcon = IconService.getInstance().getIconById(Icon.Type.ACTIVITY, iconId);
            }

            if (Objects.nonNull(activityIcon)) {
                iconDetails.put(CommonSearchFields.ID, activityIcon.id());
                iconDetails.put(CommonSearchFields.NAME, activityIcon.getName());
                activityTypeMap.put(CommonSearchFields.ICON, iconDetails);
            }
        }
    }

    /**
     * <p>
     * Updates check in/out info.
     * </p>
     *
     * @param orgId      An organization's id.
     * @param activityId Activity's id.
     * @param isCheckIn  Whether the operation is check in.
     * @param location   Location details.
     */
    public void updateCheckInOutInfo(final Long orgId, final Long activityId, final boolean isCheckIn,
                                     final long timeStamp, final String location) {
        final FilterClause filter = new FilterClause(new Criteria(activityId, Comparator.EQUAL, CommonSearchFields.ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID), filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String checkInOutField;
            final String timeStampField;

            if (isCheckIn) {
                checkInOutField = CommonSearchFields.CHECK_IN_INFO;
                timeStampField = CommonSearchFields.CHECK_IN_TIME;
            } else {
                checkInOutField = CommonSearchFields.CHECK_OUT_INFO;
                timeStampField = CommonSearchFields.CHECK_OUT_TIME;
            }
            final String index = this.getSearchIndex().getName();
            final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(activityId), index);

                updateRequest.setDoc(Map.of(checkInOutField, location, timeStampField, timeStamp));
                bulkWriteRequest.addUpdateRequest(updateRequest);
            }
            writableService().performBulkOperations(bulkWriteRequest);
        }
    }

    /**
     * <p>
     * Updates {@link CommonSearchFields#IS_DONE} for the given activities in the search index.
     * </p>
     *
     * @param orgId       An organization's id.
     * @param activityIds Activity ids to update in the index.
     * @param isDone      Whether the activities are marked as done.
     */
    public void updateIsDone(final Long orgId, final Collection<Long> activityIds, final boolean isDone) {
        final FilterClause filter = new FilterClause(new Criteria(activityIds, Comparator.CONTAINS, CommonSearchFields.ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID), filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(activityJson.getString(CommonSearchFields.ID), index);

                updateRequest.setDoc(Map.of(CommonSearchFields.IS_DONE, isDone));
                bulkWriteRequest.addUpdateRequest(updateRequest);
            }

            if (bulkWriteRequest.hasRequests()) {
                writableService().performBulkOperations(bulkWriteRequest);
            }
        }
    }

    /**
     * <p>
     * Builds requests to modify deal and it's related fields.
     * </p>
     *
     * @param orgId        Represents an organization's id.
     * @param filterClause A deal's id.
     * @param isUpdate     Whether the action's update.
     * @param dealMap      Map holding deal card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildDealModificationRequests(final Long orgId, final FilterClause filterClause,
                                                                   final boolean isUpdate, final Map<String, Object> dealMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID, CommonSearchFields.DEAL,
                CommonSearchFields.COMPANY, CommonSearchFields.CONTACT_IDS), filterClause);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final Long activityId = activityJson.getLong(CommonSearchFields.ID);
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(activityId), index);

                modifyDealRelatedFields(orgId, isUpdate, activityJson, updateRequest, dealMap);
                updateRequest.setDoc(activityJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to modify contact and it's related fields.
     * </p>
     *
     * @param contact    {@link Contact}.
     * @param isUpdate   Whether the action's update.
     * @param contactMap Map holding contact card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactModificationRequests(final Long orgId, final Contact contact, final boolean isUpdate,
                                                                      final Map<String, Object> contactMap) {
        return buildContactModificationRequests(orgId, contact.id(), contact, isUpdate, contactMap);
    }

    /**
     * <p>
     * Builds requests to modify contact and it's related fields for the given contact id.
     * </p>
     *
     * @param orgId      Organization's id.
     * @param contactId  Contact id used to locate documents.
     * @param contact    {@link Contact} used to construct nested contact details.
     * @param isUpdate   Whether the action's update.
     * @param contactMap Map holding contact card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactModificationRequests(final Long orgId, final Long contactId, final Contact contact,
                                                                      final boolean isUpdate, final Map<String, Object> contactMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final FilterClause contactIdFilter = new FilterClause(
                new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_DOT_ID)).or(
                new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_CONTACT_DOT_ID)).or(
                new Criteria(contactId, Comparator.CONTAINS, CommonSearchFields.CONTACT_IDS));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.CONTACT_IDS,
                CommonSearchFields.ID, CommonSearchFields.CONTACT, CommonSearchFields.DEAL, CommonSearchFields.COMPANY), contactIdFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();
            final Map<String, Object> partialContactMap = isUpdate ? constructEntity(orgId, contact, Deal.class, index) : null;

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final Long activityId = activityJson.getLong(CommonSearchFields.ID);
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(activityId), index);

                modifyContactFields(orgId, isUpdate, contactId, activityJson, contactMap, partialContactMap, updateRequest);
                updateRequest.setDoc(activityJson);
                updateRequests.add(updateRequest);
            }
        }

        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to modify company and it's related fields.
     * </p>
     *
     * @param company    {@link Company}.
     * @param isUpdate   Whether the action's update.
     * @param companyMap Map holding company card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyModificationRequests(final Long orgId, final Company company, final boolean isUpdate,
                                                                      final Map<String, Object> companyMap) {
        return buildCompanyModificationRequests(orgId, company.id(), company, isUpdate, companyMap);
    }

    /**
     * <p>
     * Builds requests to modify company and it's related fields for the given company id.
     * </p>
     *
     * @param orgId      Organization's id.
     * @param companyId  Company id used to locate documents.
     * @param company    {@link Company} used to construct nested company details.
     * @param isUpdate   Whether the action's update.
     * @param companyMap Map holding company card details.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyModificationRequests(final Long orgId, final Long companyId, final Company company,
                                                                      final boolean isUpdate, final Map<String, Object> companyMap) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final FilterClause companyIdFilter = new FilterClause(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_COMPANY_DOT_ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.CONTACT_IDS,
                CommonSearchFields.ID, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.DEAL), companyIdFilter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();
            final Map<String, Object> partialCompanyMap = isUpdate ? constructEntity(orgId, company, Deal.class, index) : null;

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(activityJson.getString(CommonSearchFields.ID), index);

                modifyCompanyRelatedFields(orgId, isUpdate, companyId, activityJson, companyMap, partialCompanyMap, updateRequest);
                updateRequest.setDoc(activityJson);
                updateRequests.add(updateRequest);
            }
        }

        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to update deal followers & permission related fields.
     * </p>
     *
     * @param orgId           Represents an organization's id.
     * @param isUpdate        Whether the action is update.
     * @param filterClause    {@link FilterClause}.
     * @param dealFollowerIds Deal Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildDealFollowersModificationRequests(final Long orgId, final boolean isUpdate,
                                                                            final FilterClause filterClause,
                                                                            final Collection<Long> dealFollowerIds) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();
        final String index = this.getSearchIndex().getName();
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID, CommonSearchFields.DEAL,
                CommonSearchFields.COMPANY, CommonSearchFields.CONTACT_IDS), filterClause);

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(activityJson.getString(CommonSearchFields.ID), index);

                if (isUpdate) {
                    permissionFieldModifier.addFollowersToEntity(CommonSearchFields.DEAL, activityJson, dealFollowerIds);
                } else {
                    permissionFieldModifier.removeFollowersFromEntity(
                            CommonSearchFields.DEAL, activityJson, updateRequest, CommonSearchNestedFields.DEAL_DOT_FOLLOWERS_ID);
                }
                setPermissionRelatedFields(orgId, activityJson, updateRequest, dealFollowerIds,
                        Map.of(CommonSearchFields.COMPANY, true, CommonSearchFields.DEAL, false));
                updateRequest.setDoc(activityJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Builds requests to modify contact followers & permission related fields.
     * </p>
     *
     * @param orgId              Represents an organization's id.
     * @param contactId          Represents contact's id.
     * @param isUpdate           Whether the action is update.
     * @param contactFollowerIds Contact's Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildContactFollowersModificationRequests(final Long orgId, final Long contactId,
                                                                               final boolean isUpdate,
                                                                               final Collection<Long> contactFollowerIds) {
        final Collection<UpsertRequest> upsertRequests = new ArrayList<>();
        final FilterClause filter = new FilterClause(
                new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_DOT_ID)).or(
                new Criteria(contactId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_CONTACT_DOT_ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID, CommonSearchFields.CONTACT_IDS,
                CommonSearchFields.CONTACT, CommonSearchFields.DEAL, CommonSearchFields.COMPANY), filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();
            final Collection<Long> emptyList = Collections.emptyList();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(activityJson.getString(CommonSearchFields.ID), index);
                final JsonObject contactJson = activityJson.optJsonObject(CommonSearchFields.CONTACT);

                if (isUpdate) {
                    permissionFieldModifier.addContactFollowersToEntity(contactId, CommonSearchFields.DEAL, activityJson, contactFollowerIds);
                } else {
                    permissionFieldModifier.removeContactFollowersFromEntity(contactId, CommonSearchFields.CONTACT, activityJson,
                            updateRequest, CommonSearchNestedFields.DEAL_CONTACT_DOT_FOLLOWER_IDS);
                }

                if (!Json.isEmpty(contactJson) && Objects.equals(contactId, contactJson.getLong(CommonSearchFields.ID))) {
                    final Long skippingContactId = isUpdate ? null : contactId;

                    if (isUpdate) {
                        permissionFieldModifier.addFollowersToEntity(CommonSearchFields.CONTACT, activityJson, contactFollowerIds);
                    } else {
                        updateRequest.addFieldToRemove(CommonSearchNestedFields.CONTACT_DOT_FOLLOWER_IDS);
                    }
                    setPermissionRelatedFields(orgId, skippingContactId, activityJson, updateRequest, emptyList,
                            CommonSearchFields.DEAL, CommonSearchFields.COMPANY);
                }
                updateRequest.setDoc(activityJson);
                upsertRequests.add(updateRequest);
            }
        }
        return upsertRequests;
    }

    /**
     * <p>
     * Builds requests to modify company followers & permission related fields.
     * </p>
     *
     * @param orgId              Represents an organization's id.
     * @param companyId          Company's id.
     * @param isUpdate           Whether the action is update.
     * @param companyFollowerIds Follower ids.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildCompanyFollowersModificationRequests(final Long orgId, final Long companyId,
                                                                               final boolean isUpdate,
                                                                               final Collection<Long> companyFollowerIds) {
        final Collection<UpsertRequest> upsertRequests = new ArrayList<>();
        final FilterClause filter = new FilterClause(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.DEAL_COMPANY_DOT_ID)).or(
                new Criteria(companyId, Comparator.EQUAL, CommonSearchNestedFields.CONTACT_COMPANY_DOT_ID));
        final JsonObject fetchedJson = ActivityIndexManager.getInstance().get(orgId, null, List.of(CommonSearchFields.ID,
                CommonSearchFields.CONTACT, CommonSearchFields.DEAL, CommonSearchFields.COMPANY, CommonSearchFields.CONTACT_IDS), filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = this.getSearchIndex().getName();
            final Collection<Long> emptyList = Collections.emptyList();
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject activityJson = activityArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(activityJson.getString(CommonSearchFields.ID), index);
                final JsonObject companyJson = activityJson.optJsonObject(CommonSearchFields.COMPANY);
                final Collection<Long> activityCompanyFollowerIds;

                if (!Json.isEmpty(companyJson) && Objects.equals(companyId, companyJson.getLong(CommonSearchFields.ID))) {
                    activityCompanyFollowerIds = companyFollowerIds;

                    if (isUpdate) {
                        companyJson.put(CommonSearchFields.FOLLOWER_IDS, companyFollowerIds);
                        activityJson.put(CommonSearchFields.COMPANY, companyJson);
                    } else {
                        companyJson.remove(CommonSearchFields.FOLLOWER_IDS);
                        activityJson.put(CommonSearchFields.COMPANY, companyJson);
                        updateRequest.addFieldToRemove(CommonSearchNestedFields.COMPANY_DOT_FOLLOWER_IDS);
                    }
                } else {
                    activityCompanyFollowerIds = emptyList;
                }

                if (isUpdate) {
                    permissionFieldModifier.addCompanyFollowersToEntity(companyId, CommonSearchFields.CONTACT, activityJson, companyFollowerIds);
                    permissionFieldModifier.addCompanyFollowersToEntity(companyId, CommonSearchFields.DEAL, activityJson, companyFollowerIds);
                } else {
                    permissionFieldModifier.removeCompanyFollowersFromEntity(companyId, CommonSearchFields.CONTACT, activityJson,
                            updateRequest, CommonSearchNestedFields.CONTACT_COMPANY_DOT_FOLLOWER_IDS);
                    permissionFieldModifier.removeCompanyFollowersFromEntity(companyId, CommonSearchFields.DEAL, activityJson,
                            updateRequest, CommonSearchNestedFields.DEAL_COMPANY_DOT_FOLLOWER_IDS);
                }
                setPermissionRelatedFields(orgId, activityJson, updateRequest, activityCompanyFollowerIds,
                        Map.of(CommonSearchFields.COMPANY, false, CommonSearchFields.DEAL, true));
                updateRequest.setDoc(activityJson);
                upsertRequests.add(updateRequest);
            }
        }
        return upsertRequests;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValuesIntoModel(final ActivityHolder activityHolder) {
        final Long orgId = activityHolder.orgId();
        final LinkedList<Contact> contacts = (LinkedList<Contact>) activityHolder.getContacts();
        final Activity activity = activityHolder.getActivity();
        final ActivityType activityType = activity.getType();

        contacts.stream().findFirst().ifPresent(primaryContact -> {
            contacts.remove(ZERO);
            contacts.add(ZERO, indexDataFetcher.fetchContact(orgId, primaryContact.id()));
        });

        if (Objects.nonNull(activityType) && Objects.isNull(activityType.getName())) {
            activity.setType(indexDataFetcher.fetchActivityType(orgId, activityType.id()));
        }
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param activityHolder {@link ActivityHolder}.
     * @param doc            Holds the entity data to be indexed.
     */
    @Override
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final ActivityHolder activityHolder,
                                                    final Map<String, Object> doc) {
        if (Objects.nonNull(activityHolder)) {
            final Activity activity = activityHolder.getActivity();
            final Collection<Long> contactIds = activityHolder.getContacts().stream().map(Contact::id).toList();
            final Collection<Long> collaboratorIds = activityHolder.getActivity().getCollaboratorIds();
            final Collection<Long> ownerIds = new HashSet<>();
            final Long salesOwnerId = activity.getAssigneeId();
            final Long ownerId = Objects.isNull(salesOwnerId) ? ActivityManager.getInstance().getAssigneeId(
                    orgId, activity.id()) : salesOwnerId;

            if (!contactIds.isEmpty()) {
                doc.put(CommonSearchFields.CONTACT_IDS, contactIds);
            }

            if (!collaboratorIds.isEmpty()) {
                ownerIds.addAll(collaboratorIds);
            }

            ownerIds.add(ownerId);
            doc.put(CommonSearchFields.OWNER_ID, ownerId);
            doc.put(CommonSearchFields.OWNER_IDS, ownerIds);
            IndexDocumentLoader.get().loadFollowerIdsForActivityIndex(orgId, doc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
        IndexDocumentLoader.get().loadFollowerIds(orgId, className, doc);
    }

    /**
     * <p>
     * Modifies deal related fields.
     * </p>
     *
     * @param orgId         Represents an organization's id.
     * @param isUpdate      Whether the action's update.
     * @param activityJson  Activity json.
     * @param updateRequest {@link UpsertRequest}.
     * @param dealMap       Map containing deal details.
     */
    private void modifyDealRelatedFields(final Long orgId, final boolean isUpdate, final JsonObject activityJson,
                                         final UpsertRequest updateRequest, final Map<String, Object> dealMap) {
        if (isUpdate) {
            activityJson.put(CommonSearchFields.DEAL, dealMap);
        }
        setPermissionRelatedFields(orgId, null, isUpdate, activityJson, updateRequest, dealMap,
                CommonSearchFields.COMPANY);
        updateRequest.addFieldToRemove(CommonSearchFields.DEAL);
    }

    /**
     * <p>
     * Modifies deal's company, contact's fields.
     * </p>
     *
     * @param isUpdate          Whether the action's update.
     * @param contactId         Represents contact's id.
     * @param activityJson      Activity json.
     * @param contactMap        Map holding contact card details.
     * @param partialContactMap Map holding partial contact details.
     * @param updateRequest     {@link UpsertRequest}.
     */
    private void modifyContactFields(final Long orgId, final boolean isUpdate, final Long contactId, final JsonObject activityJson,
                                     final Map<String, Object> contactMap, final Map<String, Object> partialContactMap,
                                     final UpsertRequest updateRequest) {
        final JsonObject contactJson = activityJson.optJsonObject(CommonSearchFields.CONTACT);

        if (isUpdate) {
            entityFieldModifier.modifyEntityContact(contactId, CommonSearchFields.DEAL, activityJson, partialContactMap,
                    updateRequest,  CommonSearchNestedFields.DEAL_DOT_CONTACT);
        } else {
            entityFieldModifier.removeContactFromEntity(contactId, CommonSearchFields.DEAL, activityJson, updateRequest,
                    CommonSearchNestedFields.DEAL_DOT_CONTACT);
        }

        if (Objects.nonNull(contactJson) && Objects.equals(contactId, contactJson.getLong(CommonSearchFields.ID))) {
            final Long skippingContactId;

            if (isUpdate) {
                activityJson.put(CommonSearchFields.CONTACT, contactMap);
                skippingContactId = null;
            } else {
                skippingContactId = contactId;
            }
            setPermissionRelatedFields(orgId, skippingContactId, isUpdate, activityJson, updateRequest, contactMap,
                    CommonSearchFields.DEAL, CommonSearchFields.COMPANY);
            updateRequest.addFieldToRemove(CommonSearchFields.CONTACT);
        }

        if (isUpdate && Objects.nonNull(contactMap)) {
            rewriteContactIds(contactId, (Long) contactMap.get(CommonSearchFields.ID), activityJson, updateRequest);
        }
    }

    /**
     * <p>
     * Rewrites {@code contactIds}: maps duplicate → retained, drops leftover duplicate ids, and de-dupes.
     * </p>
     *
     * @param duplicateContactId Duplicate contact id being merged away.
     * @param retainedContactId  Retained contact id.
     * @param activityJson       Activity document.
     * @param updateRequest      {@link UpsertRequest}.
     */
    private void rewriteContactIds(final Long duplicateContactId, final Long retainedContactId,
                                   final JsonObject activityJson, final UpsertRequest updateRequest) {
        final Collection<Long> contactIds = (Collection<Long>) activityJson.opt(CommonSearchFields.CONTACT_IDS);

        if (Objects.isNull(contactIds) || contactIds.isEmpty()) {
            return;
        }
        final LinkedHashSet<Long> rewrittenContactIds = new LinkedHashSet<>();
        boolean changed = false;

        for (final Long contactId : contactIds) {
            if (Objects.isNull(contactId)) {
                continue;
            }

            if (Objects.equals(contactId, duplicateContactId)) {
                rewrittenContactIds.add(retainedContactId);
                changed = true;
            } else {
                rewrittenContactIds.add(contactId);
            }
        }

        if (rewrittenContactIds.remove(duplicateContactId)) {
            changed = true;
        }

        if (changed || rewrittenContactIds.size() != contactIds.size()) {
            activityJson.put(CommonSearchFields.CONTACT_IDS, rewrittenContactIds);
            updateRequest.addFieldToRemove(CommonSearchFields.CONTACT_IDS);
        }
    }

    /**
     * <p>
     * Modifies deal's company, contact's company & company fields.
     * </p>
     *
     * @param isUpdate          whether action's update.
     * @param companyId         A company's id.
     * @param activityJson      Fetched json doc.
     * @param companyMap        Map holding required company details.
     * @param partialCompanyMap Map holding required partial company details.
     * @param updateRequest     {@link UpsertRequest}.
     */
    private void modifyCompanyRelatedFields(final Long orgId, final boolean isUpdate, final Long companyId, final JsonObject activityJson,
                                            final Map<String, Object> companyMap, final Map<String, Object> partialCompanyMap,
                                            final UpsertRequest updateRequest) {
        final JsonObject companyJson = activityJson.optJsonObject(CommonSearchFields.COMPANY);

        if (Objects.nonNull(companyJson) && Objects.equals(companyId, companyJson.getLong(CommonSearchFields.ID))) {
            if (isUpdate) {
                activityJson.put(CommonSearchFields.COMPANY, companyMap);
            }
            setPermissionRelatedFields(orgId, null, isUpdate, activityJson, updateRequest, companyMap,
                    CommonSearchFields.DEAL);
            updateRequest.addFieldToRemove(CommonSearchFields.COMPANY);
        }

        if (isUpdate) {
            entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.CONTACT, activityJson, partialCompanyMap,
                    updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
            entityFieldModifier.modifyEntityCompany(companyId, CommonSearchFields.DEAL, activityJson, partialCompanyMap,
                    updateRequest, CommonSearchNestedFields.DEAL_DOT_COMPANY);
        } else {
            entityFieldModifier.removeCompanyFromEntity(
                    companyId, CommonSearchFields.CONTACT, activityJson, updateRequest, CommonSearchNestedFields.CONTACT_DOT_COMPANY);
            entityFieldModifier.removeCompanyFromEntity(
                    companyId, CommonSearchFields.DEAL, activityJson, updateRequest, CommonSearchNestedFields.DEAL_DOT_COMPANY);
        }
    }

    /**
     * <p>
     * Sets permission related fields.
     * </p>
     *
     * @param orgId             Represents an organization's id.
     * @param skippingContactId Skipping Contact id.
     * @param isUpdate          Whether the action's update.
     * @param activityJson      Activity json.
     * @param entityMap         Map holding contact card details.
     * @param otherEntities     Entities such as deal/contact/company.
     */
    private void setPermissionRelatedFields(final Long orgId, final Long skippingContactId, final boolean isUpdate,
                                            final JsonObject activityJson, final UpsertRequest updateRequest,
                                            final Map<String, Object> entityMap, final String... otherEntities) {
        final Map<String, Collection<Long>> contactPermissionRelatedIdsMap = getContactPermissionRelatedIdsMap(
                orgId, activityJson, skippingContactId);
        final Collection<Long> ownerIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.OWNER_IDS);
        final Collection<Long> territoryIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.TERRITORY_IDS);

        if (isUpdate) {
            addOwnerAndTerritoryIds(ownerIds, territoryIds, entityMap);
        }
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, otherEntities);
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, updateRequest);
    }

    /**
     * <p>
     * Sets permission related fields.
     * </p>
     *
     * @param orgId             Represents an organization's id.
     * @param skippingContactId Skipping   Contact id.
     * @param activityJson      Activity json.
     * @param updateRequest     {@link UpsertRequest}.
     * @param followerIds       Followers ids.
     * @param otherEntities     Entities such as deal/contact/company.
     */
    private void setPermissionRelatedFields(final Long orgId, final Long skippingContactId, final JsonObject activityJson,
                                            final UpsertRequest updateRequest, final Collection<Long> followerIds,
                                            final String... otherEntities) {
        final Map<String, Collection<Long>> contactPermissionRelatedIdsMap = getContactPermissionRelatedIdsMap(
                orgId, activityJson, skippingContactId);
        final Collection<Long> ownerIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.OWNER_IDS);
        final Collection<Long> territoryIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.TERRITORY_IDS);

        ownerIds.addAll(followerIds);
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, otherEntities);
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, updateRequest);
    }

    /**
     * <p>
     * Sets permission related fields.
     * </p>
     *
     * @param orgId                  Represents an organization's id.
     * @param activityJson           Activity json.
     * @param updateRequest          {@link UpsertRequest}.
     * @param followerIds            Followers ids.
     * @param followersPermissionMap Key & value pairs of source name and whether it should be added to ownerIds field.
     */
    private void setPermissionRelatedFields(final Long orgId, final JsonObject activityJson,
                                            final UpsertRequest updateRequest, final Collection<Long> followerIds,
                                            final Map<String, Boolean> followersPermissionMap) {
        final Map<String, Collection<Long>> contactPermissionRelatedIdsMap = getContactPermissionRelatedIdsMap(
                orgId, activityJson, null);
        final Collection<Long> ownerIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.OWNER_IDS);
        final Collection<Long> territoryIds = contactPermissionRelatedIdsMap.get(CommonSearchFields.TERRITORY_IDS);

        ownerIds.addAll(followerIds);
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, followersPermissionMap);
        addOwnerAndTerritoryIds(activityJson, ownerIds, territoryIds, updateRequest);
    }

    /**
     * <p>
     * Adds owner/territory/follower from entity field values.
     * </p>
     *
     * @param activityJson Fetched activity json.
     * @param ownerIds     Holds deal/contact/company owner ids.
     * @param territoryIds Holds deal/contact/company territory ids.
     * @param entities     Entities such as deal/contact/company.
     */
    private void addOwnerAndTerritoryIds(final JsonObject activityJson, final Collection<Long> ownerIds,
                                         final Collection<Long> territoryIds, final String... entities) {

        for (final String entity : entities) {
            final JsonObject entityJson = activityJson.optJsonObject(entity);

            if (Objects.nonNull(entityJson)) {
                final Collection<Long> followerIds = (Collection<Long>) entityJson.opt(CommonSearchFields.FOLLOWER_IDS);

                if (entityJson.containsKey(CommonSearchFields.TERRITORY_ID)) {
                    territoryIds.add(entityJson.getLong(CommonSearchFields.TERRITORY_ID));
                }

                if (Objects.nonNull(followerIds) && !followerIds.isEmpty()) {
                    ownerIds.addAll(followerIds);
                }
                ownerIds.add(entityJson.getLong(CommonSearchFields.OWNER_ID));
            }
        }
    }

    /**
     * <p>
     * Adds owner/territory/follower from entity field values.
     * </p>
     *
     * @param activityJson           Fetched activity json.
     * @param ownerIds               Holds deal/contact/company owner ids.
     * @param territoryIds           Holds deal/contact/company territory ids.
     * @param followersPermissionMap Key & value pairs of source name and whether it should be added to ownerIds field.
     */
    private void addOwnerAndTerritoryIds(final JsonObject activityJson, final Collection<Long> ownerIds,
                                         final Collection<Long> territoryIds,
                                         final Map<String, Boolean> followersPermissionMap) {

        for (final Map.Entry<String, Boolean> permissionMap : followersPermissionMap.entrySet()) {
            final JsonObject entityJson = activityJson.optJsonObject(permissionMap.getKey());

            if (Objects.nonNull(entityJson)) {
                if (permissionMap.getValue()) {
                    final Collection<Long> followerIds = (Collection<Long>) entityJson.opt(CommonSearchFields.FOLLOWER_IDS);

                    if (Objects.nonNull(followerIds) && !followerIds.isEmpty()) {
                        ownerIds.addAll(followerIds);
                    }
                }

                if (entityJson.containsKey(CommonSearchFields.TERRITORY_ID)) {
                    territoryIds.add(entityJson.getLong(CommonSearchFields.TERRITORY_ID));
                }
                ownerIds.add(entityJson.getLong(CommonSearchFields.OWNER_ID));
            }
        }
    }

    /**
     * <p>
     * Adds owner/territory/follower from entity field values.
     * </p>
     *
     * @param entityMap    Map holding entity card details.
     * @param ownerIds     Owner ids.
     * @param territoryIds Territory ids.
     */
    private void addOwnerAndTerritoryIds(final Collection<Long> ownerIds, final Collection<Long> territoryIds,
                                         final Map<String, Object> entityMap) {
        final Collection<Long> followerIds = (Collection<Long>) entityMap.get(CommonSearchFields.FOLLOWER_IDS);

        if (Objects.nonNull(followerIds) && !followerIds.isEmpty()) {
            ownerIds.addAll(followerIds);
        }
        ownerIds.add((Long) entityMap.get(CommonSearchFields.OWNER_ID));
        territoryIds.add((Long) entityMap.get(CommonSearchFields.TERRITORY_ID));
    }

    /**
     * <p>
     * Adds owner and territory ids in activity json.
     * </p>
     *
     * @param activityJson Activity's json.
     * @param ownerIds     Holds deal/contact/company owner ids.
     * @param territoryIds Holds deal/contact/company territory ids.
     */
    private void addOwnerAndTerritoryIds(final JsonObject activityJson, final Collection<Long> ownerIds,
                                         final Collection<Long> territoryIds, final UpsertRequest updateRequest) {

        if (territoryIds.isEmpty()) {
            updateRequest.addFieldToRemove(CommonSearchFields.TERRITORY_IDS);
        } else {
            activityJson.put(CommonSearchFields.TERRITORY_IDS, territoryIds);
        }
        activityJson.put(CommonSearchFields.OWNER_IDS, ownerIds);
    }

    /**
     * <p>
     * Gets contact permission related ids map.
     * </p>
     *
     * @param orgId             Represents an organization's id.
     * @param activityJson      An activity's json
     * @param skippingContactId Contact id to be removed
     * @return contact permission related ids map.
     */
    private Map<String, Collection<Long>> getContactPermissionRelatedIdsMap(final Long orgId, final JsonObject activityJson,
                                                                            final Long skippingContactId) {
        final Collection<Long> contactsIds = (Collection<Long>) activityJson.opt(CommonSearchFields.CONTACT_IDS);

        if (Objects.nonNull(contactsIds) && !contactsIds.isEmpty()) {
            contactsIds.remove(skippingContactId);

            return indexDataFetcher.getContactPermissionRelatedIdsMap(orgId, contactsIds);
        }

        return Map.of(CommonSearchFields.OWNER_IDS, new HashSet<>(), CommonSearchFields.TERRITORY_IDS, new HashSet<>());
    }
}