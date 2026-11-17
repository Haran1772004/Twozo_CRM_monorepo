package com.tlc.crm.core.mcp.tools;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.event.EventAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.mcp.CrmMcpIdentity;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.mcp.CrmMcpTool;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;

import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.crm.notification.app.Event;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.mcp.tool.McpToolContext;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import jakarta.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for secure CRM MCP tools that perform create or update operations on CRM models.
 * Provides common functionality for building request payloads, converting input data into model
 * objects, validating models, and executing actions within the authenticated user's organization context.
 *
 * @param <M> the CRM model type handled by this tool
 */
public abstract class CrmSecureMcpTool<M extends TlcModel> extends CrmMcpTool implements EventAction {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CrmSecureMcpTool.class);

    protected final ConverterConfigBuilder converterConfigBuilder;
    protected final I18nService i18nService;

    private final RolePermissionService rolePermissionService;
    private final EventDataHandler eventDataHandler;

    protected CrmSecureMcpTool() {
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.i18nService = I18nServiceProvider.get();
        this.eventDataHandler = EventDataHandler.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
    }

    /**
     * Creates one or more CRM records for the specified organization.
     *
     * @param orgId  The organization identifier.
     * @param userId The user performing the operation.
     * @param locale The user's locale.
     * @param models The models to be created.
     * @return The response containing the operation result.
     */
    public JsonObject create(final Long orgId, final Long userId, final Locale locale, final Collection<M> models) {
        final JsonObject response = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(locale);
        final ProcessOutcome processOutcome = getConfigManager().create(orgId, models);
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setLocale(locale);

        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            response.put("status", "error");
            response.put("message", i18nResolver.get("i18n.common.create.failed"));

            return response;
        }
        response.put("message", i18nResolver.get("i18n.common.create.success"));
        response.put("status", "success");
        postAction(Action.CREATE, models, eventData);

        return response;
    }

    /**
     * Updates the given entities.
     *
     * @param orgId  The organization id.
     * @param userId The user id.
     * @param models Refers the models to be updated.
     */
    public JsonObject update(final Long orgId, final Long userId, final EventData eventData, final Collection<M> models) {
        final JsonObject response = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(eventData.getLocale());

        preAction(Action.UPDATE, models, eventData);

        final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, models);
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            response.put("status", "error");
            response.put("message", i18nResolver.get("i18n.common.update.failed"));

            return response;
        }
        response.put("message", i18nResolver.get("i18n.common.update.success"));
        response.put("status", "success");
        postAction(Action.UPDATE, models, eventData);

        return response;
    }

    /**
     * Deletes the given entities.
     *
     * @param orgId  The organization id.
     * @param userId The user id.
     * @param models Refers the models to be updated.
     */
    public JsonObject delete(final Long orgId, final Long userId, final EventData eventData, final Collection<M> models) {
        final JsonObject response = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(eventData.getLocale());

        try {
            preAction(Action.DELETE, models, eventData);
            getConfigManager().bulkDelete(orgId, models);
            postAction(Action.DELETE, models, eventData);
        } catch (ErrorCode errorCode) {
            response.put("status", "error");
            response.put("message", i18nResolver.get("i18n.common.delete.failed"));

            return response;
        }
        response.put("message", i18nResolver.get("i18n.common.delete.success"));
        response.put("status", "success");

        return response;
    }

    /**
     * Builds a JSON request object from the provided input payload.
     *
     * @param input The tool input containing system and custom field values.
     * @return The constructed JSON request object.
     */
    protected final JsonObject buildRequestJson(final Map<String, Object> input) {
        return Json.object(input);
    }

    /**
     * Determines whether the currently authenticated user is authorized to perform
     * all the specified actions within the given subcategory for the provided organization.
     *
     * @param ctx             Refers to the MCP tool context containing the user's identity and other relevant information.
     * @param subcategoryType The subcategory being accessed.
     * @param actionTypes     The actions that require authorization.
     * @return {@code true} if the user has permission for all specified actions;{@code false} otherwise.
     */
    protected final boolean hasPermission(final McpToolContext<CrmMcpIdentity> ctx, final SubCategory.Type subcategoryType, final Collection<Type> actionTypes) {
        final Long orgId = ctx.identity().crmUser().orgId();
        final Map<com.tlc.crm.security.permission.model.Action.Type, RolePermission> permissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                LoggedInUserInfo.getSecurityContext().getUser().getRoleId(), Category.Type.SOURCES, subcategoryType, actionTypes);
        final long grantedPermissionCount = permissionMap.values().stream().filter(RolePermission::isGranted).count();

        return grantedPermissionCount == permissionMap.size();
    }

    /**
     * Checks whether the specified source is enabled for the given MCP identity.
     *
     * @param mcpIdentity The MCP identity containing the source configuration
     * @param source      The source whose availability is to be verified
     * @return {@code true} if the source is enabled and can be used; {@code false} otherwise
     */
    protected final boolean isSourceEnabled(final CrmMcpIdentity mcpIdentity, final Source source) {
        return SourceSettingsService.getInstance().isSourceEnabled(mcpIdentity.crmUser().orgId(), source.getName());
    }

    /**
     * <p>
     * Executes post action for the given action and models, {@link EventData} and triggering the POST event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void postAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.POST);
    }

    /**
     * <p>
     * Executes pre action for the given action and models, {@link EventData} and triggering the PRE event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void preAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.PRE);
    }

    /**
     * Validates the supplied models using the specified validation groups.
     *
     * @param models          The models to validate.
     * @param validatorGroups The bean validation groups to apply.
     * @return The collection of validation errors, or an empty collection if validation succeeds.
     */
    protected final Collection<ErrorDescriptor> validate(final Collection<M> models, final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<M>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * Returns the {@link ConfigManager} instance responsible for managing configuration.
     *
     * @return The configuration manager for the type {@code T}.
     */
    protected abstract ConfigManager<M> getConfigManager();

    /**
     * Converts the complete input JSON into a CRM model.
     *
     * @param crmUser      The authenticated CRM user.
     * @param jsonObject   The input JSON payload.
     * @param i18nResolver The internationalization resolver.
     * @return The converted CRM model.
     */
    protected M convert(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        return null;
    }

    /**
     * Converts a partial input JSON into a CRM model. Intended for operations where only a subset
     * of fields is provided, such as partial updates.
     *
     * @param crmUser      The authenticated CRM user.
     * @param jsonObject   The partial input JSON payload.
     * @param i18nResolver The internationalization resolver.
     * @return The partially populated CRM model.
     */
    protected M convertPartialData(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(events, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param event     Represents the {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvent(final Event event, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvent(event, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvents(final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(eventData, eventType);
    }

}