package com.tlc.crm.core.features.internal.dataimport.audit.log.impl;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.audit.EntityNameService;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.internal.dataimport.audit.log.DataImportAuditLogger;
import com.tlc.crm.core.features.internal.dataimport.builder.model.NoteHolderBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static com.tlc.crm.fields.models.StoreType.NOTE;

/**
 * <p>
 * Implements the service for {@link DataImportAuditLogger}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class DataImportAuditLoggerImpl implements DataImportAuditLogger {

    private static final I18nKey NOTE_KEY = new I18nKey("i18n.note.display.note");

    private final AuditLogger auditLogger;
    private final NoteHolderBuilder noteHolderBuilder;
    private final EntityNameService entityNameService;

    public DataImportAuditLoggerImpl() {
        auditLogger = AuditLogger.getInstance();
        noteHolderBuilder = NoteHolderBuilder.getInstance();
        entityNameService = EntityNameService.getInstance();
    }

    private static final class Instance {
        private static final DataImportAuditLoggerImpl INSTANCE = new DataImportAuditLoggerImpl();
    }

    /**
     * Gets the instance of {@link DataImportAuditLoggerImpl}
     *
     * @return {@link DataImportAuditLoggerImpl} instance
     */
    public static DataImportAuditLoggerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents the organization id
     * @param auditLogContainer Contains imported data loaded into the {@link AuditLogContainer}
     * @param invalidIndices    Which contains the invalid index
     */
    @Override
    public void log(final Long orgId, final Long userId, final AuditLogContainer auditLogContainer, final Collection<Integer> invalidIndices) {
        final Map<Source, Map<Integer, TlcModel>> createdRecords = auditLogContainer.getCreatedRecords();
        final Map<Source, Map<Integer, Map<TlcModel, TlcModel>>> updatedRecords = auditLogContainer.getUpdatedRecords();
        final Map<Source, Map<Integer, TlcModel>> deletedRecords = auditLogContainer.getDeletedRecords();

        if (!createdRecords.isEmpty()) {
            create(orgId, userId, createdRecords, invalidIndices);
        }

        if (!updatedRecords.isEmpty()) {
            update(orgId, userId, updatedRecords, invalidIndices);
        }

        if (!deletedRecords.isEmpty()) {
            delete(orgId, userId, deletedRecords, invalidIndices);
        }
    }

    /**
     * <p>
     * Inserts audit entities for import data based on sources and records them as audit entries
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param createdRecords Which contains audit logs of created import data, categorized by their source.
     */
    private void create(final Long orgId, final Long userId, final Map<Source, Map<Integer, TlcModel>> createdRecords, final Collection<Integer> invalidIndices) {
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.ADDED;

        for (final Map.Entry<Source, Map<Integer, TlcModel>> sourceModelEntry : createdRecords.entrySet()) {
            final Source source = sourceModelEntry.getKey();
            final Map<Integer, TlcModel> sourceModels = sourceModelEntry.getValue();
            final Collection<TlcModel> tlcModels = sourceModels.values();

            if (Objects.nonNull(invalidIndices) && !invalidIndices.isEmpty()) {
                sourceModels.keySet().removeAll(invalidIndices);
            }

            switch (source) {
                case CONTACT ->
                        auditLogDataList.addAll(buildAuditLogData(userId, tlcModels, AuditSource.CONTACT, auditAction));
                case COMPANY ->
                        auditLogDataList.addAll(buildAuditLogData(userId, tlcModels, AuditSource.COMPANY, auditAction));
                case DEAL -> auditLogDataList.addAll(buildAuditLogData(userId, tlcModels, AuditSource.DEAL, auditAction));
                case PRODUCT ->
                        auditLogDataList.addAll(buildAuditLogData(userId, tlcModels, AuditSource.PRODUCT, auditAction));
                case ACTIVITY -> {
                    for (final TlcModel tlcModel : tlcModels) {
                        auditLogDataList.add(new AuditLogData(userId, tlcModel.id(), ((ActivityHolder) tlcModel).getActivity(), AuditSource.ACTIVITY, auditAction));
                    }
                }
                case NOTE -> {
                    for (final TlcModel tlcModel : tlcModels) {
                        final NoteHolder noteHolder = noteHolderBuilder.buildNoteHolder((SourceNote) tlcModel);
                        final Long entityId = noteHolder.getEntityId();
                        final String entityName = entityNameService.fetchEntityName(orgId, entityId, noteHolder.getSource());

                        auditLogDataList.add(new AuditLogData(userId, entityId, entityName, AuditSource.get(noteHolder.getSource().name()), auditAction,
                                new AuditAttribute(NOTE_KEY, null, null, AuditAction.ADDED, NOTE)));
                    }
                }
            }
        }
        auditLogger.log(orgId, auditLogDataList);
    }

    /**
     * <p>
     * Updates audit entities for import data based on sources and records them as audit entries
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param updatedRecords Which contains audit logs of updated import data, categorized by their source.
     */
    private void update(final Long orgId, final Long userId, final Map<Source, Map<Integer, Map<TlcModel, TlcModel>>> updatedRecords, final Collection<Integer> invalidIndices) {
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.UPDATED;

        for (final Map.Entry<Source, Map<Integer, Map<TlcModel, TlcModel>>> sourceModelEntry : updatedRecords.entrySet()) {
            final Source source = sourceModelEntry.getKey();
            final Map<Integer, Map<TlcModel, TlcModel>> sourceModels = sourceModelEntry.getValue();

            if (Objects.nonNull(invalidIndices) && !invalidIndices.isEmpty()) {
                sourceModels.keySet().removeAll(invalidIndices);
            }

            for (final Map<TlcModel, TlcModel> tlcModelMap : sourceModels.values()) {

                switch (source) {
                    case CONTACT -> {
                        final AuditSource auditSource = AuditSource.CONTACT;

                        tlcModelMap.forEach((existingModel, newModel) -> {
                            auditLogDataList.add(new AuditLogData(userId, newModel.id(), existingModel, newModel, auditSource, auditAction));
                        });
                    }
                    case COMPANY -> {
                        final AuditSource auditSource = AuditSource.COMPANY;

                        tlcModelMap.forEach((existingModel, newModel) -> {
                            auditLogDataList.add(new AuditLogData(userId, newModel.id(), existingModel, newModel, auditSource, auditAction));
                        });
                    }
                    case DEAL -> {
                        final AuditSource auditSource = AuditSource.DEAL;

                        tlcModelMap.forEach((existingModel, newModel) -> {
                            auditLogDataList.add(new AuditLogData(userId, newModel.id(), existingModel, newModel, auditSource, auditAction));
                        });
                    }
                    case PRODUCT -> {
                        final AuditSource auditSource = AuditSource.PRODUCT;

                        tlcModelMap.forEach((existingModel, newModel) -> {
                            auditLogDataList.add(new AuditLogData(userId, newModel.id(), existingModel, newModel, auditSource, auditAction));
                        });
                    }
                }
            }
        }
        auditLogger.log(orgId, auditLogDataList);
    }

    /**
     * <p>
     * Deletes audit entities for import data based on sources and records them as audit entries
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param deletedRecords Which contains audit logs of deleted import data, categorized by their source
     */
    private void delete(final Long orgId, final Long userId, final Map<Source, Map<Integer, TlcModel>> deletedRecords, final Collection<Integer> invalidIndices) {
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.DELETED;

        for (final Map.Entry<Source, Map<Integer, TlcModel>> sourceCollectionEntry : deletedRecords.entrySet()) {
            final Source source = sourceCollectionEntry.getKey();
            final Map<Integer, TlcModel> sourceModels = sourceCollectionEntry.getValue();
            final Collection<TlcModel> models = sourceModels.values();

            if (Objects.nonNull(invalidIndices) && !invalidIndices.isEmpty()) {
                sourceModels.keySet().removeAll(invalidIndices);
            }

            switch (source) {
                case CONTACT -> auditLogDataList.addAll(buildAuditLogData(userId, models, AuditSource.CONTACT, auditAction));
                case COMPANY -> auditLogDataList.addAll(buildAuditLogData(userId, models, AuditSource.COMPANY, auditAction));
                case DEAL -> auditLogDataList.addAll(buildAuditLogData(userId, models, AuditSource.DEAL, auditAction));
                case PRODUCT -> auditLogDataList.addAll(buildAuditLogData(userId, models, AuditSource.PRODUCT, auditAction));
                case NOTE -> {

                    for (final TlcModel tlcModel : models) {
                        final NoteHolder noteHolder = (NoteHolder) tlcModel;
                        final Long id = noteHolder.id();
                        final AuditAttribute auditAttribute = new AuditAttribute(NOTE_KEY, null, null, AuditAction.DELETED, NOTE);
                        final Pair<AuditCategory, Pair<Long, String>> entityDetails = SourceNoteManager.getInstance().getEntityDetails(orgId, id);

                        if (Objects.nonNull(entityDetails)) {
                            final Pair<Long, String> entityDetailsPair = entityDetails.getValue();

                            auditLogDataList.add(new AuditLogData(noteHolder.getNote().getCreatedBy().id(), entityDetailsPair.getKey(), entityDetailsPair.getValue(),
                                    AuditSource.get(entityDetails.getKey().getName()), auditAction, auditAttribute));
                        }
                    }
                }
                case ACTIVITY -> {
                    for (final TlcModel tlcModel : models) {
                        auditLogDataList.add(new AuditLogData(userId, tlcModel.id(), ((ActivityHolder) tlcModel).getActivity(), AuditSource.ACTIVITY, auditAction));
                    }
                }
            }
        }
        auditLogger.log(orgId, auditLogDataList);
    }

    /**
     * <p>
     * Builds the {@link TlcModel} details into {@link AuditLogData}
     * </p>
     *
     * @param userId      Represents an user id
     * @param tlcModels   Which contains the collection of {@link TlcModel}
     * @param auditSource Represents the {@link AuditSource}
     * @param auditAction Represents the {@link AuditAction}
     */
    private Collection<AuditLogData> buildAuditLogData(final Long userId, final Collection<TlcModel> tlcModels,
                                                       final AuditSource auditSource, final AuditAction auditAction) {
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();

        for (final TlcModel tlcModel : tlcModels) {
            auditLogDataList.add(new AuditLogData(userId, tlcModel.id(), tlcModel, auditSource, auditAction));
        }

        return auditLogDataList;
    }
}
