package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes the actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class ActionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionExecutor.class);

    private final CompanyActionHandlerV2 companyActionHandler;
    private final ContactActionHandlerV2 contactActionHandler;
    private final DealActionHandlerV2 dealActionHandler;
    private final ActivityActionHandlerV2 activityActionHandler;
    private final NoteActionHandlerV2 noteActionHandler;
    private final EmailActionHandlerV2 emailActionHandler;
    private final UserService userService;
    private final RolePermissionService rolePermissionService;
    private final WAActionHandlerV2 waActionHandler;

    private ActionExecutor() {
        this.companyActionHandler = CompanyActionHandlerV2.getInstance();
        this.contactActionHandler = ContactActionHandlerV2.getInstance();
        this.dealActionHandler = DealActionHandlerV2.getInstance();
        this.activityActionHandler = ActivityActionHandlerV2.getInstance();
        this.noteActionHandler = NoteActionHandlerV2.getInstance();
        this.emailActionHandler = EmailActionHandlerV2.getInstance();
        this.userService = UserService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.waActionHandler = WAActionHandlerV2.getInstance();
    }

    private static class Instance {
        private static final ActionExecutor INSTANCE = new ActionExecutor();
    }

    /**
     * Gets the instance of {@link ActionExecutor}.
     *
     * @return {@link ActionExecutor} instance.
     */
    public static ActionExecutor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Executes the actions for the workflow.
     *
     * @param orgId                     Represents the organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The action outcome.
     */
    public ActionOutcome execute(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                 final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final String actionSourceAliasName = actionConfiguration.getTriggerActionSource().getAliasName();
        final Long actionId = actionConfiguration.id();

        try {
            return switch (actionSourceAliasName) {
                case "company" ->
                        toOutcome(executeCompanyRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker), actionFailureCauseTracker);
                case "contact" ->
                        toOutcome(executeContactRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker), actionFailureCauseTracker);
                case "deal" ->
                        toOutcome(executeDealRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker), actionFailureCauseTracker);
                case "activity" ->
                        toOutcome(executeActivityRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker), actionFailureCauseTracker);
                case "note" ->
                        toOutcome(executeNoteRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker), actionFailureCauseTracker);
                case "email" ->
                        executeEmailRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
                case "whatsapp" ->
                        executeWhatsappRelatedActions(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
                default -> ActionOutcome.syncFailure();
            };
        } catch (Throwable throwable) {
            LOGGER.error("Workflow Execution: Action execution was interrupted for the workflow action: {}, Reason: {}, Stack: ", actionId, throwable.getMessage(), throwable);

            if (!actionFailureCauseTracker.hasErrors()) {
                actionFailureCauseTracker.track(actionId, throwable instanceof ErrorCode
                        ? WorkflowActionExecutionFailureCause.DB_OPERATION_FAILED : WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR);
            }
            return ActionOutcome.syncFailure();
        }
    }

    /**
     * Converts the model to an action outcome.
     *
     * @param model                     The model to convert.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The action outcome.
     */
    private ActionOutcome toOutcome(final TlcModel model, final ActionFailureCauseTracker actionFailureCauseTracker) {
        if (Objects.isNull(model) || actionFailureCauseTracker.hasErrors()) {
            return ActionOutcome.syncFailure();
        }
        return ActionOutcome.syncSuccess(model);
    }

    /**
     * Executes the company related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private TlcModel executeCompanyRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                                  final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return switch (actionConfiguration.getTriggerActionEvent().getAliasName()) {
            case "companyCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.COMPANIES, List.of(Action.Type.CREATE));

                yield companyActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            case "companyUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.COMPANIES, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield companyActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            default -> null;
        };
    }

    /**
     * Executes the contact related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private TlcModel executeContactRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                                  final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return switch (actionConfiguration.getTriggerActionEvent().getAliasName()) {
            case "contactCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.CONTACTS, List.of(Action.Type.CREATE));

                yield contactActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            case "contactUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.CONTACTS, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield contactActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            default -> null;
        };
    }

    /**
     * Executes the deal related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private TlcModel executeDealRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                               final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return switch (actionConfiguration.getTriggerActionEvent().getAliasName()) {
            case "dealCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.CREATE));

                yield dealActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            case "dealUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield dealActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            case "duplicateExistingDeal" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.VIEW, Action.Type.CREATE));

                yield dealActionHandler.duplicateExistingDeal(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            case "addProductToDeal" -> {
                final boolean hasDealPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.VIEW, Action.Type.EDIT));
                final boolean hasProductPermission = hasPermission(orgId, SubCategory.Type.PRODUCTS, List.of(Action.Type.CREATE, Action.Type.EDIT, Action.Type.VIEW));

                yield dealActionHandler.addProductToDeal(orgId, (hasDealPermission && hasProductPermission) ? userId : switchToAdminContextAndGetUserId(orgId), actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            }
            default -> null;
        };
    }

    /**
     * Executes the activity related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private TlcModel executeActivityRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                                   final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return switch (actionConfiguration.getTriggerActionEvent().getAliasName()) {
            case "activityCreate" ->
                    activityActionHandler.create(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            case "activityUpdate" ->
                    activityActionHandler.update(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            default -> null;
        };
    }

    /**
     * Executes the note related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private TlcModel executeNoteRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                               final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return switch (actionConfiguration.getTriggerActionEvent().getAliasName()) {
            case "addNoteToContact" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.CONTACT, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            case "addNoteToCompany" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.COMPANY, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            case "addNoteToDeal" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.DEAL, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
            default -> null;
        };
    }

    /**
     * Executes the email related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private ActionOutcome executeEmailRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                                     final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return emailActionHandler.sendMail(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
    }

    /**
     * Executes the WhatsApp related actions for the workflow.
     *
     * @param orgId                     The organization id.
     * @param userId                    Represents the user id.
     * @param actionConfiguration       Represents the action configuration.
     * @param nodeExecutionContext      Represents the node execution context.
     * @param actionFailureCauseTracker Represents the action failure cause tracker.
     * @return The result of the action.
     */
    private ActionOutcome executeWhatsappRelatedActions(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                                        final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        return waActionHandler.sendMessage(orgId, userId, actionConfiguration, nodeExecutionContext, actionFailureCauseTracker);
    }

    /**
     * Determines whether the currently authenticated user is authorized to perform
     * all the specified actions within the given subcategory for the provided organization.
     *
     * @param orgId           The organization identifier.
     * @param subcategoryType The subcategory being accessed.
     * @param actionTypes     The actions that require authorization.
     * @return {@code true} if the user has permission for all specified actions;{@code false} otherwise.
     */
    private boolean hasPermission(final long orgId, final SubCategory.Type subcategoryType, final Collection<Action.Type> actionTypes) {
        final Map<Action.Type, RolePermission> permissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                LoggedInUserInfo.getSecurityContext().getUser().getRoleId(), Category.Type.SOURCES, subcategoryType, actionTypes);
        final long grantedPermissionCount = permissionMap.values().stream().filter(RolePermission::isGranted).count();

        return grantedPermissionCount == permissionMap.size();
    }

    /**
     * Switches the current {@link SecurityContext} to the organization’s admin user
     * and returns the corresponding admin user ID.
     *
     * <p>As a result, any subsequent authorization or business logic will execute with admin privileges.</p>
     *
     * <p><strong>Note:</strong> This method has side effects and should be used
     * with caution, as it elevates the effective security context.</p>
     *
     * @param orgId The organization identifier.
     * @return The admin user ID loaded into the security context.
     */
    private Long switchToAdminContextAndGetUserId(final Long orgId) {
        final CrmUser adminUser = userService.getAdminUser(orgId);

        SecurityContext.clear();
        SecurityContext.load(adminUser);

        return adminUser.id();
    }
}
