package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.util.TableUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Default secure select query builder that applies permission criteria
 * when only the base table needs securing. This implementation will
 * transform the base table into a derived table when permission criteria
 * exist for it, otherwise it returns the original {@link SelectQuery}.
 *
 * @author Sathish
 * @version 1.0
 */
public class DefaultSecureSelectQueryBuilder extends AbstractSecureSelectQueryBuilder<SelectQuery> {

    private DefaultSecureSelectQueryBuilder() {
    }

    private static final class InstanceHolder {
        private static final DefaultSecureSelectQueryBuilder SECURE_SELECT_QUERY_BUILDER = new DefaultSecureSelectQueryBuilder();
    }

    /**
     * Return the singleton instance of this builder.
     *
     * @return {@link DefaultSecureSelectQueryBuilder} singleton
     */
    public static DefaultSecureSelectQueryBuilder getInstance() {
        return InstanceHolder.SECURE_SELECT_QUERY_BUILDER;
    }

    /**
     * Apply permission-driven transformations to the provided select query.
     * If the base table has associated {@link PermissionCriteria} it is
     * converted into a derived table using {@link #buildDerivedTable(java.util.Collection, String, Table, PermissionCriteria)}.
     *
     * @param selectQuery           the original select query
     * @param permissionCriteriaMap mapping of table -> PermissionCriteria (may contain base table)
     * @return transformed SelectQuery enforcing any base-table permission criteria
     */
    @Override
    public SelectQuery build(final SelectQuery selectQuery, final Map<Table, PermissionCriteria> permissionCriteriaMap) {
        final Table baseTable = selectQuery.getBaseTable();
        final PermissionCriteria baseTablePermissionCriteria = permissionCriteriaMap.remove(baseTable);

        if (Objects.nonNull(baseTablePermissionCriteria)) {
            selectQuery.updateBaseTableAsDerivedTable(buildDerivedTable(TableUtils.getAllColumns(baseTable), baseTable.getAlias(), baseTable, baseTablePermissionCriteria));
        }

        return selectQuery;
    }
}
