package com.tlc.crm.core.campaign.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.model.CampaignDefaultFilter;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.l10n.temporal.model.DateTime;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides different ways to convert data from / into {@link CampaignFilterUserPreference}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFilterPreferenceConverter extends EntityConverter<CampaignFilterUserPreference> {

    private final CampaignFolderManager campaignFolderManager;

    private static class InstanceHolder {
        private static final CampaignFilterPreferenceConverter INSTANCE = new CampaignFilterPreferenceConverter();
    }

    private CampaignFilterPreferenceConverter() {
        campaignFolderManager = CampaignFolderManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceConverter}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceConverter}.
     */
    public static CampaignFilterPreferenceConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @param jsonObject      Represents the {@link JsonObject}.
     * @return the converted filter preference.
     */
    @Override
    public CampaignFilterUserPreference convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final CampaignFilterUserPreference preference = new CampaignFilterUserPreference();

        preference.setOrgId(converterConfig.orgId());
        preference.setUserId(converterConfig.ownerId());
        CampaignFilterGroup group = CampaignFilterGroup.DEFAULT_FILTERS;

        if (jsonObject.containsKey(CampaignFields.GROUP)) {
            final JsonObject groupJson = jsonObject.getJsonObject(CampaignFields.GROUP);

            if (Objects.nonNull(groupJson)) {
                group = CampaignFilterGroup.get(groupJson.getShort(CampaignFields.ID));
            }
        }
        preference.setCampaignFilterGroup(group);

        if (jsonObject.containsKey(CampaignFields.SUBGROUP)) {
            final JsonObject subgroup = jsonObject.getJsonObject(CampaignFields.SUBGROUP);

            if (subgroup.containsKey(CampaignFields.ID)) {
                if (group.isDefaultFilterGroup()) {
                    preference.setCampaignDefaultFilter(CampaignDefaultFilter.get(subgroup.getShort(CampaignFields.ID)));
                } else {
                    preference.setCampaignFolder(new CampaignFolder(subgroup.getLong(CampaignFields.ID)));
                }
            }
        }
        preference.setModifiedTime(new DateTime(Instant.now().toEpochMilli()));

        if (jsonObject.optBoolean(CampaignFields.ALL_STATUS_SELECTED)) {
            preference.setSelectedStatuses(Arrays.stream(CampaignStatus.values()).toList());
        } else {
            final Collection<CampaignStatus> statuses = new HashSet<>();

            jsonObject.getJsonArray(CampaignFields.STATUS).forEach(statusId ->
                    statuses.add(CampaignStatus.get(NumberUtils.parseNumber(statusId).shortValue())));
            preference.setSelectedStatuses(statuses);
        }

        return preference;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig    Represents the {@link ConverterConfig}.
     * @param existingPreference Represents the existing filter preference.
     * @return the converted filter preference.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final CampaignFilterUserPreference existingPreference) {
        final CampaignFilterUserPreference preference = Objects.isNull(existingPreference)
                ? new CampaignFilterUserPreference() : existingPreference;
        final CampaignFolder campaignFolder = preference.getCampaignFolder();
        final I18nResolutionAdapter i18nResolutionAdapter = converterConfig.getI18nPlaceholderResolver();

        CampaignFilterGroup group = preference.getCampaignFilterGroup();
        CampaignDefaultFilter defaultFilter = preference.getCampaignDefaultFilter();

        if (Objects.isNull(group)) {
            group = CampaignFilterGroup.DEFAULT_FILTERS;
        }
        final JsonObject subgroupJson = Json.object();

        if (group.isDefaultFilterGroup()) {
            if (Objects.isNull(defaultFilter)) {
                defaultFilter = CampaignDefaultFilter.ALL_CAMPAIGNS;
            }
            subgroupJson.put(CampaignFields.ID, defaultFilter.getId())
                    .put(CampaignFields.NAME, i18nResolutionAdapter.get(defaultFilter.getI18nKey()));
        } else if (Objects.nonNull(campaignFolder)) {
            final CampaignFolder folder = campaignFolderManager.partialGet(preference.orgId(), campaignFolder.id());

            subgroupJson.put(CampaignFields.ID, folder.id()).put(CampaignFields.NAME, folder.getName());
        }
        final JsonObject groupJson = Json.object()
                .put(CampaignFields.ID, group.getId())
                .put(CampaignFields.NAME, i18nResolutionAdapter.get(group.getI18nKey()));

        final JsonObject filterJson = Json.object()
                .put(CampaignFields.GROUP, groupJson)
                .put(CampaignFields.SUBGROUP, subgroupJson)
                .put(CampaignFields.ALL_STATUS_SELECTED, preference.isAllStatusSelected());

        final Collection<CampaignStatus> selectedStatuses = preference.getSelectedStatuses();

        if (Objects.nonNull(selectedStatuses) && !selectedStatuses.isEmpty()) {
            filterJson.put(CampaignFields.STATUS, selectedStatuses.stream().map(CampaignStatus::getId).collect(Collectors.toUnmodifiableSet()));
        }

        return filterJson;
    }
}
