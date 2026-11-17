package com.tlc.crm.common.list.transformer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A container which holds the transformer action of entity of multiple groups
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public class TransformerActionContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerActionContainer.class);
    private final Map<String, Map<String, FieldTransformer>> fieldTransformers;
    private final Map<String, Map<String, ModelTransformer>> entityTransformers;

    private TransformerActionContainer() {
        fieldTransformers = new HashMap<>();
        entityTransformers = new HashMap<>();
    }

    private static class Instance {
        private static final TransformerActionContainer INSTANCE = new TransformerActionContainer();
    }

    public static TransformerActionContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the field transformer into container
     * </p>
     *
     * @param groupName        Represents the name of the group.
     * @param transformer      Holds {@link Transformer} details.
     * @param fieldTransformer Holds the field transformer.
     */
    public void addAction(final String groupName, final Transformer transformer, final FieldTransformer fieldTransformer) {
        final String transformerName = transformer.name();
        final Map<String, FieldTransformer> fieldTransformerMap = fieldTransformers.computeIfAbsent(groupName, group -> new HashMap<>());
        final FieldTransformer registeredFieldTransformer = fieldTransformerMap.get(transformerName);

        if (Objects.isNull(registeredFieldTransformer)) {
            LOGGER.info("Registering field transformer action {}", transformerName);
            fieldTransformerMap.put(transformerName, fieldTransformer);
        } else {
            throw ErrorCode.get(CommonErrorCodes.TRANSFORMER_IS_ALREADY_REGISTERED, " Name : " + transformerName);
        }
    }

    /**
     * <p>
     * Adds the entity transformer into container.
     * </p>
     *
     * @param groupName         Represents the name of the group.
     * @param transformer       Holds {@link Transformer} details.
     * @param modelTransformer Holds the entity transformer.
     */
    public void addEntityTransformer(final String groupName, final Transformer transformer, final ModelTransformer modelTransformer) {
        final String transformerName = transformer.name();
        final Map<String, ModelTransformer> entityTransformerMap = entityTransformers.computeIfAbsent(groupName, group -> new HashMap<>());
        final ModelTransformer registeredModelTransformer = entityTransformerMap.get(transformerName);

        if (Objects.isNull(registeredModelTransformer)) {
            LOGGER.info("Registering entity transformer action {}", transformerName);
            entityTransformerMap.put(transformerName, modelTransformer);
        } else {
            throw ErrorCode.get(CommonErrorCodes.TRANSFORMER_IS_ALREADY_REGISTERED, " Name : " + transformerName);
        }
    }

    /**
     * <p>
     * Fetches the transformer action of a field from container
     * </p>
     *
     * @param name name of default field
     * @return If transformer action available,it will return the action otherwise it will throw an error
     */
    public FieldTransformer fetchFieldTransformer(final String name) {
        final Collection<Map<String, FieldTransformer>> registeredActions = this.fieldTransformers.values();

        for (Map<String, FieldTransformer> fieldAction : registeredActions) {

            if (fieldAction.containsKey(name)) {
                return fieldAction.get(name);
            }
        }
        throw ErrorCode.get(CommonErrorCodes.TRANSFORMER_ACTION_NOT_FOUND);
    }

    /**
     * <p>
     * Fetches the transformer of a entity from container
     * </p>
     *
     * @param name name of default field
     * @return If entity transformer available, it will return the transformer otherwise it will throw an error.
     */
    public ModelTransformer fetchEntityTransformer(final String name) {
        final Collection<Map<String, ModelTransformer>> registeredActions = this.entityTransformers.values();

        for (final Map<String, ModelTransformer> fieldAction : registeredActions) {
            if (fieldAction.containsKey(name)) {
                return fieldAction.get(name);
            }
        }
        throw ErrorCode.get(CommonErrorCodes.TRANSFORMER_ACTION_NOT_FOUND);
    }

    /**
     * <p>
     * Removes the registered transformers of the given group ID.
     * </p>
     *
     * @param groupName Represents the group name.
     */
    public void clear(final String groupName) {
        entityTransformers.remove(groupName);
        fieldTransformers.remove(groupName);
    }

    /**
     * Removes all registered transformers across all groups.
     */
    public void clearAll() {
        entityTransformers.clear();
        fieldTransformers.clear();
    }
}