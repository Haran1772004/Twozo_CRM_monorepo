package com.tlc.crm.core.features.api.note.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;
import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.note.model.converter.NoteConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Note}
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class SourceNoteConverter extends EntityConverter<NoteHolder> {
    private final SourceNoteManager sourceNoteManager;
    private final NoteConverter noteConverter;

    private SourceNoteConverter() {
        noteConverter = NoteConverter.getInstance();
        sourceNoteManager = SourceNoteManager.getInstance();
    }

    private static class Instance {
        private static final SourceNoteConverter INSTANCE = new SourceNoteConverter();
    }

    /**
     * <p>
     * Gets the {@link SourceNoteConverter} Instance
     * </p>
     *
     * @return {@link SourceNoteConverter}
     */
    public static SourceNoteConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the Json data to note
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Contains the Json Object
     * @return {@link Note}
     */
    @Override
    public NoteHolder convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildNoteByPartialData(converterConfig, jsonObject);
        } else {
            return buildNote(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts the note into Json data
     * </p>
     *
     * @param noteHolder An entity that holds the note attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final NoteHolder noteHolder) {
        return noteConverter.convert(noteHolder.getNote());
    }

    /**
     * <p>
     * Builds the note for the input Json
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Contains the Json Object
     * @return {@link Note}
     */
    private NoteHolder buildNoteByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final NoteHolder noteHolder = sourceNoteManager.get(converterConfig.orgId(), jsonObject.getLong(NoteFields.ID.getName()));

        setJsonDataToModel(converterConfig, noteHolder, jsonObject);

        return noteHolder;
    }

    /**
     * <p>
     * Sets the Json to note
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param noteHolder      An entity that holds the note attributes
     * @param jsonObject      Contains the Json data
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final NoteHolder noteHolder, final JsonObject jsonObject) {
        final String sourceTypeKey = NoteFields.SOURCE.getName();
        final String entityIdKey = NoteFields.ENTITY_ID.getName();

        if (jsonObject.containsKey(sourceTypeKey)) {
            noteHolder.setSource(Source.getType(jsonObject.getString(sourceTypeKey)));
        }

        if (jsonObject.containsKey(entityIdKey)) {
            noteHolder.setEntityId(jsonObject.getLong(entityIdKey));
        }
        noteHolder.setNote(noteConverter.convert(converterConfig, jsonObject));
        setRelatedIds(noteHolder);
    }

    /**
     * <p>
     * Constructs the note for the Json provided
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Contains the Json data
     * @return {@link Note}
     */
    private NoteHolder buildNote(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final NoteHolder noteHolder = new NoteHolder();

        setJsonDataToModel(converterConfig, noteHolder, jsonObject);

        return noteHolder;
    }

    /**
     * <p>
     * Sets deal related source ids.
     * </p>
     *
     * @param noteHolder An entity that holds the note attributes
     */
    private void setRelatedIds(final NoteHolder noteHolder) {
        final Long orgId = noteHolder.orgId();
        final Map<Source, Long> relatedIds = noteHolder.getRelatedSourceIds();
        final Long entityId = noteHolder.getEntityId();

        if (Objects.isNull(relatedIds)) {

            switch (noteHolder.getSource()) {
                case DEAL -> {
                    final Map<String, Long> dealRelatedIds = DealManager.getInstance().getDealContactAndCompanyIds(orgId, entityId);
                    final Long contactId = dealRelatedIds.get(OMNTCONTACTNOTE.CONTACT_ID);
                    final Long companyId = dealRelatedIds.get(OMNTCOMPANYNOTE.COMPANY_ID);

                    if (Objects.nonNull(contactId)) {
                        noteHolder.addRelatedSourceId(Source.CONTACT, contactId);
                    }

                    if (Objects.nonNull(companyId)) {
                        noteHolder.addRelatedSourceId(Source.COMPANY, companyId);
                    }
                }
                case CONTACT -> {
                    final Long contactCompanyId = ContactManager.getInstance().getContactCompanyId(entityId, orgId);

                    if (Objects.nonNull(contactCompanyId)) {
                        noteHolder.addRelatedSourceId(Source.COMPANY, contactCompanyId);
                    }
                }
            }
        }
    }
}
