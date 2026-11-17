package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.DateRange;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
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
 * Implements the {@link DateRange} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class DateRangeManager extends AbstractConfigManager<DateRange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateRangeManager.class);

    private static class Instance {
        private static final DateRangeManager INSTANCE = new DateRangeManager();
    }

    private DateRangeManager() {

    }

    /**
     * Gets the instance of {@link DateRangeManager}
     *
     * @return {@link DateRangeManager}
     */
    public static DateRangeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETDATERANGE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of date range IDS
     * @return {@link Collection<DateRange>}
     */
    @Override
    public Collection<DateRange> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.DATE_RANGE_NOT_FOUND, "i18n.entity.field.type.dateRange.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of date range IDs
     * @return
     */
    @Override
    public Collection<DateRange> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param dateRange which contains the dateRange data
     * @return {@link DateRange} which contains the dateRange data
     */
    @Override
    public DateRange create(final DateRange dateRange) {
        final DataContainer targetContainer = DataContainer.create();

        loadDateRangeIntoContainer(true, dateRange, targetContainer);
        orgDataStore(dateRange.orgId()).commitChanges(targetContainer);

        return dateRange;
    }

    /**
     * <p>
     * Loads the date range details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param dateRange       An entity that holds the date range attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDateRangeIntoContainer(final boolean isCreateEvent, final DateRange dateRange, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(dateRange.orgId()).get(getTable(), List.of(dateRange.id()));

        loadDateRangeIntoContainer(isCreateEvent, dateRange, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the date range details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param dateRange       An entity that holds the date range attributes
     * @param sourceContainer Holds the user defined and system field values of existing date range
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDateRangeIntoContainer(final boolean isCreateEvent, final DateRange dateRange,
                                            final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dateRangeTable = getTable();
        final Long id = dateRange.id();
        final Row dateRangeRow = isCreateEvent ? new Row(dateRangeTable) : sourceContainer.getRow(dateRangeTable, id);

        if (!isCreateEvent && Objects.isNull(dateRangeRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (dateRangeRow.isNewRow()) {
            dateRangeRow.setOrgId(dateRange.orgId());
            dateRange.setId(dateRangeRow.getPKValue());
        }
        dateRangeRow.set(OMETDATERANGE.FROM, dateRange.getFrom());
        dateRangeRow.set(OMETDATERANGE.TO, dateRange.getTo());

        targetContainer.updateRow(dateRangeRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      represents the Organisation ID
     * @param dateRanges represents the {@link Collection<DateRange>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<DateRange> dateRanges) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = dateRanges.stream().map(DateRange::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        dateRanges.forEach(dateRange -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadDateRangeIntoContainer(false, dateRange, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(dateRange.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Date range update failed", errorCode);
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
     * Converts row into {@link DateRange}
     * </p>
     *
     * @param row Date range row to be converted
     * @return {@link DateRange}
     */
    public DateRange convertRowToModel(final Row row) {
        final DateRange dateRange = new DateRange();

        dateRange.setId(row.getPKValue());
        dateRange.setOrgId(row.getOrgId());
        dateRange.setFrom(row.get(OMETDATERANGE.FROM));
        dateRange.setTo(row.get(OMETDATERANGE.TO));

        return dateRange;
    }
}