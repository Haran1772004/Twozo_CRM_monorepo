package com.tlc.crm.core.segment.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.core.segment.service.SegmentFolderService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.core.segment.models.SegmentDefaultFilter;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides different ways to convert data from / into {@link SegmentFilterUserPreference}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentFilterPreferenceConverter extends EntityConverter<SegmentFilterUserPreference> {

    private final SegmentFolderService segmentFolderService;

    private static class Instance {
        private static final SegmentFilterPreferenceConverter INSTANCE = new SegmentFilterPreferenceConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceConverter}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceConverter}.
     */
    public static SegmentFilterPreferenceConverter getInstance() {
        return Instance.INSTANCE;
    }

    private SegmentFilterPreferenceConverter() {
        this.segmentFolderService = SegmentFolderService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final SegmentFilterUserPreference preference = new SegmentFilterUserPreference();

        if (Objects.isNull(jsonObject)) {
            return null;
        }
        setJsonDataToModel(converterConfig, preference, jsonObject);

        return preference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final SegmentFilterUserPreference existingPreference) {
        final SegmentFilterUserPreference preference = Objects.isNull(existingPreference) ? new SegmentFilterUserPreference() : existingPreference;
        final SegmentFolder segmentFolder = preference.getSegmentFolder();
        final I18nResolutionAdapter i18nResolutionAdapter = converterConfig.getI18nPlaceholderResolver();

        SegmentFilterGroup group = preference.getSegmentFilterGroup();
        SegmentDefaultFilter defaultFilter = preference.getSegmentDefaultFilter();

        if (Objects.isNull(group)) {
            group = SegmentFilterGroup.DEFAULT_FILTERS;
        }
        final JsonObject subgroupJson;

        if (group.isDefaultFilterGroup()) {

            if (Objects.isNull(defaultFilter)) {
                defaultFilter = SegmentDefaultFilter.ALL_SEGMENTS;
            }
            subgroupJson = Json.object()
                    .put(SegmentFields.ID.getName(), defaultFilter.getValue())
                    .put(SegmentFields.NAME.getName(), i18nResolutionAdapter.get(defaultFilter.getI18nKey()));
        } else if (Objects.nonNull(segmentFolder)) {
            final SegmentFolder folder = segmentFolderService
                    .partialGet(existingPreference.getOrgId(), segmentFolder.id());

            subgroupJson = Json.object()
                    .put(SegmentFields.ID.getName(), folder.id())
                    .put(SegmentFields.NAME.getName(), folder.getName());
        } else {
            subgroupJson = null;
        }
        final JsonObject jsonObject = Json.object()
                .put(SegmentFields.GROUP.getName(), Json.object()
                        .put(SegmentFields.ID.getName(), group.getValue())
                        .put(SegmentFields.NAME.getName(), i18nResolutionAdapter.get(group.getI18nKey())))
                .put(SegmentFields.MODIFIED_TIME.getName(), Optional.ofNullable(preference.getModifiedTime())
                        .map(DateTime::getTimeStamp).orElse(null));

        if (Objects.nonNull(subgroupJson)) {
            jsonObject.put(SegmentFields.SUBGROUP.getName(), subgroupJson);
        }
        final JsonObject sortJson = preference.getSortJson();

        if (Objects.nonNull(sortJson)) {
            jsonObject.put(SegmentFields.SORT_CONFIG.getName(), sortJson);
        }
        final JsonObject limitJson = preference.getLimitJson();

        if (Objects.nonNull(limitJson)) {
            jsonObject.put(ListActionFields.LIMIT, limitJson);
        }

        return jsonObject;
    }


    /**
     * <p>
     * Populates the request scoped values such as search, pagination and sort.
     * </p>
     *
     * @param preference Represents the preference.
     * @param data       Request data.
     */
    public void populateDefaultPreferences(final SegmentFilterUserPreference preference, final JsonObject data) {
        preference.setSearchText(data.optString(SegmentFields.SEARCH_TEXT.getName(), null));
        preference.setLimitJson(data.optJsonObject(ListActionFields.LIMIT));
    }

    /**
     * <p>
     * Sets the persisted filter preference values.
     * </p>
     *
     * @param converterConfig Converter configuration.
     * @param preference      Represents the filter preference.
     * @param jsonObject      Filter criteria JSON.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final SegmentFilterUserPreference preference,
                                    final JsonObject jsonObject) {
        if (Objects.nonNull(converterConfig)) {
            preference.setOrgId(converterConfig.orgId());
            preference.setUserId(converterConfig.ownerId());
        }

        SegmentFilterGroup group = SegmentFilterGroup.DEFAULT_FILTERS;

        if (jsonObject.containsKey(SegmentFields.GROUP.getName())) {
            final JsonObject groupJson = jsonObject.getJsonObject(SegmentFields.GROUP.getName());

            if (Objects.nonNull(groupJson)) {
                final SegmentFilterGroup filterGroup = SegmentFilterGroup.get(groupJson.getShort(SegmentFields.ID.getName()));

                if (Objects.nonNull(filterGroup)) {
                    group = filterGroup;
                }
            }
        }
        preference.setSegmentFilterGroup(group);

        if (jsonObject.containsKey(SegmentFields.SUBGROUP.getName())) {
            final JsonObject subgroup = jsonObject.getJsonObject(SegmentFields.SUBGROUP.getName());

            if (Objects.nonNull(subgroup) && subgroup.containsKey(SegmentFields.ID.getName())) {

                if (group.isDefaultFilterGroup()) {
                    preference.setSegmentDefaultFilter(SegmentDefaultFilter.get(subgroup.getInt(SegmentFields.ID.getName())));
                } else {
                    preference.setSegmentFolder(new SegmentFolder(subgroup.getLong(SegmentFields.ID.getName())));
                }
            }
        }
        preference.setSortJson(jsonObject.optJsonObject(SegmentFields.SORT_CONFIG.getName()));
        preference.setLimitJson(jsonObject.optJsonObject(ListActionFields.LIMIT));

        if (jsonObject.containsKey(SegmentFields.MODIFIED_TIME.getName())) {
            preference.setModifiedTime(new DateTime(jsonObject.getLong(SegmentFields.MODIFIED_TIME.getName())));
        }
    }
}