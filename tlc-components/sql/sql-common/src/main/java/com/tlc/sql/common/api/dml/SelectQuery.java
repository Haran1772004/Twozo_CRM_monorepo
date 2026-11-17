package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.internal.dml.SelectQueryImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;


/**
 * @author Abishek
 * @version 1.0
 */
public interface SelectQuery extends MultiQuery
{
	static SelectQuery get(Table table)
	{
		return new SelectQueryImpl(table);
	}

	void setLimitClause(LimitClause limit);

    LimitClause getLimitClause();

	void addSelectClause(Column column);

	void addSelectClause(Collection<Column> column);

	void addSelectClause(Table table);

	void addSelectClause(Table... table);

	/**
	 * Returns all {@link SelectClause} entries currently registered in the SELECT clause,
	 * ordered by their natural sort order.
	 *
	 * @return a {@link SortedSet} of {@link SelectClause} instances; never {@code null}, but may be empty
	 */
	SortedSet<SelectClause> getSelectClauses();

	void removeSelectClause(Column column);

	SortedSet<Column> getSelectClause();

	Map<Table, Column> getSelectPKInfo();

	void addPkIndexAndSelectClause();

	void addOrderByClause(OrderByClause column);

	void addOrderByClause(List<OrderByClause> columns);

	boolean removeOrderByClause(OrderByClause column);

	List<OrderByClause> getOrderByClause();

	void addGroupByClause(GroupByClause column);

	void addGroupByClause(List<GroupByClause> columns);

	boolean removeGroupByClause(GroupByClause column);

	List<GroupByClause> getGroupByClause();

    /**
     * Adds a {@link HavingClause} to the query.
     *
     * @param havingClause the {@link HavingClause} to add to the query
     */
    void addHavingClause(final HavingClause havingClause);

    /**
     * Fetches the {@link HavingClause} of the query.
     */
    HavingClause getHavingClause();

	/**
	 * Removes the {@link HavingClause} from the query.
	 *
	 * @param havingClause the {@link HavingClause} to be removed from the query
	 */
	void removeHavingClause(final HavingClause havingClause);

	/**
	 * <p>
	 * Checks whether the max allowed joins limit reached
	 * </p>
	 *
	 * @return True if max joins reached, false otherwise
	 */
	boolean hasReachedMaxJoins();

	/**
	 * Adds a static numeric value to the SELECT clause of the query.
	 * <p>
	 * Static values are literal values that appear in the SELECT list without being bound to any table column.
	 * Multiple static numeric values can be added to the same query.
	 * </p>
	 *
	 * @param staticValue the numeric value to add to the SELECT clause (e.g., 1, 100, 3.14)
	 */
	void addStaticSelectClause(final Number staticValue);

	/**
	 * Adds a static string value to the SELECT clause of the query.
	 * <p>
	 * Static values are literal values that appear in the SELECT list without being bound to any table column.
	 * Multiple static string values can be added to the same query.
	 * </p>
	 *
	 * @param staticValue the string value to add to the SELECT clause (e.g., "status", "ACTIVE")
	 */
	void addStaticSelectClause(final String staticValue);

	/**
	 * Retrieves all static values that have been added to the SELECT clause.
	 * <p>
	 * Static values are returned in the order they were added. This includes both numeric and string values
	 * that were added via {@link #addStaticSelectClause(Number)} and {@link #addStaticSelectClause(String)} methods.
	 * </p>
	 *
	 * @return a {@link List} of static values in the SELECT clause, or an empty list if no static values were added
	 */
	List<Object> getStaticSelectClause();

    /**
     * Replace the current base table of this SelectQuery with the supplied derived table. This is used by secure query
     * builders when the base table must be represented as a derived selection (for example a permission-filtered subquery or CTE).
     * Implementations should update the query's internal table list, aliases and select clauses as necessary
     * so that subsequent SQL generation uses the derived table.
     *
     * @param derivedTable the DerivedTable that should become the new base table
     */
    void updateBaseTableAsDerivedTable(final DerivedTable derivedTable);
}
