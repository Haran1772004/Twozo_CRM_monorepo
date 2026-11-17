package com.tlc.crm.core.internal.async.bulk.processor;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Processor responsible for handling async create operations for {@link TlcModel} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AsyncCreateProcessor<T extends TlcModel> extends AsyncActionProcessor<T> {
}
