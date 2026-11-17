package com.tlc.crm.core.features.action.export;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.entity.export.action.AbstractEntityDataExportAction;
import com.tlc.crm.entity.list.model.Provider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Exports the activity information.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@DataExporter(name = "activity")
public class ActivityExportAction extends AbstractEntityDataExportAction {

    private static final String TITLE = "title";
    private static final String ACTIVITY_TITLE = "activityTitle";
    private static final String DEAL = "deal";

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param roleId Represents the role id.
     * @return Holds the permission details of the data export.
     */
    @Override
    protected RolePermission getPermission(final Long orgId, final Long roleId) {
        return rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, SubCategory.Type.ACTIVITIES, Action.Type.EXPORT_RECORDS);
    }

    /**
     * Gets the name of the provider for the list action.
     *
     * @return The {@link Provider}.
     */
    @Override
    protected Provider getProvider() {
        return Provider.ACTIVITY_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListType}.
     */
    @Override
    protected ListType getListType() {
        return ListType.ACTIVITY_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param entityRows A {@link Map} contains the collection of entity rows
     */
    protected void preProcess(final Collection<Map<String, Object>> entityRows) {
        for (final Map<String, Object> entityRow : entityRows) {

            if (entityRow.containsKey(TITLE)) {
                extractAndPut(entityRow, TITLE, ACTIVITY_TITLE, TITLE);
            }

            if (entityRow.containsKey(DEAL)) {
                extractAndPut(entityRow, DEAL, TITLE, DEAL);
            }
        }
    }

    /**
     * <p>
     * Extracts a nested value from a map within the given row and inserts it into the row with a new key
     * </p>
     *
     * @param row       The map representing row of data
     * @param key       The key used to retrieve the nested map from the row
     * @param nestedKey The key used to retrieve the nested value from the nested map
     * @param targetKey The key under which the extracted value will be stored in the row
     */
    private void extractAndPut(final Map<String, Object> row, final String key, final String nestedKey, final String targetKey) {
        if (row.get(key) instanceof final Map<?, ?> map) {
            final Object nestedValue = map.get(nestedKey);

            if (Objects.nonNull(nestedValue)) {
                row.put(targetKey, nestedValue.toString());
            }
        }
    }
}
