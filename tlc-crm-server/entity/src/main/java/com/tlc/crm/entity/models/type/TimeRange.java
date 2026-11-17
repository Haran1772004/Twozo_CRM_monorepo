package com.tlc.crm.entity.models.type;

import com.tlc.crm.fields.models.StoreType;

/**
 * <p>
 * A model that represents time range data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class TimeRange extends Range {

    public TimeRange() {
        super(StoreType.TIME_RANGE);
    }

}
