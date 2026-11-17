package com.tlc.sql.common.api.dml;

import java.util.Collection;
import java.util.List;


/**
 * @author Abishek
 * @version 1.0
 */
public interface MultiQuery extends Query
{
	void addJoinClause(JoinClause join);
	
	void addJoinClause(List<JoinClause> joins);
	
	List<JoinClause> getJoinClause();

	boolean removeJoinClause(JoinClause join);

	/**
	 * <p>
	 * Replaces the old {@link JoinClause} with the new {@link JoinClause}.
	 * </p>
	 *
	 * @param oldJoinClause Represents the old join clause.
	 * @param newJoinClause Represents the new join clause.
	 */
	void replaceJoinClause(final JoinClause oldJoinClause, final JoinClause newJoinClause);
	
	Collection<Table> getTables();
}
