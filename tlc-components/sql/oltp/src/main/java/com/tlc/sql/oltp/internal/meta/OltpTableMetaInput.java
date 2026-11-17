package com.tlc.sql.oltp.internal.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.TableEngine;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.TableMetaInput;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.internal.meta.engine.DefaultOltpEngine;
import com.tlc.sql.oltp.internal.meta.engine.OltpTableEngineConfiguration;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OltpTableMetaInput extends TableMetaInput<OltpTableEngineConfiguration, OltpColumnDefinition> {

    private final Map<String, UKDefinition> ukDefinitions;
    private final Map<String, IndexDefinition> indexDefinitions;

    public OltpTableMetaInput(final String tableName, final OltpTableEngineConfiguration engineConfiguration) {
        super(tableName, engineConfiguration);
        this.ukDefinitions = new HashMap<>();
        this.indexDefinitions = new HashMap<>();
    }

    @Override
    public TableEngine buildTableEngine() {

        return switch (engineConfiguration.getName().toLowerCase()) {
            case "default" -> new DefaultOltpEngine();
            default -> throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_TABLE_ENGINE);
        };
    }

    public void addUKDefinition(final UKDefinition ukdefinition) {
        ukDefinitions.put(ukdefinition.getName(), ukdefinition);
    }

    public void addIndexDefinition(IndexDefinition indexDefinition) {
        indexDefinitions.put(indexDefinition.getName(), indexDefinition);
    }
}