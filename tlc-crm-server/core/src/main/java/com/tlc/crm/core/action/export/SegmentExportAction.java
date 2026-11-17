package com.tlc.crm.core.action.export;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.export.action.DataExportAction;
import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.export.req.DataExportRequest;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.entity.export.DataExportHandler;
import com.tlc.crm.core.action.export.internal.SegmentDataExportHandler;
import com.tlc.crm.core.segment.converter.SegmentFilterPreferenceConverter;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.service.SegmentService;
import com.tlc.crm.entity.export.formatter.DataExportFormatter;
import com.tlc.crm.entity.export.internal.model.DataExportFormatRequest;
import com.tlc.crm.entity.export.internal.model.FileExportRequest;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Handles the export action of segments (segment management list). Resolves the segments
 * matching the requested filter criteria and exports them as rows aligned with the segment
 * management list columns: segment name, folder name, created by &amp; on, last updated by &amp; on.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@DataExporter(name = "segment")
public class SegmentExportAction implements DataExportAction {

    private final DataExportHandler dataExportHandler;
    private final SegmentFilterPreferenceConverter segmentFilterPreferenceConverter;
    private final SegmentService segmentService;
    private final I18nService i18nService;
    private final DataExportFormatter dataExportFormatter;

    /**
     * <p>
     * Creates the segment export action and initializes the handlers, converters
     * and services required to perform the export.
     * </p>
     */
    public SegmentExportAction() {
        dataExportHandler = SegmentDataExportHandler.getInstance();
        segmentFilterPreferenceConverter = SegmentFilterPreferenceConverter.getInstance();
        segmentService = SegmentService.getInstance();
        i18nService = I18nServiceProvider.get();
        dataExportFormatter = DataExportFormatter.getInstance();
    }

    /**
     * <p>
     * Exports the segments matching the filter criteria and sort configuration present in the
     * request parameters. Builds a row for each segment with localized column headers and values,
     * and hands the rows over to the {@link DataExportHandler} to generate the export file.
     * </p>
     *
     * @param dataExportRequest Represents the {@link DataExportRequest}.
     * @return {@code true} if the export is processed successfully.
     */
    @Override
    public boolean doExport(final DataExportRequest dataExportRequest) {
        final long orgId = dataExportRequest.orgId();
        final Long userId = dataExportRequest.userId();
        final Locale locale = dataExportRequest.locale();
        final I18nResolver i18nResolver = i18nService.getResolver(locale);
        final JsonObject parameters = dataExportRequest.parameters();
        final JsonObject criteriaJson = parameters.getJsonObject(SegmentFields.CRITERIA.getName());
        final SegmentFilterUserPreference preference = segmentFilterPreferenceConverter.convert(
                new ConverterConfig(orgId, userId, locale, false, null), criteriaJson);

        segmentFilterPreferenceConverter.populateDefaultPreferences(preference, parameters);
        preference.setSortJson(parameters.optJsonObject(ListActionFields.SORT));
        final long totalRecords = segmentService.getTotalCount(orgId, preference);

        if (totalRecords > 0) {
            preference.setLimitJson(Json.object()
                    .put(ListActionFields.START, NumericalConstant.ONE)
                    .put(ListActionFields.COUNT, totalRecords));
        }
        final Collection<Segment> segments = totalRecords == 0 ? Collections.emptyList() : segmentService.get(orgId, preference);
        final Collection<Map<String, Object>> segmentRows = new ArrayList<>((int) totalRecords);

        final String nameKey = i18nResolver.get("i18n.segment.list.field.name");
        final String folderKey = i18nResolver.get("i18n.segment.list.field.folder");
        final String createdByAndOnKey = i18nResolver.get("i18n.segment.list.field.createdByAndOn");
        final String lastUpdatedKey = i18nResolver.get("i18n.segment.list.field.lastUpdatedByAndOn");

        for (final Segment segment : segments) {
            final Map<String, Object> row = new HashMap<>(NumericalConstant.FOUR);

            row.put(nameKey, segment.getName());

            final SegmentFolder folder = segment.getFolder();

            row.put(folderKey, Objects.nonNull(folder) && Objects.nonNull(folder.getName()) ? folder.getName() : "-");

            putByAndOn(row, createdByAndOnKey, segment.getCreatedBy(), segment.getCreatedTime());
            putByAndOn(row, lastUpdatedKey, segment.getModifiedBy(), segment.getModifiedTime());

            segmentRows.add(row);
        }
        final FileExportRequest fileExportRequest = new FileExportRequest();

        fileExportRequest.setOrgId(orgId);
        fileExportRequest.setUserId(userId);
        fileExportRequest.setId(dataExportRequest.exportId());
        fileExportRequest.setExtension(Extension.get(dataExportRequest.type()));
        fileExportRequest.setExportProvider(ExportProviderType.LIST);
        fileExportRequest.setI18nPlaceholderResolver(new I18NCRMResolutionAdapter(i18nResolver, orgId));
        fileExportRequest.setHeaders(List.of(nameKey, folderKey, createdByAndOnKey, lastUpdatedKey));
        fileExportRequest.setEntityRows(segmentRows);

        dataExportHandler.exportData(fileExportRequest);

        return true;
    }

    /**
     * <p>
     * Puts the "by and on" value for the given column into the row, formed by combining the
     * full name of the user with the formatted date and time, separated by {@code &}. If the user or the date and time
     * is not available, a placeholder ("-") is put instead.
     * </p>
     *
     * @param row       Represents the export row to which the value is added.
     * @param columnKey Represents the localized column key under which the value is put.
     * @param user      Represents the {@link CrmUser} who performed the action.
     * @param dateTime  Represents the {@link DateTime} at which the action was performed.
     */
    private void putByAndOn(final Map<String, Object> row, final String columnKey,
                            final CrmUser user, final DateTime dateTime) {
        if (Objects.isNull(user) || Objects.isNull(dateTime)) {
            row.put(columnKey, "-");
            return;
        }
        dataExportFormatter.format(new DataExportFormatRequest(columnKey, dateTime.getUtcTimeStamp(), StoreType.DATE_TIME, row));
        row.put(columnKey, String.join(" & ", user.getFullName(), (String) row.get(columnKey)));
    }
}