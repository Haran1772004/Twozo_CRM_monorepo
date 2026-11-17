package com.tlc.crm.core.features.api.note.model;

import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;

/**
 * A model which represents the {@link RecentNote}.
 *
 * @param orgId    Refers the organization id.
 * @param entityId Refers the entity id.
 * @param content  Refers the content.
 * @param source   Refers the source.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record RecentNote(long orgId, long entityId, String content, Source source) {
}
