package com.tlc.sql.common.internal.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Abishek
 * @version 1.0
 */
public class SelectQueryImpl extends MultiQueryImpl implements SelectQuery
{

	private static final Integer MAX_ALLOWED_JOINS = 4;

	private LimitClause range = null;
	private HavingClause havingClause = null;

	private final Map<Table, Column> pkSelectClause;
	private final List<OrderByClause> sortColumns;
	private final List<GroupByClause> groupByColumns;

	private final SortedSet<SelectClause> selectClauses;
	private final Map<Table, Collection<Column>> selectedTables;

	protected static final Logger LOGGER = Logger.getLogger(SelectQueryImpl.class.getName());

	public SelectQueryImpl(Table baseTable)
	{
		super(baseTable);
		this.pkSelectClause = new HashMap<>();
		this.sortColumns = new ArrayList<>();
		this.groupByColumns = new ArrayList<>();

		this.selectClauses = new TreeSet<>();
		this.selectedTables = new TreeMap<>();
	}

	@Override
	public void addSelectClause(Collection<Column> columns)
	{
		columns.forEach(this::addColumn);
	}

	@Override
	public void addSelectClause(Column column)
	{
		addColumn(column);
	}

	@Override
	public void addSelectClause(Table ... tables)
	{
		if(tables != null)
		{
			for(Table table : tables)
			{
				addSelectClause(table);
			}
		}
	}

	/**
	 * Returns all {@link SelectClause} entries currently registered in the SELECT clause,
	 * ordered by their natural sort order.
	 *
	 * @return a {@link SortedSet} of {@link SelectClause} instances; never {@code null}, but may be empty
	 */
	@Override
	public SortedSet<SelectClause> getSelectClauses() {
		return selectClauses;
	}

	@Override
	public void addSelectClause(Table table)
	{
		final TableDefinition<?> tableDef = table.getTableDefinition();
		tableDef.getColumns().stream().map(table::getColumn).forEach(this::addColumn);
	}

	private void addColumn(Column column)
	{
		final Table table = column.getTable();
		if(column.isPkColumn())
		{
			pkSelectClause.put(table, column);
		}
		selectedTables.computeIfAbsent(table, selectedTable -> new HashSet<>()).add(column);
		selectClauses.add(new SelectClause(column));
	}

	@Override
	public void removeSelectClause(Column column)
	{
		final Table table = column.getTable();

		selectClauses.remove(new SelectClause(column));

		if (selectedTables.containsKey(table)) {
			final Collection<Column> columns = selectedTables.get(table);

			columns.remove(column);

			if (columns.isEmpty() && !getTables().contains(table)) {
				selectedTables.remove(table);
			}
		}
	}

	@Override
	public SortedSet<Column> getSelectClause()
	{
		return Collections.unmodifiableSortedSet(selectClauses.stream()
				.map(SelectClause::getColumn)
				.filter(Objects::nonNull)
				.collect(TreeSet::new, TreeSet::add, TreeSet::addAll));
	}

	@Override
	public void addOrderByClause(OrderByClause column)
	{
		sortColumns.add(column);		
	}

	@Override
	public void addOrderByClause(List<OrderByClause> columns)
	{
		if (columns != null && columns.size() > 0)
		{
			sortColumns.addAll(columns);		
		}
	}

	@Override
	public boolean removeOrderByClause(OrderByClause column)
	{
		return sortColumns.remove(column);
	}

	@Override
	public List<OrderByClause> getOrderByClause()
	{
		return Collections.unmodifiableList(sortColumns);
	}

	@Override
	public void addGroupByClause(GroupByClause column)
	{
		groupByColumns.add(column);
	}

	@Override
	public void addGroupByClause(List<GroupByClause> columns)
	{
		if (columns != null && columns.size() > 0)
		{
			groupByColumns.addAll(columns);
		}
	}

	@Override
	public boolean removeGroupByClause(GroupByClause column)
	{
		return groupByColumns.remove(column);
	}

	@Override
	public List<GroupByClause> getGroupByClause()
	{
		return Collections.unmodifiableList(groupByColumns);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param havingClause the {@link HavingClause} to add to the query
	 */
	@Override
	public void addHavingClause(final HavingClause havingClause) {
		this.havingClause = havingClause;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the {@link HavingClause} of the query
	 */
	@Override
	public HavingClause getHavingClause() {
		return havingClause;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeHavingClause(final HavingClause havingClause) {
		this.havingClause = null;
	}

	@Override
	public void setLimitClause(LimitClause limit)
	{
		this.range = limit;
	}

	@Override
	public LimitClause getLimitClause()
	{
		return range;
	}

	@Override
	public Map<Table, Column> getSelectPKInfo()
	{
		return Collections.unmodifiableMap(pkSelectClause);
	}

	@Override
	public void addPkIndexAndSelectClause()
	{
		if(selectClauses.isEmpty())
		{
			final Collection<Table> allTables = getTables();
			for(Table table : allTables)
			{
				addSelectClause(table);
				final Column pkColumn = table.getPKColumn();
				pkSelectClause.put(table, pkColumn);
			}
		}
		else
		{
			for (Table table : selectedTables.keySet())
			{
				if(!pkSelectClause.containsKey(table))
				{
					final Column pkColumn = table.getPKColumn();

					selectClauses.add(new SelectClause(pkColumn));
					pkSelectClause.put(table, pkColumn);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return True if max joins reached, false otherwise
	 */
	@Override
	public boolean hasReachedMaxJoins() {
		return MAX_ALLOWED_JOINS <= getJoinClause().size();
	}

	/**
	 * Adds a static numeric value to the SELECT clause of the query.
	 * <p>
	 * Static values are literal values that appear in the SELECT list without being bound to any table column.
	 * Multiple static numeric values can be added to the same query.
	 * </p>
	 *
	 * @param staticValue the numeric value to add to the SELECT clause (e.g., 1, 100, 3.14)
	 */
	@Override
	public void addStaticSelectClause(final Number staticValue) {
		selectClauses.add(new SelectClause(staticValue));
	}

	/**
	 * Adds a static string value to the SELECT clause of the query.
	 * <p>
	 * Static values are literal values that appear in the SELECT list without being bound to any table column.
	 * Multiple static string values can be added to the same query.
	 * </p>
	 *
	 * @param staticValue the string value to add to the SELECT clause (e.g., "status", "ACTIVE")
	 */
	@Override
	public void addStaticSelectClause(final String staticValue) {
		selectClauses.add(new SelectClause(staticValue));
	}

	/**
	 * Retrieves all static values that have been added to the SELECT clause.
	 * <p>
	 * Static values are returned in the order they were added. This includes both numeric and string values
	 * that were added via {@link #addStaticSelectClause(Number)} and {@link #addStaticSelectClause(String)} methods.
	 * </p>
	 *
	 * @return a {@link List} of static values in the SELECT clause, or an empty list if no static values were added
	 */
	@Override
	public List<Object> getStaticSelectClause() {
		return selectClauses.stream().map(SelectClause::getStaticValue).filter(Objects::nonNull).toList();
	}

	/**
	 * Updates the base table of the query to a derived table (CTE).
	 * <p>
	 * This method checks if the provided derived table has the same name and alias as the current base table.
	 * If they match, the base table is updated to the derived table. Otherwise, an error is thrown.
	 * </p>
	 *
	 * @param derivedTable the derived table to set as the new base table
	 * @throws com.tlc.commons.code.ErrorCode if the derived table's name or alias does not match the current base table
	 */
	@Override
	public void updateBaseTableAsDerivedTable(final DerivedTable derivedTable) {
		final Table baseTable = getBaseTable();

		if (Objects.equals(baseTable.getName(), derivedTable.getName()) && Objects.equals(baseTable.getAlias(), derivedTable.getAlias())) {
			setBaseTable(derivedTable);
		} else {
			throw ErrorCode.get(SQLErrorCodes.INVALID_DERIVED_BASE_TABLE);
		}
	}
}
