package com.tlc.crm.core.features.api.merge.service;

import com.tlc.commons.model.TlcEnum;

/**
 * <p>
 * Enum representing various cross-module callback types for entity merge operations.
 * Each constant maps to a specific module that provides merge-related data loading.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum EntityMergeCallbackType implements TlcEnum {

    /**
     * Callback for loading messenger (chat) mapping rows during entity merge.
     */
    CHAT_MERGE,
}

