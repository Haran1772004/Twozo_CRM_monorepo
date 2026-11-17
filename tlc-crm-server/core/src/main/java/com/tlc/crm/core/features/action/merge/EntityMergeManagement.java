package com.tlc.crm.core.features.action.merge;

import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>
 * Manages duplicate data of the entities.
 * </p>
 *
 * @param <T> {@link Entity}.
 * @author Ajay.
 * @version 1.0
 */
public abstract class EntityMergeManagement<T extends Entity> extends AuthorizationController {

    private final IndexDataPublisher indexDataPublisher;

    protected EntityMergeManagement() {
        indexDataPublisher = IndexDataPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  {@link CrmRequest}.
     * @param response {@link CrmResponse}.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        handle(request, response);
    }

    /**
     * <p>
     * Handles the requests pertaining to duplicate data.
     * </p>
     *
     * @param request  {@link CrmRequest}.
     * @param response {@link CrmResponse}.
     */
    protected abstract void handle(final CrmRequest request, final CrmResponse response);

    /**
     * <p>
     * Gets the entity manager instance used for shared merge validations.
     * </p>
     *
     * @return {@link EntityConfigManager}.
     */
    protected abstract EntityConfigManager<T> getEntityManager();

    /**
     * <p>
     * Publishes merge index action for the retained entity and duplicate ids.
     * </p>
     *
     * @param orgId         Organization's id.
     * @param retained      Retained entity after merge.
     * @param duplicateIds  Duplicate entity ids that were merged away.
     */
    protected void publishMergeIndexData(final Long orgId, final T retained, final Collection<Long> duplicateIds) {
        indexDataPublisher.publishIndexData(orgId, List.of(retained), Action.MERGE, duplicateIds);
    }

    /**
     * <p>
     * Publishes index action for the given entity.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param entity Entity to index.
     * @param action Dictates the course of action.
     */
    protected void publishIndexData(final Long orgId, final T entity, final Action action) {
        indexDataPublisher.publishIndexData(orgId, List.of(entity), action);
    }

    /**
     * <p>
     * Builds a unique id collection from the given json array.
     * </p>
     *
     * @param idsArray Represents the array of ids.
     * @return The unique collection of ids.
     */
    protected Collection<Long> buildDuplicateEntityIds(final JsonArray idsArray) {
        final Collection<Long> ids = new TreeSet<>();

        for (int index = 0; index < idsArray.size(); index++) {
            ids.add((Long) idsArray.get(index));
        }

        return ids;
    }

    /**
     * <p>
     * Builds all entity ids including primary and duplicates.
     * </p>
     *
     * @param primaryId    Represents the primary id.
     * @param duplicateIds Represents the duplicate ids.
     * @return The unique ids including primary and duplicates.
     */
    protected Collection<Long> buildAllIds(final Long primaryId, final Collection<Long> duplicateIds) {
        final Set<Long> ids = new TreeSet<>();

        ids.add(primaryId);
        ids.addAll(duplicateIds);

        return ids;
    }

    /**
     * <p>
     * Validates entity existence and throws the given error code when invalid.
     * </p>
     *
     * @param exists            Represents the existence flag
     * @param errorCodeProvider {@link ErrorCodeProvider}.
     * @param i18nKey           Represents the i18n key
     */
    protected void validateExists(final boolean exists, final ErrorCodeProvider errorCodeProvider, final String i18nKey) {

        if (!exists) {
            throw ErrorCode.get(errorCodeProvider, i18nKey);
        }
    }

    /**
     * <p>
     * Validates that the primary and secondary ids are different.
     * </p>
     *
     * @param primaryId         Represents the primary id.
     * @param secondaryId       Represents the secondary id.
     * @param errorCodeProvider  {@link ErrorCodeProvider}.
     */
    protected void validateDistinctIds(final Long primaryId, final Long secondaryId, final ErrorCodeProvider errorCodeProvider) {
        if (Objects.equals(primaryId, secondaryId)) {
            throw ErrorCode.get(errorCodeProvider);
        }
    }

    /**
     * <p>
     * Validates both primary and secondary entities exist and are distinct.
     * </p>
     *
     * @param orgId                    Represents the organization id.
     * @param primaryId                Represents the primary id.
     * @param secondaryId              Represents the secondary id.
     * @param sameIdErrorCodeProvider  Represents same-id validation error.
     * @param notFoundErrorCodeProvider Represents not-found validation error.
     * @param i18nKey                  Represents the i18n key for not-found error.
     * @return True if both ids are valid and available.
     */
    protected boolean validatePrimaryAndSecondaryEntities(final Long orgId, final Long primaryId, final Long secondaryId,
                                                          final ErrorCodeProvider sameIdErrorCodeProvider,
                                                          final ErrorCodeProvider notFoundErrorCodeProvider,
                                                          final String i18nKey) {
        validateDistinctIds(primaryId, secondaryId, sameIdErrorCodeProvider);
        validateExists(getEntityManager().exists(orgId, Set.of(primaryId, secondaryId)), notFoundErrorCodeProvider, i18nKey);

        return true;
    }

    /**
     * <p>
     * Validates merge input where one primary id is merged with multiple duplicate ids.
     * </p>
     *
     * @param orgId                     Represents the organization id.
     * @param primaryId                 Represents the primary id.
     * @param duplicateIds              Represents the duplicate ids.
     * @param notFoundErrorCodeProvider Represents not-found validation error.
     * @param i18nKey                   Represents the i18n key for not-found error.
     */
    protected void validatePrimaryAndDuplicateEntities(final Long orgId, final Long primaryId, final Collection<Long> duplicateIds,
                                                       final ErrorCodeProvider notFoundErrorCodeProvider, final String i18nKey) {
        if (Objects.nonNull(duplicateIds) && duplicateIds.contains(primaryId)) {
            throw ErrorCode.get(ContactErrorCodes.SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED);
        }
        final Collection<Long> ids = buildAllIds(primaryId, duplicateIds);

        validateExists(getEntityManager().exists(orgId, ids), notFoundErrorCodeProvider, i18nKey);

    }
}