package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.Counter;
import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.StaticOption;
import com.tlc.crm.entity.sql.resource.OMETOPTION;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Provides the {@link Option} related DB fetch action.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OptionManager extends AbstractReadableConfigManager<Option> {

    private final OrderManager orderManager;

    public OptionManager() {
        orderManager = OrderManager.getInstance();
    }

    private static class Instance {
        private static final OptionManager INSTANCE = new OptionManager();
    }

    /**
     * <p>
     * Gets the {@link OptionManager} instance
     * </p>
     *
     * @return The  {@link OptionManager}
     */
    public static OptionManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param row Row that holds the information of model
     * @return The {@link Option}
     */
    @Override
    public Option convertRowToModel(final Row row) {
        final boolean isEditable = row.get(OMETOPTION.IS_EDITABLE);
        final Option option = isEditable ? new Option() : new StaticOption();

        option.setOrgId(row.getOrgId());
        option.setId(row.getPKValue());
        option.setOption(row.get(OMETOPTION.OPTION));
        option.setFieldId(row.get(OMETOPTION.FIELD_ID));
        option.setOrder(row.get(OMETOPTION.ORDER));
        option.setDefault(row.get(OMETOPTION.IS_DEFAULT));
        option.setEditable(isEditable);

        return option;
    }

    /**
     * {@inheritDoc}
     *
     * @return The option table
     */
    @Override
    public Table getTable() {
        return Table.get(OMETOPTION.TABLE);
    }

    /**
     * <p>
     * Creates a collection of {@link Option} for the given options
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param options Which contains a collection of {@link Option}
     */
    public void create(final long orgId, final Collection<Option> options) {
        final DataContainer dataContainer = DataContainer.create();
        final Table optionTable = getTable();

        for (final Option option : options) {
            final Row optionRow = new Row(optionTable);

            optionRow.setOrgId(orgId);
            optionRow.set(OMETOPTION.FIELD_ID, option.getFieldId());
            optionRow.set(OMETOPTION.OPTION, option.getOption());
            optionRow.set(OMETOPTION.ORDER, option.getOrder());
            optionRow.set(OMETOPTION.IS_EDITABLE, option.isEditable());
            optionRow.set(OMETOPTION.IS_DEFAULT, option.isDefault());

            dataContainer.addNewRow(optionRow);
        }
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Creates the option for the given {@link Option}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param option Represents the {@link Option}
     */
    public void create(final long orgId, final Option option) {
        create(orgId, List.of(option));
    }

    /**
     * <p>
     * Updates the order of activity type for the specified organization
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param optionIds Represents the collection of option ids
     */
    public void updateOptionsOrder(final Long orgId, final Collection<Long> optionIds) {
        orderManager.update(orgId, new Counter(), optionIds, getTable());
    }

    /**
     * <p>
     * Fetches a collection of {@link Option} for the given organization id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @return The collection of {@link Option}
     */
    public Collection<Option> getOptions(final long orgId, final long fieldId) {
        final Table optionTable = getTable();

        return get(new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.ORG_ID), orgId))
                .and(Criteria.eq(optionTable.getColumn(OMETOPTION.FIELD_ID), fieldId)));
    }

    /**
     * <p>
     * Fetches a collection of {@link Option} ids that are exists in the given option ids for the given organization id and field id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @return The collection of {@link Option} ids
     */
    public Collection<Long> fetchExistingOptions(final long orgId, final long fieldId, final Collection<Long> optionIds) {
        final Table optionTable = getTable();
        final Collection<Option> options = get(new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.ORG_ID), orgId))
                .and(Criteria.eq(optionTable.getColumn(OMETOPTION.FIELD_ID), fieldId)).and(Criteria.in(optionTable.getColumn(OMETOPTION.ID), optionIds)));

        return options.stream().map(Option::id).toList();
    }
}
