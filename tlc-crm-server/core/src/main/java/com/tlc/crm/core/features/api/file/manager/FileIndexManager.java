package com.tlc.crm.core.features.api.file.manager;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.search.manager.AbstractNoteAndFileIndexManager;
import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Attachment}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class FileIndexManager extends AbstractNoteAndFileIndexManager<FileHolder> {

    private static class Instance {
        private static final FileIndexManager INSTANCE = new FileIndexManager();
    }

    private FileIndexManager() {
    }

    /**
     * <p>
     * Gets the {@link FileIndexManager}.
     * </p>
     *
     * @return {@link FileIndexManager}.
     */
    public static FileIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.FILE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Collection<FileHolder> fileHolders) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

        for (final FileHolder fileHolder : fileHolders) {
            final Collection<EntityAttachment> entityAttachments = fileHolder.getAttachments();

            for (final EntityAttachment entityAttachment : entityAttachments) {
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(entityAttachment.id()), index);

                updateRequest.setDoc(Map.of(CommonSearchFields.NAME, entityAttachment.getFileName()));
                bulkWriteRequest.addUpdateRequest(updateRequest);
            }
        }
        writableService().performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(fileHolders, Action.UPDATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<Map.Entry<String, Object>> fetchValue(final String name, final FileHolder fileHolder) {
        final Collection<Map.Entry<String, Object>> values = new ArrayList<>();

        if (Objects.equals(AttachmentFields.ENTITY_ID.getName(), name)) {
            values.add(indexDataFetcher.fetchEntityValue(fileHolder.orgId(), fileHolder.getEntityId(), fileHolder.getSource()));
        }

        if (Objects.equals(AttachmentFields.RELATED_ENTITY_IDS.getName(), name)) {
            final Map<String, Long> relatedIds = fileHolder.getRelatedEntityIds();

            relatedIds.remove(fileHolder.getSource());
            relatedIds.values().removeIf(Objects::isNull);

            if (!relatedIds.isEmpty()) {
                for (final Map.Entry<String, Long> relatedSourceIds : relatedIds.entrySet()) {
                    values.add(indexDataFetcher.fetchEntityValue(fileHolder.orgId(), relatedSourceIds.getValue(), relatedSourceIds.getKey()));
                }
            }
        }

        return values.isEmpty() ? null : values;
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param fileHolder {@link FileHolder}.
     * @param doc        Holds the entity data to be indexed.
     */
    @Override
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final FileHolder fileHolder, final Map<String, Object> doc) {
        indexDocumentLoader.loadPermissionRelatedFieldsForFileIndex(orgId, doc);
    }
}