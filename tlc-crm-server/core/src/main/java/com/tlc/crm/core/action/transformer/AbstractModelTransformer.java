package com.tlc.crm.core.action.transformer;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.list.transformer.ModelTransformer;

import com.tlc.crm.core.api.analytics.permission.DashboardPermissionService;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.handler.SourceAccessControlledModelHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * <p>
 * An abstract class for model transformers, providing a common foundation for transformation logic
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AbstractModelTransformer implements ModelTransformer {

    protected AbstractModelTransformer() {
    }

    /**
     * <p>
     * Groups user IDs under a specified field in the provided entity row and updates the entities map
     * </p>
     *
     * @param field     The field under which the user IDs should be grouped
     * @param entityRow The row of data representing the entity
     * @param entities  A map of entities keyed by their primary key, where each entity is a map of fields and values
     */
    protected void groupUserIdsFields(final String field, final Map<String, Object> entityRow,
                                      final Map<Long, Map<String, Object>> entities) {
        final Long userId = (Long) entityRow.get(field);

        if (Objects.isNull(userId)) {
            return;
        }
        final Long pkValue = (Long) entityRow.get(TransformerFields.ID.getName());

        if (entities.containsKey(pkValue)) {
            final Map<String, Object> entity = entities.get(pkValue);

            if (entity.containsKey(field)) {
                final Object value = entity.get(field);

                if (value instanceof Collection) {
                    final Collection<Long> userIds = (Collection<Long>) entity.get(field);

                    if (!userIds.contains(userId)) {
                        userIds.add(userId);
                        entity.put(field, userIds);
                    }
                }
            }
        } else {
            final Collection<Object> userIds = new HashSet<>();

            userIds.add(userId);
            entityRow.put(field, userIds);
            entities.put(pkValue, entityRow);
        }
    }

    /**
     * <p>
     * Collects user IDs from the given entity row and stores them in the specified collection.
     * </p>
     *
     * @param fieldKey  The key in the entity row that contains the user ID
     * @param entityRow A map representing an entity's data, from which the user ID will be extracted
     * @param userIds   A collection to store the extracted user IDs
     * @param users     A map to store the extracted user ID against the provided field key
     */
    protected void collectUserIds(final String fieldKey, final Map<String, Object> entityRow,
                                  final Collection<Long> userIds, final Map<String, Object> users) {
        if (entityRow.containsKey(fieldKey)) {
            final Object userId = entityRow.get(fieldKey);

            if (Objects.nonNull(userId)) {
                if (userId instanceof Collection) {
                    userIds.addAll((Collection<Long>) userId);
                } else {
                    userIds.add((long) userId);
                }
                users.put(fieldKey, userId);
            }
        }
    }

    /**
     * <p>
     * Collects the pk values from the given data
     * </p>
     *
     * @param records The collection of Map containing model data
     * @return The collection of model ids
     */
    protected Collection<Long> collectPkValues(final Collection<Map<String, Object>> records) {
        final String idKey = TransformerFields.ID.getName();

        return records.stream().filter(modelRecord -> modelRecord.containsKey(idKey)).map(modelRecord -> (Long) modelRecord.get(idKey))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the permission access for the given model data
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param records The collection of Map containing model data
     * @return The map containing key as model id and value as action access map
     */
    protected Map<Long, Map<String, Boolean>> getPermissionAccess(final Long orgId, final Collection<Map<String, Object>> records) {
        return SourceAccessControlledModelHandler.getInstance().getModelsPermissionAccess(orgId, collectPkValues(records));
    }

    /**
     * Retrieves the permission access map for dashboards within an organization.
     *
     * @param orgId   The organization ID.
     * @param isTrash Indicates whether the dashboards are in the trash.
     * @param records A collection of maps containing dashboard data.
     * @return A map where the key is the dashboard ID, and the value is another map containing action permissions.
     */
    protected Map<Long, Map<String, Boolean>> getDashboardPermissionAccess( final Long orgId, boolean isTrash,
                                                                            final Collection<Map<String, Object>> records )
    {
        return DashboardPermissionService.INSTANCE()
                                         .getDashboardPermissionAccess( orgId, isTrash, groupDashboardIdAndCreator( records ) );
    }

    /**
     * Groups dashboard IDs with their respective creator user IDs.
     *
     * @param records A collection of maps where each entry contains "id" and "createdBy" keys.
     * @return A map where the key is the dashboard ID and the value is the creator's user ID.
     * @throws ClassCastException if the values are not of type Long.
     */
    private Map<Long, Pair<Long, Boolean>> groupDashboardIdAndCreator(final Collection<Map<String, Object>> records )
    {
        return records.stream()
                      .collect( Collectors.toMap( record -> (Long) record.get( TransformerFields.ID.getName() ),
                                                  record -> Pair.of( (Long) record.get( TransformerFields.CREATED_BY.getName() ),
                                                                     (Boolean) record.get( TransformerFields.IS_CURATED.getName() ) ) ) );
    }

    /**
     * Maps a primary key value into a hyperlink-style property map and inserts it into the given entity row.
     *
     * @param pkValue   The primary key value to be used as the link target.
     * @param entityRow The map representing the row of entity data.
     * @param fieldKey  The name of the field under which the primary key will be stored in the "linkProperties" map.
     */
    protected final void mapLinkProperties(final Long pkValue, final Map<String, Object> entityRow,
                                           final String fieldKey) {
        final Map<String, Object> fieldMap = new HashMap<>();

        fieldMap.put(fieldKey, pkValue);
        entityRow.put(TransformerFields.LINK_PROPERTIES.getName(), fieldMap);
    }
}
