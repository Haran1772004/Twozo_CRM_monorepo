package com.tlc.sql.olap.internal.handler;

import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;

public interface OlapDDLHandler<T extends OlapTableDefinition> extends DDLHandler<OlapColumnDefinition, T> {
}