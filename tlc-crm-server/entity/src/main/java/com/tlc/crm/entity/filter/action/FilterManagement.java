package com.tlc.crm.entity.filter.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.filter.api.models.converter.FilterConverter;
import com.tlc.crm.entity.filter.fields.AdditionalOptionFields;
import com.tlc.crm.entity.filter.fields.ComparatorFields;
import com.tlc.crm.entity.filter.fields.ConnectorFields;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.filter.fields.FieldSourceAttributes;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.filter.fields.FilterVisibilityAttributes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.entity.service.filter.FilterSourceService;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.i18n.I18nResolver;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.validator.TlcModel;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Filter} related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see CrmConfigAction
 */
@SourceAuthorizer(actions = com.tlc.crm.security.permission.model.Action.Type.SHARE_VIEW, needCommonApiHandling = true)
@WebAction(path = "/filter/mgmt")
public class FilterManagement extends CrmConfigAction<Filter> {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final FilterConverter filterConverter;
    private final FilterManager filterManager;
    private final ComparatorService comparatorService;
    private final FilterFieldService filterFieldService;
    private final FilterSourceService filterSourceService;
    private final FieldTypeService fieldTypeService;
    private final DateTimeUtil dateTimeUtil;
    private final OrganizationService organizationService;
    private final com.tlc.iam.client.UserService userService;
    private final UserService crmUserService;
    private final CrmUserConverter crmUserConverter;
    private final TeamConverter teamConverter;
    private final TerritoryConverter territoryConverter;

    public FilterManagement() {
        filterManager = FilterManager.getInstance();
        filterConverter = FilterConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        comparatorService = ComparatorService.getInstance();
        filterFieldService = FilterFieldService.getSecureInstance();
        filterSourceService = FilterSourceService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userService = com.tlc.iam.client.UserService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.crmUserConverter = CrmUserConverter.getInstance();
        this.teamConverter = TeamConverter.getInstance();
        this.territoryConverter = TerritoryConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  which holds the crm request.
     * @param response which holds the crm response.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData = crmEventRequest.getEventData();

        switch (requestJson.getString(FieldKeys.TYPE.getName())) {
            case "getFilter" -> fetchAndDispatchFilter(orgId, data, request, response);
            case "deleteFilter" -> {
                final Long filterId = data.getLong("id");
                final Map<Long, Filter.Permissions> permissions = filterManager.getPermissions(orgId, userId, List.of(filterId));

                if (permissions.containsKey(filterId) && permissions.get(filterId).isDeletable()) {
                    delete(response, userId, get(orgId, filterId), eventData);
                    return;
                }
                throw ErrorCode.get(FilterErrorCodes.ACCESS_DENIED, String.format(response.resolveI18nString("i18n.filter.delete.access.restriction.error"), orgId, filterId));
            }
            case "updateFavouriteStatus" -> {
                final Long filterId = data.getLong(FilterFields.ID.getName());

                filterManager.updateFavouriteFilter(orgId, filterId, userId, data.getBoolean(FilterFields.IS_FAVORITE.getName()));
                response.put(ApiKeys.UPDATED_RECORD, filterId);
            }
            case "requestDetails" -> {
                response.put("URL", request.exchange().getURL());
                response.put("URI", request.exchange().getURI());
            }
        }
    }

    /**
     * Fetches the filtered data based on the input parameters and dispatches the result
     * to the appropriate handler using the provided request and response objects.
     *
     * @param orgId    The ID of the organization for which the filter is applied.
     * @param data     The JSON input containing filter criteria and other metadata.
     * @param request  The incoming CRM request object.
     * @param response The CRM response object to send the filtered result.
     */
    private void fetchAndDispatchFilter(final long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Filter filter = get(orgId, data.getLong(FilterFields.ID.getName()));
        final FilterVisibility filterVisibility = filter.getFilterVisibility();

        if (Objects.nonNull(filterVisibility)) {
            final Collection<CrmUser> crmUsers = filterVisibility.getUsers();

            if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                final Collection<Long> userIds = crmUsers.stream().map(CrmUser::id).collect(Collectors.toUnmodifiableSet());
                response.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    /**
                     * {@inheritDoc}
                     *
                     * @param users Refers the collection of users.
                     */
                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmUser crmUser : crmUsers) {
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                            }
                            response.put(FilterFields.FILTER.getName(), convert(orgId, request.userId(), filter, request.i18nResolver(), response));
                        } catch (Throwable exception) {
                            sendError(response, null, ErrorCode.get(FilterErrorCodes.FILTER_NOT_FOUND, "i18n.filter.not.found", exception));
                        } finally {
                            response.send();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     *
                     * @param throwable Cause of the failure.
                     */
                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(response, null, ErrorCode.get(EntityErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
                    }
                };
                userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream()
                        .map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
            } else {
                response.put(FilterFields.FILTER.getName(), convert(orgId, request.userId(), filter, request.i18nResolver(), response));
            }
        } else {
            response.put(FilterFields.FILTER.getName(), convert(orgId, request.userId(), filter, request.i18nResolver(), response));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ConfigManager} instance.
     */
    @Override
    public ConfigManager<Filter> getConfigManager() {
        return filterManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request
     * @param jsonObject Holds entity details
     * @return
     */
    @Override
    public Filter convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return filterConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param filter which holds the values of filter.
     * @return {@link JsonObject} instance holds the filter values as json.
     */
    @Override
    public JsonObject convert(final Filter filter) {
        return filterConverter.convert(filter);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    which holds the crm request.
     * @param jsonObject which holds the filter values as json.
     * @return {@link Filter} instance.
     */
    @Override
    public Filter convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return filterConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Organization id
     * @param userId  Represents the user id.
     * @param action  Perform action like create, update or delete
     * @param filters Collection of models
     * @return true if the given details are valid
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Filter> filters) {
        return switch (action) {
            case CREATE -> validate(orgId, filters, Collections.emptyMap(), Collections.emptyMap());
            case UPDATE -> {
                final Set<Long> filterIds = filters.stream().map(Filter::id).collect(Collectors.toSet());
                final Map<Long, Filter.Permissions> permissions = filterManager.getPermissions(orgId, userId, filterIds);

                for (final Map.Entry<Long, Filter.Permissions> filterPermission : permissions.entrySet()) {
                    if (!filterPermission.getValue().isEditable()) {
                        throw ErrorCode.get(FilterErrorCodes.ACCESS_DENIED, String.format("i18n.filter.edit.access.restriction.error", orgId, filterPermission.getKey()));
                    }
                }
                yield validate(orgId, filters, permissions, filterManager.getVisibility(orgId, filterIds));
            }
            default -> true;
        };
    }

    /**
     * <p>
     * Validates the {@link Filter} entities.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param filters            Which holds the values of filter.
     * @param permissions        Holds details of the filter permissions.
     * @param filterVisibilities Holds details of the filter visibilities.
     * @return A flag that indicates whether the given {@link Filter} entities are valid or not.
     */
    private boolean validate(final Long orgId, final Collection<Filter> filters, final Map<Long, Filter.Permissions> permissions,
                             final Map<Long, FilterVisibility> filterVisibilities) {
        for (final Filter filter : filters) {
            final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();
            final FilterVisibility filterVisibility = filter.getFilterVisibility();
            final Set<Long> sourceIds = new HashSet<>();
            final Set<Long> comparatorIds = new HashSet<>();
            final Long filterId = filter.id();
            final Filter.Permissions filterPermissions = permissions.get(filterId);

            for (final FilterCriteria criteria : criteriaList) {
                final Comparator comparator = criteria.getComparator();
                final FieldSource fieldSource = criteria.getFieldSource();
                final CriteriaField criteriaField = criteria.getCriteriaField();

                if (Objects.nonNull(comparator) && Objects.nonNull(comparator.getId())) {
                    comparatorIds.add(comparator.getId());
                }

                if (Objects.nonNull(fieldSource) && Objects.nonNull(fieldSource.id())) {
                    sourceIds.add(fieldSource.id());
                }

                if (!(Objects.nonNull(criteriaField) && filterFieldService.exists(orgId, criteriaField.getId(),
                        !(criteriaField instanceof CriteriaOption)))) {
                    return false;
                }
            }

            if (Objects.nonNull(filterPermissions) && !filterPermissions.isShareable() && validateVisibilityChanges(filterVisibilities.get(filterId), filterVisibility)) {
                return false;
            }

            if ((!comparatorIds.isEmpty() && !comparatorService.exists(comparatorIds))
                    || (!sourceIds.isEmpty() && !filterSourceService.exists(sourceIds))) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Validates the changes of the old {@link FilterVisibility} and new {@link FilterVisibility}
     * </p>
     *
     * @param oldFilterVisibility Holds the details of the old filter visibility.
     * @param newFilterVisibility Holds the details of the new filter visibility.
     * @return A flag that indicates whether the visibility of the filter has been changed or not.
     */
    private boolean validateVisibilityChanges(final FilterVisibility oldFilterVisibility, final FilterVisibility newFilterVisibility) {
        if (Objects.equals(newFilterVisibility, oldFilterVisibility)) {
            final BiFunction<Collection<? extends TlcModel>, Collection<? extends TlcModel>, Boolean> modelChangesValidator = (oldModels, newModels) -> {
                final Set<Long> oldModelIds = Objects.isNull(oldModels) ? Collections.emptySet() : oldModels.stream().map(TlcModel::id).collect(Collectors.toUnmodifiableSet());
                final Set<Long> newModelIds = Objects.isNull(newModels) ? Collections.emptySet() : newModels.stream().map(TlcModel::id).collect(Collectors.toUnmodifiableSet());

                if (oldModelIds.isEmpty() && newModelIds.isEmpty()) {
                    return false;
                }

                return !Objects.equals(oldModelIds, newModelIds);
            };

            return modelChangesValidator.apply(oldFilterVisibility.getUsers(), newFilterVisibility.getUsers())
                    || modelChangesValidator.apply(oldFilterVisibility.getTeams(), newFilterVisibility.getTeams())
                    || modelChangesValidator.apply(oldFilterVisibility.getTerritories(), newFilterVisibility.getTerritories());
        }
        return true;
    }

    /**
     * <p>
     * Converts the {@link Filter} into {@link JsonObject}.
     * </p>
     *
     * @param orgId       Represents an org id
     * @param userId      Represents an user id
     * @param filter      which holds the values of {@link Filter}.
     * @param crmResponse needs to resolve i18n keys / strings.
     * @return {@link JsonObject} object holds the model values as json.
     */
    private JsonObject convert(final long orgId, final long userId, final Filter filter, final I18nResolver i18nResolver, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final CrmUser creator = filter.getCreator();

        jsonObject.put(FilterFields.ID.getName(), filter.id());
        jsonObject.put(FilterFields.NAME.getName(), filter.getName());
        jsonObject.put(FilterFields.SOURCE_ID.getName(), filter.getFilterProvider().getFieldSource().id());
        jsonObject.put(FilterFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(filter.getCreatedTime()));
        jsonObject.put(FilterFields.MODIFIED_TIME.getName(), dateTimeUtil.getTimeStamp(filter.getModifiedTime()));
        jsonObject.put(FilterFields.IS_DEFAULT.getName(), filter.isDefault());
        jsonObject.put(FilterFields.FILTER_VISIBILITY.getName(), convertFilterVisibilityIntoJson(userId, filter.getFilterVisibility(), crmResponse));
        jsonObject.put(FilterFields.CRITERIA_LIST.getName(), convertCriteriaListIntoJson(orgId, filter.getCriteriaList(), crmResponse, i18nResolver));

        if (Objects.nonNull(creator)) {
            jsonObject.put(FilterFields.IS_SHARED_FILTER.getName(), !Objects.equals(creator.id(), userId));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the list of {@link FilterCriteria} into {@link JsonArray}.
     * </p>
     *
     * @param filterCriteriaList a list that holds the attributes of criteria.
     * @param crmResponse        needs to resolve i18n keys / strings.
     * @return {@link JsonArray} holds the criteria attributes.
     */
    private JsonArray convertCriteriaListIntoJson(final long orgId, final Collection<FilterCriteria> filterCriteriaList,
                                                  final CrmResponse crmResponse, final I18nResolver i18nResolver) {
        return filterCriteriaList.stream().collect(Json::array, (jsonArray, criteria) ->
                jsonArray.put(convertCriteriaIntoJson(orgId, criteria, crmResponse, i18nResolver)), (jsonArray, ignoredJsonArray) -> {
        });
    }

    /**
     * <p>
     * Converts the {@link FilterCriteria} into {@link JsonObject}.
     * </p>
     *
     * @param filterCriteria a model that holds the attributes of filterCriteria.
     * @param crmResponse    needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the filterCriteria attributes.
     */
    private JsonObject convertCriteriaIntoJson(final long orgId, final FilterCriteria filterCriteria, final CrmResponse crmResponse,
                                               final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final FilterConnector filterConnector = filterCriteria.getConnector();
        final List<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();

        if (Objects.nonNull(filterConnector)) {
            jsonObject.put(CriteriaFields.CONNECTOR.getName(), convertConnectorIntoJson(filterConnector, crmResponse));
        }

        if (Objects.nonNull(criteriaValues)) {
            jsonObject.put(CriteriaFields.VALUES.getName(), (JsonArray) criteriaValues.stream().collect(Json::array,
                    (jsonArray, value) -> jsonArray.put(convertValueIntoJson(value)), (jsonArray, ignoredJsonArray) -> {
                    }));
        }
        jsonObject.put(CriteriaFields.ID.getName(), filterCriteria.getId());
        jsonObject.put(CriteriaFields.SOURCE.getName(), convertFieldSourceIntoJson(filterCriteria.getFieldSource(), crmResponse));
        jsonObject.put(CriteriaFields.FIELD.getName(), convertFieldIntoJson(filterCriteria.getCriteriaField(), crmResponse));
        jsonObject.put(CriteriaFields.COMPARATOR.getName(), convertComparatorIntoJson(filterCriteria.getComparator(), crmResponse));

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link FieldSource} into {@link JsonObject}.
     * </p>
     *
     * @param fieldSource holds the values of {@link FieldSource}.
     * @return {@link JsonObject}.
     */
    private JsonObject convertFieldSourceIntoJson(final FieldSource fieldSource, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldSourceAttributes.ID.getName(), fieldSource.id());
        jsonObject.put(FieldSourceAttributes.NAME.getName(), crmResponse.resolveI18nPlaceholder(fieldSource.getDisplayName()));

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Comparator} into {@link JsonObject}.
     * </p>
     *
     * @param comparator  a model that holds the attributes of comparator.
     * @param crmResponse needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the comparator attributes.
     */
    private JsonObject convertComparatorIntoJson(final Comparator comparator, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ComparatorFields.ID.getName(), comparator.getId());
        jsonObject.put(ComparatorFields.NAME.getName(), crmResponse.resolveI18nString(comparator.getName()));
        jsonObject.put(ComparatorFields.HAS_VALUE.getName(), comparator.isValueNeeded());
        jsonObject.put(ComparatorFields.IS_MULTI_VALUE_ALLOWED.getName(), comparator.isMultiValueAllowed());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link CriteriaField} into {@link JsonObject}.
     * </p>
     *
     * @param criteriaField a model that holds the attributes of criteriaField.
     * @param crmResponse   needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the criteriaField attributes.
     */
    private JsonObject convertFieldIntoJson(final CriteriaField criteriaField, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final FieldType fieldType = criteriaField.getFieldType();
        final AdditionalOption additionalOption = criteriaField.getAdditionalOption();

        jsonObject.put(FilterFieldAttributes.NAME.getName(), crmResponse.resolveI18nPlaceholder(criteriaField.getName()));
        jsonObject.put(FilterFieldAttributes.FIELD_TYPE_ID.getName(), fieldType.id());
        jsonObject.put(criteriaField instanceof CriteriaOption ? FilterFieldAttributes.OPTION_ID.getName()
                : FilterFieldAttributes.FIELD_ID.getName(), criteriaField.getId());

        if (Objects.nonNull(additionalOption)) {
            jsonObject.put(FilterFieldAttributes.ADDITIONAL_OPTION.getName(),
                    convertAdditionalOptionsToJson(additionalOption, fieldType.storeType(), crmResponse));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link AdditionalOption} into {@link JsonObject}.
     * </p>
     *
     * @param additionalOption a model that holds the attributes of {@link AdditionalOption}.
     * @param storeType        {@link StoreType} indicates the type of fields.
     * @param crmResponse      needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the additional option attributes.
     */
    private JsonObject convertAdditionalOptionsToJson(final AdditionalOption additionalOption, final StoreType storeType,
                                                      final CrmResponse crmResponse) {
        final AdditionalOption childAdditionalOption = additionalOption.getAdditionalOption();
        final JsonObject additionalOptionJson = Json.object();
        final Long entityId = additionalOption.getOptionEntityId();
        final String name = additionalOption.getOptionName();
        final FieldSubType subType = fieldTypeService.getSubType(entityId);

        additionalOptionJson.put(AdditionalOptionFields.ID.getName(), additionalOption.getId());
        additionalOptionJson.put(AdditionalOptionFields.ENTITY_ID.getName(), entityId);
        additionalOptionJson.put(AdditionalOptionFields.NAME.getName(), Objects.isNull(name) ? subType.name() : name);
        additionalOptionJson.put(AdditionalOptionFields.DISPLAY_NAME.getName(), storeType.hasSubtypes()
                ? crmResponse.resolveI18nString(subType.displayName()) : name);

        if (Objects.nonNull(childAdditionalOption)) {
            additionalOptionJson.put(FilterFieldAttributes.ADDITIONAL_OPTION.getName(),
                    convertAdditionalOptionsToJson(childAdditionalOption, storeType, crmResponse));
        }

        return additionalOptionJson;
    }

    /**
     * <p>
     * Converts the {@link FilterConnector} into {@link JsonObject}.
     * </p>
     *
     * @param filterConnector a model that holds the attributes of connector.
     * @param crmResponse     needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the connector attributes.
     */
    private JsonObject convertConnectorIntoJson(final FilterConnector filterConnector, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ConnectorFields.ID.getName(), filterConnector.getId());
        jsonObject.put(ConnectorFields.NAME.getName(), filterConnector.name());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link CriteriaValue} into {@link JsonObject}.
     * </p>
     *
     * @param criteriaValue a model that holds the attributes of criteriaValue.
     * @return {@link JsonObject} holds the criteriaValue attributes.
     */
    private JsonObject convertValueIntoJson(final CriteriaValue criteriaValue) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CriteriaValueAttributes.ID.getName(), criteriaValue.getId());
        jsonObject.put(CriteriaValueAttributes.VALUE.getName(), criteriaValue.getValue());
        jsonObject.put(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), criteriaValue.getValueEntityId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link FilterVisibility} into {@link JsonObject}.
     * </p>
     *
     * @param userId           Represents the user id.
     * @param filterVisibility a model that holds the attributes of filterVisibility.
     * @param crmResponse      needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the filterVisibility attributes.
     */
    private JsonObject convertFilterVisibilityIntoJson(final long userId, final FilterVisibility filterVisibility, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final Collection<CrmUser> users = filterVisibility.getUsers();
        final Collection<Team> teams = filterVisibility.getTeams();
        final Collection<Territory> territories = filterVisibility.getTerritories();
        final VisibilityPermission visibilityPermission = filterVisibility.getVisibilityPermission();

        jsonObject.put(FilterVisibilityAttributes.ID.getName(), filterVisibility.getId());
        jsonObject.put(FilterVisibilityAttributes.PREVENT_SHARE.getName(), filterVisibility.isPreventShare());
        jsonObject.put(FilterVisibilityAttributes.VISIBILITY_ID.getName(), filterVisibility.getVisibility().getId());

        if (Objects.nonNull(visibilityPermission)) {
            jsonObject.put(FilterVisibilityAttributes.VISIBILITY_PERMISSION_ID.getName(),visibilityPermission.getId());
        }

        if (Objects.nonNull(users) && !users.isEmpty()) {
            jsonObject.put(FilterVisibilityAttributes.USERS.getName(), crmUserConverter.convertUsersIntoJson(userId, users));
        }

        if (Objects.nonNull(teams) && !teams.isEmpty()) {
            jsonObject.put(FilterVisibilityAttributes.TEAMS.getName(), teamConverter.convertTeamsIntoJson(teams));
        }

        if (Objects.nonNull(territories) && !territories.isEmpty()) {
            jsonObject.put(FilterVisibilityAttributes.TERRITORIES.getName(), territoryConverter.convertTerritoriesIntoJson(territories));
        }

        return jsonObject;
    }
}
