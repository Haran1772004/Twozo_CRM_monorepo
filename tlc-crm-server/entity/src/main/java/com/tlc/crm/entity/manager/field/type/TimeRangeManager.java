package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
import com.tlc.crm.fields.sql.resource.OMETTIMERANGE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link TimeRange} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class TimeRangeManager extends AbstractConfigManager<TimeRange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeRangeManager.class);

    private static class Instance {
        private static final TimeRangeManager INSTANCE = new TimeRangeManager();
    }

    private TimeRangeManager() {

    }

    /**
     * Gets the instance of {@link TimeRangeManager}
     *
     * @return {@link TimeRangeManager}
     */
    public static TimeRangeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETTIMERANGE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of time range IDS
     * @return {@link Collection<TimeRange>}
     */
    @Override
    public Collection<TimeRange> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.TIME_RANGE_NOT_FOUND, String.format("Org ID : %d, Entity ID : %s", orgId, ids));
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of time range IDs
     * @return
     */
    @Override
    public Collection<TimeRange> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link TimeRange} which contains the timeRange data
     * @paramtimeRange which contains the timeRange data
     */
    @Override
    public TimeRange create(final TimeRange timeRange) {
        final DataContainer targetContainer = DataContainer.create();

        loadTimeRangeIntoContainer(true, timeRange, targetContainer);
        orgDataStore(timeRange.orgId()).commitChanges(targetContainer);

        return timeRange;
    }

    /**
     * <p>
     * Loads the time range details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param timeRange       An entity that holds the time range attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadTimeRangeIntoContainer(final boolean isCreateEvent, final TimeRange timeRange, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(timeRange.orgId()).get(getTable(), List.of(timeRange.id()));

        loadTimeRangeIntoContainer(isCreateEvent, timeRange, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the timeRange details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param timeRange       An entity that holds the time range attributes
     * @param sourceContainer Holds the user defined and system field values of existing time range
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadTimeRangeIntoContainer(final boolean isCreateEvent, final TimeRange timeRange,
                                            final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table timeRangeTable = getTable();
        final Long id = timeRange.id();
        final Row timeRangeRow = isCreateEvent ? new Row(timeRangeTable) : sourceContainer.getRow(timeRangeTable, id);

        if (!isCreateEvent && Objects.isNull(timeRangeRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (timeRangeRow.isNewRow()) {
            timeRangeRow.setOrgId(timeRange.orgId());
            timeRange.setId(timeRangeRow.getPKValue());
        }
        timeRangeRow.set(OMETDATERANGE.FROM, timeRange.getFrom());
        timeRangeRow.set(OMETDATERANGE.TO, timeRange.getTo());

        targetContainer.updateRow(timeRangeRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      represents the Organisation ID
     * @param timeRanges represents the {@link Collection<TimeRange>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<TimeRange> timeRanges) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = timeRanges.stream().map(TimeRange::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        timeRanges.forEach(timeRange -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTimeRangeIntoContainer(false, timeRange, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(timeRange.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Time range update failed", errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed")));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link TimeRange}
     * </p>
     *
     * @param row Time range row to be converted
     * @return {@link TimeRange}
     */
    public TimeRange convertRowToModel(final Row row) {
        final TimeRange timeRange = new TimeRange();

        timeRange.setId(row.getPKValue());
        timeRange.setOrgId(row.getOrgId());
        timeRange.setFrom(row.get(OMETDATERANGE.FROM));
        timeRange.setTo(row.get(OMETDATERANGE.TO));

        return timeRange;
    }
}