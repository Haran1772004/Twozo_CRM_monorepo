package com.tlc.crm.core.features.invoice.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * <p>
 * Resolves invoice UI action constraints (edit / clone) from status and linked-entity access.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceService {

    public static final String I18N_EDIT_DISABLED_ENTITY_ACCESS = "i18n.invoice.action.edit.disabled.entity.access";
    public static final String I18N_EDIT_DISABLED_PUBLISHED = "i18n.invoice.action.edit.disabled.published";
    public static final String I18N_EDIT_DISABLED_OPENED = "i18n.invoice.action.edit.disabled.opened";
    public static final String I18N_CLONE_DISABLED_ENTITY_ACCESS = "i18n.invoice.action.clone.disabled.entity.access";

    private final UserService userService;
    private final I18nService i18nService;

    private static class Instance {
        private static final InvoiceService INSTANCE = new InvoiceService();
    }

    private InvoiceService() {
        this.userService = UserService.getInstance();
        this.i18nService = I18nServiceProvider.get();
    }

    public static InvoiceService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Determines whether the user is missing access to at least one linked contact, company,
     * deal, or product referenced by the invoice.
     * </p>
     *
     * @param orgId             Represents the organization ID.
     * @param userId            Represents the ID of the user whose access is being checked.
     * @param roleId            Represents the role ID used to resolve permission scope.
     * @param entityIdsBySource Represents the linked entity IDs grouped by their source type.
     * @return {@code true} when the user cannot access at least one linked entity, {@code false} otherwise.
     */
    public boolean isEntityAccessRestricted(final long orgId, final long userId, final long roleId,
                                            final Map<Source, Collection<Long>> entityIdsBySource) {
        final EntityPermissionValidator validator = new EntityPermissionValidator(orgId, userId, roleId);
        final RolePermissionService rolePermissionService = RolePermissionService.getInstance();

        if (Objects.nonNull(entityIdsBySource)) {
            for (final Map.Entry<Source, Collection<Long>> entry : entityIdsBySource.entrySet()) {
                final Source source = entry.getKey();
                final Collection<Long> entityIds = entry.getValue();

                if (Objects.isNull(source) || Objects.isNull(entityIds) || entityIds.isEmpty()) {
                    continue;
                }
                final StoreType storeType = storeTypeFor(source);

                if (Objects.isNull(storeType)) {
                    continue;
                }
                final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(source.getName());
                final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES,
                        subCategoryType, Action.Type.ACCESS);

                if (Objects.isNull(permission) || !validator.validate(Set.copyOf(entityIds), storeType, permission.getScope(), false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * <p>
     * Resolves the i18n key explaining why edit is disabled, based on invoice status and
     * linked-entity access restriction.
     * </p>
     *
     * @param status                 Represents the current status of the invoice.
     * @param entityAccessRestricted Represents whether access to a linked entity is restricted.
     * @return An {@link Optional} containing the i18n key when edit is disabled, or empty when edit is allowed.
     */
    public Optional<String> resolveEditDisabledReasonI18nKey(final Invoice.Status status, final boolean entityAccessRestricted) {
        if (Objects.nonNull(status)) {
            if (Invoice.Status.PUBLISHED == status) {
                return Optional.of(I18N_EDIT_DISABLED_PUBLISHED);
            }

            if (Invoice.Status.VIEWED == status) {
                return Optional.of(I18N_EDIT_DISABLED_OPENED);
            }

            if (Invoice.Status.VOIDED == status) {
                return Optional.of(I18N_EDIT_DISABLED_PUBLISHED);
            }
        }

        if (entityAccessRestricted) {
            return Optional.of(I18N_EDIT_DISABLED_ENTITY_ACCESS);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Resolves the i18n key explaining why clone is disabled, based on linked-entity access restriction.
     * </p>
     *
     * @param entityAccessRestricted Represents whether access to a linked entity is restricted.
     * @return An {@link Optional} containing the i18n key when clone is disabled, or empty when clone is allowed.
     */
    public Optional<String> resolveCloneDisabledReasonI18nKey(final boolean entityAccessRestricted) {
        if (entityAccessRestricted) {
            return Optional.of(I18N_CLONE_DISABLED_ENTITY_ACCESS);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Resolves localized text for the given i18n key, using the logged-in user's configured
     * regional language preference.
     * </p>
     *
     * @param key Represents the i18n key to resolve.
     * @return The localized text for the key, resolved against the logged-in user's language preference.
     */
    public String resolveI18nValue(final String key) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = userService.getUser(crmUser.orgId(), crmUser.id())
                .getPreferenceHolder().get(UserRegionalPreference.TYPE);

        return i18nService.getResolver(Locale.forLanguageTag(userRegionalPreference.getLanguage().getName())).get(key);
    }

    /**
     * <p>
     * Appends action constraint fields, including edit/clone restriction flags and their localized
     * reason messages, to a JSON invoice payload.
     * </p>
     *
     * @param json                   Represents the JSON invoice payload to append fields to.
     * @param status                 Represents the current status of the invoice.
     * @param entityAccessRestricted Represents whether access to a linked entity is restricted.
     */
    public void appendActionConstraints(final JsonObject json, final Invoice.Status status, final boolean entityAccessRestricted) {
        putActionConstraints(json::put, status, entityAccessRestricted);
    }

    /**
     * <p>
     * Appends action constraint fields, including edit/clone restriction flags and their localized
     * reason messages, to a list row map.
     * </p>
     *
     * @param row                    Represents the list row map to append fields to.
     * @param status                 Represents the current status of the invoice.
     * @param entityAccessRestricted Represents whether access to a linked entity is restricted.
     */
    public void appendActionConstraints(final Map<String, Object> row, final Invoice.Status status,
                                        final boolean entityAccessRestricted) {
        putActionConstraints(row::put, status, entityAccessRestricted);
    }

    /**
     * <p>
     * Resolves the edit/clone restriction flags and their localized reason messages, then writes
     * them into the destination via the given field-setter.
     * </p>
     *
     * @param put                    Represents the field-setter used to write the resolved values.
     * @param status                 Represents the current status of the invoice.
     * @param entityAccessRestricted Represents whether access to a linked entity is restricted.
     */
    private void putActionConstraints(final BiConsumer<String, Object> put, final Invoice.Status status, final boolean entityAccessRestricted) {
        final Optional<String> editReasonKey = resolveEditDisabledReasonI18nKey(status, entityAccessRestricted);

        put.accept(InvoiceFields.EDIT_RESTRICTED, editReasonKey.isPresent());
        editReasonKey.ifPresent(key -> put.accept(InvoiceFields.EDIT_RESTRICTED_INFO, resolveI18nValue(key)));

        final Optional<String> cloneReasonKey = resolveCloneDisabledReasonI18nKey(entityAccessRestricted);

        put.accept(InvoiceFields.CLONE_RESTRICTED, cloneReasonKey.isPresent());
        cloneReasonKey.ifPresent(key -> put.accept(InvoiceFields.CLONE_RESTRICTED_INFO, resolveI18nValue(key)));
    }

    /**
     * <p>
     * Resolves the {@link StoreType} corresponding to a given invoice {@link Source}.
     * </p>
     *
     * @param source Represents the source of the linked entity.
     * @return The corresponding {@link StoreType}, or {@code null} when the source has no mapped store type.
     */
    private StoreType storeTypeFor(final Source source) {
        return switch (source) {
            case CONTACT -> StoreType.CONTACT;
            case COMPANY -> StoreType.COMPANY;
            case DEAL -> StoreType.DEAL;
            default -> null;
        };
    }
}