package com.tlc.sql.common.api.dml.update;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.internal.update.DataCorrector;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public interface DMLUpdateManager {

    void handleDMLCorrection(final SortedMap<Integer, DataCorrector> dataCorrectors);

    DataContainer doDMLUpdate(final List<File> current, final List<File> newFiles, final Set<String> ignoreTables,
                              final Map<String, String> dynamicData);

    DataContainer getDataFromDatabase(final DataContainer existingXmlContainer, final Set<String> ignoreTables);

    void loadChanges(final DataContainer container, final List<DeleteQuery> seqQueries, final DataContainer cC,
                     final DataContainer dC, final DataContainer nC, final Set<String> ignore);
}
