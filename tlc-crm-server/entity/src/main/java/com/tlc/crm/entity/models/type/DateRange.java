package com.tlc.crm.entity.models.type;

import com.tlc.crm.fields.models.StoreType;

/**
 * <p>
 * A model that represents a date range data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class DateRange extends Range {

    public DateRange() {
        super(StoreType.DATE_RANGE);
    }
}
