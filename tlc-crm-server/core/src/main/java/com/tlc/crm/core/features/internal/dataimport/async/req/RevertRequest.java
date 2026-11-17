package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.commons.json.JsonObject;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A model that holds the revert request parameters.
 *
 * @param id          Refers the identifier of the revert request.
 * @param importId    Refers the import id.
 * @param orgId       Refers the organization id.
 * @param userId      Refers the user id.
 * @param locale      Refers the {@link Locale} of the request.
 * @param requestJson Refers the request json.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record RevertRequest(String id, long importId, long orgId, long userId,
                            Locale locale, JsonObject requestJson, Consumer<Void> cleanUpConsumer) {
}