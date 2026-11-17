package com.tlc.crm.core.features.action.tag;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.features.internal.status.TagErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.TagService;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Tag} related web actions
 * </p>
 *
 * @author EswariNivethaVU
 * @version 1.0
 * @see CrmConfigAction
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_ALL_TAGS)
@WebAction(path = "/tag/mgmt")
public class TagManagement extends CrmConfigAction<Tag> {

    private final static int DEFAULT_START_VALUE = 1;
    private final static int DEFAULT_LIMIT = 20;

    private final TagConverter tagConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final TagManager tagManager;
    private final SourceTagManager sourceTagManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final ProductManager productManager;
    private final UserService crmUserService;
    private final OrganizationService organizationService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final TagService tagService;
    protected final FieldService fieldService;
    protected final FieldSourceService fieldSourceService;
    private final ListStateProcessor listStateProcessor;

    public TagManagement() {
        tagConverter = TagConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        contactManager = ContactManager.getInstance();
        productManager = ProductManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        tagManager = TagManager.getInstance();
        sourceTagManager = SourceTagManager.getInstance();
        crmUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        tagService = TagService.getInstance();
        fieldService = FieldService.getSecureInstance();
        fieldSourceService = FieldSourceService.getInstance();
        listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TagFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(TagFields.DATA.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "list" -> fetchAndDispatchTagList(orgId, request.userId(), data, response);
            case "getTagsBySource" -> fetchSourceTagList(orgId, data, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AbstractConfigManager} of Tag
     */
    @Override
    public ConfigManager<Tag> getConfigManager() {
        return tagManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Tag} filled with the input json data
     */
    @Override
    public Tag convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return tagConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonObject} filled with the tag data
     */
    @Override
    public JsonObject convert(final Tag tag) {
        return tagConverter.convert(tag);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Tag} filled with the data
     */
    @Override
    public Tag convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return tagConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Builds the tag for given entity ids
     * </p>
     *
     * @param jsonObject Which Contains the {@link JsonObject}
     * @param tag        An entity that holds the tag attributes
     */
    private void buildTag(final JsonObject jsonObject, final Tag tag) {
        final String entityIds = TagFields.ENTITY_IDS.getName();

        if (jsonObject.containsKey(entityIds)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(entityIds);

            for (int index = 0; index < jsonArray.size(); index++) {
                tag.addEntityId(jsonArray.getLong(index));
            }
        } else {
            throw (ErrorCode.get(TagErrorCodes.ENTITY_IDS_REQUIRED, "i18n.tag.entity.not.found"));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the Organization id
     * @param userId Represents the user id.
     * @param action Performs action like create, update or delete
     * @param tags   {@link Collection} of tag
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Tag> tags) {

        switch (action) {
            case CREATE -> {
                final Collection<String> tagNames = tags.stream().map(Tag::getName).toList();

                if (!tagNames.isEmpty() && tagService.exists(orgId, tagNames)) {
                    throw ErrorCode.get(TagErrorCodes.TAG_NAME_ALREADY_EXISTS, "i18n.tag.name.already.exists");
                }
            }
            case UPDATE -> {
                final Collection<String> tagNames = tags.stream().map(Tag::getName).filter(Objects::nonNull).toList();
                final Collection<Long> tagIds = tags.stream().map(Tag::id).filter(Objects::nonNull).toList();

                if (!tagNames.isEmpty() && tagService.exists(orgId, tagNames, tagIds)) {
                    throw ErrorCode.get(TagErrorCodes.TAG_NAME_ALREADY_EXISTS, "i18n.tag.name.already.exists");
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Updates tags in bulk. For partial bulk update, supports {@code ids} with a common {@code color};
     * all other requests are handled by the super class.
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     * @param isPartial   A flag indicating whether the update is partial or full
     */
    @Override
    protected void bUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse, final EventData eventData, final boolean isPartial) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TagFields.DATA.getName());

        if (isPartial && data.containsKey(ActionFields.IDS)) {
            final JsonArray tagIds = data.getJsonArray(ActionFields.IDS);
            final String color = data.getString(TagFields.COLOR.getName());
            final Collection<Tag> tags = new ArrayList<>();

            for (int index = NumericalConstant.ZERO; index < tagIds.size(); index++) {
                final JsonObject partial = Json.object();

                partial.put(TagFields.ID.getName(), tagIds.getLong(index));
                partial.put(TagFields.COLOR.getName(), color);
                tags.add(convertPartialData(crmRequest, partial));
            }
            update(crmRequest.orgId(), crmRequest.userId(), crmResponse, data, eventData,
                    tags, requestJson.getString(TagFields.TYPE.getName()));
        } else {
            super.bUpdate(crmRequest, crmResponse, eventData, isPartial);
        }
    }

    /**
     * <p>
     * Check the given source id exists or not
     * </p>
     *
     * @param orgId      Represents Organization id
     * @param sourceName Represents the name of the source
     * @param entityIds  Represents the {@link Set} of entity id
     */
    private void checkSourceExistence(final Long orgId, final String sourceName, final Set<Long> entityIds) {
        switch (sourceName) {
            case Contact.ENTITY_TYPE -> {
                if (Objects.isNull(entityIds) || !contactManager.exists(orgId, entityIds)) {
                    throw ErrorCode.get(TagErrorCodes.ENTITY_IDS_NOT_FOUND, "i18n.tag.entity.not.found");
                }
            }
            case Deal.ENTITY_TYPE -> {
                if (Objects.isNull(entityIds) || !dealManager.exists(orgId, entityIds)) {
                    throw ErrorCode.get(TagErrorCodes.ENTITY_IDS_NOT_FOUND, "i18n.tag.entity.not.found");
                }
            }
            case Product.ENTITY_TYPE -> {
                if (Objects.isNull(entityIds) || !productManager.exists(orgId, entityIds)) {
                    throw ErrorCode.get(TagErrorCodes.ENTITY_IDS_NOT_FOUND, "i18n.tag.entity.not.found");
                }
            }
            case Company.ENTITY_TYPE -> {
                if (Objects.isNull(entityIds) || !companyManager.exists(orgId, entityIds)) {
                    throw ErrorCode.get(TagErrorCodes.ENTITY_IDS_NOT_FOUND, "i18n.tag.entity.not.found");
                }
            }
            default -> throw ErrorCode.get(TagErrorCodes.SOURCE_NOT_FOUND, "i18n.tag.entity.not.found");
        }
    }

    /**
     * <p>
     * Fetches a list of tags for the given organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the crm user {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTagList(final Long orgId, final Long userId, final JsonObject jsonObject,
                                         final CrmResponse crmResponse) {
        final ListPaginationState paginationState = new ListPaginationState(orgId, userId, ListType.TAG_LIST);
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(jsonObject, paginationState);
        final int start = limitJson.getInt(ListActionFields.START);
        final int limit = limitJson.getInt(ListActionFields.COUNT);
        final String searchText = jsonObject.optString(TagFields.SEARCH_TEXT.getName(), "");
        final long tagCount = tagManager.getTagCount(orgId, searchText);
        final String createdAtKey = TagFields.CREATED_AT.getName();
        final JsonObject sortJson = jsonObject.optJsonObject(TagFields.SORT.getName());
        final String field;
        final SortType sortType;

        if (Objects.nonNull(sortJson)) {
            field = sortJson.optString(TagFields.FIELD.getName(), createdAtKey);
            sortType = SortType.getSortType(sortJson.optString(TagFields.TYPE.getName(), TagFields.DESC.getName()));
        } else {
            field = createdAtKey;
            sortType = SortType.DESCENDING;
        }

        final Collection<Tag> tags = tagCount > 0 && start <= tagCount ? tagManager.getTagList(orgId, start, limit, field, sortType, searchText)
                : Collections.emptyList();
        final JsonArray jsonArray = Json.array();
        final Consumer<CrmResponse> responseConsumer = (response) -> {
            response.put(TagFields.TOTAL_RECORDS.getName(), tagCount);
            response.put(TagFields.HAS_MORE.getName(), (start + limit) <= tagCount);
            response.put(TagFields.LIST.getName(), jsonArray);
        };

        if (tags.isEmpty()) {
            responseConsumer.accept(crmResponse);
        } else {
            final Collection<Long> userIds = tags.stream().map(tag -> tag.getCreatedBy().id()).collect(Collectors.toUnmodifiableSet());
            final Collection<Long> iamUserIds = crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (final Tag tag : tags) {
                        final CrmUser crmUser = crmUserService.getUser(orgId, tag.getCreatedBy().id());
                        final JsonObject tagJson;

                        crmUser.setUser(userMap.get(crmUser.getIamId()));
                        tag.setCreatedBy(crmUser);
                        tagJson = tagConverter.convert(tag);
                        tagJson.put(TagFields.CRITERIA.getName(), buildTagCriteria(orgId, tag));
                        jsonArray.put(tagJson);
                    }
                    responseConsumer.accept(crmResponse);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(TagErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Builds one criteria entry per source (Company/Contact/Deal) that the given tag is actually used on.
     * Each entry's field id is resolved dynamically per source's "Tags" field.
     * </p>
     *
     * @param orgId Represents the organisation id
     * @param tag   Represents the {@link Tag}
     * @return {@link JsonArray} of criteria, one per source the tag is mapped to
     */
    private JsonObject buildTagCriteria(final Long orgId, final Tag tag) {
        final JsonObject criteriaJson = Json.object();
        final Long tagId = tag.id();
        final String tagName = tag.getName();

        final JsonObject valueJson = Json.object()
                .put(CriteriaFields.VALUE_ENTITY_ID.getName(), tagId)
                .put(CriteriaFields.VALUE.getName(), tagName);

        criteriaJson.put(CriteriaFields.CONNECTOR_ID.getName(), NumericalConstant.ONE);
        criteriaJson.put(CriteriaFields.COMPARATOR_ID.getName(), NumericalConstant.ONE);
        criteriaJson.put(CriteriaFields.VALUES.getName(), Json.array().put(valueJson));

        addSourceCriteria(criteriaJson, Source.COMPANY, Company.ENTITY_TYPE, CriteriaFields.COMPANY_CRITERIA.getName());
        addSourceCriteria(criteriaJson, Source.CONTACT, Contact.ENTITY_TYPE, CriteriaFields.CONTACT_CRITERIA.getName());
        addSourceCriteria(criteriaJson, Source.DEAL, Deal.ENTITY_TYPE, CriteriaFields.DEAL_CRITERIA.getName());

        return criteriaJson;
    }

    /**
     * Adds the source-specific criteria using the default Tags field.
     *
     * @param criteriaJson Criteria JSON
     * @param source       Represents the source
     * @param sourceName   Represents the source name
     * @param criteriaKey  Represents the criteria key
     */
    private void addSourceCriteria(final JsonObject criteriaJson, final Source source, final String sourceName, final String criteriaKey) {
        final Optional<FieldInfo> optionalTagFieldInfo = fieldService.getDefaultFieldByAliasName(source, FieldKeys.TAGS.getName());

        if (optionalTagFieldInfo.isEmpty()) {
            return;
        }
        final FieldSource fieldSource = fieldSourceService.getSource(sourceName);
        final JsonObject fieldJson = Json.object().put(CriteriaFields.FIELD_ID.getName(), optionalTagFieldInfo.get().id());
        final JsonObject sourceCriteriaJson = Json.object()
                .put(CriteriaFields.SOURCE_ID.getName(), fieldSource.id())
                .put(CriteriaFields.FIELD.getName(), fieldJson);

        criteriaJson.put(criteriaKey, sourceCriteriaJson);
    }

    /**
     * <p>
     * Fetches a list of tags for the given entity
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the crm user {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchSourceTagList(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final String sourceName = jsonObject.getString(TagFields.SOURCE.getName());
        final Long entityId = jsonObject.getLong(TagFields.ID.getName());
        final Collection<Tag> tags = sourceTagManager.getMappedTags(orgId, entityId, sourceName);
        final Optional<FieldInfo> optionalTagFieldInfo = fieldService.getDefaultFieldByAliasName(Source.get(sourceName), FieldKeys.TAGS.getName());

        optionalTagFieldInfo.ifPresent(fieldInfo -> crmResponse.put(FieldKeys.FIELD_ID.getName(), fieldInfo.id()));
        crmResponse.put(TagFields.TAGS.getName(), tagConverter.convertTagsForSummary(tags));
        crmResponse.send();
    }
}
