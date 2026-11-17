package com.tlc.crm.core.action.dropdown.segment;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides a static dropdown that returns available segment groups
 * (e.g. Properties) nested with their subgroups (Contact/Company properties),
 * each resolved with its source id and display name.
 * </p>
 *
 * @author Mohamed Yasar K
 */
@StaticDropdown(name = "segmentGroupList")
public class SegmentGroupList extends AbstractDropdownAction {

    /**
     * <p>
     * Builds the list of segment group options for the given org. Each
     * {@link SegmentCondition.Group} becomes a top-level entry, with its
     * associated {@link SegmentCondition.Type} values nested as subgroups.
     * </p>
     *
     * @param dropdownRequest the incoming dropdown request holding the org context
     * @return the container populated with the resolved segment group options
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final Map<SegmentCondition.Group, List<JsonObject>> subGroupsByGroup = new HashMap<>();
        final I18nResolutionAdapter i18nResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();

        for (final SegmentCondition.Type type : SegmentCondition.Type.values()) {
            final JsonObject subGroupJson = buildSubGroupJson(orgId, type, i18nResolutionAdapter);

            if (Objects.isNull(subGroupJson)) {
                continue;
            }
            subGroupsByGroup
                    .computeIfAbsent(type.getGroup(), key -> new ArrayList<>())
                    .add(subGroupJson);
        }
        final OptionContainer optionContainer = new OptionContainer(subGroupsByGroup.size());

        for (final Map.Entry<SegmentCondition.Group, List<JsonObject>> entry : subGroupsByGroup.entrySet()) {
            final JsonObject groupJson = Json.object()
                    .put(OptionKeys.NAME, i18nResolutionAdapter.get(entry.getKey().getI18nKey()));
            final JsonArray jsonArray = Json.array();

            for (final JsonObject subGroup : entry.getValue()) {
                jsonArray.put(subGroup);
            }
            groupJson.put(SegmentFields.SUBGROUPS.getName(), jsonArray);

            optionContainer.addOption(groupJson);
        }

        return optionContainer;
    }

    /**
     * <p>
     * Builds the JSON representation of a single subgroup entry (a
     * {@link SegmentCondition.Type}) including its resolved source details.
     * </p>
     *
     * @param orgId                 the organization id used to resolve source settings
     * @param type                  the segment condition type to build the subgroup entry for
     * @param i18nResolutionAdapter represents the i18n adaptor
     * @return the subgroup option JSON, or {@code null} if the type has no associated source
     */
    private JsonObject buildSubGroupJson(final Long orgId, final SegmentCondition.Type type, final I18nResolutionAdapter i18nResolutionAdapter) {
        final Source source = SegmentCondition.Type.toSource(type);

        if (Objects.isNull(source)) {
            return null;
        }
        final FieldSource fieldSource = FieldSourceService.getInstance().retrieveSourceWithSettings(orgId, source.getName());
        final JsonObject sourceJson = buildSourceJson(source, fieldSource, i18nResolutionAdapter);
        final String sourceName = sourceJson.getString(OptionKeys.NAME);
        final String subGroupName = type.getAlternateName(sourceName, i18nResolutionAdapter);

        return Json.object()
                .put(OptionKeys.ID, type.getId())
                .put(OptionKeys.NAME, subGroupName)
                .put(SegmentFields.SOURCE.getName(), sourceJson);
    }

    /**
     * <p>
     * Builds the JSON representation of a source, resolving its display id and
     * name from org-level settings when available, falling back to the raw
     * {@link Source} values otherwise.
     * </p>
     *
     * @param source                the default source values to fall back to
     * @param fieldSource           the org-configured field source, may be {@code null}
     * @param i18nResolutionAdapter represents the i18nResolutionAdapter
     * @return the resolved source JSON
     */
    private JsonObject buildSourceJson(final Source source, final FieldSource fieldSource, final I18nResolutionAdapter i18nResolutionAdapter) {
        long sourceId = source.getId();
        String sourceName = source.getName();

        if (Objects.nonNull(fieldSource)) {
            sourceId = fieldSource.id();
            sourceName = resolveSourceName(fieldSource, i18nResolutionAdapter);
        }

        return Json.object()
                .put(OptionKeys.ID, sourceId)
                .put(OptionKeys.NAME, sourceName);
    }

    /**
     * <p>
     * Resolves the display name for a field source, preferring the
     * org-configured alternate name, then the i18n display name key,
     * then the raw source name.
     * </p>
     *
     * @param fieldSource           the field source to resolve the name for
     * @param i18nResolutionAdapter represents the i18nResolutionAdapter
     * @return the resolved display name
     */
    private String resolveSourceName(final FieldSource fieldSource, final I18nResolutionAdapter i18nResolutionAdapter) {
        final SourceSettings sourceSettings = fieldSource.getSettings();
        final String alternateName = Objects.nonNull(sourceSettings) ? sourceSettings.getAlternateName() : null;

        if (Objects.nonNull(alternateName) && !alternateName.isBlank()) {
            return alternateName;
        }
        final I18nKey displayName = fieldSource.getDisplayName();

        if (Objects.nonNull(displayName)) {
            return i18nResolutionAdapter.get(displayName);
        }
        return fieldSource.getName();
    }
}