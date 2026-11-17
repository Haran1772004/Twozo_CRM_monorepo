package com.tlc.crm.entity.export.internal;

import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.export.AbstractDataExportHandler;
import com.tlc.crm.entity.export.internal.model.DataExportFormatRequest;
import com.tlc.crm.entity.export.internal.model.FileExportRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A handler that transforms the entity data and writes the result to a file.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class EntityDataExportHandler extends AbstractDataExportHandler {

    /**
     * <p>
     * Creates the entity data export handler. Private to enforce the singleton pattern.
     * </p>
     */
    private EntityDataExportHandler() {
        super();
    }

    /**
     * <p>
     * Holds the singleton instance of the {@link EntityDataExportHandler}, ensuring
     * it is lazily initialized when first accessed.
     * </p>
     */
    private static final class InstanceHolder {

        private static final EntityDataExportHandler INSTANCE = new EntityDataExportHandler();
    }

    /**
     * <p>
     * Gets the singleton instance of the {@link EntityDataExportHandler}.
     * </p>
     *
     * @return The {@link EntityDataExportHandler} instance.
     */
    public static EntityDataExportHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Exports the entity data by transforming the entity rows in the given request
     * and writing the result to a file for the source of the request.
     * </p>
     *
     * @param fileExportRequest Represents the {@link FileExportRequest}.
     */
    @Override
    public void exportData(final FileExportRequest fileExportRequest) {
        final Collection<Map<String, Object>> data = transformExportData(fileExportRequest);
        final String source = fileExportRequest.getSource().getName();

        fileExportRequest.setEntityRows(data);
        export(source, fileExportRequest);
    }

    /**
     * <p>
     * Transforms the entity rows of the given request into export rows. Each non-null value
     * that has a matching {@link FieldConfig} is formatted based on its store type and
     * keyed by the localized display name of the field.
     * </p>
     *
     * @param fileExportRequest Represents the {@link FileExportRequest}.
     * @return The transformed rows to be exported.
     */
    private Collection<Map<String, Object>> transformExportData(final FileExportRequest fileExportRequest) {
        final Collection<Map<String, Object>> entityRows = fileExportRequest.getEntityRows();
        final Map<String, FieldConfig> fieldConfigs = fileExportRequest.getFieldConfigs().stream()
                .collect(Collectors.toMap(FieldConfig::getName, Function.identity()));
        final Collection<Map<String, Object>> result = new ArrayList<>();

        for (final Map<String, Object> rowMap : entityRows) {
            final Set<Map.Entry<String, Object>> rowValues = rowMap.entrySet();
            final Map<String, Object> rowData = new HashMap<>();

            for (final Map.Entry<String, Object> rowValueEntry : rowValues) {
                final Object value = rowValueEntry.getValue();

                if (Objects.nonNull(value)) {
                    final String fieldKey = rowValueEntry.getKey();
                    final FieldConfig fieldConfig = fieldConfigs.get(fieldKey);

                    if (Objects.nonNull(fieldConfig)) {
                        dataExportFormatter.format(new DataExportFormatRequest(fieldKey, value, fieldConfig.getFieldType().storeType(), rowMap));
                        rowData.put(fileExportRequest.getI18nPlaceholderResolver().get(fieldConfig.getDisplayName()), rowMap.get(fieldKey));
                    }
                }
            }
            result.add(rowData);
        }

        return result;
    }
}