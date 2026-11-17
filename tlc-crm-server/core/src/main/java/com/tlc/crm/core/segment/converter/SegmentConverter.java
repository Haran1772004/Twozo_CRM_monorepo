package com.tlc.crm.core.segment.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.fields.SegmentFolderFields;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.core.segment.service.SegmentFolderService;
import com.tlc.crm.core.segment.service.SegmentService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.AdditionalOptionFields;
import com.tlc.crm.entity.filter.fields.ComparatorFields;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.filter.fields.FieldSourceAttributes;
import com.tlc.crm.entity.filter.fields.ConnectorFields;
import com.tlc.crm.entity.filter.fields.FilterFieldAttributes;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.FilterSourceService;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Segment}
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentConverter extends EntityConverter<Segment> {

    private final UserService userService;
    private final FilterSourceService filterSourceService;
    private final ComparatorService comparatorService;
    private final SegmentFolderService segmentFolderService;
    private final FieldSourceService fieldSourceService;
    private final FieldTypeService fieldTypeService;
    private final SegmentService segmentService;
    private final SegmentGroupConverter segmentGroupConverter;

    private static class Instance {
        private static final SegmentConverter INSTANCE = new SegmentConverter();
    }

    public static SegmentConverter getInstance() {
        return Instance.INSTANCE;
    }

    private SegmentConverter() {
        userService = UserService.getInstance();
        filterSourceService = FilterSourceService.getInstance();
        comparatorService = ComparatorService.getInstance();
        segmentFolderService = SegmentFolderService.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        this.segmentService = SegmentService.getInstance();
        this.segmentGroupConverter = SegmentGroupConverter.getInstance();
    }

    /**
     * <p>
     * Converts the json object into {@link Segment} instance.
     * </p>
     *
     * @param converterConfig Converter configuration settings.
     * @param jsonObject      holds the segment attributes.
     * @return {@link Segment} instance that hold the converted data.
     */
    @Override
    public Segment convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructSegmentByPartialData(converterConfig, jsonObject)
                : constructSegment(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts {@link Segment} into {@link JsonObject}.
     * </p>
     *
     * @param converterConfig Converter configuration settings.
     * @param segment         a model that holds the attributes of segment.
     * @return {@link JsonObject} holds the segment attributes.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Segment segment) {
        final JsonObject jsonObject = Json.object();
        final SegmentFolder segmentFolder = segment.getFolder();

        jsonObject.put(SegmentFields.ID.getName(), segment.id())
                .put(SegmentFields.NAME.getName(), segment.getName())
                .put(SegmentFields.STATUS_ID.getName(), segment.getStatus().getId())
                .put(SegmentFields.AUDIT.getName(), convertAuditJson(segment));

        if (Objects.nonNull(segmentFolder)) {
            jsonObject.put(SegmentFields.FOLDER.getName(), convertFolderJson(segmentFolder));
        }

        if ((Objects.isNull(converterConfig) || !converterConfig.isPartialData()) && Objects.nonNull(segment.getSegmentConditionGroups())) {
            jsonObject.put(SegmentFields.GROUPS.getName(), convertGroups(converterConfig, segment));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link Segment} into {@link JsonObject}.
     * </p>
     *
     * @param segment a model that holds the attributes of segment.
     * @return {@link JsonObject} holds the segment attributes.
     */
    @Override
    public JsonObject convert(final Segment segment) {
        return convert(null, segment);
    }

    /**
     * <p>
     * Converts the json object into {@link Segment} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      holds the segment attributes.
     * @return {@link Segment} instance that hold the converted data.
     */
    private Segment constructSegment(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Segment segment = new Segment();
        final String idKey = SegmentFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            segment.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, segment, jsonObject);

        return segment;
    }

    /**
     * <p>
     * Converts the partial data to {@link Segment} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      holds the segment attributes.
     * @return {@link Segment} instance that hold the converted data.
     */
    private Segment constructSegmentByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Segment segment = this.segmentService.get(converterConfig.orgId(), jsonObject.getLong(SegmentFields.ID.getName()));

        setJsonDataToModel(converterConfig, segment, jsonObject);

        return segment;
    }

    /**
     * <p>
     * Sets the json data into {@link Segment} instance.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param segment         a model that holds the attributes of segment.
     * @param jsonObject      holds the segment attributes.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final Segment segment, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final CrmUser owner = userService.getUser(orgId, converterConfig.ownerId());
        final String nameKey = SegmentFields.NAME.getName();
        final String statusIdKey = SegmentFields.STATUS_ID.getName();
        final String folderKey = SegmentFields.FOLDER.getName();
        final String groupsKey = SegmentFields.GROUPS.getName();

        segment.setOrgId(orgId);
        segment.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey).trim() : segment.getName());

        if (Objects.isNull(segment.getCreatedBy())) {
            segment.setCreatedBy(owner);
        }
        segment.setModifiedBy(owner);

        if (jsonObject.containsKey(statusIdKey)) {
            segment.setStatus(Segment.Status.get(jsonObject.getShort(statusIdKey)));
        }

        if (jsonObject.containsKey(folderKey)) {
            setFolder(segment, jsonObject.optJsonObject(folderKey));
        }

        if (jsonObject.containsKey(groupsKey)) {
            setGroups(segment, jsonObject.getJsonArray(groupsKey));
        }
    }

    /**
     * <p>
     * Sets the folder details in {@link Segment} instance.
     * </p>
     *
     * @param segment    a model that holds the attributes of segment.
     * @param folderJson holds the folder attributes, same shape as {@link #convertFolderJson(SegmentFolder)} produces.
     */
    private void setFolder(final Segment segment, final JsonObject folderJson) {
        final Long folderId = Objects.isNull(folderJson) ? null : folderJson.getNullableValue(SegmentFields.ID.getName());

        segment.setFolder(Objects.isNull(folderId) ? null : segmentFolderService.get(segment.orgId(), folderId));
    }

    /**
     * <p>
     * Sets the group details in {@link Segment} instance.
     * </p>
     *
     * @param segment    a model that holds the attributes of segment.
     * @param groupArray holds the group attributes.
     */
    private void setGroups(final Segment segment, final JsonArray groupArray) {
        final String idKey = SegmentFields.ID.getName();
        final String connectorKey = CriteriaFields.CONNECTOR.getName();
        final String conditionsKey = SegmentFields.CONDITIONS.getName();
        final int size = groupArray.size();

        segment.setSegmentConditionGroups(null);

        for (int index = 0; index < size; index++) {
            final JsonObject groupJson = groupArray.getJsonObject(index);
            final SegmentConditionGroup segmentConditionGroup = new SegmentConditionGroup();

            if (groupJson.containsKey(connectorKey)) {
                segmentConditionGroup.setConnector(FilterConnector.get(groupJson.getJsonObject(connectorKey).getInt(ConnectorFields.ID.getName())));
            } else {
                segmentConditionGroup.setConnector(FilterConnector.AND);
            }

            if (groupJson.containsKey(conditionsKey)) {
                setConditions(segmentConditionGroup, groupJson.getJsonArray(conditionsKey));
            }
            segment.addGroup(segmentConditionGroup);
        }
    }

    /**
     * <p>
     * Sets the condition details in {@link SegmentConditionGroup} instance.
     * </p>
     *
     * @param segmentConditionGroup a model that holds the attributes of segment group.
     * @param conditionArray        holds the condition attributes.
     */
    private void setConditions(final SegmentConditionGroup segmentConditionGroup, final JsonArray conditionArray) {
        final int size = conditionArray.size();

        segmentConditionGroup.setConditions(null);

        for (int index = 0; index < size; index++) {
            final JsonObject conditionJson = conditionArray.getJsonObject(index);
            final SegmentCondition segmentCondition = new SegmentCondition();

            if (conditionJson.containsKey(SegmentFields.TYPE.getName())) {
                final JsonObject jsonObject = conditionJson.getJsonObject(SegmentFields.TYPE.getName());

                if (jsonObject.containsKey(SegmentFields.ID.getName())) {
                    segmentCondition.setType(SegmentCondition.Type.get(jsonObject.getShort(SegmentFields.ID.getName())));
                }
            }
            setConditionDetail(segmentCondition, conditionJson);

            segmentConditionGroup.addCondition(segmentCondition);
        }
    }

    /**
     * <p>
     * Converts the given {@link SegmentCondition.Type} into its public JSON representation.
     * </p>
     *
     * @param converterConfig the config holding org context and i18n resolution
     * @param type            the segment condition type to convert, may be {@code null}
     * @return the JSON representation of the type, or {@code null} if {@code type} is {@code null}
     */
    private JsonObject convertTypeJson(final ConverterConfig converterConfig, final SegmentCondition.Type type) {
        if (Objects.isNull(type)) {
            return null;
        }
        final String displayName = resolveTypeDisplayName(converterConfig, type);

        return Json.object()
                .put(SegmentFields.ID.getName(), type.getId())
                .put(SegmentFields.NAME.getName(), displayName);
    }

    /**
     * <p>
     * Resolves the display label for a segment condition type, preferring the
     * org-configured alternate name over the default source display name.
     * </p>
     *
     * @param converterConfig the config holding org context and i18n resolution
     * @param type            the segment condition type to resolve the label for
     * @return the resolved display label, or {@code null} if no field source is configured
     */
    private String resolveTypeDisplayName(final ConverterConfig converterConfig, final SegmentCondition.Type type) {
        final Source source = SegmentCondition.Type.toSource(type);

        if (Objects.isNull(source)) {
            return null;
        }
        final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(converterConfig.orgId(), source.getName());

        if (Objects.isNull(fieldSource)) {
            return null;
        }
        final I18nResolutionAdapter i18nPlaceholderResolver = converterConfig.getI18nPlaceholderResolver();
        final SourceSettings sourceSettings = fieldSource.getSettings();
        final String alternateName = Objects.nonNull(sourceSettings) ? sourceSettings.getAlternateName() : null;

        if (Objects.nonNull(alternateName) && !alternateName.isBlank()) {
            return type.getAlternateName(alternateName, i18nPlaceholderResolver);
        }
        final String displayName = resolveDefaultDisplayName(fieldSource, i18nPlaceholderResolver);

        return Objects.nonNull(displayName) ? type.getAlternateName(displayName, i18nPlaceholderResolver) : null;
    }

    /**
     * <p>
     * Resolves the default display name for a field source, using its i18n key
     * if present, falling back to its raw name otherwise.
     * </p>
     *
     * @param fieldSource             the field source to resolve the name for
     * @param i18nPlaceholderResolver the adapter used to resolve i18n keys, may be {@code null}
     * @return the resolved default display name
     */
    private String resolveDefaultDisplayName(final FieldSource fieldSource, final I18nResolutionAdapter i18nPlaceholderResolver) {
        final I18nKey nameKey = fieldSource.getDisplayName();

        if (Objects.nonNull(nameKey)) {
            return Objects.nonNull(i18nPlaceholderResolver) ? i18nPlaceholderResolver.get(nameKey) : nameKey.getKey();
        }
        return fieldSource.getName();
    }

    /**
     * <p>
     * Sets the type specific detail in {@link SegmentCondition} instance.
     * </p>
     *
     * @param condition     a model that holds the attributes of segment condition.
     * @param conditionJson holds the condition attributes.
     */
    private void setConditionDetail(final SegmentCondition condition, final JsonObject conditionJson) {
        switch (condition.getType()) {
            case CONTACT_PROPERTY, COMPANY_PROPERTY -> {
                condition.setConditionCriteria(constructCriteriaCondition(conditionJson));
                condition.setConnector(condition.getConditionCriteria().getFilterCriteria().getConnector());
            }
        }
    }

    /**
     * <p>
     * Constructs the {@link SegmentConditionCriteria} from the given {@link JsonObject}.
     * </p>
     *
     * @param conditionJson holds the details of the {@link SegmentConditionCriteria}.
     * @return {@link SegmentConditionCriteria} instance that hold the converted data.
     */
    private SegmentConditionCriteria constructCriteriaCondition(final JsonObject conditionJson) {
        final SegmentConditionCriteria conditionCriteria = new SegmentConditionCriteria();

        conditionCriteria.setFilterCriteria(constructFilterCriteria(conditionJson.getJsonObject(CriteriaFields.CRITERIA.getName())));

        return conditionCriteria;
    }

    /**
     * <p>
     * Constructs the {@link FilterCriteria} from the given {@link JsonObject}.
     * </p>
     *
     * @param criteriaJson holds the criteria attributes.
     * @return {@link FilterCriteria} instance that hold the converted data.
     */
    private FilterCriteria constructFilterCriteria(final JsonObject criteriaJson) {
        final FilterCriteria criteria = new FilterCriteria();
        final String idKey = CriteriaFields.ID.getName();
        final String sourceKey = CriteriaFields.SOURCE.getName();
        final String comparatorKey = CriteriaFields.COMPARATOR.getName();
        final String connectorKey = CriteriaFields.CONNECTOR.getName();
        final String fieldKey = CriteriaFields.FIELD.getName();
        final String valuesKey = CriteriaFields.VALUES.getName();

        if (criteriaJson.containsKey(idKey)) {
            criteria.setId(criteriaJson.getLong(idKey));
        }

        if (criteriaJson.containsKey(sourceKey)) {
            setFieldSource(criteria, criteriaJson.getJsonObject(sourceKey));
        }

        if (criteriaJson.containsKey(comparatorKey)) {
            setComparator(criteria, criteriaJson.getJsonObject(comparatorKey));
        }

        if (criteriaJson.containsKey(connectorKey)) {
            setConnector(criteria, criteriaJson.getJsonObject(connectorKey));
        }

        if (criteriaJson.containsKey(fieldKey)) {
            setCriteriaField(criteria, criteriaJson.getJsonObject(fieldKey));
        }

        if (criteriaJson.containsKey(valuesKey)) {
            setCriteriaValue(criteria, criteriaJson.optJsonArray(valuesKey));
        }

        return criteria;
    }

    /**
     * <p>
     * Sets the field source details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param criteria   a model that holds the attributes of filter criteria.
     * @param sourceJson holds the field source attributes, same shape as {@link #convertFieldSourceJson(ConverterConfig, FieldSource)} produces.
     */
    private void setFieldSource(final FilterCriteria criteria, final JsonObject sourceJson) {
        final FieldSource fieldSource = filterSourceService.get(sourceJson.getLong(FieldSourceAttributes.ID.getName()));

        criteria.setFieldSource(fieldSource);
    }

    /**
     * <p>
     * Sets the comparator details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param criteria       a model that holds the attributes of filter criteria.
     * @param comparatorJson holds the comparator attributes, same shape as {@link #convertComparatorJson(ConverterConfig, Comparator)} produces.
     */
    private void setComparator(final FilterCriteria criteria, final JsonObject comparatorJson) {
        final Comparator comparator = comparatorService.get(comparatorJson.getLong(ComparatorFields.ID.getName()));

        criteria.setComparator(comparator);
    }

    /**
     * <p>
     * Sets the connector details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param criteria      a model that holds the attributes of filter criteria.
     * @param connectorJson holds the connector attributes, same shape as {@link #convertConnectorJson(FilterConnector)} produces.
     */
    private void setConnector(final FilterCriteria criteria, final JsonObject connectorJson) {
        criteria.setConnector(FilterConnector.get(connectorJson.getInt(ConnectorFields.ID.getName())));
    }

    /**
     * <p>
     * Sets the field details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param criteria  a model that holds the attributes of filter criteria.
     * @param fieldJson holds the field attributes.
     */
    private void setCriteriaField(final FilterCriteria criteria, final JsonObject fieldJson) {
        final boolean isField = fieldJson.containsKey(FilterFieldAttributes.FIELD_ID.getName());
        final String additionalOptionKey = FilterFieldAttributes.ADDITIONAL_OPTION.getName();
        final Long filterFieldId = fetchFilterFieldId(fieldJson);
        final CriteriaField criteriaField = isField ? new CriteriaField() : new CriteriaOption();

        if (fieldJson.containsKey(additionalOptionKey)) {
            criteriaField.setAdditionalOption(constructAdditionalOption(fieldJson.getJsonObject(additionalOptionKey)));
        }
        criteriaField.setId(filterFieldId);
        criteria.setCriteriaField(criteriaField);
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
                additionalOption.setAdditionalOption(constructAdditionalOption(additionalOptionJson.getJsonObject(additionalOptionKey)));
            }

            return additionalOption;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Sets the value details in {@link FilterCriteria} instance.
     * </p>
     *
     * @param criteria  a model that holds the attributes of filter criteria.
     * @param jsonArray holds the values attribute.
     */
    private void setCriteriaValue(final FilterCriteria criteria, final JsonArray jsonArray) {

        if (Objects.isNull(jsonArray)) {
            return;
        }
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);
            final String idKey = CriteriaValueAttributes.ID.getName();
            final String valueKey = CriteriaValueAttributes.VALUE.getName();
            final String valueEntityIdKey = CriteriaValueAttributes.VALUE_ENTITY_ID.getName();
            final CriteriaValue criteriaValue = new CriteriaValue();

            if (jsonObject.containsKey(valueKey)) {
                criteriaValue.setValue(jsonObject.get(valueKey));
            }

            if (jsonObject.containsKey(idKey)) {
                criteriaValue.setId(jsonObject.getLong(idKey));
            }

            if (jsonObject.containsKey(valueEntityIdKey)) {
                criteriaValue.setValueEntityId(jsonObject.getLong(valueEntityIdKey));
            }
            criteria.addCriteriaValue(criteriaValue);
        }
    }

    /**
     * <p>
     * Constructs the audit json from the given {@link Segment}.
     * </p>
     *
     * @param segment a model that holds the attributes of segment.
     * @return {@link JsonObject} holds the audit attributes.
     */
    private JsonObject convertAuditJson(final Segment segment) {
        final JsonObject createdJson = Json.object();
        final JsonObject modifiedJson = Json.object();
        final CrmUser createdBy = segment.getCreatedBy();
        final DateTime createdTime = segment.getCreatedTime();
        final CrmUser modifiedBy = segment.getModifiedBy();
        final DateTime modifiedTime = segment.getModifiedTime();

        if (Objects.nonNull(createdBy)) {
            createdJson.put(SegmentFields.BY.getName(), convertUserJson(createdBy));
        }

        if (Objects.nonNull(createdTime)) {
            createdJson.put(SegmentFields.TIME.getName(), createdTime.getTimeStamp());
        }

        if (Objects.nonNull(modifiedBy)) {
            modifiedJson.put(SegmentFields.BY.getName(), convertUserJson(modifiedBy));
        }

        if (Objects.nonNull(modifiedTime)) {
            modifiedJson.put(SegmentFields.TIME.getName(), modifiedTime.getTimeStamp());
        }

        return Json.object().put(SegmentFields.CREATED.getName(), createdJson).put(SegmentFields.MODIFIED.getName(), modifiedJson);
    }

    /**
     * <p>
     * Constructs the user json from the given {@link CrmUser}.
     * </p>
     *
     * @param user a model that holds the attributes of crm user.
     * @return {@link JsonObject} holds the user attributes.
     */
    private JsonObject convertUserJson(final CrmUser user) {
        return Json.object()
                .put(SegmentFields.ID.getName(), user.id())
                .put(SegmentFields.NAME.getName(), user.getFullName());
    }

    /**
     * <p>
     * Constructs the folder json from the given {@link SegmentFolder}.
     * </p>
     *
     * @param folder a model that holds the attributes of segment folder.
     * @return {@link JsonObject} holds the folder attributes.
     */
    private JsonObject convertFolderJson(final SegmentFolder folder) {
        return Json.object()
                .put(SegmentFolderFields.ID.getName(), folder.id())
                .put(SegmentFolderFields.NAME.getName(), folder.getName())
                .put(SegmentFolderFields.IS_STARRED.getName(), folder.isStarred());
    }

    /**
     * <p>
     * Constructs the collection of group json from the given {@link Segment}.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param segment         a model that holds the attributes of segment.
     * @return {@link JsonArray} holds the group attributes.
     */
    private JsonArray convertGroups(final ConverterConfig converterConfig, final Segment segment) {
        final JsonArray groupsArray = Json.array();
        final Collection<SegmentConditionGroup> segmentConditionGroups = segment.getSegmentConditionGroups();

        if (Objects.isNull(segmentConditionGroups) || segmentConditionGroups.isEmpty()) {
            return groupsArray;
        }
        int index = 0;

        for (final SegmentConditionGroup segmentConditionGroup : segmentConditionGroups) {
            final JsonObject groupJson = Json.object();
            final FilterConnector filterConnector = segmentConditionGroup.getConnector();

            if (Objects.nonNull(filterConnector) && index != 0) {
                groupJson.put(CriteriaFields.CONNECTOR.getName(), convertConnectorJson(filterConnector));
            }

            if (Objects.nonNull(segmentConditionGroup.getConditions())) {
                groupJson.put(SegmentFields.CONDITIONS.getName(), convertConditions(converterConfig, segmentConditionGroup));
            }
            groupsArray.put(groupJson);
            index++;
        }

        return groupsArray;
    }

    /**
     * <p>
     * Constructs the collection of condition json from the given {@link SegmentConditionGroup}.
     * </p>
     *
     * @param converterConfig       represents the converter config
     * @param segmentConditionGroup a model that holds the attributes of segment group.
     * @return {@link JsonArray} holds the condition attributes.
     */
    private JsonArray convertConditions(final ConverterConfig converterConfig, final SegmentConditionGroup segmentConditionGroup) {
        final JsonArray conditionsArray = Json.array();
        final Collection<SegmentCondition> groupConditions = segmentConditionGroup.getConditions();

        if (Objects.isNull(groupConditions) || groupConditions.isEmpty()) {
            return conditionsArray;
        }

        int index = 0;

        for (final SegmentCondition segmentCondition : groupConditions) {
            final JsonObject conditionJson = Json.object();
            final SegmentCondition.Type type = segmentCondition.getType();

            if (Objects.nonNull(type)) {
                conditionJson.put(SegmentFields.TYPE.getName(), convertTypeJson(converterConfig, type));
            }

            setConditionJson(converterConfig, segmentCondition, conditionJson, index == 0);
            conditionsArray.put(conditionJson);
            index++;
        }

        return conditionsArray;
    }

    /**
     * <p>
     * Sets the type specific json in {@link SegmentCondition} json.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param condition       a model that holds the attributes of segment condition.
     * @param conditionJson   holds the condition attributes.
     */
    private void setConditionJson(final ConverterConfig converterConfig, final SegmentCondition condition,
                                  final JsonObject conditionJson, final boolean isFirstCondition) {
        switch (condition.getType()) {
            case CONTACT_PROPERTY, COMPANY_PROPERTY ->
                    setCriteriaConditionJson(converterConfig, condition.getConditionCriteria(), conditionJson, isFirstCondition);
        }
    }

    /**
     * <p>
     * Sets the criteria condition json in the given condition json.
     * </p>
     *
     * @param converterConfig   represents the converter config
     * @param criteriaCondition a model that holds the attributes of segment criteria condition.
     * @param conditionJson     holds the condition attributes.
     */
    private void setCriteriaConditionJson(final ConverterConfig converterConfig, final SegmentConditionCriteria criteriaCondition,
                                          final JsonObject conditionJson, final boolean isFirstCondition) {
        final FilterCriteria filterCriteria;

        if (Objects.nonNull(criteriaCondition) && Objects.nonNull(filterCriteria = criteriaCondition.getFilterCriteria())) {
            conditionJson.put(CriteriaFields.CRITERIA.getName(), convertCriteriaJson(converterConfig, filterCriteria, isFirstCondition));
        }
    }

    /**
     * <p>
     * Constructs the criteria json from the given {@link FilterCriteria}.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param criteria        a model that holds the attributes of filter criteria.
     * @return {@link JsonObject} holds the criteria attributes.
     */
    private JsonObject convertCriteriaJson(final ConverterConfig converterConfig, final FilterCriteria criteria,
                                           final boolean isFirstCondition) {
        final JsonObject criteriaJson = Json.object().put(CriteriaFields.ID.getName(), criteria.getId());
        final FieldSource fieldSource = criteria.getFieldSource();
        final Comparator comparator = criteria.getComparator();
        final FilterConnector filterConnector = criteria.getConnector();
        final CriteriaField criteriaField = criteria.getCriteriaField();
        final Collection<CriteriaValue> criteriaValues = criteria.getCriteriaValues();

        if (Objects.nonNull(fieldSource)) {
            criteriaJson.put(CriteriaFields.SOURCE.getName(), convertFieldSourceJson(converterConfig, fieldSource));
        }

        if (Objects.nonNull(comparator)) {
            criteriaJson.put(CriteriaFields.COMPARATOR.getName(), convertComparatorJson(converterConfig, comparator));
        }

        if (Objects.nonNull(filterConnector) && !isFirstCondition) {
            criteriaJson.put(CriteriaFields.CONNECTOR.getName(), convertConnectorJson(filterConnector));
        }

        if (Objects.nonNull(criteriaField)) {
            criteriaJson.put(CriteriaFields.FIELD.getName(), convertCriteriaFieldJson(converterConfig, criteriaField));
        }

        if (Objects.nonNull(criteriaValues)) {
            criteriaJson.put(CriteriaFields.VALUES.getName(), convertCriteriaValuesJson(criteriaValues));
        }

        return criteriaJson;
    }

    /**
     * <p>
     * Constructs the field source json from the given {@link FieldSource}.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param fieldSource     a model that holds the attributes of field source.
     * @return {@link JsonObject} holds the field source attributes.
     */
    private JsonObject convertFieldSourceJson(final ConverterConfig converterConfig, final FieldSource fieldSource) {
        final I18nResolutionAdapter adapter = Objects.nonNull(converterConfig) ? converterConfig.getI18nPlaceholderResolver() : null;
        final I18nKey name = fieldSource.getDisplayName();
        final String displayName;

        if (Objects.nonNull(name)) {
            if (Objects.nonNull(adapter)) {
                displayName = adapter.get(name);
            } else {
                displayName = name.getKey();
            }
        } else {
            displayName = fieldSource.getName();
        }

        return Json.object()
                .put(FieldSourceAttributes.ID.getName(), fieldSource.id())
                .put(FieldSourceAttributes.NAME.getName(), displayName);
    }

    /**
     * <p>
     * Constructs the comparator json from the given {@link Comparator}.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param comparator      a model that holds the attributes of comparator.
     * @return {@link JsonObject} holds the comparator attributes.
     */
    private JsonObject convertComparatorJson(final ConverterConfig converterConfig, final Comparator comparator) {
        final I18nResolutionAdapter adapter = Objects.nonNull(converterConfig) ? converterConfig.getI18nPlaceholderResolver() : null;
        final I18nKey name = comparator.getName();
        final String resolvedName;

        if (Objects.nonNull(name)) {
            if (Objects.nonNull(adapter)) {
                resolvedName = adapter.get(name);
            } else {
                resolvedName = name.getKey();
            }
        } else {
            resolvedName = comparator.getAliasName();
        }

        return Json.object()
                .put(ComparatorFields.ID.getName(), comparator.getId())
                .put(ComparatorFields.NAME.getName(), resolvedName)
                .put(ComparatorFields.HAS_VALUE.getName(), comparator.isValueNeeded())
                .put(ComparatorFields.IS_MULTI_VALUE_ALLOWED.getName(), comparator.isMultiValueAllowed());
    }

    /**
     * <p>
     * Constructs the connector json from the given {@link FilterConnector}.
     * </p>
     *
     * @param connector represents the filter connector.
     * @return {@link JsonObject} holds the connector attributes.
     */
    private JsonObject convertConnectorJson(final FilterConnector connector) {
        return Json.object()
                .put(ConnectorFields.ID.getName(), connector.getId())
                .put(ConnectorFields.NAME.getName(), connector.name());
    }

    /**
     * <p>
     * Constructs the field json from the given {@link CriteriaField}.
     * </p>
     *
     * @param converterConfig represents the converter config
     * @param criteriaField   a model that holds the attributes of criteria field.
     * @return {@link JsonObject} holds the field attributes.
     */
    private JsonObject convertCriteriaFieldJson(final ConverterConfig converterConfig, final CriteriaField criteriaField) {
        final JsonObject fieldJson = Json.object();
        final FieldType fieldType = criteriaField.getFieldType();
        final AdditionalOption additionalOption = criteriaField.getAdditionalOption();
        final I18nResolutionAdapter adapter = Objects.nonNull(converterConfig) ? converterConfig.getI18nPlaceholderResolver() : null;

        fieldJson.put(criteriaField instanceof CriteriaOption ? FilterFieldAttributes.OPTION_ID.getName()
                : FilterFieldAttributes.FIELD_ID.getName(), criteriaField.getId());
        final I18nKey name = criteriaField.getName();
        final String resolvedName;

        if (Objects.nonNull(name)) {
            if (Objects.nonNull(adapter)) {
                resolvedName = adapter.get(name);
            } else {
                resolvedName = name.getKey();
            }
        } else {
            resolvedName = criteriaField.getAliasName();
        }
        fieldJson.put(FilterFieldAttributes.NAME.getName(), resolvedName);
        fieldJson.put(FilterFieldAttributes.ALIAS_NAME.getName(), criteriaField.getAliasName());

        if (Objects.nonNull(fieldType)) {
            fieldJson.put(FilterFieldAttributes.FIELD_TYPE_ID.getName(), fieldType.id());
        }

        if (Objects.nonNull(additionalOption)) {
            fieldJson.put(FilterFieldAttributes.ADDITIONAL_OPTION.getName(), convertAdditionalOptionsToJson(additionalOption,
                    fieldType.storeType(), adapter));
        }

        return fieldJson;
    }

    /**
     * <p>
     * Converts the {@link AdditionalOption} into {@link JsonObject}.
     * </p>
     *
     * @param additionalOption      a model that holds the attributes of {@link AdditionalOption}.
     * @param storeType             {@link StoreType} indicates the type of fields.
     * @param i18nResolutionAdapter needs to resolve i18n keys / strings.
     * @return {@link JsonObject} holds the additional option attributes.
     */
    private JsonObject convertAdditionalOptionsToJson(final AdditionalOption additionalOption, final StoreType storeType,
                                                      final I18nResolutionAdapter i18nResolutionAdapter) {
        final AdditionalOption childAdditionalOption = additionalOption.getAdditionalOption();
        final JsonObject additionalOptionJson = Json.object();
        final Long entityId = additionalOption.getOptionEntityId();
        final String name = additionalOption.getOptionName();
        final FieldSubType subType = fieldTypeService.getSubType(entityId);

        additionalOptionJson.put(AdditionalOptionFields.ID.getName(), additionalOption.getId());
        additionalOptionJson.put(AdditionalOptionFields.ENTITY_ID.getName(), entityId);
        additionalOptionJson.put(AdditionalOptionFields.NAME.getName(), Objects.isNull(name) ? subType.name() : name);
        additionalOptionJson.put(AdditionalOptionFields.DISPLAY_NAME.getName(), storeType.hasSubtypes()
                ? i18nResolutionAdapter.get(subType.displayName()) : name);

        if (Objects.nonNull(childAdditionalOption)) {
            additionalOptionJson.put(FilterFieldAttributes.ADDITIONAL_OPTION.getName(),
                    convertAdditionalOptionsToJson(childAdditionalOption, storeType, i18nResolutionAdapter));
        }

        return additionalOptionJson;
    }

    /**
     * <p>
     * Constructs the collection of value json from the given {@link CriteriaValue} collection.
     * </p>
     *
     * @param values collection of {@link CriteriaValue}.
     * @return {@link JsonArray} holds the value attributes.
     */
    private JsonArray convertCriteriaValuesJson(final Collection<CriteriaValue> values) {
        final JsonArray jsonArray = Json.array();

        if (Objects.isNull(values) || values.isEmpty()) {
            return jsonArray;
        }

        for (final CriteriaValue value : values) {
            jsonArray.put(Json.object()
                    .put(CriteriaValueAttributes.ID.getName(), value.getId())
                    .put(CriteriaValueAttributes.VALUE.getName(), value.getValue())
                    .put(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), value.getValueEntityId()));
        }

        return jsonArray;
    }
}