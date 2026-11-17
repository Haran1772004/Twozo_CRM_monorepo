package com.tlc.crm.core.features.action.filter;

import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;

import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.MinMaxValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.sql.resource.MACNOTEACTIVITY;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides the action which can be perform note related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.NOTE)
public class NoteFilterAction extends FilterAction {

    private final NoteManager noteManager;

    public NoteFilterAction() {
        noteManager = NoteManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information.
     * @param filterCriteria         holds the details of criteria.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    @Override
    public WhereClause buildWhereClause(final FieldInfo fieldInfo, final FilterCriteria filterCriteria,
                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null :
                criteriaValues.stream().findAny().get();
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final FilterOptions filterOptions = FilterOptions.get(filterCriteria.getCriteriaField().getAliasName());
        final Operator operator = Operator.getOperator(filterCriteria.getComparator().getAliasName());
        Column column = null;

        if (Objects.isNull(filterOptions)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }
        final WhereClause filter = switch (filterOptions) {
            case RECENT_NOTE -> {
                final Table noteTable = noteManager.getTable();
                column = noteTable.getColumn(OMNTNOTE.CREATED_TIME);
                final MinMaxValueCriteriaAppenderRequisites maxValueCriteriaRequisites = new MinMaxValueCriteriaAppenderRequisites.Builder()
                        .setFieldInfo(fieldInfo)
                        .setRelatedSourceType(FilterSourceType.NOTE)
                        .build(column);

                setRequisitesIntoMaxValueCriteriaAppender(maxValueCriteriaRequisites, criteriaAppenderHolder);
                yield new WhereClause(new Criteria(noteTable.getColumn(OMNTNOTE.CONTENT), value, operator));
            }
            default -> super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        };
        final NullableFieldCriteriaAppenderRequisites criteriaRequisites = new NullableFieldCriteriaAppenderRequisites.Builder().build(column);

        setRequisitesIntoNullableFieldCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
        return filter;
    }

    /**
     * {@inheritDoc}
     *
     * @return Array of {@link Table} holds the mapping tables of current source.
     */
    @Override
    protected Table[] getLocalMapperTables() {
        return new Table[]{
                Table.get(OMNTCOMPANYNOTE.TABLE), Table.get(OMNTCONTACTNOTE.TABLE), Table.get(OMNTDEALNOTE.TABLE), Table.get(MACNOTEACTIVITY.TABLE)
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return tag {@link Table}.
     */
    @Override
    public Table getTable() {
        return noteManager.getTable();
    }
}
