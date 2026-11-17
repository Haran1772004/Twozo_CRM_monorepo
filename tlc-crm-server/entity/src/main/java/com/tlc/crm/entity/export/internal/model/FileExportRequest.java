package com.tlc.crm.entity.export.internal.model;

import com.tlc.attachment.io.Extension;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Represents the file export details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class FileExportRequest extends ExportRequest {

    private Source source;
    private Extension extension;
    private Collection<String> headers;
    private Collection<Map<String, Object>> entityRows;
    private I18nResolutionAdapter i18NResolutionAdapter;
    private Collection<FieldConfig> fieldConfigs;

    public Extension getExtension() {
        return extension;
    }

    public void setExtension(final Extension extension) {
        this.extension = extension;
    }

    public Collection<String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Collection<String> headers) {
        this.headers = headers;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Collection<Map<String, Object>> getEntityRows() {
        return entityRows;
    }

    public void setEntityRows(final Collection<Map<String, Object>> entityRows) {
        this.entityRows = entityRows;
    }

    public I18nResolutionAdapter getI18nPlaceholderResolver() {
        return i18NResolutionAdapter;
    }

    public void setI18nPlaceholderResolver(final I18nResolutionAdapter i18NResolutionAdapter) {
        this.i18NResolutionAdapter = i18NResolutionAdapter;
    }

    public Collection<FieldConfig> getFieldConfigs() {
        return fieldConfigs;
    }

    public void setFieldConfigs(final Collection<FieldConfig> fieldConfigs) {
        this.fieldConfigs = fieldConfigs;
    }
}
