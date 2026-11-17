package com.tlc.sql.olap.internal.parser;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.parser.MetaDataParser;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapDataType;
import com.tlc.sql.olap.api.meta.OlapDecimalColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;
import com.tlc.sql.olap.internal.meta.OlapTableMetaInput;
import com.tlc.sql.olap.internal.meta.engine.OlapTableEngineConfiguration;
import org.w3c.dom.Element;

import java.util.Objects;

public abstract class OlapMetaDataParser<E extends OlapTableEngineConfiguration, M extends OlapTableMetaInput<E>, T extends OlapTableDefinition> extends MetaDataParser<E, OlapColumnDefinition, T, M> {

    @Override
    protected DataType getDataType(final String dataTypeStr) {

        try {
            return CommonDataType.get(dataTypeStr);
        } catch (ErrorCode errorCode) {

            if (errorCode.equals(ErrorCode.get(ErrorCodes.NOT_SUPPORTED))) {
                return OlapDataType.get(dataTypeStr);
            } else {
                throw errorCode;
            }
        }
    }

    @Override
    protected OlapColumnDefinition buildColumnDefinition(final Element columnElement, final String columnName, final DataType dataType,
                                                         final int convertedMaxLength, final Object convertedDefaultValue,
                                                         final boolean isNullable) {
        return new OlapColumnDefinition(columnName, dataType, convertedMaxLength, convertedDefaultValue, isNullable);
    }

    @Override
    protected OlapColumnDefinition buildDecimalColumnDefinition(final String columnName, final int convertedMaxLength,
                                                                final int convertedPrecision, final int convertedScale,
                                                                final Object convertedDefaultValue, final boolean isNullable) {
        return new OlapDecimalColumnDefinition(columnName, convertedMaxLength, convertedPrecision, convertedScale, convertedDefaultValue, isNullable);
    }

    @Override
    protected DatabaseType getDatabaseType() {
        return DatabaseType.OLAP;
    }
}