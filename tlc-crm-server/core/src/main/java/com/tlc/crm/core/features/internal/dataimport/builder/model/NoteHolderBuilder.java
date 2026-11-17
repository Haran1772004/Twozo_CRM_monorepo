package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * The builder which builds the {@link NoteHolder} model into the {@link SourceModelContainer}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class NoteHolderBuilder {

    private NoteHolderBuilder() {
    }

    private static final class Instance {
        private static final NoteHolderBuilder INSTANCE = new NoteHolderBuilder();
    }

    /**
     * Gets the instance of {@link NoteHolderBuilder}
     *
     * @return {@link NoteHolderBuilder} instance
     */
    public static NoteHolderBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Builds the {@link NoteHolder} by extracting the source details from the {@link SourceNote}.
     *
     * @param sourceNote Refers the {@link SourceNote} details.
     * @return The {@link NoteHolder} instance.
     */
    public NoteHolder buildNoteHolder(final SourceNote sourceNote) {
        final NoteHolder noteHolder = new NoteHolder();
        final Company company = sourceNote.getCompany();
        final Contact contact = sourceNote.getContact();
        final Deal deal = sourceNote.getDeal();

        noteHolder.setNote(sourceNote);

        final BiConsumer<NoteHolder.Source, Long> sourceNoteConsumer = (source, entityId) -> {
            noteHolder.setSource(source);
            noteHolder.setEntityId(entityId);
            noteHolder.addRelatedSourceId(source, entityId);
        };

        if (Objects.nonNull(deal)) {
            sourceNoteConsumer.accept(NoteHolder.Source.DEAL, deal.id());

            if (Objects.nonNull(contact)) {
                noteHolder.addRelatedSourceId(NoteHolder.Source.CONTACT, contact.id());
            }

            if (Objects.nonNull(company)) {
                noteHolder.addRelatedSourceId(NoteHolder.Source.COMPANY, company.id());
            }
        } else if (Objects.nonNull(contact)) {
            sourceNoteConsumer.accept(NoteHolder.Source.CONTACT, contact.id());

            if (Objects.nonNull(company)) {
                noteHolder.addRelatedSourceId(NoteHolder.Source.COMPANY, company.id());
            }
        } else if (Objects.nonNull(company)) {
            sourceNoteConsumer.accept(NoteHolder.Source.COMPANY, company.id());
        }

        return noteHolder;
    }

}