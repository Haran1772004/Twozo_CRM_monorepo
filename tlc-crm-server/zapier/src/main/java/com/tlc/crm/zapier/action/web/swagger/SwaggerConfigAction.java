package com.tlc.crm.zapier.action.web.swagger;

import com.tlc.crm.common.action.secure.CrmSecureAction;

/**
 * Abstract base class for serving Swagger configuration files and resources.
 * Provides a foundation for actions that deliver Swagger UI assets,
 * Swagger JSON documentation, or any related configuration needed to initialize
 * and display the Swagger API documentation within the application.
 * <p>
 * Subclasses are expected to implement specific logic for serving files such as:
 * - Swagger UI HTML (e.g., index.html)
 * - Swagger initializer scripts (e.g., swagger-initializer.js)
 * - Swagger JSON specification files
 * <p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class SwaggerConfigAction extends CrmSecureAction {
}
