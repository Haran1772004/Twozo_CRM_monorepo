package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.fields.sql.resource.OMETDATETIMERANGE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
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
 * Implements the {@link DateTimeRange} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class DateTimeRangeManager extends AbstractConfigManager<DateTimeRange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeRangeManager.class);

    private static class Instance {
        private static final DateTimeRangeManager INSTANCE = new DateTimeRangeManager();
    }

    private DateTimeRangeManager() {

    }

    /**
     * Gets the instance of {@link DateTimeRangeManager}
     *
     * @return {@link DateTimeRangeManager}
     */
    public static DateTimeRangeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETDATETIMERANGE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of date time range IDS
     * @return {@link Collection<DateTimeRange>}
     */
    @Override
    public Collection<DateTimeRange> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.DATE_TIME_RANGE_NOT_FOUND, String.format("Org ID : %d, Entity ID : %s",
                    orgId, ids));
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of date time range IDs
     * @return
     */
    @Override
    public Collection<DateTimeRange> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param dateTimeRange which contains the date time range data
     * @return {@link DateTimeRange} which contains the date time range data
     */
    @Override
    public DateTimeRange create(final DateTimeRange dateTimeRange) {
        final DataContainer targetContainer = DataContainer.create();

        loadDateTimeRangeIntoContainer(true, dateTimeRange, targetContainer);
        orgDataStore(dateTimeRange.orgId()).commitChanges(targetContainer);

        return dateTimeRange;
    }

    /**
     * <p>
     * Loads the date time range details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param dateTimeRange   An entity that holds the date time range attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDateTimeRangeIntoContainer(final boolean isCreateEvent, final DateTimeRange dateTimeRange,
                                               final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(dateTimeRange.orgId()).get(getTable(), List.of(dateTimeRange.id()));

        loadDateTimeRangeIntoContainer(isCreateEvent, dateTimeRange, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the date time range details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param dateTimeRange   An entity that holds the date time range attributes
     * @param sourceContainer Holds the user defined and system field values of existing date time range
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDateTimeRangeIntoContainer(final boolean isCreateEvent, final DateTimeRange dateTimeRange,
                                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dateTimeRangeTable = getTable();
        final Long id = dateTimeRange.id();
        final Row dateTimeRangeRow = isCreateEvent ? new Row(dateTimeRangeTable) : sourceContainer.getRow(dateTimeRangeTable, id);

        if (!isCreateEvent && Objects.isNull(dateTimeRangeRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (dateTimeRangeRow.isNewRow()) {
            dateTimeRangeRow.setOrgId(dateTimeRange.orgId());
            dateTimeRange.setId(dateTimeRangeRow.getPKValue());
        }
        dateTimeRangeRow.set(OMETDATETIMERANGE.FROM, dateTimeRange.getFrom());
        dateTimeRangeRow.set(OMETDATETIMERANGE.TO, dateTimeRange.getTo());

        targetContainer.updateRow(dateTimeRangeRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          represents the Organisation ID
     * @param dateTimeRanges represents the {@link Collection<DateTimeRange>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<DateTimeRange> dateTimeRanges) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = dateTimeRanges.stream().map(DateTimeRange::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        dateTimeRanges.forEach(dateTimeRange -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadDateTimeRangeIntoContainer(false, dateTimeRange, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(dateTimeRange.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Date time range update failed", errorCode);
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
     * Converts row into {@link DateTimeRange}
     * </p>
     *
     * @param row Date time range row to be converted
     * @return {@link DateTimeRange}
     */
    public DateTimeRange convertRowToModel(final Row row) {
        final DateTimeRange dateTimeRange = new DateTimeRange();

        dateTimeRange.setId(row.getPKValue());
        dateTimeRange.setOrgId(row.getOrgId());
        dateTimeRange.setFrom(row.get(OMETDATETIMERANGE.FROM));
        dateTimeRange.setTo(row.get(OMETDATETIMERANGE.TO));

        return dateTimeRange;
    }
}