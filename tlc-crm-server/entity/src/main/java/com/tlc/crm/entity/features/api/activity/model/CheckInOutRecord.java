package com.tlc.crm.entity.features.api.activity.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * This class contains the activity check-in or check-out data
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class CheckInOutRecord implements TlcModel {

    @Max(value = 90, message = "i18n.activity.check.latitude.invalid", groups = {Group.Create.class})
    @Min(value = -90, message = "i18n.activity.check.latitude.invalid", groups = {Group.Create.class})
    @NotNull(message = "i18n.activity.check.latitude.invalid", groups = {Group.Create.class})
    private Double latitude;

    @Max(value = 180, message = "i18n.activity.check.longitude.invalid", groups = {Group.Create.class})
    @Min(value = -180, message = "i18n.activity.check.longitude.invalid", groups = {Group.Create.class})
    @NotNull(message = "i18n.activity.check.longitude.invalid", groups = {Group.Create.class})
    private Double longitude;

    @NotBlank(message = "i18n.activity.check.location.invalid", groups = {Group.Create.class})
    private String location;

    @NotNull(message = "i18n.activity.check.org.id.required", groups = {Group.Create.class})
    private Long orgId;

    @NotNull(message = "i18n.activity.check.activity.id.required", groups = {Group.Create.class})
    private Long activityId;

    @NotNull(message = "i18n.activity.check.user.id.required", groups = {Group.Create.class})
    private Long userId;

    @NotNull(message = "i18n.activity.check.time.required", groups = {Group.Create.class})
    private Long time;
    private String details;
    private JsonObject detailsJson;
    private boolean isCheckIn;

    public Long getActivityId() {
        return activityId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setActivityId(final Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(final Long time) {
        this.time = time;
    }

    public String getDataAsString() {
        return details;
    }

    public void setDataAsString(final String details) {
        this.details = details;
        this.detailsJson = Objects.isNull(details) ? null : Json.object(details);
    }

    public JsonObject getDataJson() {
        return detailsJson;
    }

    public void setDataJson(final JsonObject detailsJson) {
        this.details = detailsJson.toString();
        this.detailsJson = detailsJson;
    }

    public boolean isCheckIn() {
        return isCheckIn;
    }

    public void setCheckIn(final boolean checkIn) {
        isCheckIn = checkIn;
    }

    @Override
    public Long id() {
        return 0L;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}