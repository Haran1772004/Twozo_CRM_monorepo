package com.tlc.crm.core.features.internal.dataimport.service.impl;

import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.service.DataImportService;
import com.tlc.crm.core.features.internal.dataimport.builder.DataContainerBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;

import com.tlc.crm.core.iam.role.permission.controller.ImportLimitController;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Provides the services for the import related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class DataImportServiceImpl implements DataImportService {

    private DataImportServiceImpl() {
    }

    private static final class Instance {
        private static final DataImportServiceImpl DATA_IMPORT_SERVICE = new DataImportServiceImpl();
    }

    /**
     * Gets the instance of {@link DataImportServiceImpl}.
     *
     * @return {@link DataImportServiceImpl} instance.
     */
    public static DataImportServiceImpl getInstance() {
        return Instance.DATA_IMPORT_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo           Which holds the {@link ImportInfo} details.
     * @param sourceModelContainer Is a repository that encompasses all the model values extracted from the sourceRecordContainer.
     * @param errorTracker         Tracks the errors that arise while importing the data into the database.
     * @return The data containers for the import process grouped by the index of rows.
     */
    @Override
    public Map<Integer, DataContainer> fetchDataContainersByIndex(final ImportInfo importInfo,
                                                                  final SourceModelContainer sourceModelContainer,
                                                                  final ErrorTracker errorTracker) {
        final Map<Integer, DataContainer> indexedDataContainers = new TreeMap<>();
        final Map<Integer, Map<Source, TlcModel>> dataMap = sourceModelContainer.getDataMap();
        final ImportLimitController limitController = new ImportLimitController();

        if (!dataMap.isEmpty()) {
            final Collection<Integer> indices = new HashSet<>(dataMap.keySet());
            final DataContainerBuilder dataContainerBuilder = DataContainerBuilder.create(importInfo.orgId(), importInfo.id(),
                    sourceModelContainer, limitController, errorTracker);
            final Map<Integer, Map<Source, TlcModel>> groupedModelsByIndex = new TreeMap<>();
            final Map<Source, Collection<TlcModel>> sourceModelMap = new EnumMap<>(Source.class);

            errorTracker.invalidateIndices(sourceModelContainer);
            indices.removeAll(errorTracker.getInvalidIndices());
            final Comparator<Source> sourceComparator = Comparator.comparingInt(Source::getId);

            for (final int index : indices) {

                for (final Map.Entry<Source, TlcModel> sourceModelEntry : dataMap.get(index).entrySet()) {
                    final Source source = sourceModelEntry.getKey();
                    final Integer referredIndex = Objects.requireNonNullElse(sourceModelContainer.getParentIndex(index, source), index);
                    final TlcModel model = sourceModelEntry.getValue();

                    groupedModelsByIndex.computeIfAbsent(referredIndex, indexKey -> new TreeMap<>(sourceComparator))
                            .putIfAbsent(source, model);
                    sourceModelMap.computeIfAbsent(source, newSource -> new HashSet<>()).add(model);
                }
            }
            dataContainerBuilder.loadSourceDataContainers(sourceModelMap);

            try {
                for (final Map.Entry<Integer, Map<Source, TlcModel>> groupedModelsEntry : groupedModelsByIndex.entrySet()) {
                    final int index = groupedModelsEntry.getKey();

                    indexedDataContainers.put(index, dataContainerBuilder.build(index, groupedModelsEntry.getValue()));
                }
            } finally {
                limitController.clearLimitHistory(LimitActionType.IMPORT_CREATE);
                limitController.clearLimitHistory(LimitActionType.IMPORT_UPDATE);
            }
        }

        return indexedDataContainers;
    }
}
