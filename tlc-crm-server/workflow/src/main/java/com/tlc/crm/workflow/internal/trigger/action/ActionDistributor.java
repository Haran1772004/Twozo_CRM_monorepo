package com.tlc.crm.workflow.internal.trigger.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;

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

import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.WAActionHandler;
import com.tlc.validator.TlcModel;

import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionHistory;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowActionExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;

import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext.ActionJsonContext;
import com.tlc.crm.workflow.internal.trigger.action.publish.ActionPublisher;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.EmailActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.action.navigate.ActionNavigator;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.CompanyActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.ContactActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.DealActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.ActivityActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.NoteActionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Serves as a dispatcher for the {@link WorkflowAction} within a workflow,
 * ensuring that each type of action is sent to its designated handler for processing.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionDistributor implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionDistributor.class);

    private final CompanyActionHandler companyActionHandler;
    private final ContactActionHandler contactActionHandler;
    private final DealActionHandler dealActionHandler;
    private final ActivityActionHandler activityActionHandler;
    private final NoteActionHandler noteActionHandler;
    private final EmailActionHandler emailActionHandler;
    private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;
    private final ActionPublisher actionPublisher;
    private final Encoder encoder;
    private final UserService userService;
    private final RolePermissionService rolePermissionService;
    private final WAActionHandler waActionHandler;

    private ActionDistributor() {
        this.companyActionHandler = CompanyActionHandler.getInstance();
        this.contactActionHandler = ContactActionHandler.getInstance();
        this.dealActionHandler = DealActionHandler.getInstance();
        this.activityActionHandler = ActivityActionHandler.getInstance();
        this.noteActionHandler = NoteActionHandler.getInstance();
        this.emailActionHandler = EmailActionHandler.getInstance();
        this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        this.actionPublisher = ActionPublisher.getInstance();
        this.encoder = Base64.getEncoder();
        this.userService = UserService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.waActionHandler = WAActionHandler.getInstance();
    }

    private static class Instance {
        private static final ActionDistributor INSTANCE = new ActionDistributor();
    }

    /**
     * Gets the instance of {@link ActionDistributor}.
     *
     * @return {@link ActionDistributor} instance.
     */
    public static ActionDistributor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Dispatches the {@link WorkflowAction} to its designated handler for processing.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     */
    public void distribute(final Long orgId, final Long userId, final ActionExecutionContext actionExecutionContext) {

        secureProcess(orgId, userId, () -> {
            final ActionNavigator actionNavigator = actionExecutionContext.navigator();
            final WorkflowAction workflowAction = actionNavigator.next();

            if (null != workflowAction) {
                final WorkflowActionExecutionHistory workflowActionExecutionHistory = new WorkflowActionExecutionHistory();
                final Long actionId = workflowAction.getId();
                final Long executionHistoryId = actionExecutionContext.executionHistoryId();
                final ActionFailureCauseTracker actionFailureCauseTracker = actionExecutionContext.tracker();
                final String actionSourceAliasName = workflowAction.getTriggerActionSource().getAliasName();

                workflowActionExecutionHistory.setActionId(actionId);
                workflowActionExecutionHistory.setOrgId(orgId);
                workflowActionExecutionHistory.setExecutionHistoryId(executionHistoryId);
                workflowActionExecutionHistory.setExecutedTime(Instant.now().toEpochMilli());

                try {
                    final TlcModel tlcModel = switch (actionSourceAliasName) {
                        case "company" ->
                                executeCompanyRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "contact" ->
                                executeContactRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "deal" -> executeDealRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "activity" ->
                                executeActivityRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "note" -> executeNoteRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "email" -> executeEmailRelatedActions(orgId, userId, workflowAction, actionExecutionContext);
                        case "whatsapp" -> executeWhatsappRelatedActions(orgId, userId, workflowAction, actionExecutionContext, workflowActionExecutionHistory);
                        default -> null;
                    };

                    if (null == tlcModel) {
                        workflowActionExecutionHistory.setActionExecutionStatus(WorkflowActionExecutionStatus.FAILURE);
                        workflowActionExecutionHistory.setFailureCause(actionFailureCauseTracker.getFailureCause(actionId));

                        workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                    } else {
                        workflowActionExecutionHistory.setActionExecutionStatus(WorkflowActionExecutionStatus.SUCCESS);
                        actionExecutionContext.addCreatedEntity(Pair.of(actionSourceAliasName, tlcModel));

                        if (actionNavigator.hasNext()) {
                            final ActionJsonContext actionJsonContext = actionExecutionContext.actionJsonContext();

                            actionJsonContext.removeWorkflowActionId(actionId);
                            actionJsonContext.addCreatedModelJson(Pair.of(actionSourceAliasName, tlcModel));

                            actionPublisher.publish(encoder.encodeToString(String.format("%s:%s:%s:%s", orgId, userId, executionHistoryId, actionId).getBytes()),
                                    actionExecutionContext.actionJsonContext().get());
                        } else {
                            workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.SUCCESS);
                        }
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Workflow Execution: Action execution was interrupted for the workflow action: {}, Reason: {}, Stack: ", actionId, throwable.getMessage(), throwable);

                    if (!actionFailureCauseTracker.hasErrors()) {
                        actionFailureCauseTracker.track(actionId, throwable instanceof ErrorCode
                                ? WorkflowActionExecutionFailureCause.DB_OPERATION_FAILED : WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR);
                    }

                    workflowActionExecutionHistory.setFailureCause(actionFailureCauseTracker.getFailureCause(actionId));
                    workflowActionExecutionHistory.setActionExecutionStatus(WorkflowActionExecutionStatus.FAILURE);
                    workflowExecutionHistoryManager.updateExecutionHistoryStatus(orgId, executionHistoryId, WorkflowExecutionStatus.FAILURE);
                } finally {
                    workflowExecutionHistoryManager.createActionExecutionHistory(workflowActionExecutionHistory);
                }
            }
        });
    }

    /**
     * Executes the company related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeCompanyRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                                  final ActionExecutionContext actionExecutionContext) {
        return switch (workflowAction.getTriggerActionEvent().getAliasName()) {
            case "companyCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.COMPANIES, List.of(Action.Type.CREATE));

                yield companyActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            case "companyUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.COMPANIES, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield companyActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            default -> null;
        };
    }

    /**
     * Executes the contact related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeContactRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                                  final ActionExecutionContext actionExecutionContext) {
        return switch (workflowAction.getTriggerActionEvent().getAliasName()) {
            case "contactCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.CONTACTS, List.of(Action.Type.CREATE));

                yield contactActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            case "contactUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.CONTACTS, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield contactActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            default -> null;
        };
    }

    /**
     * Executes the deal related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeDealRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                               final ActionExecutionContext actionExecutionContext) {
        return switch (workflowAction.getTriggerActionEvent().getAliasName()) {
            case "dealCreate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.CREATE));

                yield dealActionHandler.create(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            case "dealUpdate" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.EDIT, Action.Type.VIEW));

                yield dealActionHandler.update(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            case "duplicateExistingDeal" -> {
                final boolean hasPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.VIEW, Action.Type.CREATE));

                yield dealActionHandler.duplicateExistingDeal(orgId, hasPermission ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            case "addProductToDeal" -> {
                final boolean hasDealPermission = hasPermission(orgId, SubCategory.Type.DEALS, List.of(Action.Type.VIEW, Action.Type.EDIT));
                final boolean hasProductPermission = hasPermission(orgId, SubCategory.Type.PRODUCTS, List.of(Action.Type.CREATE, Action.Type.EDIT, Action.Type.VIEW));

                yield dealActionHandler.addProductToDeal(orgId, (hasDealPermission && hasProductPermission) ? userId : switchToAdminContextAndGetUserId(orgId), workflowAction, actionExecutionContext);
            }
            default -> null;
        };
    }

    /**
     * Executes the activity related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeActivityRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                                   final ActionExecutionContext actionExecutionContext) {
        return switch (workflowAction.getTriggerActionEvent().getAliasName()) {
            case "activityCreate" ->
                    activityActionHandler.create(orgId, userId, workflowAction, actionExecutionContext);
            case "activityUpdate" ->
                    activityActionHandler.update(orgId, userId, workflowAction, actionExecutionContext);
            default -> null;
        };
    }

    /**
     * Executes the note related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeNoteRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                               final ActionExecutionContext actionExecutionContext) {
        return switch (workflowAction.getTriggerActionEvent().getAliasName()) {
            case "addNoteToContact" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.CONTACT, workflowAction, actionExecutionContext);
            case "addNoteToCompany" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.COMPANY, workflowAction, actionExecutionContext);
            case "addNoteToDeal" ->
                    noteActionHandler.mapRelatedSource(orgId, userId, Source.DEAL, workflowAction, actionExecutionContext);
            default -> null;
        };
    }

    /**
     * Executes the email related workflow actions to their respective handlers.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeEmailRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                                final ActionExecutionContext actionExecutionContext) {
        return emailActionHandler.sendMail(orgId, userId, workflowAction, actionExecutionContext);
    }

    /**
     * Executes the whatsapp related workflow actions
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link TlcModel}.
     */
    private TlcModel executeWhatsappRelatedActions(final Long orgId, final Long userId, final WorkflowAction workflowAction,
                                                final ActionExecutionContext actionExecutionContext, final WorkflowActionExecutionHistory workflowActionExecutionHistory) {
        return waActionHandler.sendMessage(orgId, userId, workflowAction, actionExecutionContext, workflowActionExecutionHistory);
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
