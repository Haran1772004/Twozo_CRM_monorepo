package com.tlc.crm.core.list.field.selector;

import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.entity.list.filter.ListQueryComposer;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * It selects the additional fields which is needed for the activity list response.
 * </p>
 *
 * @author kavinila
 * @version 1.0
 */
public final class ActivityListQueryComposer implements ListQueryComposer {

    private static final String ALIAS_NAME_FORMAT = "%s_%s";

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final SourceActivityManager activityManager;

    private static class Instance {
        private static final ActivityListQueryComposer INSTANCE = new ActivityListQueryComposer();
    }

    public ActivityListQueryComposer() {
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        dealManager = DealManager.getInstance();
        activityManager = SourceActivityManager.getInstance();
    }

    /**
     * Gets the {@link ActivityListQueryComposer} Instance
     *
     * @return the {@link ActivityListQueryComposer}
     */
    public static ActivityListQueryComposer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Selects the additional fields which is needed for the activity list
     * </p>
     *
     * @param fieldConfig Holds the details of the field
     * @param selectQuery The data retrieval query
     */
    @Override
    public void select(final FieldConfig fieldConfig, final SelectQuery selectQuery) {
        final JoinClause.JoinType joinType = JoinClause.JoinType.LEFT;
        final String fieldName = fieldConfig.getName();

        switch (fieldConfig.getFieldType().storeType()) {
            case CONTACTS_ONBOARD -> {
                final Table contactTable = getAliasTable(contactManager.getTable(), fieldName);
                final Table companyTable = getAliasTable(companyManager.getTable(), fieldName);
                final Table emailTable = getAliasTable(EmailManager.getInstance().getTable(), fieldName);
                final Table phoneTable = getAliasTable(PhoneManager.getInstance().getTable(), fieldName);
                final Table contactCompanyTable = getAliasTable(contactManager.getContactCompanyTable(), fieldName);
                final Table contactEmailTable = getAliasTable(contactManager.getContactEmailTable(), fieldName);
                final Table contactPhoneTable = getAliasTable(contactManager.getContactPhoneTable(), fieldName);
                final Column pkColumn = contactTable.getPKColumn();

                selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.FACEBOOK, getAliasName(fieldName, ContactFields.FACEBOOK)));
                selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.LINKEDIN, getAliasName(fieldName, ContactFields.LINKEDIN)));
                selectQuery.addSelectClause(contactTable.getColumn(MCOCONTACT.TWITTER, getAliasName(fieldName, ContactFields.TWITTER)));
                selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME, getAliasName(fieldName, ContactFields.COMPANY)));
                selectQuery.addSelectClause(emailTable.getColumn(OMETEMAIL.VALUE, getAliasName(fieldName, ContactFields.EMAILS)));
                selectQuery.addSelectClause(phoneTable.getColumn(OMETPHONE.NUMBER, getAliasName(fieldName, ContactFields.PHONES)));

                selectQuery.addJoinClause(new JoinClause(pkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), joinType));
                selectQuery.addJoinClause(new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), joinType));
                selectQuery.addJoinClause(new JoinClause(pkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), joinType));
                selectQuery.addJoinClause(new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), joinType));
                selectQuery.addJoinClause(new JoinClause(pkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), joinType));
                selectQuery.addJoinClause(new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), joinType));
            }
            case COMPANY -> {
                final Table companyTable = companyManager.getTable();
                final Table companyAliasTable = getAliasTable(companyTable, fieldName);

                selectQuery.addSelectClause(companyAliasTable.getColumn(MCOCOMPANY.ID, getAliasName(fieldName, CompanyFields.ID)));
                selectQuery.addSelectClause(companyAliasTable.getColumn(MCOCOMPANY.WEBSITE, getAliasName(fieldName, CompanyFields.WEBSITE)));
            }
            case DEAL -> {
                final Table dealTable = getAliasTable(dealManager.getTable(), fieldName);
                final Table monetaryTable = getAliasTable(MonetaryManager.getInstance().getTable(), fieldName);
                final Table pipelineStageTable = getAliasTable(PipelineStageManager.getInstance().getTable(), fieldName);

                selectQuery.addSelectClause(monetaryTable.getColumn(OMETMONETARY.VALUE, getAliasName(fieldName, DealFields.VALUE)));
                selectQuery.addSelectClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME, getAliasName(fieldName, DealFields.PIPELINE_STAGE)));
                selectQuery.addSelectClause(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE, getAliasName(fieldName, DealFields.EXPECTED_CLOSE_DATE)));

                selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn(), joinType));
                selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable.getPKColumn(), joinType));
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
        final Table activityTable = activityManager.getTable();

        switch (fieldConfig.getFieldType().storeType()) {
            case OWNER -> {
                final Table table = getAliasTable(Table.get(OMCRMUSER.TABLE), String.join("_", fieldName, StoreType.OWNER.getName()));

                switch (fieldName) {
                    case "assignee" ->
                            selectQuery.addJoinClause(new JoinClause(activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), table.getPKColumn(), JoinClause.JoinType.LEFT));
                    case "creator" ->
                            selectQuery.addJoinClause(new JoinClause(activityTable.getColumn(OMACACTIVITY.CREATOR_ID), table.getPKColumn(), JoinClause.JoinType.LEFT));
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
     * Gets the alias table.
     *
     * @param table     Represents custom field table.
     * @param fieldName Represents the field name.
     * @return {@link Table}.
     */
    private Table getAliasTable(final Table table, final String fieldName) {
        return Table.get(table.getName(), getAliasName(fieldName, table.getAlias()));
    }

    /**
     * Gets the alias name.
     *
     * @param fieldName    Represents the field name.
     * @param subFieldName Represents the sub-field name.
     * @return the alias name.
     */
    private String getAliasName(final String fieldName, final String subFieldName) {
        return String.format(ALIAS_NAME_FORMAT, fieldName, subFieldName);
    }
}
