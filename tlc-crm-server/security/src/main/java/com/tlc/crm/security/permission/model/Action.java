package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.i18n.I18nKey;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * <p>
 * A model that represents the action data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Action implements Serializable {

    private Long id;
    private final Type type;
    private SubCategory subCategory;
    private I18nKey displayName;
    private Requisites requisites;
    private Collection<Dependency> dependencies;

    public Action(final String name) {
        this(Type.get(name));
    }

    public Action(final Type type) {
        this.type = type;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return type.name;
    }

    public Type getType() {
        return type;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public void setSubCategory(final SubCategory subCategory) {
        this.subCategory = subCategory;
    }

    public SubCategory getSubCategory() {
        return subCategory;
    }

    public Requisites getRequisites() {
        return requisites;
    }

    public void setRequisites(final Requisites requisites) {
        this.requisites = requisites;
    }

    public void setRequisites(final String requisites) {
        this.requisites = new Requisites(Json.object(requisites));
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(final Dependency dependency) {
        if (Objects.isNull(dependencies)) {
            dependencies = new HashSet<>();
        }
        dependencies.add(dependency);
    }

    public void setDependencies(final Collection<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * <p>
     * Provides a flag that indicates whether the action type has scope or not.
     * </p>
     *
     * @return A flag that indicates whether the action type has scope or not.
     */
    public boolean hasScope() {
        return switch (type) {
            case ACCESS, VIEW, CREATE, EDIT, DELETE -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Provides a copy of the current {@link Action}.
     * </p>
     *
     * @return Copied {@link Action}.
     */
    public Action copy() {
        final Action action = new Action(type);

        action.setId(id);
        action.setDisplayName(displayName);
        action.setRequisites(requisites);
        action.setDependencies(dependencies);

        if (Objects.nonNull(subCategory)) {
            action.setSubCategory(subCategory.copy());
        }
        return action;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean equals(final Object action) {
        return this == action || (action instanceof Action && hashCode() == action.hashCode());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, type, subCategory);
    }

    /**
     * <p>
     * A model that represents the dependencies of the action.
     * </p>
     */
    public static class Dependency implements Serializable {

        private final Action action;
        private final Map<String, Map<Short, Requisites>> requisitesMap;

        public Dependency(final Action action) {
            this(action, null);
        }

        public Dependency(final String requisites) {
            this(null, requisites);
        }

        public Dependency(final Action action, final String requisitesAsString) {
            final JsonArray requisites = Objects.isNull(requisitesAsString) ? Json.array() : Json.array(requisitesAsString);
            final String orderKey = RequisitesAttributes.ORDER.getName();
            final String typeKey = RequisitesAttributes.TYPE.getName();
            final int requisitesSize = requisites.size();

            this.action = action;
            this.requisitesMap = new HashMap<>();

            for (int index = 0; index < requisitesSize; index++) {
                final JsonObject requisite = requisites.getJsonObject(index);

                if (requisite.containsKey(orderKey) && requisite.containsKey(typeKey)) {
                    final Map<Short, Requisites> typeRequisites = requisitesMap.computeIfAbsent(requisite.getString(typeKey), type -> new TreeMap<>());

                    typeRequisites.put(requisite.getShort(orderKey), new Requisites(requisite));
                }
            }
        }

        /**
         * <p>
         * Provides the dependent {@link Action} of the current action.
         * </p>
         *
         * @return Dependent {@link Action}.
         */
        public Action getDependentAction() {
            return action;
        }

        /**
         * <p>
         * Provides the {@link Requisites} of the field.
         * </p>
         *
         * @return Field {@link Requisites}.
         */
        public Collection<Requisites> getFieldRequisites() {
            final Map<Short, Requisites> fieldRequisites = requisitesMap.get(RequisitesAttributes.FIELD.getName());

            return Objects.isNull(fieldRequisites) ? Collections.emptyList() : fieldRequisites.values();
        }

        /**
         * <p>
         * Provides the {@link Requisites} of the subtype.
         * </p>
         *
         * @param dependsOnValue Represents the dependent value.
         * @return Subtype {@link Requisites}.
         */
        public Optional<Requisites> getSubTypeRequisites(final String dependsOnValue) {
            final Map<Short, Requisites> subTypeRequisites = requisitesMap.get(RequisitesAttributes.SUB_TYPE.getName());

            return Objects.isNull(subTypeRequisites) ? Optional.empty()
                    : subTypeRequisites.values().stream().filter(requisites -> Objects.equals(dependsOnValue, requisites.getDependsOn())).findFirst();
        }
    }

    /**
     * <p>
     * Provides the constant value of the actions.
     * </p>
     */
    public enum Type {

        ACCESS("access"),
        VIEW("view"),
        CREATE("create"),
        EDIT("edit"),
        DELETE("delete"),
        ASSIGN_RECORDS("assignRecords", EDIT),
        BULK_ASSIGN_RECORDS("bulkAssignRecords", EDIT),
        CLONE_RECORDS("cloneRecords", CREATE),
        BULK_UPDATE_RECORDS("bulkUpdateRecords", EDIT),
        RESTORE_RECORDS("restoreRecords"),
        MERGE_RECORDS("mergeRecords", EDIT),
        EXPORT_RECORDS("exportRecords", VIEW),
        ACCESS_VIEW("accessView", VIEW),
        SHARE_VIEW("shareView", VIEW),
        IMPORT_RECORDS("importRecords"),
        ACCESS_RECYCLE_BIN("accessRecycleBin"),
        EVERYONE("everyone"),
        MY_TEAMS("myTeams"),
        MY_TERRITORIES("myTerritories"),
        COMMON("common"),
        SUBMIT("submit", true),
        EXECUTE("execute", true),
        MANAGE_FOLLOWERS("manageFollowers"),
        ;

        private final String name;
        private final Type baseType;
        private final boolean isPrivate;

        Type(final String name) {
            this(name, null);
        }

        Type(final String name, final Type baseType) {
            this(name, baseType, false);
        }

        Type(final String name, final boolean isPrivate) {
            this(name, null, isPrivate);
        }

        Type(final String name, final Type baseType, final boolean isPrivate) {
            this.name = name;
            this.baseType = Objects.isNull(baseType) ? this : baseType;
            this.isPrivate = isPrivate;
        }

        /**
         * <p>
         * Provides the name of the {@link Action}.
         * </p>
         *
         * @return Name of the {@link Action}.
         */
        public String getName() {
            return name;
        }

        /**
         * <p>
         * Provides the base type of the {@link Action}.
         * </p>
         *
         * @return Base type of the {@link Action}.
         */
        public Type getBaseType() {
            return baseType;
        }

        /**
         * <p>
         * Provides a flag that indicates whether the action is primary or not.
         * </p>
         *
         * @return Primary action.
         */
        public boolean isPrimaryAction() {
            return ACCESS == this;
        }

        /**
         * <p>
         * Provides the {@link Type} for the given name.
         * </p>
         *
         * @param name Represents the action name.
         * @return {@link Type} holds the details of the action.
         */
        public static Type get(final String name) {
            return switch (name.toLowerCase()) {
                case "access" -> ACCESS;
                case "view" -> VIEW;
                case "create" -> CREATE;
                case "edit" -> EDIT;
                case "delete" -> DELETE;
                case "assignrecords" -> ASSIGN_RECORDS;
                case "bulkassignrecords" -> BULK_ASSIGN_RECORDS;
                case "clonerecords" -> CLONE_RECORDS;
                case "bulkupdaterecords" -> BULK_UPDATE_RECORDS;
                case "restorerecords" -> RESTORE_RECORDS;
                case "mergerecords" -> MERGE_RECORDS;
                case "exportrecords" -> EXPORT_RECORDS;
                case "accessview" -> ACCESS_VIEW;
                case "shareview" -> SHARE_VIEW;
                case "importrecords" -> IMPORT_RECORDS;
                case "accessrecyclebin" -> ACCESS_RECYCLE_BIN;
                case "everyone" -> EVERYONE;
                case "myteams" -> MY_TEAMS;
                case "myterritories" -> MY_TERRITORIES;
                case "submit" -> SUBMIT;
                case "execute" -> EXECUTE;
                case "managefollowers" -> MANAGE_FOLLOWERS;
                default -> throw ErrorCode.get(PermissionErrorCodes.ACTION_NOT_FOUND, name);
            };
        }

        /**
         * <p>
         * Determines whether the current action requires ID validation.
         * </p>
         *
         * @return true if the action requires ID validation such as editing, viewing, or deleting; otherwise, returns false.
         */
        public boolean requiresIdValidation() {
            return switch (this) {
                case EDIT, VIEW, DELETE -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Provides a flag that indicates whether the action requires dependency validation.
         * </p>
         *
         * @return true if the action requires dependency validation for actions such as creation or editing; otherwise, returns false.
         */
        public boolean requiresDependencyValidation() {
            return switch (this) {
                case CREATE, EDIT -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks if the current type type is crud action
         * </p>
         *
         * @return ture if the action represents the CRUD operation; otherwise, returns false.
         */
        public boolean isCrudAction() {
            return switch (this) {
                case CREATE, EDIT, VIEW, DELETE -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Provides a flag that indicates whether this action represents the CRUD operation or not.
         * </p>
         *
         * @return ture if the action represents the CRUD operation; otherwise, returns false.
         */
        public boolean isBasicAction() {
            return switch (this) {
                case ACCESS, VIEW, CREATE, EDIT, DELETE -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Provides a flag that indicates whether this action is private or not
         * </p>
         *
         * @return ture if the action is private or not
         */
        public boolean isPrivate() {
            return isPrivate;
        }

        public boolean isBulk() {
            return switch (this) {
                case BULK_ASSIGN_RECORDS, BULK_UPDATE_RECORDS -> true;
                default -> false;
            };
        }
    }
}
