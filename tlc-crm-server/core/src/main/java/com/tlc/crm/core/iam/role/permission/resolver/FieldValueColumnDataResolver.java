package com.tlc.crm.core.iam.role.permission.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.entity.timeline.field.TimelineFields;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.model.SecureColumnValueResolver;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A concrete implementation of {@link SecureColumnValueResolver} that resolves secure values
 * for columns based on field values.
 *
 * <p>
 * This class provides a mechanism to process and transform sensitive column values,
 * such as applying encryption, masking, or other security transformations.
 * </p>
 *
 * <h3>Customization:</h3>
 * <p>
 * Extend this class and override the {@link SecureColumnValueResolver#resolve(Collection, Column)} method to implement
 * custom security logic.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see SecureColumnValueResolver
 */
public class FieldValueColumnDataResolver implements SecureColumnValueResolver {

    private static final String NAME = "mtlfieldvaluetimeline_new_value";

    private final DataPermissionHandlerRegistry dataPermissionHandlerRegistry;

    private static final class InstanceHolder {
        private static final FieldValueColumnDataResolver COLUMN_DATA_RESOLVER = new FieldValueColumnDataResolver();
    }

    private FieldValueColumnDataResolver() {
        dataPermissionHandlerRegistry = DataPermissionHandlerRegistry.getInstance();
    }

    public static FieldValueColumnDataResolver getInstance() {
        return InstanceHolder.COLUMN_DATA_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @return Name of the {@link SecureColumnValueResolver}.
     */
    @Override
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows   Holds the details of {@link Row}.
     * @param column Holds the details of {@link Column}.
     */
    @Override
    public void resolve(final Collection<Row> rows, final Column column) {
        final Map<Long, Collection<Integer>> contactIdAndRowIndexesGroup = new HashMap<>();
        final Map<Integer, JsonObject> oldJsonMap = new HashMap<>();
        final Map<Integer, JsonArray> emailsByRowIndex = new HashMap<>();
        final Map<Integer, Map<Long, JsonObject>> contactsByRowIndex = new HashMap<>();
        final List<Row> entityRows = List.copyOf(rows);
        final String toKey = TimelineFields.TO.getName();
        final String contactIdKey = TimelineFields.CONTACT_ID.getName();
        final String contactsKey = TimelineFields.CONTACTS.getName();
        final String emailAddressKey = TimelineFields.EMAIL_ADDRESS.getName();
        final int rowSize = entityRows.size();

        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            final Row row = entityRows.get(rowIndex);
            final String value = row.get(column);

            if (Objects.isNull(value)) {
                return;
            }
            final JsonObject jsonObject;

            try {
                jsonObject = Json.object(value);
            } catch (final Throwable throwable) {
                continue;
            }
            final JsonObject toJson = jsonObject.optJsonObject(toKey);

            if (Objects.nonNull(toJson)) {
                final JsonArray contactsArray = toJson.getJsonArray(contactsKey);
                final JsonArray addressArray = toJson.getJsonArray(emailAddressKey);
                final int contactArraySize = contactsArray.size();

                for (int contactIndex = 0; contactIndex < contactArraySize; contactIndex++) {
                    final JsonObject contactJson = contactsArray.getJsonObject(contactIndex);
                    final Long contactId = contactJson.getLong(contactIdKey);

                    contactsByRowIndex.computeIfAbsent(rowIndex, index -> new HashMap<>()).put(contactId, contactJson);
                    contactIdAndRowIndexesGroup.computeIfAbsent(contactId, conId -> new HashSet<>()).add(rowIndex);
                }

                if (0 < contactArraySize) {
                    oldJsonMap.put(rowIndex, jsonObject);
                    emailsByRowIndex.put(rowIndex, addressArray);
                }
            }
        }
        final BiConsumer<Integer, JsonObject> emailAddressAdder = (rowIndex, contactJson) -> {
            final String emailAddress = (String) contactJson.opt(emailAddressKey);
            final JsonArray emailArray = emailsByRowIndex.get(rowIndex);

            if (Objects.nonNull(emailAddress)) {
                emailArray.put(emailAddress);
            }
        };
        final RolePermission permission = dataPermissionHandlerRegistry.get().getPermission(Category.Type.SOURCES,
                SubCategory.Type.CONTACTS, Action.Type.ACCESS);

        if (permission.isGranted()) {
            if (!contactIdAndRowIndexesGroup.isEmpty()) {
                validateAndRemoveContacts(permission.getScope(), contactIdAndRowIndexesGroup, contactsByRowIndex, emailAddressAdder);
            }
        } else {
            removeAllContacts(contactsByRowIndex, emailAddressAdder);
        }
        loadResolvedDataIntoRows(entityRows, column, emailsByRowIndex, contactsByRowIndex, oldJsonMap);
    }

    /**
     * <p>
     * Validates the permission to access the contacts and removes any contacts haven't permission.
     * </p>
     *
     * @param scope                       Represents the scope of the permission.
     * @param contactIdAndRowIndexesGroup Holds the contact id and their respective row indexes.
     * @param contactsByRowIndex          Holds the row indexes and the available contact details.
     * @param emailAddressAdder           Represents the {@link BiConsumer} to adds the email address of the provided contact.
     */
    private void validateAndRemoveContacts(final Scope scope, final Map<Long, Collection<Integer>> contactIdAndRowIndexesGroup,
                                           final Map<Integer, Map<Long, JsonObject>> contactsByRowIndex,
                                           final BiConsumer<Integer, JsonObject> emailAddressAdder) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(user.orgId(), user.id(), user.getRoleId());
        final Map<Long, Boolean> entityValidationResults = entityPermissionValidator.validateByIndividual(contactIdAndRowIndexesGroup.keySet(), StoreType.CONTACT, scope);

        for (final Map.Entry<Long, Boolean> entityValidationResult : entityValidationResults.entrySet()) {
            if (!entityValidationResult.getValue()) {
                final Long contactId = entityValidationResult.getKey();
                final Collection<Integer> rowIndexes = contactIdAndRowIndexesGroup.getOrDefault(contactId, Collections.emptySet());

                for (final Integer rowIndex : rowIndexes) {
                    emailAddressAdder.accept(rowIndex, contactsByRowIndex.get(rowIndex).remove(contactId));
                }
            }
        }
    }

    /**
     * <p>
     * Removes all the contact details from the container.
     * </p>
     *
     * @param contactsByRowIndex Holds the row indexes and the available contact details.
     * @param emailAddressAdder  Represents the {@link BiConsumer} to adds the email address of the provided contact.
     */
    private void removeAllContacts(final Map<Integer, Map<Long, JsonObject>> contactsByRowIndex,
                                   final BiConsumer<Integer, JsonObject> emailAddressAdder) {
        for (final Map.Entry<Integer, Map<Long, JsonObject>> rowContacts : contactsByRowIndex.entrySet()) {
            for (final JsonObject contactJson : rowContacts.getValue().values()) {
                emailAddressAdder.accept(rowContacts.getKey(), contactJson);
            }
            rowContacts.getValue().clear();
        }
    }

    /**
     * <p>
     * Loads the given rows with the resolved values.
     * </p>
     *
     * @param entityRows         Represents the entity rows.
     * @param column             Represents the {@link Column} of the row.
     * @param emailsByRowIndex   Holds the row indexes and the available email addresses.
     * @param contactsByRowIndex Holds the row indexes and the available contact details.
     * @param oldJsonMap         Holds the old json values.
     */
    private void loadResolvedDataIntoRows(final List<Row> entityRows, final Column column, final Map<Integer, JsonArray> emailsByRowIndex,
                                          final Map<Integer, Map<Long, JsonObject>> contactsByRowIndex,
                                          final Map<Integer, JsonObject> oldJsonMap) {
        final String toKey = TimelineFields.TO.getName();
        final String contactsKey = TimelineFields.CONTACTS.getName();
        final String emailAddressKey = TimelineFields.EMAIL_ADDRESS.getName();

        for (final Map.Entry<Integer, JsonObject> oldJsonEntry : oldJsonMap.entrySet()) {
            final Integer rowIndex = oldJsonEntry.getKey();
            final JsonObject oldJson = oldJsonEntry.getValue();
            final JsonObject toJson = oldJson.getJsonObject(toKey);
            final Map<Long, JsonObject> contactJsons = contactsByRowIndex.get(rowIndex);
            final JsonArray contactsArray = Json.array();

            for (final Map.Entry<Long, JsonObject> contactJsonEntry : contactJsons.entrySet()) {
                contactsArray.put(contactJsonEntry.getValue());
            }
            toJson.put(contactsKey, contactsArray);
            toJson.put(emailAddressKey, emailsByRowIndex.get(rowIndex));
            oldJson.put(toKey, toJson);
            entityRows.get(rowIndex).set(column, oldJson.toString());
        }
    }
}
