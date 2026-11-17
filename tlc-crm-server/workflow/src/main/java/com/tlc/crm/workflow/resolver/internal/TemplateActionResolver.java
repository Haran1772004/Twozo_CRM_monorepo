package com.tlc.crm.workflow.resolver.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.resolver.ActionResolver;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the implementation for {@link ActionResolver}
 * </P>
 *
 * @author KarthickV
 * @version 1.0
 */
public final class TemplateActionResolver extends ActionResolver {

    private static final String FIELD_NAME = "fieldName";

    private static class Instance {
        private static final TemplateActionResolver INSTANCE = new TemplateActionResolver();
    }

    /**
     * Gets the {@link TemplateCriteriaResolver} instance.
     *
     * @return {@link TemplateCriteriaResolver}.
     */
    public static TemplateActionResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                   Represents the organization id
     * @param workflow                Represents the {@link Workflow}
     * @param isCommonActionSource    A boolean flag indicating whether the action source are common or not
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @param triggerActionFieldJson  The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray       The {@link JsonArray} containing the mapping of fields
     */
    @Override
    protected void fillActionFieldValuesToJson(final Long orgId, final Workflow workflow, final boolean isCommonActionSource, final I18nResolutionAdapter i18NResolutionAdapter,
                                               final JsonObject triggerActionFieldJson, final JsonArray fieldMappingArray) {
        final long fieldId = triggerActionFieldJson.getLong(WorkflowAttributes.FIELD_ID);
        final String aliasName = isCommonActionSource ? fieldService.getField(orgId, fieldId).aliasName() : workflowFieldService.get(fieldId).getAliasName();
        final Consumer<JsonObject> valueConsumer = getValueConsumer(i18NResolutionAdapter);
        final int fieldMappingArraySize = fieldMappingArray.size();

        outerLoop:
        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonArray fieldsArray = fieldMappingArray.getJsonArray(fieldMappingArrayIndex);
            final int fieldsArraySize = fieldsArray.size();

            if (fieldsArraySize > 0) {

                for (int fieldsArrayIndex = 0; fieldsArrayIndex < fieldsArraySize; fieldsArrayIndex++) {
                    final JsonObject fieldJson = fieldsArray.getJsonObject(fieldsArrayIndex);

                    if (fieldJson.containsKey(FIELD_NAME) && aliasName.equals(fieldJson.getString(FIELD_NAME))) {

                        if (fieldJson.containsKey(WorkflowAttributes.VALUES)) {
                            final JsonArray valuesArray = fieldJson.getJsonArray(WorkflowAttributes.VALUES);

                            for (int valuesArrayIndex = 0; valuesArrayIndex < valuesArray.size(); valuesArrayIndex++) {
                                final JsonObject valueJson = valuesArray.getJsonObject(valuesArrayIndex);

                                if (valueJson.containsKey(WorkflowAttributes.DEPENDS_ON)) {
                                    valueConsumer.accept(valueJson);
                                }
                            }
                            triggerActionFieldJson.put(WorkflowAttributes.VALUES, valuesArray);
                        }

                        break outerLoop;
                    }
                }
            } else {
                fieldMappingArray.remove(fieldsArray);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the organization id
     * @param i18NResolutionAdapter         Represents the resolver of the I18N strings
     * @param emailActionFieldJson The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray    The {@link JsonArray} containing the mapping of fields
     */
    @Override
    protected void fillEmailValuesToJson(final Long orgId, final I18nResolutionAdapter i18NResolutionAdapter,
                                         final JsonObject emailActionFieldJson, final JsonArray fieldMappingArray) {
        final String aliasName = workflowFieldService.get(emailActionFieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName();
        final Consumer<JsonObject> valueConsumer = getValueConsumer(i18NResolutionAdapter);
        final int fieldMappingArraySize = fieldMappingArray.size();

        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonObject fieldJson = fieldMappingArray.getJsonObject(fieldMappingArrayIndex);

            if (aliasName.equals(fieldJson.getString(FIELD_NAME))) {

                if (fieldJson.containsKey(WorkflowAttributes.VALUES)) {
                    final JsonArray valuesArray = fieldJson.getJsonArray(WorkflowAttributes.VALUES);
                    final int valuesArraySize = valuesArray.size();

                    for (int valuesArrayIndex = 0; valuesArrayIndex < valuesArraySize; valuesArrayIndex++) {
                        final JsonObject valueJson = valuesArray.getJsonObject(valuesArrayIndex);

                        if (valueJson.containsKey(WorkflowAttributes.DEPENDS_ON)) {
                            valueConsumer.accept(valueJson);
                        }
                    }
                    emailActionFieldJson.put(WorkflowAttributes.VALUES, valuesArray);
                }
                fieldMappingArray.remove(fieldJson);
                break;
            }
        }
    }

    /**
     * <p>
     * Retrieves a {@link Consumer} that processes a {@link JsonObject} by replacing its value
     * </p>
     *
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @return A {@link Consumer} that handles {@link JsonObject} values.
     */
    private Consumer<JsonObject> getValueConsumer(final I18nResolutionAdapter i18NResolutionAdapter) {
        return (valueJson) -> {
            final String value = valueJson.getString(WorkflowAttributes.VALUE);
            final StepOption stepOption = stepOptionService.getStepOptionsByAliasName(List.of(value)).get(value);

            if (Objects.nonNull(stepOption)) {
                final JsonObject stepOptionJson = Json.object();

                stepOptionJson.put(WorkflowAttributes.ID, stepOption.id());
                stepOptionJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(stepOption.name()));

                valueJson.put(WorkflowAttributes.VALUE, stepOptionJson);
            }
        };
    }
}
