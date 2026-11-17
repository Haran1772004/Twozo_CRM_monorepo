package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

/**
 * @author Abishek
 * @version 1.0
 */

public enum Operator
{
    /*
     *  Common Conditions
     */
    EQUAL(1), NOT_EQUAL(2, true), IN(3), NOT_IN(4, true),

    /*
	 *  Number Related Conditions
	 */
	GREATER_EQUAL(101), LESS_EQUAL(102), LESS_THAN(103), GREATER_THAN(104), BETWEEN(105), NOT_BETWEEN(106, true), BIT_AND(107), BIT_OR(108), ADD(109), SUBTRACT(110), MULTIPLY(111), DIVIDE(112), MODULUS(113), INCREMENT(114), DECREMENT(115),
	/*
	 *  String Related Conditions
	 */
	STARTS_WITH(201), NOT_STARTS_WITH(202, true), ENDS_WITH(203), NOT_ENDS_WITH(204, true), CONTAINS(205), NOT_CONTAINS(206, true), LIKE(207), REGEX(208),

	/*
		Special Conditions
	 */
	EXISTS(301),

	/*
		Array Conditions
	 */
	HAS_ANY(401), NOT_HAS_ANY(402, true),

	;
	private final int id;
	private final boolean isInvertedOperator;

	Operator(final int id) {
		this(id, false);
	}

	Operator(final int id, final boolean isInvertedOperator) {
		this.id = id;
		this.isInvertedOperator = isInvertedOperator;
	}

	public int getId()
	{
		return id;
	}

	public boolean isInvertedOperator() {
		return isInvertedOperator;
	}

	/**
	 * <p>
	 * Checks whether the given operator is related to pattern matcher or not.
	 * </p>
	 *
	 * @param operator Represents the operator.
	 * @return Pattern matching operator or not.
	 */
	public static boolean isPatternMatchable(final Operator operator) {
		return switch (operator) {
			case LIKE, CONTAINS, NOT_CONTAINS, STARTS_WITH, NOT_STARTS_WITH, ENDS_WITH, NOT_ENDS_WITH, REGEX -> true;
			default -> false;
		};
	}

	public static Operator getOperator(String name)
	{
		return switch (name) {
			case "equal" -> EQUAL;
			case "notEqual" -> NOT_EQUAL;
			case "in" -> IN;
			case "notIn" -> NOT_IN;
			case "gte" -> GREATER_EQUAL;
			case "lte" -> LESS_EQUAL;
			case "gt" -> GREATER_THAN;
			case "lt" -> LESS_THAN;
			case "startsWith" -> STARTS_WITH;
			case "notStartsWith" -> NOT_STARTS_WITH;
			case "contains" -> CONTAINS;
			case "notContains" -> NOT_CONTAINS;
			case "endsWith" -> ENDS_WITH;
			case "notEndsWith" -> NOT_ENDS_WITH;
			case "add" -> ADD;
			case "subtract" -> SUBTRACT;
			case "multiply" -> MULTIPLY;
			case "divide" -> DIVIDE;
			case "modulus" -> MODULUS;
			case "increment" -> INCREMENT;
			case "decrement" -> DECREMENT;
			default -> throw ErrorCode.get(ErrorCodes.UNKNOWN_INPUT, name);
		};
	}

	/**
	 * <p>
	 * Gets the inverted {@link Operator} for the given operator.
	 * </p>
	 *
	 * @param operator Represents the {@link Operator}.
	 * @return Inverted {@link Operator}.
	 */
	public static Operator getInvertedOperator(final Operator operator) {
		return switch (operator) {
			case EQUAL -> Operator.NOT_EQUAL;
			case NOT_EQUAL -> Operator.EQUAL;
			case IN -> Operator.NOT_IN;
			case NOT_IN -> Operator.IN;
			case BETWEEN -> Operator.NOT_BETWEEN;
			case NOT_BETWEEN -> Operator.BETWEEN;
			case STARTS_WITH -> Operator.NOT_STARTS_WITH;
			case NOT_STARTS_WITH -> Operator.STARTS_WITH;
			case ENDS_WITH -> Operator.NOT_ENDS_WITH;
			case NOT_ENDS_WITH -> Operator.ENDS_WITH;
			case CONTAINS -> Operator.NOT_CONTAINS;
			case NOT_CONTAINS -> Operator.CONTAINS;
			case HAS_ANY -> Operator.NOT_HAS_ANY;
			case NOT_HAS_ANY -> Operator.HAS_ANY;
			case ADD -> Operator.SUBTRACT;
			case SUBTRACT -> Operator.ADD;
			case MULTIPLY -> Operator.DIVIDE;
			case DIVIDE -> Operator.MULTIPLY;
			case INCREMENT -> Operator.DECREMENT;
			case DECREMENT -> Operator.INCREMENT;
			default -> operator;
		};
	}

	/**
	 * <p>
	 * Checks whether the given operator is a relational operator.
	 * </p>
	 *
	 * @return A flag indicating whether the operator is a relational operator.
	 */
	public boolean isRelationalOperator() {
		return switch (this) {
			case GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL, EQUAL, NOT_EQUAL -> true;
			default -> false;
		};
	}

	/**
	 * <p>
	 * Checks whether the given operator is an equality operator.
	 * </p>
	 *
	 * @return A flag indicating whether the operator is an equality operator.
	 */
	public boolean isEqualityOperator() {
		return switch (this) {
			case EQUAL, NOT_EQUAL -> true;
			default -> false;
		};
	}

	/**
	 * <p>
	 * Checks whether the given operator is a multivalue operator.
	 * </p>
	 *
	 * @return A flag indicating whether the operator is a multivalue operator.
	 */
	public boolean isMultiValueOperator() {
		return switch (this) {
			case IN, NOT_IN -> true;
			default -> false;
		};
	}

	/**
	 * <p>
	 * Converts a single-value operator to its equivalent multi-value operator.
	 * </p>
	 *
	 * @return The multi-value equivalent of the current operator, or the operator itself if no multi-value equivalent exists.
	 */
    public Operator convertToMultiValueOperator() {
        return switch (this) {
			case EQUAL -> IN;
			case NOT_EQUAL -> NOT_IN;
			default -> this;
		};
    }

    /**
     * Returns the logical inverse of this operator.
     *
     * @return the inverted {@link Operator}, or the operator itself if no inverse is defined.
     */
    public Operator invertOperator() {
        return switch (this) {
			case LESS_THAN -> GREATER_THAN;
			case LESS_EQUAL -> GREATER_EQUAL;
			case GREATER_THAN -> LESS_THAN;
			case GREATER_EQUAL -> LESS_EQUAL;
			case EQUAL -> NOT_EQUAL;
			case NOT_EQUAL -> EQUAL;
			case IN -> NOT_IN;
			case NOT_IN -> IN;
			case BETWEEN -> NOT_BETWEEN;
			case NOT_BETWEEN -> BETWEEN;
			case STARTS_WITH -> NOT_STARTS_WITH;
			case NOT_STARTS_WITH -> STARTS_WITH;
			case ENDS_WITH -> NOT_ENDS_WITH;
			case NOT_ENDS_WITH -> ENDS_WITH;
			case CONTAINS -> NOT_CONTAINS;
			case NOT_CONTAINS -> CONTAINS;
			default -> this;
		};
    }

	public boolean isUnary() {
		return this == INCREMENT || this == DECREMENT;
	}

	public boolean isBinary() {
		return this == EQUAL || this == NOT_EQUAL || this == GREATER_EQUAL || this == LESS_EQUAL || this == LESS_THAN
				|| this == GREATER_THAN || this == BIT_AND || this == BIT_OR || this == ADD || this == SUBTRACT
				|| this == MULTIPLY || this == DIVIDE || this == MODULUS;
	}

	@Override
	public String toString() {
		return switch (this) {
			case EQUAL -> "=";
			case NOT_EQUAL -> "!=";
			case GREATER_EQUAL -> ">=";
			case LESS_EQUAL -> "<=";
			case LESS_THAN -> "<";
			case GREATER_THAN -> ">";
			case BIT_AND -> "&";
			case BIT_OR -> "|";
			case ADD -> "+";
			case SUBTRACT -> "-";
			case MULTIPLY -> "*";
			case DIVIDE -> "/";
			case MODULUS -> "%";
			case INCREMENT -> "++";
			case DECREMENT -> "--";
			default -> null;
		};
	}
}

