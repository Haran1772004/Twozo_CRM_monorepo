package com.tlc.crm.icon.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.icon.internal.status.IconErrorCodes;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model which represents the Icon
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class Icon implements Serializable {

    public enum Type {
        ACTIVITY,
        TIMELINE,
        NOTIFICATION,
        CHART,
        MENU,
        ;

        public static Type getIconType(final String type) {

            if (Objects.isNull(type)) {
                throw ErrorCode.get(IconErrorCodes.INVALID_TYPE);
            }

            return switch (type.trim().toUpperCase()) {
                case "ACTIVITY" -> ACTIVITY;
                case "TIMELINE" -> TIMELINE;
                case "NOTIFICATION" -> NOTIFICATION;
                case "CHART" -> CHART;
                case "MENU" -> MENU;
                default -> throw ErrorCode.get(IconErrorCodes.INVALID_TYPE, type);
            };
        }
    }

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.entity.icon.invalid.id")
    private Long id;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.icon.invalid.name")
    private String name;

    private Type type;
    private I18nKey i18nName;

    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public I18nKey getI18nName() {
        return i18nName;
    }

    public void setI18nName(final I18nKey i18nName) {
        this.i18nName = i18nName;
    }
}
