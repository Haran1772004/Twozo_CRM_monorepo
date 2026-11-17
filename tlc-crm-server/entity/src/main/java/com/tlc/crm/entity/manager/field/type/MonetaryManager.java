package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Monetary} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class MonetaryManager extends AbstractConfigManager<Monetary> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonetaryManager.class);

    private static class Instance {
        private static final MonetaryManager INSTANCE = new MonetaryManager();
    }

    private MonetaryManager() {
    }

    /**
     * Gets the instance of {@link MonetaryManager}
     *
     * @return {@link MonetaryManager}
     */
    public static MonetaryManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETMONETARY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of monetary IDS
     * @return {@link Collection<Monetary>}
     */
    @Override
    public Collection<Monetary> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.MONETARY_NOT_FOUND, String.format("Org ID : %d, Entity ID : %s", orgId, ids));
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of monetary IDs
     * @return
     */
    @Override
    public Collection<Monetary> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param monetary which contains the monetary data
     * @return {@link Monetary} which contains the monetary data
     */
    @Override
    public Monetary create(final Monetary monetary) {
        final DataContainer targetContainer = DataContainer.create();

        loadMonetaryIntoContainer(true, monetary, targetContainer);
        orgDataStore(monetary.orgId()).commitChanges(targetContainer);

        return monetary;
    }

    /**
     * <p>
     * Loads the monetary details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param monetary        An entity that holds the monetary attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadMonetaryIntoContainer(final boolean isCreateEvent, final Monetary monetary, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(monetary.orgId()).get(getTable(), List.of(monetary.id()));

        loadMonetaryIntoContainer(isCreateEvent, monetary, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the monetary details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param monetary        An entity that holds the monetary attributes
     * @param sourceContainer Holds the user defined and system field values of existing monetary
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadMonetaryIntoContainer(final boolean isCreateEvent, final Monetary monetary,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table monetaryTable = getTable();
        final Long id = monetary.id();
        final Row monetaryRow = isCreateEvent ? new Row(monetaryTable) : sourceContainer.getRow(monetaryTable, id);

        if (!isCreateEvent && Objects.isNull(monetaryRow)) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (monetaryRow.isNewRow()) {
            monetaryRow.setOrgId(monetary.orgId());
            monetary.setId(monetaryRow.getPKValue());
        }
        monetaryRow.set(OMETMONETARY.CURRENCY_ID, monetary.getCurrency().getId());
        monetaryRow.set(OMETMONETARY.VALUE, monetary.getValue());

        targetContainer.updateRow(monetaryRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        represents the Organisation ID
     * @param monetaryList represents the {@link Collection<Monetary>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Monetary> monetaryList) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = monetaryList.stream().map(Monetary::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        monetaryList.forEach(monetary -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadMonetaryIntoContainer(false, monetary, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(monetary.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Monetary update failed", errorCode);
                failedEntities.add(new ErrorDescriptor(errorCode));
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
     * Converts row into {@link Monetary}
     * </p>
     *
     * @param row Monetary row to be converted
     * @return {@link Monetary}
     */
    public Monetary convertRowToModel(final Row row) {
        final Monetary monetary = new Monetary();
        final Short currencyId = row.get(OMETMONETARY.CURRENCY_ID);
        final Object value = row.get(OMETMONETARY.VALUE);

        if (Objects.nonNull(currencyId)) {
            monetary.setCurrency(Currency.getCurrencyById(currencyId));
        }
        monetary.setId(row.getPKValue());
        monetary.setOrgId(row.getOrgId());
        monetary.setValue(value instanceof final BigDecimal bigDecimalValue ? bigDecimalValue
                : BigDecimal.valueOf((Double) value));

        return monetary;
    }
}