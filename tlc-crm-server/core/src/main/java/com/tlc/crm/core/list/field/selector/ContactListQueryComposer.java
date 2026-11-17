package com.tlc.crm.core.list.field.selector;

import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.entity.list.filter.ListQueryComposer;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Selects the additional fields which is needed for the contact list response.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class ContactListQueryComposer implements ListQueryComposer {

    private static final String ALIAS_NAME_FORMAT = "%s_%s";

    private final ContactManager contactManager;

    private static class Instance {
        private static final ContactListQueryComposer INSTANCE = new ContactListQueryComposer();
    }

    public ContactListQueryComposer() {
        contactManager = ContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactListQueryComposer} Instance
     * </p>
     *
     * @return the {@link ContactListQueryComposer}
     */
    public static ContactListQueryComposer getInstance() {
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
        final String fieldName = fieldConfig.getName();
        final Table contactTable = contactManager.getTable();

        switch (fieldConfig.getFieldType().storeType()) {
            case EMAILS ->
                    selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL, getAliasName(fieldName, ContactFields.PRIMARY_EMAIL)));
            case PHONES ->
                    selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE, getAliasName(fieldName, ContactFields.PRIMARY_PHONE)));
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
        final Table contactTable = contactManager.getTable();

        switch (fieldConfig.getFieldType().storeType()) {
            case EMAILS -> {
                final Table table = getAliasTable(Table.get(OMETEMAIL.TABLE), String.join("_", fieldName, StoreType.EMAILS.getName()));

                selectQuery.addSelectClause(table.getColumn(OMETEMAIL.VALUE));
                selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), table.getPKColumn(), JoinClause.JoinType.LEFT));
            }
            case PHONES -> {
                final Table table = getAliasTable(Table.get(OMETPHONE.TABLE), String.join("_", fieldName, StoreType.PHONES.getName()));

                selectQuery.addSelectClause(table.getColumn(OMETPHONE.NUMBER));
                selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), table.getPKColumn(), JoinClause.JoinType.LEFT));
            }
            case OWNER -> {
                final Table table = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldName, StoreType.OWNER.getName()));

                switch (fieldName) {
                    case "salesOwner" -> selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.OWNER_ID), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "createdBy" -> selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.CREATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "updatedBy" -> selectQuery.addJoinClause(new JoinClause(contactTable.getColumn(MCOCONTACT.UPDATED_BY), table.getPKColumn(), JoinClause.JoinType.LEFT));
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
        final String fieldName = fieldConfig.getName();

        return switch (fieldConfig.getFieldType().storeType()) {
            case EMAILS -> {
                final Table primaryEmailAliasTable = getAliasTable(Table.get(OMETEMAIL.TABLE), String.join("_", fieldName, StoreType.EMAILS.getName()));

                yield new OrderByClause(primaryEmailAliasTable.getColumn(OMETEMAIL.VALUE), orderType);
            }
            case PHONES -> {
                final Table primaryPhoneAliasTable = getAliasTable(Table.get(OMETPHONE.TABLE), String.join("_", fieldName, StoreType.PHONES.getName()));

                yield new OrderByClause(primaryPhoneAliasTable.getColumn(OMETPHONE.NUMBER), orderType);
            }
            case OWNER -> {
                final Table ownerAliasTable = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldName, StoreType.OWNER.getName()));

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
        return Table.get(table.getName(), getAliasName(fieldName, table.getAlias()));
    }

    /**
     * <p>
     * Gets the alias name.
     * </p>
     *
     * @param fieldName    Represents the field name.
     * @param subFieldName Represents the sub-field name.
     * @return the alias name.
     */
    private String getAliasName(final String fieldName, final String subFieldName) {
        return String.format(ALIAS_NAME_FORMAT, fieldName, subFieldName);
    }
}
