package com.tlc.crm.core.internal.features.listener;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;
import com.tlc.crm.core.features.api.note.model.RecentNote;

import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;

import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowAddListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * A listener that listens create/update actions of {@link RecentNote}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SourceNoteListener implements RowAddListener, RowUpdateListener {

    private static final short MAX_LENGTH = 25;
    private static final String NOTE_ID_KEY = "NOTE_ID";
    private static final String COMPANY_ID_KEY = "COMPANY_ID";
    private static final String CONTACT_ID_KEY = "CONTACT_ID";
    private static final String DEAL_ID_KEY = "DEAL_ID";
    private static final String ELLIPSIS = "...";

    private final NoteManager noteManager;
    private final SourceNoteManager sourceNoteManager;

    private SourceNoteListener() {
        this.noteManager = NoteManager.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
    }

    private final static class Instance {
        private static final SourceNoteListener INSTANCE = new SourceNoteListener();
    }

    public static SourceNoteListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of {@link Row}.
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        final Collection<RecentNote> recentNotes = new HashSet<>();
        final Optional<Row> optionalRow = rows.stream().findFirst();

        if (optionalRow.isPresent()) {
            final Map<Long, String> noteContentById = new HashMap<>();
            final long orgId = optionalRow.get().getOrgId();

            for (final Row row : rows) {
                final long noteId = row.get(NOTE_ID_KEY);
                final String recentNote;

                if (noteContentById.containsKey(noteId)) {
                    recentNote = noteContentById.get(noteId);
                } else {
                    final Note note = noteManager.get(orgId, noteId);
                    final String trimmedContent = note.getTrimmedContent(MAX_LENGTH);
                    recentNote = note.getContentBody().length() > MAX_LENGTH ? StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, trimmedContent, ELLIPSIS) : trimmedContent;

                    noteContentById.put(noteId, recentNote);
                }

                switch (row.getTable().getName()) {
                    case OMNTCOMPANYNOTE.TABLE ->
                            recentNotes.add(new RecentNote(orgId, row.get(COMPANY_ID_KEY), recentNote, Source.COMPANY));
                    case OMNTCONTACTNOTE.TABLE ->
                            recentNotes.add(new RecentNote(orgId, row.get(CONTACT_ID_KEY), recentNote, Source.CONTACT));
                    case OMNTDEALNOTE.TABLE ->
                            recentNotes.add(new RecentNote(orgId, row.get(DEAL_ID_KEY), recentNote, Source.DEAL));
                }
            }

            sourceNoteManager.updateEntityRecentNote(orgId, recentNotes);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of {@link Row}.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        rows.forEach(row -> sourceNoteManager.updateRecentNote(noteManager.convertRowToModel(row, null)));
    }

}
