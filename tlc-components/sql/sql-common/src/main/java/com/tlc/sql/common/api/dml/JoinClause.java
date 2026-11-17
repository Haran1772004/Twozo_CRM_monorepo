package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.List;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class JoinClause
{
	public enum JoinType
	{
		LEFT, RIGHT, INNER;

		/**
		 * <p>
		 * Gets the {@link JoinType} with its name
		 * </p>
		 *
		 * @param name Represents the name of the join type
		 * @return The {@link JoinType}
		 */
		public static JoinType getJoinType(final String name) {
			return switch (name.toUpperCase()) {
				case "INNER" -> INNER;
				case "LEFT" -> LEFT;
				case "RIGHT" -> RIGHT;
                default -> throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_JOIN_TYPE);
            };
		}
	}

	private final Table localTable;
	private final Table remoteTable;

	private final List<Relation> relations;
	private final JoinType joinType;

	private final WhereClause conditions;
	private final Connector initialConnectorOfCondition;

	public JoinClause(Table localTable, String localColumn, Table remoteTable, String remoteColumn)
	{
		this(localTable.getColumn(localColumn), remoteTable.getColumn(remoteColumn), JoinType.INNER);
	}
	public JoinClause(Table localTable, String localColumn, Table remoteTable, String remoteColumn, JoinType joinType)
	{
		this(localTable, localTable.getColumn(localColumn), remoteTable, remoteTable.getColumn(remoteColumn), joinType);
	}

	public JoinClause(Column localColumn, Column remoteColumn)
	{
		this(localColumn, remoteColumn, JoinType.INNER);
	}

	public JoinClause(Column localColumn, Column remoteColumn, JoinType joinType)
	{
		this(localColumn.getTable(), remoteColumn.getTable(), List.of(new Relation(localColumn, remoteColumn)), joinType);
	}

	public JoinClause(Column localColumn, Column remoteColumn, JoinType joinType, WhereClause conditions, Connector initialConnectorOfCondition)
	{
		this(localColumn.getTable(), remoteColumn.getTable(), List.of(new Relation(localColumn, remoteColumn)), joinType, conditions, initialConnectorOfCondition);
	}

	public JoinClause(Table localTable, Column localColumn, Table remoteTable, Column remoteColumn, JoinType joinType)
	{
		this(localTable, remoteTable, List.of(new Relation(localColumn, remoteColumn)), joinType);
	}

	public JoinClause(final Table localTable, final Table remoteTable, final List<Relation> relations, final JoinType joinType) {
		this(localTable, remoteTable, relations, joinType, null, null);
	}

	public JoinClause(final Table localTable, final Table remoteTable, final List<Relation> relations, final JoinType joinType, final WhereClause conditions, final Connector initialConnectorOfCondition)
	{
		this.localTable = Objects.requireNonNull(localTable);
		this.remoteTable = Objects.requireNonNull(remoteTable);
		this.relations = List.copyOf(relations);
		this.joinType = Objects.requireNonNull(joinType);
		this.conditions = conditions;
		this.initialConnectorOfCondition = initialConnectorOfCondition;
	}

	public List<Relation> getRelations()
	{
		return relations;
	}

	public JoinType getJoinType()
	{
		return joinType;
	}

	public Table getLocalTable()
	{
		return localTable;
	}

	public Table getRemoteTable()
	{
		return remoteTable;
	}

	public WhereClause getConditions() {
		return conditions;
	}

	public Connector getInitialConnectorOfCondition() {
		return initialConnectorOfCondition;
	}

	public static class Relation
	{
		private final Column localColumn;
		private final Column remoteColumn;

		public Relation(Column localColumn, Column remoteColumn)
		{
			this.localColumn = Objects.requireNonNull(localColumn);
			this.remoteColumn = Objects.requireNonNull(remoteColumn);
		}

		public Column getLocalColumn()
		{
			return localColumn;
		}

		public Column getRemoteColumn()
		{
			return remoteColumn;
		}
	}
}
