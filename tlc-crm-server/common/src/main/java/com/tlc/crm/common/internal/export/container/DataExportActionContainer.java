package com.tlc.crm.common.internal.export.container;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.export.action.DataExportAction;
import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the information about the registered data export actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public class DataExportActionContainer {

    private final Map<String, Map<String, DataExportAction>> dataExportActionContainer = new HashMap<>();

    private static class Instance {
        private static final DataExportActionContainer INSTANCE = new DataExportActionContainer();
    }

    public static DataExportActionContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Registers the given data export action of the given group
     *
     * @param groupName        Data exporter group name
     * @param exporterName     Data exporter name
     * @param dataExportAction An action that does the data export work {@link DataExportAction}
     */
    public void addExportAction(final String groupName, final String exporterName, final DataExportAction dataExportAction) {
        final DataExportAction existingDataExportAction = fetchExportAction(exporterName);

        if (Objects.isNull(existingDataExportAction)) {
            final Map<String, DataExportAction> dataExportActions = dataExportActionContainer.computeIfAbsent(groupName, actions -> new HashMap<>());

            if (dataExportActions.containsKey(exporterName)) {
                throw ErrorCode.get(DataExportErrorCodes.DUPLICATE_DATA_EXPORTERS_IDENTIFIED, exporterName);
            } else {
                dataExportActions.put(exporterName, dataExportAction);
            }
        } else {
            throw ErrorCode.get(DataExportErrorCodes.DUPLICATE_DATA_EXPORTERS_IDENTIFIED, exporterName);
        }

    }

    /**
     * Fetches the export action by name
     *
     * @param exporterName Data exporter name
     * @return {@link DataExportAction}
     */
    public DataExportAction getExportAction(final String exporterName) {
        final DataExportAction dataExportAction = fetchExportAction(exporterName);

        if (Objects.isNull(dataExportAction)) {
            throw ErrorCode.get(DataExportErrorCodes.INVALID_DATA_EXPORTER_NAME, "i18n.data.export.name.invalid");
        }

        return dataExportAction;
    }

    /**
     * Fetches the export action by name
     *
     * @param exporterName Data exporter name
     * @return {@link DataExportAction}
     */
    private DataExportAction fetchExportAction(final String exporterName) {
        final Optional<DataExportAction> dataExportAction = dataExportActionContainer.values().stream()
                .filter(actionMap -> Objects.nonNull(actionMap.get(exporterName)))
                .map(actionMap -> actionMap.get(exporterName))
                .findAny();

        return dataExportAction.isPresent() ? dataExportAction.get() : null;
    }

    /**
     * Calculates the count of the data export actions registered under a single group
     *
     * @param groupName Data exporter group name
     * @return Count of the data export actions registered under the given group
     */
    public int count(final String groupName) {
        final Map<String, DataExportAction> exportActionMap = dataExportActionContainer.get(groupName);

        return Objects.nonNull(exportActionMap) ? exportActionMap.size() : 0;
    }

    /**
     * Clears the export actions registered for the given group
     *
     * @param groupName Data export group name
     */
    public void clear(final String groupName) {
        dataExportActionContainer.remove(groupName);
    }

    /**
     * Clears all the export actions registered to this container
     */
    public void clearAll() {
        dataExportActionContainer.clear();
    }
}
