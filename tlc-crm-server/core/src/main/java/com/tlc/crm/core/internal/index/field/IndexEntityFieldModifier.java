package com.tlc.crm.core.internal.index.field;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Modifies entity fields in indexing document.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexEntityFieldModifier extends AbstractIndexFieldModifier {

    private IndexEntityFieldModifier() {
    }

    private static class Instance {
        private static final IndexEntityFieldModifier INSTANCE = new IndexEntityFieldModifier();
    }

    /**
     * <p>
     * Gets the {@link IndexEntityFieldModifier} Instance
     * </p>
     *
     * @return {@link IndexEntityFieldModifier}
     */
    public static IndexEntityFieldModifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Modifies the entity details in doc json.
     * </p>
     *
     * @param entityId      Entity's id.
     * @param entityKey     Entity's name such as Deal/Contact/Company.
     * @param docJson       Doc about to be indexed.
     * @param entityMap     Entity details.
     * @param updateRequest {@link UpsertRequest}.
     */
    public void modifyEntity(final Long entityId, final String entityKey, final JsonObject docJson,
                             final Map<String, Object> entityMap, final UpsertRequest updateRequest) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson) && Objects.equals(entityId, entityJson.getLong(CommonSearchFields.ID))) {
            docJson.put(entityKey, entityMap);
            updateRequest.addFieldToRemove(entityKey);
        }
    }

    /**
     * <p>
     * Modifies entity's contact in doc json.
     * </p>
     *
     * @param contactId     Contact's id.
     * @param entityKey     Entity's name such as Deal/Contact.
     * @param docJson       Doc about to be indexed.
     * @param contactMap    Contact details.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Field to be removed.
     */
    public void modifyEntityContact(final Long contactId, final String entityKey, final JsonObject docJson,
                                    final Map<String, Object> contactMap, final UpsertRequest updateRequest,
                                    final String fieldToRemove) {
        modifyEntity(contactId, entityKey, CommonSearchFields.CONTACT, docJson, contactMap, updateRequest, fieldToRemove);
    }

    /**
     * <p>
     * Modifies entity's company in doc json.
     * </p>
     *
     * @param companyId  Company's id.
     * @param entityKey  Entity's name such as Deal/Contact.
     * @param docJson    Doc about to be indexed.
     * @param companyMap Company details.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Field to be removed.
     */
    public void modifyEntityCompany(final Long companyId, final String entityKey, final JsonObject docJson,
                                    final Map<String, Object> companyMap, final UpsertRequest updateRequest,
                                    final String fieldToRemove) {
        modifyEntity(companyId, entityKey, CommonSearchFields.COMPANY, docJson, companyMap, updateRequest, fieldToRemove);
    }

    /**
     * <p>
     * Removes entity (entity key value) from the doc json.
     * </p>
     *
     * @param entityId      Entity's id.
     * @param entityKey     Entity's name such as Deal/Contact/Company.
     * @param docJson       Doc about to be indexed.
     * @param updateRequest {@link UpsertRequest}.
     */
    public void removeEntity(final Long entityId, final String entityKey, final JsonObject docJson,
                             final UpsertRequest updateRequest) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson) && Objects.equals(entityId, entityJson.getLong(CommonSearchFields.ID))) {
            updateRequest.addFieldToRemove(entityKey);
            docJson.remove(entityKey);
        }
    }

    /**
     * <p>
     * Removes contact from the entity json.
     * </p>
     *
     * @param contactId     Contact's id.
     * @param entityKey     Entity's name such as Deal.
     * @param docJson       Doc about to be indexed.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeContactFromEntity(final Long contactId, final String entityKey, final JsonObject docJson,
                                        final UpsertRequest updateRequest, final String fieldToRemove) {
        removeField(contactId, entityKey, CommonSearchFields.CONTACT, docJson, updateRequest, fieldToRemove);
    }

    /**
     * <p>
     * Removes company from the entity json.
     * </p>
     *
     * @param companyId     Company's id.
     * @param entityKey     Entity's name such as Deal/Contact.
     * @param docJson       Doc about to be indexed.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeCompanyFromEntity(final Long companyId, final String entityKey, final JsonObject docJson,
                                        final UpsertRequest updateRequest, final String fieldToRemove) {
        removeField(companyId, entityKey, CommonSearchFields.COMPANY, docJson, updateRequest, fieldToRemove);
    }
}