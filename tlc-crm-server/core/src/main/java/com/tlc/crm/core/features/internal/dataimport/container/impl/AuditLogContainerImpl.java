package com.tlc.crm.core.features.internal.dataimport.container.impl;

import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Provides the implementations for {@link AuditLogContainer} services
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class AuditLogContainerImpl implements AuditLogContainer {

    private final Map<Source, Map<Integer, TlcModel>> createdModels;
    private final Map<Source, Map<Integer, Map<TlcModel, TlcModel>>> updatedModels;
    private final Map<Source, Map<Integer, TlcModel>> deletedModels;

    public AuditLogContainerImpl() {
        createdModels = new HashMap<>();
        updatedModels = new HashMap<>();
        deletedModels = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param source      Represents the {@link Source}
     * @param tlcModelMap Which contains the collection of {@link TlcModel} models
     */
    @Override
    public void addImportedModels(final Source source, final Map<Integer, TlcModel> tlcModelMap) {
        createdModels.put(source, tlcModelMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param source    Represents the {@link Source}
     * @param tlcModels Which contains the collection of old and new {@link TlcModel}
     */
    @Override
    public void addUpdatedModels(final Source source, final Map<Integer, Map<TlcModel, TlcModel>> tlcModels) {
        updatedModels.put(source, tlcModels);
    }

    /**
     * {@inheritDoc}
     *
     * @param source    Represents the {@link Source}
     * @param tlcModels Which contains the collection of model
     */
    @Override
    public <T extends TlcModel> void addDeletedModels(final Source source, final Map<Integer, TlcModel> tlcModels) {
        deletedModels.put(source, tlcModels);
    }

    /**
     * {@inheritDoc}
     *
     * @return The map of source to collection of TlcModel representing inserted models
     */
    @Override
    public Map<Source, Map<Integer, TlcModel>> getCreatedRecords() {
        return createdModels;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map of source to collection of TlcModel representing updated models
     */
    @Override
    public Map<Source, Map<Integer, Map<TlcModel, TlcModel>>> getUpdatedRecords() {
        return updatedModels;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map of source to collection of TlcModel representing deleted models
     */
    @Override
    public Map<Source, Map<Integer, TlcModel>> getDeletedRecords() {
        return deletedModels;
    }
}
