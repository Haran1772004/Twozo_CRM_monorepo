package com.tlc.crm.core.features.api.note.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.search.IndexManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;
import com.tlc.crm.core.features.api.search.manager.AbstractNoteAndFileIndexManager;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.service.WritableDocumentService;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Note}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class NoteIndexManager extends AbstractNoteAndFileIndexManager<NoteHolder> {

    private static class Instance {
        private static final NoteIndexManager INSTANCE = new NoteIndexManager();
    }

    private NoteIndexManager() {
    }

    /**
     * <p>
     * Gets the {@link NoteIndexManager}.
     * </p>
     *
     * @return {@link NoteIndexManager}.
     */
    public static NoteIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.NOTE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Collection<NoteHolder> noteHolders) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

        for (final NoteHolder noteHolder : noteHolders) {
            final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(noteHolder.id()), index);

            updateRequest.setDoc(Map.of(CommonSearchFields.NAME, noteHolder.getNote().getContentBody()));
            bulkWriteRequest.addUpdateRequest(updateRequest);
        }
        writableService().performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(noteHolders, Action.UPDATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<Map.Entry<String, Object>> fetchValue(final String name, final NoteHolder noteHolder) {
        final Collection<Map.Entry<String, Object>> values = new ArrayList<>();
        final Long orgId = noteHolder.orgId();
        final Map<Source, Long> relatedSourceIds = noteHolder.getRelatedSourceIds();

        if (Objects.equals(NoteFields.ENTITY_ID.getName(), name)) {
            values.add(indexDataFetcher.fetchEntityValue(orgId, noteHolder.getEntityId(), noteHolder.getSource().name()));
        }

        if (Objects.nonNull(relatedSourceIds) && Objects.equals(NoteFields.RELATED_ENTITY_IDS.getName(), name)) {
            relatedSourceIds.values().removeIf(Objects::isNull);

            for (final Map.Entry<Source, Long> relatedEntityIds : relatedSourceIds.entrySet()) {
                values.add(indexDataFetcher.fetchEntityValue(orgId, relatedEntityIds.getValue(), relatedEntityIds.getKey().name()));
            }
        }
        return values.isEmpty() ? null : values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRelatedIndexes(final Collection<NoteHolder> noteHolders, final Action action) {
        final DealIndexManager dealIndexManager = DealIndexManager.getInstance();
        final CompanyIndexManager companyIndexManager = CompanyIndexManager.getInstance();
        final ContactIndexManager contactIndexManager = ContactIndexManager.getInstance();

        switch (action) {
            case CREATE, UPDATE -> {
                for (final NoteHolder noteHolder : noteHolders) {
                    loadEntityRequests(noteHolder.getEntityId(), noteHolder.getSource(), noteHolder, dealIndexManager,
                            companyIndexManager, contactIndexManager, action);
                }
            }
            case DELETE -> {
                for (final NoteHolder noteHolder : noteHolders) {
                    loadNotesFieldDeleteRequest(noteHolder.orgId(), noteHolder.id(),
                            List.of(dealIndexManager, companyIndexManager, contactIndexManager));
                }
            }
        }
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param noteHolder {@link NoteHolder}.
     * @param doc        Holds the entity data to be indexed.
     */
    @Override
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final NoteHolder noteHolder,
                                                    final Map<String, Object> doc) {
        IndexDocumentLoader.get().loadPermissionRelatedFieldsForNoteIndex(orgId, doc);
    }

    /**
     * <p>
     * Loads the entity requests into their {@link BulkWriteRequest}.
     * </p>
     *
     * @param entityId            For update the entity into note document.
     * @param source              For identifies the source entity.
     * @param noteHolder          Holds the details of the note and their mappings.
     * @param dealIndexManager    Represents {@link DealIndexManager}.
     * @param companyIndexManager Represents {@link CompanyIndexManager}.
     * @param contactIndexManager Represents {@link ContactIndexManager}.
     */
    private void loadEntityRequests(final Long entityId, final Source source, final NoteHolder noteHolder,
                                    final DealIndexManager dealIndexManager, final CompanyIndexManager companyIndexManager,
                                    final ContactIndexManager contactIndexManager, final Action action) {
        switch (source) {
            case DEAL -> loadEntityRequest(entityId, dealIndexManager, noteHolder, action);
            case COMPANY -> loadEntityRequest(entityId, companyIndexManager, noteHolder, action);
            case CONTACT -> loadEntityRequest(entityId, contactIndexManager, noteHolder, action);
        }
    }

    /**
     * <p>
     * Loads the note into entity document.
     * </p>
     *
     * @param entityId     For update the entity into note document.
     * @param indexManager {@link IndexManager} Represents entity's index manager.
     * @param noteHolder   Holds the details of the note and their mappings.
     */
    private <T extends TlcModel> void loadEntityRequest(final Long entityId, final AbstractIndexManager<T> indexManager,
                                                        final NoteHolder noteHolder, final Action action) {
        final JsonObject indexJson = indexManager.get(noteHolder.orgId(), entityId, List.of(CommonSearchFields.NOTES, CommonSearchFields.ID), null);

        if (Objects.nonNull(indexJson) && 0 != indexJson.size()) {
            final Long noteId = noteHolder.getNote().id();
            final String index = indexManager.getSearchIndex().getName();
            final String note = noteHolder.getNote().getContentBody();
            final String noteKey = NoteFields.NAME.getName();
            final String entityIdAsString = entityId.toString();
            final UpsertRequest entityUpsertRequest = UpsertRequest.get(entityIdAsString, index);
            final JsonObject entityJson = indexJson.getJsonArray(index).toList().stream().findAny().
                    map(entity -> Json.object((Map<String, ?>) entity)).orElseGet(Json::object);
            final JsonArray notes = entityJson.containsKey(CommonSearchFields.NOTES) ?
                    entityJson.getJsonArray(CommonSearchFields.NOTES) : Json.array();

            switch (action) {
                case CREATE -> {
                    final JsonObject noteJson = Json.object();

                    noteJson.put(CommonSearchFields.ID, noteId);
                    noteJson.put(noteKey, note);
                    notes.put(noteJson);
                    entityJson.put(CommonSearchFields.NOTES, notes);
                }
                case UPDATE -> {
                    final int size = notes.size();

                    for (int count = 0; count < size; count++) {
                        final JsonObject noteJson = notes.getJsonObject(count);

                        if (Objects.equals(noteJson.getLong(CommonSearchFields.ID), noteId)) {
                            noteJson.put(noteKey, note);
                            break;
                        }
                    }
                }
            }
            final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

            entityUpsertRequest.setDoc(entityJson);
            bulkWriteRequest.addUpdateRequest(entityUpsertRequest);
            writableService().performBulkOperations(bulkWriteRequest);
        }
    }

    /**
     * <p>
     * Loads note delete field delete request.
     * </p>
     *
     * @param orgId         Represents an organization's id.
     * @param noteId        Represents a {@link Note} id.
     * @param indexManagers Collection of index managers.
     */
    private void loadNotesFieldDeleteRequest(final Long orgId, final Long noteId, final Collection<AbstractIndexManager<?>> indexManagers) {
        final FilterClause filter = new FilterClause(new Criteria(noteId, Comparator.EQUAL, NoteFields.NOTES_DOT_ID.getName()));
        final WritableDocumentService writableDocumentService = writableService();

        for (final AbstractIndexManager<?> indexManager : indexManagers) {
            final String index = indexManager.getSearchIndex().getName();
            final JsonObject indexJson = indexManager.get(orgId, null, List.of(CommonSearchFields.ID,
                    CommonSearchFields.NOTES), filter);

            if (0 != indexJson.size()) {
                final JsonObject entityJson = indexJson.getJsonArray(index).toList().stream().findAny().
                        map(entity -> Json.object((Map<String, ?>) entity)).orElse(Json.object());
                final JsonArray notes = entityJson.optJsonArray(CommonSearchFields.NOTES);

                if (Objects.nonNull(notes)) {
                    final int size = notes.size();

                    for (int count = 0; count < size; count++) {
                        final JsonObject noteJson = notes.getJsonObject(count);

                        if (Objects.equals(noteJson.getLong(CommonSearchFields.ID), noteId)) {
                            notes.remove(count);
                            break;
                        }
                    }
                    entityJson.put(CommonSearchFields.NOTES, notes);
                }
                final UpsertRequest upsertRequest = UpsertRequest.get(entityJson.getString(CommonSearchFields.ID), index);
                final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

                upsertRequest.setDoc(entityJson);
                bulkWriteRequest.addUpdateRequest(upsertRequest);
                writableDocumentService.performBulkOperations(bulkWriteRequest);
                break;
            }
        }
    }
}