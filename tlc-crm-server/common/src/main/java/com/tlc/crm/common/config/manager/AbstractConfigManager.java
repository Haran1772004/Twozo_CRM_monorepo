package com.tlc.crm.common.config.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractConfigManager<T extends TlcModel> implements ConfigManager<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigManager.class);

    /**
     * {@inheritDoc}
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the {@link ReadableDataStore}
     *
     * @return the {@link ReadableDataStore}
     */
    protected final ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final T model) {
        return exists(model.orgId(), model.id());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final Long orgId, final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), id));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    public T get(final Long orgId, final Long id) {
        final Collection<T> models = get(orgId, List.of(id));

        if (models.isEmpty()) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        } else {
            return models.stream().findFirst().get();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organisation ID
     * @param id    represents the ID
     * @return
     */
    @Override
    public T partialGet(final Long orgId, final Long id) {
        final Collection<T> models = partialGet(orgId, List.of(id));

        if (models.isEmpty()) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        } else {
            return models.stream().findFirst().get();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
                LOGGER.error("Create action: Entity create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed"), ErrorKeys.CREATE_ACTION_FAILED, String.valueOf(index)));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T update(final T model) {
        bulkUpdate(model.orgId(), List.of(model));

        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessOutcome bulkUpdate(final Long orgId, final Collection<T> models) {
        return update(orgId, models);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final T model) {
        delete(model.orgId(), List.of(model));
    }

    /**
     * <p>
     * Deletes the rows that matches the given records
     * </p>
     *
     * @param orgId  Organisation ID
     * @param models Collection of models
     */
    @Override
    public void bulkDelete(final Long orgId, final Collection<T> models) {
        delete(orgId, models);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Long orgId, final Collection<T> models) {
        orgDataStore(orgId).delete(getTable(), models.stream().map(TlcModel::id).filter(Objects::nonNull).toList());
    }

    public abstract Table getTable();

    /**
     * <p>
     * Updates the rows that are matches the given records
     * Failure of one record doesn't impact other records from update operation
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param models Holds the details of the entities.
     * @return Collection of updated entities
     */
    protected abstract ProcessOutcome update(final Long orgId, final Collection<T> models);
}
