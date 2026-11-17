package com.tlc.crm.core.segment.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.manager.SegmentFilterPreferenceManager;
import com.tlc.crm.core.segment.models.SegmentDefaultFilter;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.sql.resource.OMKSRECENTLYAPPLIEDFILTER;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.validator.ProcessOutcome;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link SegmentFilterUserPreference} related DB actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractConfigManager
 */
public class SegmentFilterPreferenceManagerImpl extends AbstractConfigManager<SegmentFilterUserPreference> implements SegmentFilterPreferenceManager {

    private static class Instance {
        private static final SegmentFilterPreferenceManagerImpl INSTANCE = new SegmentFilterPreferenceManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceManagerImpl}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceManagerImpl}.
     */
    public static SegmentFilterPreferenceManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SegmentFilterPreferenceManagerImpl() {
    }

    /**
     * {@inheritDoc}
     *
     * @return OMKSRecentlyAppliedFilter table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMKSRECENTLYAPPLIEDFILTER.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SegmentFilterUserPreference> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table table = getTable();

        return orgDataStore(orgId).get(table, ids).getRows(table).map(row -> convertRowToPreference(orgId, row)).toList();
    }

    /**
     * <p>
     * Gets the preference of the given user.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return {@link SegmentFilterUserPreference}.
     */
    @Override
    public SegmentFilterUserPreference get(final Long orgId, final Long userId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(table.getColumn(OMKSRECENTLYAPPLIEDFILTER.ORG_ID), orgId))
                        .and(Criteria.eq(table.getColumn(OMKSRECENTLYAPPLIEDFILTER.USER_ID), userId))
        );
        final Collection<SegmentFilterUserPreference> preferences = orgDataStore(orgId).get(selectQuery)
                .getRows(table).map(row -> convertRowToPreference(orgId, row)).toList();

        if (preferences.isEmpty()) {
            return null;
        }

        return preferences.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SegmentFilterUserPreference> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference create(final SegmentFilterUserPreference preference) {
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(true, preference, null, targetContainer);
        orgDataStore(preference.orgId()).commitChanges(targetContainer);

        return preference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<SegmentFilterUserPreference> preferences) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = preferences.stream().map(SegmentFilterUserPreference::id).filter(Objects::nonNull).toList();
        final DataContainer sourceContainer = orgDataStore(orgId).get(getTable(), ids);
        final DataContainer targetContainer = DataContainer.create();

        preferences.forEach(preference -> {
            loadPreferenceIntoContainer(false, preference, sourceContainer, targetContainer);
            processOutcome.addProcessedRecord(preference.id());
        });
        orgDataStore(orgId).commitChanges(targetContainer);

        return processOutcome;
    }

    /**
     * <p>
     * Checks whether the preference exists for the given user.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return {@code true} if preference exists.
     */
    @Override
    public boolean exists(final Long orgId, final Long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(
                Criteria.eq(table.getColumn(OMKSRECENTLYAPPLIEDFILTER.ORG_ID), orgId))
                .and(Criteria.eq(table.getColumn(OMKSRECENTLYAPPLIEDFILTER.USER_ID), userId))
        );

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Loads the segment filter preference details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related
     *                        to create event or update event.
     * @param preference      Holds the {@link SegmentFilterUserPreference} attributes.
     * @param sourceContainer Holds the old preference values.
     * @param targetContainer Holds the updated preference values.
     */
    public void loadPreferenceIntoContainer(final boolean isCreateEvent, final SegmentFilterUserPreference preference,
                                            final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final DateTime currentTime = new DateTime(Instant.now().toEpochMilli());
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, preference.id());

        if (row.isNewRow()) {
            preference.setId(row.getPKValue());
            row.setOrgId(preference.getOrgId());
        }
        row.set(OMKSRECENTLYAPPLIEDFILTER.USER_ID, preference.getUserId());
        row.set(OMKSRECENTLYAPPLIEDFILTER.GROUP_ID, preference.getSegmentFilterGroup().getValue());
        final SegmentDefaultFilter segmentDefaultFilter = preference.getSegmentDefaultFilter();

        row.set(OMKSRECENTLYAPPLIEDFILTER.DEFAULT_FILTER_ID, Objects.isNull(segmentDefaultFilter) ? null : segmentDefaultFilter.getValue());
        final SegmentFolder segmentFolder = preference.getSegmentFolder();

        row.set(OMKSRECENTLYAPPLIEDFILTER.FOLDER_ID, Objects.isNull(segmentFolder) ? null : segmentFolder.id());
        final JsonObject configJson = Json.object();
        final JsonObject sortJson = preference.getSortJson();

        if (Objects.nonNull(sortJson)) {
            configJson.put(SegmentFields.SORT_CONFIG.getName(), sortJson);
        }
        final JsonObject limitJson = preference.getLimitJson();

        if (Objects.nonNull(limitJson)) {
            configJson.put(ListActionFields.LIMIT, limitJson);
        }
        row.set(OMKSRECENTLYAPPLIEDFILTER.CONFIG, Json.isEmpty(configJson) ? null : configJson);
        row.set(OMKSRECENTLYAPPLIEDFILTER.MODIFIED_TIME, currentTime);

        preference.setModifiedTime(currentTime);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts the preference {@link Row} into
     * {@link SegmentFilterUserPreference}.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Represents the preference row.
     * @return {@link SegmentFilterUserPreference}.
     */
    private SegmentFilterUserPreference convertRowToPreference(final Long orgId, final Row row) {
        final SegmentFilterUserPreference preference = new SegmentFilterUserPreference();

        preference.setId(row.getPKValue());
        preference.setOrgId(orgId);
        preference.setUserId(row.get(OMKSRECENTLYAPPLIEDFILTER.USER_ID));
        preference.setSegmentFilterGroup(SegmentFilterGroup.get(row.get(OMKSRECENTLYAPPLIEDFILTER.GROUP_ID)));
        final Short defaultFilterId = row.get(OMKSRECENTLYAPPLIEDFILTER.DEFAULT_FILTER_ID);

        if (Objects.nonNull(defaultFilterId)) {
            preference.setSegmentDefaultFilter(SegmentDefaultFilter.get(defaultFilterId));
        }
        final Long folderId = row.get(OMKSRECENTLYAPPLIEDFILTER.FOLDER_ID);

        if (Objects.nonNull(folderId)) {
            preference.setSegmentFolder(new SegmentFolder(folderId));
        }
        preference.setModifiedTime(row.get(OMKSRECENTLYAPPLIEDFILTER.MODIFIED_TIME));
        final String config = row.get(OMKSRECENTLYAPPLIEDFILTER.CONFIG);

        if (Objects.nonNull(config)) {
            final JsonObject configJson = Json.object(config);

            preference.setSortJson(configJson.optJsonObject(SegmentFields.SORT_CONFIG.getName()));
            preference.setLimitJson(configJson.optJsonObject(ListActionFields.LIMIT));
        }

        return preference;
    }
}