package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.models.StoreType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>
 * Represents the operator types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum OperatorType
{
    IS(1, "i18n.crm.analytics.comparator.is", true, false, true, false),
    IS_NOT(2, "i18n.crm.analytics.comparator.is.not", true, false, true, false),
    IS_EMPTY(3, "i18n.crm.analytics.comparator.is.empty", false, false, true, false),
    NOT_EMPTY(4, "i18n.crm.analytics.comparator.is.not.empty", false, false, true, false),
    IS_ANY_OF(5, "i18n.crm.analytics.comparator.is.any.of", true, false, false, false),
    IS_NOT_ANY_OF(6, "i18n.crm.analytics.comparator.is.not.any.of", true, false, false, false),
    LESS_THAN(7, "i18n.crm.analytics.comparator.less.than", true, false, true, false),
    GREATER_THAN(8, "i18n.crm.analytics.comparator.greater.than", true, false, true, false),
    CONTAINS(9, "i18n.crm.analytics.comparator.contains", true, false, true, false),
    DOES_NOT_CONTAINS(10, "i18n.crm.analytics.comparator.does.not.contains", true, false, true, false),
    STARTS_WITH(11, "i18n.crm.analytics.comparator.starts.with", true, false, true, false),
    ENDS_WITH(12, "i18n.crm.analytics.comparator.ends.with", true, false, true, false),
    TOP_N(13, "i18n.crm.analytics.comparator.top.n", true, false, false, false),
    BOTTOM_N(14, "i18n.crm.analytics.comparator.bottom.n", true, false, false, false),
    TOP_N_P(15, "i18n.crm.analytics.comparator.top.n.p", true, false, false, false),
    BOTTOM_N_P(16, "i18n.crm.analytics.comparator.bottom.n.p", true, false, false, false),
    FROM(17, "i18n.crm.analytics.comparator.from", true, false, false, false),
    TO(18, "i18n.crm.analytics.comparator.to", true, false, false, false),
    BETWEEN(19, "i18n.crm.analytics.comparator.between", true, false, true, true),
    NOT_BETWEEN(20, "i18n.crm.analytics.comparator.not.between", true, false, false, false),
    ABOVE(21, "i18n.crm.analytics.comparator.above", true, false, false, false),
    BELOW(22, "i18n.crm.analytics.comparator.below", true, false, false, false),
    IS_BEFORE(23, "i18n.crm.analytics.comparator.is.before", true, false, false, false),
    IS_AFTER(24, "i18n.crm.analytics.comparator.is.after", true, false, false, false),
    EQUAL(25, "i18n.crm.analytics.comparator.equal", true, false, true, false),
    GREATER_THAN_OR_EQUAL_TO(26, "i18n.crm.analytics.comparator.greater.than.or.equal.to", true, false, true, false),
    LESS_THAN_OR_EQUAL_TO(27, "i18n.crm.analytics.comparator.less.than.or.equal.to", true, false, true, false),
    NOT_EQUAL_TO(28, "i18n.crm.analytics.comparator.not.equal.to", true, false, true, false),
    IS_ANYTHING(29, "i18n.crm.analytics.comparator.is.anything", true, false, true, false),
    INCLUDES(30, "i18n.crm.analytics.comparator.includes", true, false, true, false),
    DOES_NOT_INCLUDE(31, "i18n.crm.analytics.comparator.does.not.include", true, false, true, false),
    ON(32, "i18n.crm.analytics.comparator.on", false, true, true, true),
    DAY_IS(33, "i18n.crm.analytics.comparator.day.is", false, true, true, false),
    AFTER(34, "i18n.crm.analytics.comparator.after", false, true, true, false),
    BEFORE(35, "i18n.crm.analytics.comparator.before", false, true, true, false),
    SINCE(36, "i18n.crm.analytics.comparator.since", false, true, true, true),
    IN_THE_LAST(37, "i18n.crm.analytics.comparator.in.the.last", true, true, true, true),
    IN_THE_NEXT(38, "i18n.crm.analytics.comparator.in.the.next", true, false, false, false),
    NOT_IN_THE_LAST(39, "i18n.crm.analytics.comparator.not.in.the.last", true, true, true, false),
    NOT_IN_THE_NEXT(40, "i18n.crm.analytics.comparator.not.in.the.next", true, false, false, false),
    BETWEEN_NUMBER(41, "i18n.crm.analytics.comparator.between.number", true, false, true, false);

    private final int typeCode;
    private final String i18nKeyOfDisplayName;
    private final boolean hasValue;
    private final boolean hasFunction;
    private final boolean isEnabled;
    private final boolean isPeriodicFilterEnabled;

    /**
     * Retrieves the list of supported Operators for a given StoreType ID.
     *
     * @param storeType Represents the {@link StoreType}
     * @return A Set of supported OperatorTypes.
     */
    public static Set<OperatorType> getSupportedOperators(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_STRING, TEXT, PHONE, EMAIL, ADDRESS, PHONES, EMAILS, IDENTIFIER ->
                    EnumSet.of(IS, IS_NOT, INCLUDES, DOES_NOT_INCLUDE, CONTAINS, DOES_NOT_CONTAINS, STARTS_WITH, ENDS_WITH, IS_EMPTY, NOT_EMPTY);
            case SINGLE_NUMBER, MONETARY, PRICE_TAGS, DEAL_VALUE, PRODUCT ->
                    EnumSet.of(EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN_OR_EQUAL_TO, NOT_EQUAL_TO, BETWEEN_NUMBER);
            case DROPDOWN, MULTI_SELECT, OWNER, COMPANY, CONTACT, CONTACT_ONBOARD, COMPANY_ONBOARD, CONTACTS_ONBOARD,
                 DEAL, TERRITORY, TAGS, OWNERS -> EnumSet.of(IS, IS_NOT, IS_EMPTY, NOT_EMPTY);
            case TIME -> EnumSet.of(ON, DAY_IS, AFTER, BEFORE, SINCE);
            case DATE -> EnumSet.of(ON, DAY_IS, AFTER, BEFORE, SINCE, IS_EMPTY, NOT_EMPTY, BETWEEN);
            case DATE_TIME -> EnumSet.of(ON, DAY_IS, AFTER, BEFORE, SINCE, IN_THE_LAST, NOT_IN_THE_LAST, IS_EMPTY, NOT_EMPTY, BETWEEN);
            default -> Collections.emptySet();
        };
    }


    /**
     * Retrieves the {@link OperatorType} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link OperatorType} enum associated with the provided code.
     * @throws IllegalArgumentException if the provided code does not match any known operation type.
     */
    public static OperatorType getOperatorType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> IS;
            case 2 -> IS_NOT;
            case 3 -> IS_EMPTY;
            case 4 -> NOT_EMPTY;
            case 5 -> IS_ANY_OF;
            case 6 -> IS_NOT_ANY_OF;
            case 7 -> LESS_THAN;
            case 8 -> GREATER_THAN;
            case 9 -> CONTAINS;
            case 10 -> DOES_NOT_CONTAINS;
            case 11 -> STARTS_WITH;
            case 12 -> ENDS_WITH;
            case 13 -> TOP_N;
            case 14 -> BOTTOM_N;
            case 15 -> TOP_N_P;
            case 16 -> BOTTOM_N_P;
            case 17 -> FROM;
            case 18 -> TO;
            case 19 -> BETWEEN;
            case 20 -> NOT_BETWEEN;
            case 21 -> ABOVE;
            case 22 -> BELOW;
            case 23 -> IS_BEFORE;
            case 24 -> IS_AFTER;
            case 25 -> EQUAL;
            case 26 -> GREATER_THAN_OR_EQUAL_TO;
            case 27 -> LESS_THAN_OR_EQUAL_TO;
            case 28 -> NOT_EQUAL_TO;
            case 29 -> IS_ANYTHING;
            case 30 -> INCLUDES;
            case 31 -> DOES_NOT_INCLUDE;
            case 32 -> ON;
            case 33 -> DAY_IS;
            case 34 -> AFTER;
            case 35 -> BEFORE;
            case 36 -> SINCE;
            case 37 -> IN_THE_LAST;
            case 38 -> IN_THE_NEXT;
            case 39 -> NOT_IN_THE_LAST;
            case 40 -> NOT_IN_THE_NEXT;
            case 41 -> BETWEEN_NUMBER;
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_OPERATOR_TYPE, "i18n.analytics.invalid.operator.type" );
         };
    }
}
