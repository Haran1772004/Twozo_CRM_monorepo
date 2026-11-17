package com.tlc.crm.core.list.field.selector;

import com.tlc.crm.common.list.model.FieldConfig;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;

import com.tlc.crm.entity.list.filter.ListQueryComposer;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * Handles the {@link ListQueryComposer} of products.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ProductListQueryComposer implements ListQueryComposer {

    private static final String ALIAS_NAME_FORMAT = "%s_%s";

    private final ProductManager productManager;

    private static class Instance {
        private static final ProductListQueryComposer INSTANCE = new ProductListQueryComposer();
    }

    public ProductListQueryComposer() {
        productManager = ProductManager.getInstance();
    }

    /**
     * Gets the {@link ProductListQueryComposer} Instance.
     *
     * @return The {@link ProductListQueryComposer}.
     */
    public static ProductListQueryComposer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldConfig Holds the details of the field.
     * @param selectQuery The data retrieval query.
     */
    @Override
    public void select(final FieldConfig fieldConfig, final SelectQuery selectQuery) {
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldConfig The configuration describing the field to join on.
     * @param selectQuery The selectQuery where the  join clause is added on.
     */
    @Override
    public void loadJoinClause(final FieldConfig fieldConfig, final SelectQuery selectQuery) {
        final String fieldName = fieldConfig.getName();
        final Table productTable = productManager.getTable();

        switch (fieldConfig.getFieldType().storeType()) {
            case OWNER -> {
                final Table table = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldName, StoreType.OWNER.getName()));

                switch (fieldName) {
                    case "salesOwner" ->
                            selectQuery.addJoinClause(new JoinClause(productTable.getColumn(MPOPRODUCT.OWNER_ID), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "createdBy" ->
                            selectQuery.addJoinClause(new JoinClause(productTable.getColumn(MPOPRODUCT.CREATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "updatedBy" ->
                            selectQuery.addJoinClause(new JoinClause(productTable.getColumn(MPOPRODUCT.UPDATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
                }
                selectQuery.addSelectClause(table.getColumn(OMCRMUSER.NAME));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldConfig The field configuration to sort by.
     * @param orderType   The type of ordering (ASC or DESC).
     * @return An OrderByClause representing the ORDER BY expression.
     */
    @Override
    public OrderByClause getOrderByClause(final FieldConfig fieldConfig, final OrderByClause.OrderType orderType) {
        return switch (fieldConfig.getFieldType().storeType()) {
            case OWNER -> {
                final Table ownerAliasTable = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldConfig.getName(), StoreType.OWNER.getName()));

                yield new OrderByClause(ownerAliasTable.getColumn(OMCRMUSER.NAME), orderType);
            }
            default -> null;
        };
    }

    /**
     * Returns an aliased version of the given table for the specified field.
     * This is typically used when the same table needs to be joined multiple times
     * with different contexts.
     *
     * @param table     The original table reference.
     * @param fieldName The field for which the alias is being created.
     * @return A {@code Table} with an alias applied based on the field name.
     */
    private Table getAliasTable(final Table table, final String fieldName) {
        return Table.get(table.getName(), String.format(ALIAS_NAME_FORMAT, fieldName, table.getAlias()));
    }

}