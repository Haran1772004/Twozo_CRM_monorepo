package com.tlc.sql.common.api.dml;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * <p>
 * Holds the sql function related details
 * </p>
 *
 * @author karthick
 * @version 1.0
 */
@Setter
@Getter
public abstract class SQLFunction {
    private String name;
    private Collection<Object> parameters;

    public enum ReturnType {
        INT,
        TEXT
    }

    public abstract ReturnType getReturnType();
}
