package com.tlc.crm.core.workflow.model;

/**
 * Represents the origin of the {@link com.tlc.validator.TlcModel}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ModelSource {

    /**
     * Represents a model is submitted through a web request.
     */
    WEB_REQUEST,

    /**
     * Represents a model is submitted through a workflow.
     */
    WORKFLOW,

    /**
     * Represents a model is submitted through a web-form.
     */
    WEB_FORM,

    /**
     * Represents a model is submitted through import.
     */
    IMPORT;
}
