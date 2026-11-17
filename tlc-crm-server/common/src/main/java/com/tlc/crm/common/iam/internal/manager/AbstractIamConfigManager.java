package com.tlc.crm.common.iam.internal.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides abstract way to configure the {@link AbstractIamConfigManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public abstract class AbstractIamConfigManager<T extends TlcModel> implements IamConfigManger<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIamConfigManager.class);

    protected AbstractIamConfigManager() {
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} to handle the DB related actions
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} to handle the DB related actions
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore secureOrgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the entity ids
     * @return If any id is existing it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param models Represents the collection of models
     * @return Created response to be sent
     */
    @Override
    public ProcessOutcome create(final Long orgId, final Collection<T> models) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Counter counter = new Counter();

        for (final Iterator<T> iterator = models.iterator(); iterator.hasNext(); counter.getAndIncrement()) {
            final T model = iterator.next();

            try {
                create(model);
                processOutcome.addProcessedRecord(model.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Entity create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
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
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param models Represents the collection of models
     */
    @Override
    public void delete(final Long orgId, final Collection<T> models) {
        orgDataStore(orgId).delete(getTable(), models.stream().map(TlcModel::id).filter(Objects::nonNull).toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param criteria Represents an criteria
     * @return The count of entities
     */
    @Override
    public long getTotalCount(final Long orgId, final Criteria criteria) {
        final CountQuery countQuery = CountQuery.get(getTable());

        if (Objects.nonNull(criteria)) {
            countQuery.setWhereClause(new WhereClause(criteria));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the total count of entities
     * </p>
     *
     * @param orgId    Represents the organization id
     * @return The total count of entities
     */
    @Override
    public long getTotalCount(final Long orgId) {
        return getTotalCount(orgId, null);
    }

    /**
     * <p>
     * Fetches the table
     * </p>
     *
     * @return The table
     */
    public abstract Table getTable();
}
