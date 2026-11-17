package com.tlc.analytics.config.keys.view.filter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to filter fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class FilterFieldKeys
{
    public static final String FILTER_TYPE    = "filterType";
    public static final String OPERATOR_TYPE  = "operatorType";
    public static final String VALUE          = "value";
    public static final String VIEW_COLUMN_ID = "viewColumnId";
    public static final String FROM           = "from";
    public static final String TO             = "to";
    public static final String FORMAT         = "format";
    public static final String CREATED_AT     = "createdAt";
    public static final String MODIFIED_AT    = "modifiedAt";
    public static final String IS_DEFAULT = "isDefault";
    public static final String PERIODIC_FILTERS = "periodicFilters";

    public static final String FILTERS = "filters";
    public static final String COMPARATORS = "comparators";
    public static final String DROPDOWN = "dropdown";
    public static final String FIELD_TYPE = "fieldType";
    public static final String VALUES = "values";
    public static final String FUNCTIONS = "functions";
    public static final String USER_LIST = "userList";
    public static final String CONTACT_LIST = "contactList";
    public static final String COMPANY_LIST = "companyList";
    public static final String FIELD_VALUE = "fieldValue";
    public static final String FUNCTION_ID = "functionId";
    public static final String DAYS = "DAYS";
    public static final String COMPARATOR_ID = "comparatorId";
    public static final String FIELD_ID = "fieldId";
    public static final String DEFAULT_PERIODIC_FILTER_VALUE = "defaultPeriodicFilterValue";
    public static final String FUNCTION_IDS = "functionIds";
    public static final String TYPE = "type";
    public static final String COMPARATOR = "comparator";
    public static final String CONFIG = "config";
    public static final String ANA_ACTUAL_COLUMN_ID = "anaActualColumnId";
    public static final String IS_CUSTOM_FILTER = "isCustomFilter";
    public static final String HAVING_OPERATION_TYPE="havingOperationType";
    public static final String MAX_DATE = "maxDate";
}
