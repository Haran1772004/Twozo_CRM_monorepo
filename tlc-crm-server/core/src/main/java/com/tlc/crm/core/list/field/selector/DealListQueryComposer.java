package com.tlc.crm.core.list.field.selector;

import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.entity.list.filter.ListQueryComposer;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * It selects the additional fields which is needed for the deal list response.
 * </p>
 *
 * @author kavinila
 * @version 1.0
 */
public final class DealListQueryComposer implements ListQueryComposer {

    private static final String ALIAS_NAME_FORMAT = "%s_%s";

    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final DealManager dealManager;

    private static class Instance {
        private static final DealListQueryComposer INSTANCE = new DealListQueryComposer();
    }

    public DealListQueryComposer() {
        productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * Gets the {@link DealListQueryComposer} Instance
     *
     * @return the {@link DealListQueryComposer}
     */
    public static DealListQueryComposer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Selects the additional fields which is needed for the deal list
     * </p>
     *
     * @param fieldConfig Holds the details of the field
     * @param selectQuery The data retrieval query
     */
    @Override
    public void select(final FieldConfig fieldConfig, final SelectQuery selectQuery) {
        final String fieldName = fieldConfig.getName();

        switch (fieldConfig.getFieldType().storeType()) {
            case DEAL_VALUE -> {
                final Column productDetailsHolderDealIdColumn = getAliasTable(productDetailsHolderManager.getTable(),
                        fieldName).getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID, ListActionFields.PRODUCT_DETAILS_HOLDER_DEAL_ID);

                selectQuery.addSelectClause(productDetailsHolderDealIdColumn);
                selectQuery.addJoinClause(new JoinClause(dealManager.getTable().getPKColumn(), productDetailsHolderDealIdColumn, JoinClause.JoinType.LEFT));
            }
        }
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
        final Table dealTable = dealManager.getTable();

        switch (fieldConfig.getFieldType().storeType()) {
            case OWNER -> {
                final Table table = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldName, StoreType.OWNER.getName()));

                switch (fieldName) {
                    case "salesOwner" ->
                            selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.OWNER_ID), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "createdBy" ->
                            selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.CREATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "updatedBy" ->
                            selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.UPDATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
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
     * Gets the alias table
     *
     * @param table     Represents the {@link Table}
     * @param fieldName Represents the field name
     * @return {@link Table}
     */
    private Table getAliasTable(final Table table, final String fieldName) {
        return Table.get(table.getName(), String.format(ALIAS_NAME_FORMAT, fieldName, table.getAlias()));
    }
}
