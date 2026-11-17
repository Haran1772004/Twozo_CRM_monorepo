package com.tlc.crm.email.notification;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.WritableDataStore;

import com.tlc.validator.ProcessOutcome;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the common email related services.
 * </p>
 *
 * @param <T>
 */
public abstract class AbstractNotificationManager<T extends NotificationModel> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractNotificationManager.class);

    /**
     * <p>
     * Gets the {@link OrgDataStore}.
     * </p>>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}.
     * </p>
     *
     * @return the {@link ReadableDataStore}
     */
    protected final ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Gets the {@link WritableDataStore}
     * </p>
     *
     * @return {@link WritableDataStore}
     */
    protected final WritableDataStore writableDataStore() {
        return DataStoreProvider.getWritableDataStore();
    }

    /**
     * <p>
     * Converts the stream of rows into collection of models
     * </p>
     *
     * @param rows {@link Stream} of rows
     * @return {@link Collection} of model which extends the {@link NotificationModel}
     */
    public Collection<T> convertRowsToModel(final Stream<Row> rows) {
        return Objects.isNull(rows) ? List.of() : rows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Converts the row data into model
     * </p>
     *
     * @param row Row that holds the information of model
     * @return the model which extends the {@link NotificationModel}
     */
    protected T convertRowToModel(final Row row) {
        return null;
    }

    /**
     * <p>
     * Gets the model which extends the {@link NotificationModel}.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the id
     * @return model which extends the {@link NotificationModel}
     */
    public T get(final Long orgId, final Long id) {
        final Collection<T> models = get(orgId, List.of(id));

        if (models.isEmpty()) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        } else {
            return models.stream().findFirst().get();
        }
    }

    /**
     * <p>
     * Gets the {@link Collection} of model which extends the {@link NotificationModel}.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of id
     * @return {@link Collection} of model which extends the {@link NotificationModel}
     */
    protected Collection<T> get(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Creates the {@link Collection} of model which extends the {@link NotificationModel}
     * </p>
     *
     * @param models Represents the {@link Collection} of records
     * @return A {@link Map} of responses.
     */
    public ProcessOutcome create(final Long orgId, final Collection<T> models) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        int index = 0;
        for (final Iterator<T> iterator = models.iterator(); iterator.hasNext(); index++) {
            final T model = iterator.next();

            try {
                create(model);
                processOutcome.addProcessedRecord(model.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Notification create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed")));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Creates the model which extends the {@link NotificationModel}
     * </p>
     *
     * @param model Represents the model
     * @return model which extends the {@link NotificationModel}
     */
    protected T create(final T model) {
        return null;
    }

    /**
     * <p>
     * Updates the row that matches the given record.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param models Represents the {@link Collection} of model
     * @return The {@link ProcessOutcome}
     */
    protected ProcessOutcome update(final Long orgId, final Collection<T> models) {
        return new ProcessOutcome();
    }

    /**
     * Checks whether the given {@link Collection} records exists or not
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of id
     * @return true, if all given ids exists
     */
    public final boolean exists(final Long orgId, final Set<Long> ids) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * <p>
     * Checks whether the given record is exists or not.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the user id
     * @return true, if the record exists; otherwise false
     */
    public boolean exists(final Long orgId, final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), id));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Deletes the given {@link Collection} of record.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param ids    Represents the {@link Collection} of model
     */
    protected void delete(final long orgId, final long userId, final Collection<Long> ids) {

    }

    /**
     * <p>
     * Gets the table of current service class.
     * </p>
     *
     * @return {@link Table}
     */
    public abstract Table getTable();
}
