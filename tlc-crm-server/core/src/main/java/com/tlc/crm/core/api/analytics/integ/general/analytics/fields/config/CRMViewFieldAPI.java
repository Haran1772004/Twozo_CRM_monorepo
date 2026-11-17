package com.tlc.crm.core.api.analytics.integ.general.analytics.fields.config;

import com.tlc.analytics.config.model.table.AnalyticsDataType;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.Position;
import com.tlc.analytics.config.model.view.report.FieldCategory;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.chart.GraphSubType;

/**
 * Retrieving different types of metadata related to CRM view fields
 *
 * @author james
 * @version 1.0
 */
public interface CRMViewFieldAPI
{

    /**
     * Retrieves the OperationType object for an operation based on the provided type code.
     *
     * @param typeCode the type code representing the operation type
     * @return the OperationType object corresponding to the type code
     */
    default OperationType getOperationType( final Integer typeCode )
    {
        return OperationType.getOperationType( typeCode );
    }

    /**
     * Retrieves the Axis object based on the provided type code.
     *
     * @param typeCode the type code representing the axis type
     * @return the Axis object corresponding to the type code
     */
    default Axis getAxis( final int typeCode )
    {
        return Axis.getType( typeCode );
    }

    /**
     * Retrieves the GraphSubType object based on the provided type code.
     *
     * @param typeCode the type code representing the graph subtype
     * @return the GraphSubType object corresponding to the type code
     */
    default GraphSubType getGraphSubType( final int typeCode )
    {
        return GraphSubType.getGraphSubType( typeCode );
    }

    /**
     * Retrieves the FieldCategory object based on the provided type code.
     *
     * @param typeCode the type code representing the field type
     * @return the FieldCategory object corresponding to the type code
     */
    default FieldCategory getFieldType(final int typeCode )
    {
        return FieldCategory.getFieldType( typeCode );
    }

    /**
     * Retrieves the Position object based on the provided type code.
     *
     * @param typeCode the type code representing the position
     * @return the Position object corresponding to the type code
     */
    default Position getPosition( final int typeCode )
    {
        return Position.getPosition( typeCode );
    }

    /**
     * Resolves CHDataType to its base AnalyticsDataType (NUMERICAL, STRING, or DATE).
     * This method resolves ClickHouse-specific data types to their base categories.
     *
     * @param chDataTypeCode the ClickHouse data type code
     * @return the base AnalyticsDataType (NUMERICAL, STRING, or DATE)
     */
    default AnalyticsDataType resolveAnalyticsDataType(final int chDataTypeCode )
    {
        return AnalyticsDataType.resolveFromAnalyticsDataType( chDataTypeCode );
    }
}
