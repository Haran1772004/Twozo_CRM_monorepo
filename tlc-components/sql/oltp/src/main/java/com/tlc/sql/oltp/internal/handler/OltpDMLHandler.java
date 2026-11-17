package com.tlc.sql.oltp.internal.handler;

import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;

public interface OltpDMLHandler extends DMLHandler {

    String getNextValueSequenceSQL(String sequenceName);

    String getNextValueLimitSequenceSQL(String sequenceName);
}