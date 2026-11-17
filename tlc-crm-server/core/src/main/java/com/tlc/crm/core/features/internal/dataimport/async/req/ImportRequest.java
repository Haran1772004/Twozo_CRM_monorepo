package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.commons.json.JsonObject;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A model that holds the import request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ImportRequest {

    private final String id;
    private final long importId;
    private final long orgId;
    private final long userId;
    private final Locale locale;
    private final JsonObject requestJson;
    private final Consumer<Void> cleanUpConsumer;

    /**
     * @param id          Refers the identifier of the import request.
     * @param importId    Refers the import detail id.
     * @param orgId       Refers the organization id.
     * @param userId      Refers the user id.
     * @param locale      Refers the {@link Locale} of the request.
     * @param requestJson Refers the request json.
     */
    public ImportRequest(String id, long importId, long orgId, long userId, Locale locale, JsonObject requestJson,
                         final Consumer<Void> cleanUpConsumer) {
        this.id = id;
        this.importId = importId;
        this.orgId = orgId;
        this.userId = userId;
        this.locale = locale;
        this.requestJson = requestJson;
        this.cleanUpConsumer = cleanUpConsumer;
    }

    public String id() {
        return id;
    }

    public long importId() {
        return importId;
    }

    public long orgId() {
        return orgId;
    }

    public long userId() {
        return userId;
    }

    public Locale locale() {
        return locale;
    }

    public JsonObject requestJson() {
        return requestJson;
    }

    public void clean() {
        Objects.requireNonNull(cleanUpConsumer, "Import request cleanup activity should not be null").accept(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ImportRequest) obj;
        return Objects.equals(this.id, that.id) &&
                this.importId == that.importId &&
                this.orgId == that.orgId &&
                this.userId == that.userId &&
                Objects.equals(this.locale, that.locale) &&
                Objects.equals(this.requestJson, that.requestJson) &&
                Objects.equals(this.cleanUpConsumer, that.cleanUpConsumer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, importId, orgId, userId, locale, requestJson, cleanUpConsumer);
    }

    @Override
    public String toString() {
        return "ImportRequest[" +
                "id=" + id + ", " +
                "importId=" + importId + ", " +
                "orgId=" + orgId + ", " +
                "userId=" + userId + ", " +
                "locale=" + locale + ", " +
                "requestJson=" + requestJson + ", " +
                "cleanUpConsumer=" + cleanUpConsumer + ']';
    }
}