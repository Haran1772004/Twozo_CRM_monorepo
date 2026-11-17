package com.tlc.file.common.action.req;

import com.tlc.attachment.io.File;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.fields.WebFields;

import com.tlc.web.WebExchange;
import com.tlc.web.WebFormData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the multipart/form-data request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see AbstractFileRequest
 */
public final class FileMultiPartRequest extends AbstractFileRequest {

    private final WebFormData formData;
    private final JsonObject body;
    private final Map<String, List<File>> formDataFiles;
    private final List<File> files;

    public FileMultiPartRequest(final WebExchange exchange) {
        super(exchange);
        this.formData = exchange.getWebFormData();
        this.formDataFiles = new HashMap<>();
        this.body = getRequestBody();
        this.files = formDataFiles.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Gets the list of files from request
     *
     * @return the {@link List} of {@link File}
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Gets the list of files from request
     *
     * @param key query parameter
     * @return the {@link List} of {@link File}
     */
    private List<File> getFiles(final String key) {
        if (Objects.nonNull(formDataFiles) && !formDataFiles.containsKey(key)) {
            formDataFiles.put(key, this.formData.getFiles(key));
        }
        return formDataFiles.get(key);
    }

    /**
     * Gets the value of query parameter from request
     *
     * @param key query parameter
     * @return the {@link String} value of the key
     */
    private String get(final String key) {
        return this.formData.getString(key);
    }

    /**
     * Returns the {@link JsonObject} of multipart request
     *
     * @return the {@link JsonObject} of request
     */
    private JsonObject getRequestBody() {
        final JsonObject multipartRequestJson = Json.object();
        final String typeKey = WebFields.TYPE;

        multipartRequestJson.put(typeKey, get(typeKey));
        setMultipartReqToJson(multipartRequestJson);

        return multipartRequestJson;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link JsonObject} of request
     */
    @Override
    public JsonObject getRequestJson() {
        return body;
    }

    /**
     * Sets the multipart request to Json
     *
     * @param multipartRequestJson the {@link JsonObject} of multipart request
     */
    private void setMultipartReqToJson(final JsonObject multipartRequestJson) {
        final Collection<String> params = this.formData.getParams();
        final JsonObject multipartData = Json.object();
        final JsonArray jsonArray = Json.array();

        if (Objects.nonNull(params)) {
            params.forEach(param -> {
                if (formData.isFileItem(param)) {
                    setAttachment(getFiles(param), jsonArray);
                } else {
                    multipartData.put(param, get(param));
                }
            });

            if (jsonArray.size() > 0) {
                multipartData.put(WebFields.ATTACHMENTS, jsonArray);
            }
        }
        multipartRequestJson.put(WebFields.DATA, multipartData);
    }

    /**
     * Sets the attachment into the json array.
     *
     * @param files     the {@link List} of files
     * @param jsonArray the {@link JsonArray} of attachment.
     */
    private void setAttachment(final List<File> files, final JsonArray jsonArray) {
        files.forEach(file -> {
            final JsonObject attachment = Json.object();

            attachment.put(WebFields.FILE, file);
            jsonArray.put(attachment);
        });
    }

}
