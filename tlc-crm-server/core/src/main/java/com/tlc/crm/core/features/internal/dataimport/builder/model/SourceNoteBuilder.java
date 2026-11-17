package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;

import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.note.model.converter.NoteConverter;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.validator.TlcModel;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builder that can construct the {@link SourceNote} for importing
 *
 * @author sundar
 * @version 1.0
 * @see AbstractModelBuilder
 */
public class SourceNoteBuilder extends AbstractModelBuilder<SourceNote> {

    private static final Source SOURCE = Source.NOTE;
    private final NoteConverter noteConverter;

    private SourceNoteBuilder() {
        this.noteConverter = NoteConverter.getInstance();
    }

    private static final class Instance {
        private static final SourceNoteBuilder SOURCE_NOTE_BUILDER = new SourceNoteBuilder();
    }

    /**
     * Gets the instance of {@link SourceNoteBuilder}
     *
     * @return {@link SourceNoteBuilder}
     */
    public static SourceNoteBuilder getInstance() {
        return Instance.SOURCE_NOTE_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the sourceModelContainer
     * @param index      Represents the index.
     * @return The constructed object of type {@link Note} using the data from the DataHolder for importing
     */
    @Override
    public Optional<SourceNote> build(final DataHolder dataHolder, final int index) {
        final JsonObject noteJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);
        final String noteKey = NoteFields.NOTE.getName();

        if (noteJson.containsKey(noteKey)) {
            noteJson.put(NoteFields.CONTENT.getName(), noteJson.get(noteKey));
        }

        if (Objects.nonNull(noteJson)) {
            final SourceNote sourceNote = new SourceNote();

            noteConverter.setJsonDataToModel(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, noteJson), sourceNote, noteJson);

            loadRelatedSourceData(dataHolder, sourceNote, index);

            return Optional.of(sourceNote);
        }
        return Optional.empty();
    }

    /**
     * Loads the related source model into the given {@link SourceNote}.
     *
     * @param dataHolder The holder for the properties used to store the model in the sourceModelContainer.
     * @param sourceNote Represents the model that will be updated with the related data.
     * @param index      Represents the index.
     */
    private void loadRelatedSourceData(final DataHolder dataHolder, final SourceNote sourceNote, final int index) {
        final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();
        final BiConsumer<Source, Consumer<TlcModel>> relatedSourceConsumer = (source, modelConsumer) -> {
            final Optional<TlcModel> optionalRelatedSourceModel = getRelatedSourceModel(index, sourceModelContainer, SOURCE, source);

            optionalRelatedSourceModel.ifPresent(modelConsumer);
        };

        for (final Source source : sourceModelContainer.sources()) {

            switch (source) {
                case COMPANY -> relatedSourceConsumer.accept(source, model -> sourceNote.setCompany((Company) model));
                case CONTACT -> relatedSourceConsumer.accept(source, model -> sourceNote.setContact((Contact) model));
                case DEAL -> relatedSourceConsumer.accept(source, model -> sourceNote.setDeal((Deal) model));
            }
        }
    }
}