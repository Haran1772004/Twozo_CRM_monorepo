package com.tlc.crm.core.action.analytics.reports.data.export.model;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code EmailContentModel} represents the content structure used for building
 * email notifications related to data exports or analytics reports.
 * <p>
 * This model includes basic user-related and content-specific fields such as:
 * username, graph name, and a downloadable link.
 * It implements {@link EmailNotifiableModel} to support notification processing.
 * </p>
 *
 * @author Kathir
 * @version 1.0
 */
@Getter
@Setter
public class AnalyticsExportEmailRequest implements EmailNotifiableModel
{
    private String receiver;
    private String chartName;
    private String fileDownloadUrl;
}
