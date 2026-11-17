package com.tlc.crm.core.features.internal.dataimport.container;

import com.tlc.crm.core.features.internal.dataimport.container.impl.AuditLogContainerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.Map;

/**
 * <p>
 * Represents a container managing the services related to data import audit log entries
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface AuditLogContainer {

    /**
     * <p>
     * Create a {@link AuditLogContainer} instance
     * </p>
     *
     * @return The {@link AuditLogContainerImpl} instance
     */
    static AuditLogContainer create() {
        return new AuditLogContainerImpl();
    }

    /**
     * <p>
     * Stores an entry in the audit log for the imported models
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param tlcModelMap Which contains the collection of {@link TlcModel} models
     */
    void addImportedModels(final Source source, final Map<Integer, TlcModel> tlcModelMap);

    /**
     * <p>
     * Updates an entry in the audit log for the imported models
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param tlcModelMap Which contains the collection of old and new {@link TlcModel}
     */
    void addUpdatedModels(final Source source, final Map<Integer, Map<TlcModel, TlcModel>> tlcModelMap);

    /**
     * <p>
     * Deletes an entry in the audit log for the imported models
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param tlcModelMap Which contains the collection of tlc model
     */
    <T extends TlcModel> void addDeletedModels(final Source source, final Map<Integer, TlcModel> tlcModelMap);

    /**
     * <p>
     * Gets the audit log detailing creates in imported models
     * </p>
     *
     * @return The map of audit log detailing creates in imported models
     */
    Map<Source, Map<Integer, TlcModel>> getCreatedRecords();

    /**
     * <p>
     * Gets the audit log detailing updates in imported models
     * </p>
     *
     * @return The map of audit log detailing updates in imported models
     */
    Map<Source, Map<Integer, Map<TlcModel, TlcModel>>> getUpdatedRecords();

    /**
     * <p>
     * Gets the audit log detailing deletes in imported models
     * </p>
     *
     * @return The map of audit log detailing deletes in imported models
     */
    Map<Source, Map<Integer, TlcModel>> getDeletedRecords();
}
