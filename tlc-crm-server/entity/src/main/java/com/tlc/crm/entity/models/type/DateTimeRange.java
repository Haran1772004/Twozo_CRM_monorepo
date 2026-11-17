package com.tlc.crm.entity.models.type;

import com.tlc.crm.fields.models.StoreType;

/**
 * <p>
 * A model that represents a date and time range data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class DateTimeRange extends Range {

    public DateTimeRange() {
        super(StoreType.DATE_TIME_RANGE);
    }

}
