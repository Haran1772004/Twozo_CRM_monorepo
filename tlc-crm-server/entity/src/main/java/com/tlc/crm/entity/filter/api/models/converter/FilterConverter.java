package com.tlc.crm.entity.filter.api.models.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.FilterProvider;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.filter.fields.AdditionalOptionFields;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.filter.fields.FilterVisibilityAttributes;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.list.ProviderService;
import com.tlc.crm.fields.models.FieldSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Filter}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterConverter extends EntityConverter<Filter> {

    private final FilterManager filterManager;
    private final ProviderService providerService;
    private final UserService userService;
    private final ComparatorService comparatorService;

    private static class Instance {
        private static final FilterConverter INSTANCE = new FilterConverter();
    }

    public static FilterConverter getInstance() {
        return Instance.INSTANCE;
    }

    private FilterConverter() {
        filterManager = FilterManager.getInstance();
        providerService = ProviderService.getInstance();
        userService = UserService.getInstance();
        comparatorService = ComparatorService.getInstance();
    }

    /**
     * <p>
     * Converts the json object into {@link Filter} instance.
     * </p>
     *
     * @param converterConfig Converter configuration settings.
     * @param jsonObject      holds the filter attributes.
     * @return {@link Filter} instance that hold the converted data.
     */
    @Override
    public Filter convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructFilterByPartialData(converterConfig, jsonObject)
                : constructFilter(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts the json object into {@link Filter} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      holds the filter attributes.
     * @return {@link Filter} instance that hold the converted data.
     */
    private Filter constructFilter(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Filter filter = new Filter();
        final String idKey = FilterFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            filter.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, filter, jsonObject);

        return filter;
    }

    /**
     * <p>
     * Converts the partial data to {@link Filter} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      holds the filter attributes.
     * @return {@link Filter} instance that hold the converted data.
     */
    private Filter constructFilterByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Filter filter = filterManager.get(converterConfig.orgId(), jsonObject.getLong(FilterFields.ID.getName()));

        setJsonDataToModel(converterConfig, filter, jsonObject);

        return filter;
    }

    /**
     * <p>
     * Sets the json data into {@link Filter} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param filter          a model that holds the attributes of filter.
     * @param jsonObject      holds the filter attributes.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final Filter filter, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final Long userId = converterConfig.ownerId();
        final String nameKey = FilterFields.NAME.getName();
        final String filterVisibilityKey = FilterFields.FILTER_VISIBILITY.getName();
        final String sourceIdKey = FilterFields.SOURCE_ID.getName();

        filter.setOrgId(orgId);
        filter.setName(jsonObject.containsKey(nameKey) ? (String) jsonObject.opt(nameKey) : filter.getName());
        filter.setCreator(userService.getUser(orgId, userId));
        setProvider(filter, jsonObject.getLong(sourceIdKey));

        if (jsonObject.containsKey(filterVisibilityKey)) {
            filter.setFilterVisibility(constructFilterVisibility(orgId, jsonObject.getJsonObject(filterVisibilityKey)));
        }

        if (jsonObject.containsKey(FilterFields.CRITERIA_LIST.getName())) {
            setCriteriaDetails(converterConfig, filter, jsonObject);
        }
    }

    /**
     * <p>
     * Sets the data provider instance in {@link Filter}.
     * </p>
     *
     * @param filter   a model that holds the attributes of filter.
     * @param sourceId id of the source.
     */
    private void setProvider(final Filter filter, final Long sourceId) {
        final FilterProvider filterProvider = new FilterProvider();

        filterProvider.setId(providerService.getIdBySourceId(sourceId));
        filter.setFilterProvider(filterProvider);
    }

    /**
     * <p>
     * Constructs the {@link FilterVisibility} from the given {@link JsonObject}.
     * </p>
     *
     * @param orgId          Represents the organisation id.
     * @param visibilityJson Holds the details of the {@link FilterVisibility}.
     */
    private FilterVisibility constructFilterVisibility(final Long orgId, final JsonObject visibilityJson) {
        final FilterVisibility filterVisibility = new FilterVisibility();
        final String idKey = FilterVisibilityAttributes.ID.getName();
        final String usersKey = FilterVisibilityAttributes.USERS.getName();
        final String teamsKey = FilterVisibilityAttributes.TEAMS.getName();
        final String territoriesKey = FilterVisibilityAttributes.TERRITORIES.getName();
        final String visibilityPermissionIdKey = FilterVisibilityAttributes.VISIBILITY_PERMISSION_ID.getName();

        if (visibilityJson.containsKey(idKey)) {
            filterVisibility.setId(visibilityJson.getLong(idKey));
        }
        filterVisibility.setOrgId(orgId);
        filterVisibility.setPreventShare(visibilityJson.optBoolean(FilterVisibilityAttributes.PREVENT_SHARE.getName()));
        filterVisibility.setVisibility(Visibility.get(visibilityJson.getShort(FilterVisibilityAttributes.VISIBILITY_ID.getName())));

        if (visibilityJson.containsKey(visibilityPermissionIdKey)) {
            filterVisibility.setVisibilityPermission(VisibilityPermission.get(visibilityJson.getShort(visibilityPermissionIdKey)));
        }

        if (visibilityJson.containsKey(usersKey)) {
            filterVisibility.setUsers(constructUsers(orgId, visibilityJson.getJsonArray(usersKey)));
        }

        if (visibilityJson.containsKey(teamsKey)) {
            filterVisibility.setTeams(constructTeams(visibilityJson.getJsonArray(teamsKey)));
        }

        if (visibilityJson.containsKey(territoriesKey)) {
            filterVisibility.setTerritories(constructTerritories(visibilityJson.getJsonArray(territoriesKey)));
        }

        return filterVisibility;
    }

    /**
     * <p>
     * Constructs the collection of {@link Territory} from {@link JsonArray}.
     * </p>
     *
     * @param jsonArray Holds the ids of {@link Territory}.
     * @return Collection of constructed {@link Territory}.
     */
    private Collection<Territory> constructTerritories(final JsonArray jsonArray) {
        final Collection<Territory> territories = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final Territory territory = new Territory();

            territory.setId(jsonArray.getLong(index));
            territories.add(territory);
        }

        return territories;
    }

    /**
     * <p>
     * Constructs the collection of {@link Team} from {@link JsonArray}.
     * </p>
     *
     * @param jsonArray Holds the ids of {@link Team}.
     * @return Collection of constructed {@link Team}.
     */
    private Collection<Team> constructTeams(final JsonArray jsonArray) {
        final Collection<Team> teams = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final Team team = new Team();

            team.setId(jsonArray.getLong(index));
            teams.add(team);
        }

        return teams;
    }

    /**
     * <p>
     * Constructs the collection of {@link CrmUser} from {@link JsonArray}.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param jsonArray Holds the ids of {@link CrmUser}.
     * @return Collection of constructed {@link CrmUser}.
     */
    private Collection<CrmUser> constructUsers(final Long orgId, final JsonArray jsonArray) {
        final Collection<CrmUser> users = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final CrmUser user = userService.getUser(orgId, jsonArray.getLong(index));

            if (Objects.nonNull(user)) {
                users.add(user);
            }
        }

        return users;
    }

    /**
     * <p>
     * Sets the criteria details in {@link Filter} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param filter          a model that holds the attributes of filter.
     * @param jsonObject      holds the filter attributes.
     */
    private void setCriteriaDetails(final ConverterConfig converterConfig, final Filter filter, final JsonObject jsonObject) {
        final String criteriaKey = FilterFields.CRITERIA_LIST.getName();
        final String idKey = CriteriaFields.ID.getName();
        final String connectorIdKey = CriteriaFields.CONNECTOR_ID.getName();
        final String valuesKey = CriteriaFields.VALUES.getName();
        final String fieldKey = CriteriaFields.FIELD.getName();
        final String comparatorIdKey = CriteriaFields.COMPARATOR_ID.getName();
        final String sourceIdKey = CriteriaFields.SOURCE_ID.getName();
        final Collection<FilterCriteria> oldFilterCriteriaList = filter.getCriteriaList();
        final boolean isPartialData = converterConfig.isPartialData();

        filter.setCriteriaList(null);

        if (jsonObject.containsKey(criteriaKey)) {
            final JsonArray criteriaArray = jsonObject.getJsonArray(criteriaKey);

            for (int index = 0; index < criteriaArray.size(); index++) {
                final JsonObject criteriaJson = criteriaArray.optJsonObject(index);
                final FilterCriteria filterCriteria = new FilterCriteria();

                if (criteriaJson.containsKey(idKey)) {
                    filterCriteria.setId(criteriaJson.getLong(idKey));
                }

                if (criteriaJson.containsKey(sourceIdKey)) {
                    setFieldSource(criteriaJson.getLong(sourceIdKey), filterCriteria);
                }

                if (criteriaJson.containsKey(fieldKey)) {
                    setFilterField(filterCriteria, criteriaJson.getJsonObject(fieldKey));
                }

                if (criteriaJson.containsKey(comparatorIdKey)) {
                    setComparator(filterCriteria, criteriaJson.getLong(comparatorIdKey));
                }

                if (criteriaJson.containsKey(connectorIdKey)) {
                    filterCriteria.setConnector(FilterConnector.get(criteriaJson.getInt(connectorIdKey)));
                }

                if (criteriaJson.containsKey(valuesKey)) {
                    setCriteriaValue(filterCriteria, criteriaJson.getJsonArray(valuesKey));
                }
                filter.addCriteria(filterCriteria);
            }

            if (isPartialData) {
                final Set<Long> currentCriteriaIds = filter.getCriteriaList().stream()
                        .map(FilterCriteria::getId).filter(Objects::nonNull).collect(Collectors.toSet());

                oldFilterCriteriaList.stream().filter(oldCriteria -> !currentCriteriaIds.contains(oldCriteria.getId()))
                        .forEach(filter::addCriteria);
            }
        } else if (isPartialData) {
            filter.setCriteriaList(oldFilterCriteriaList);
        }
    }

    /**
     * <p>
     * Sets the field source details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param sourceId       field source id
     * @param filterCriteria a model that holds the attributes of filter criteria.
     */
    private void setFieldSource(final long sourceId, final FilterCriteria filterCriteria) {
        final FieldSource fieldSource = new FieldSource();

        fieldSource.setId(sourceId);
        filterCriteria.setFieldSource(fieldSource);
    }

    /**
     * <p>
     * Constructs the additional option from additional option json.
     * </p>
     *
     * @param additionalOptionJson which holds the values of additional option.
     * @return {@link AdditionalOption} instance.
     */
    private AdditionalOption constructAdditionalOption(final JsonObject additionalOptionJson) {
        final String optionNameKey = AdditionalOptionFields.NAME.getName();
        final String additionalOptionEntityIdKey = AdditionalOptionFields.ENTITY_ID.getName();

        if (additionalOptionJson.containsKey(additionalOptionEntityIdKey) && additionalOptionJson.containsKey(optionNameKey)) {
            final AdditionalOption additionalOption = new AdditionalOption();
            final String idKey = AdditionalOptionFields.ID.getName();
            final String additionalOptionKey = FilterFieldAttributes.ADDITIONAL_OPTION.getName();

            additionalOption.setOptionEntityId(additionalOptionJson.getLong(additionalOptionEntityIdKey));
            additionalOption.setOptionName(additionalOptionJson.getString(optionNameKey));

            if (additionalOptionJson.containsKey(idKey)) {
                additionalOption.setId(additionalOptionJson.getLong(idKey));
            }

            if (additionalOptionJson.containsKey(additionalOptionKey)) {
                final JsonObject childAdditionalOptionJson = additionalOptionJson.getJsonObject(additionalOptionKey);

                additionalOption.setAdditionalOption(constructAdditionalOption(childAdditionalOptionJson));
            }

            return additionalOption;
        } else {
            throw ErrorCode.get(FilterErrorCodes.INVALID_ADDITIONAL_OPTION);
        }
    }

    /**
     * <p>
     * Sets the field details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param filterCriteria a model that holds the attributes of filterCriteria.
     * @param fieldJson      holds the field attributes.
     */
    private void setFilterField(final FilterCriteria filterCriteria, final JsonObject fieldJson) {
        final boolean isField = fieldJson.containsKey(FilterFieldAttributes.FIELD_ID.getName());
        final String additionalOptionKey = FilterFieldAttributes.ADDITIONAL_OPTION.getName();
        final Long filterFieldId = fetchFilterFieldId(fieldJson);
        final CriteriaField criteriaField = isField ? new CriteriaField() : new CriteriaOption();

        if (fieldJson.containsKey(additionalOptionKey)) {
            criteriaField.setAdditionalOption(constructAdditionalOption(fieldJson.getJsonObject(additionalOptionKey)));
        }
        criteriaField.setId(filterFieldId);
        filterCriteria.setCriteriaField(criteriaField);
    }

    /**
     * <p>
     * Fetches the filter field id from field json.
     * </p>
     *
     * @param fieldJson which holds the values of filter field.
     * @return id of the filter field.
     */
    private Long fetchFilterFieldId(final JsonObject fieldJson) {
        final String fieldIdKey = FilterFieldAttributes.FIELD_ID.getName();
        final String optionIdKey = FilterFieldAttributes.OPTION_ID.getName();
        final long filterFieldId;

        if (fieldJson.containsKey(fieldIdKey)) {
            filterFieldId = fieldJson.getLong(fieldIdKey);
        } else if (fieldJson.containsKey(optionIdKey)) {
            filterFieldId = fieldJson.getLong(optionIdKey);
        } else {
            throw ErrorCode.get(FilterErrorCodes.FILTER_FIELD_ID_NOT_FOUND);
        }

        return filterFieldId;
    }

    /**
     * <p>
     * Sets the comparator details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param filterCriteria a model that holds the attributes of filterCriteria.
     * @param comparatorId   comparator id.
     */
    private void setComparator(final FilterCriteria filterCriteria, final long comparatorId) {
        final Comparator comparator = comparatorService.get(comparatorId);

        filterCriteria.setComparator(comparator);
    }

    /**
     * <p>
     * Sets the value details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param filterCriteria a model that holds the attributes of filterCriteria.
     * @param jsonArray      holds the values attribute.
     */
    private void setCriteriaValue(final FilterCriteria filterCriteria, final JsonArray jsonArray) {
        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);
            final String idKey = CriteriaValueAttributes.ID.getName();
            final String valueKey = CriteriaValueAttributes.VALUE.getName();
            final String valueEntityIdKey = CriteriaValueAttributes.VALUE_ENTITY_ID.getName();
            final CriteriaValue criteriaValue = new CriteriaValue();

            if (jsonObject.containsKey(valueKey)) {
                criteriaValue.setValue(jsonObject.get(valueKey));
            } else {
                throw ErrorCode.get(FilterErrorCodes.VALUE_NOT_FOUND);
            }

            if (jsonObject.containsKey(idKey)) {
                criteriaValue.setId(jsonObject.getLong(idKey));
            }

            if (jsonObject.containsKey(valueEntityIdKey)) {
                criteriaValue.setValueEntityId(jsonObject.getLong(valueEntityIdKey));
            }
            filterCriteria.addCriteriaValue(criteriaValue);
        }
    }

    /**
     * <p>
     * Converts {@link Filter} into {@link JsonObject}.
     * </p>
     *
     * @param filter a model that holds the attributes of filter.
     * @return {@link JsonObject} holds the filter attributes.
     */
    @Override
    public JsonObject convert(final Filter filter) {
        return null;
    }
}
