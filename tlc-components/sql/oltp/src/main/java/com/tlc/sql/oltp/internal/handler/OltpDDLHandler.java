package com.tlc.sql.oltp.internal.handler;

import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;

public interface OltpDDLHandler extends DDLHandler<OltpColumnDefinition, OltpTableDefinition> {

    String getInitializeSequenceSQL(String sequenceName, int start, int incrementBy);

    String getCreateSequenceSQL(String sequenceName, int start, int incrementBy);

    String getUpdateSequenceSQL(String sequenceName, int incrementBy);

    String getDropSequenceSQL(String sequenceName);

    String getAddForeignKeySQL(OltpTableDefinition tableDefinition, FKDefinition fkDefinition);

    String getAddIndexSQL(OltpTableDefinition tableDefinition, IndexDefinition indexDefinition);

    String getAddUniqueKeySQL(OltpTableDefinition tableDefinition, UKDefinition ukDefinition);

    String getDropForeignKeySQL(OltpTableDefinition tableDefinition, FKDefinition fkDefinition);

    String getDropIndexSQL(OltpTableDefinition tableDefinition, IndexDefinition indexDefinition);

    String getDropUniqueKeySQL(OltpTableDefinition tableDefinition, UKDefinition ukDefinition);

    String[] getUpdateColumnLengthSQL(OltpTableDefinition tableDefinition, OltpColumnDefinition columnDefinition);

    String[] getUpdateForeignKeySQL(OltpTableDefinition tableDefinition, FKDefinition fkDefinition);

    String[] getUpdateIndexSQL(OltpTableDefinition tableDefinition, IndexDefinition indexDefinition);

    String[] getUpdateUniqueKeySQL(OltpTableDefinition tableDefinition, UKDefinition ukDefinition);
}