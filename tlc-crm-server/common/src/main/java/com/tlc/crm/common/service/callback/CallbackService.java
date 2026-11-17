package com.tlc.crm.common.service.callback;

import com.tlc.commons.model.TlcEnum;
import com.tlc.commons.service.registry.auto.service.AutoService;

/**
 * <p>
 * Base callback service interface for cross-module communication within SERVER.
 * </p>
 * <p>
 * This provides a generic mechanism for higher-level modules to define callback contracts
 * that lower-level modules implement and register via {@link com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar}.
 * </p>
 * <p>
 * Module hierarchy (high → low): commons → entity → core → messenger/notifications etc.
 * Callback direction (low → high via registry): messenger → core → entity → commons.
 * </p>
 * <p>
 * Usage:
 * <ol>
 *   <li>Define a specific callback type enum (implements {@link TlcEnum}) in the calling module.</li>
 *   <li>Create a specific callback interface extending this in the calling module.</li>
 *   <li>Create a registry (extends {@link com.tlc.commons.service.registry.auto.service.AutoServiceRegistry}) in the calling module.</li>
 *   <li>Implement and annotate with {@link com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar} in the provider module.</li>
 * </ol>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface CallbackService<T extends TlcEnum> extends AutoService<T> {
}