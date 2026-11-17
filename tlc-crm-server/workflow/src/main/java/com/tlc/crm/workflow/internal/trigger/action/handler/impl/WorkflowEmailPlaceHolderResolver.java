package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.TriggerSource;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.core.notification.email.template.service.MailPlaceHolderResolver;
import com.tlc.notification.model.template.PlaceHolder;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Utility class responsible for resolving and formatting placeholders
 * used within workflow-related email templates.
 *
 * <p><b>Note:</b> This class is {@code final} and cannot be subclassed.
 * It is intended to be a static utility holder and does not maintain state.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowEmailPlaceHolderResolver {

    private final MailPlaceHolderExtractor placeHolderExtractor;
    private final MailPlaceHolderResolver placeHolderResolver;
    private final UserService crmUserService;

    private WorkflowEmailPlaceHolderResolver() {
        this.placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
        this.placeHolderResolver = MailPlaceHolderResolver.getInstance();
        this.crmUserService = UserService.getInstance();
    }

    private static class Instance {
        private static final WorkflowEmailPlaceHolderResolver WORKFLOW_EMAIL_PLACE_HOLDER_RESOLVER = new WorkflowEmailPlaceHolderResolver();
    }

    /**
     * Gets the instance of {@code WorkflowEmailPlaceHolderResolver}.
     *
     * @return The instance of {@code WorkflowEmailPlaceHolderResolver}.
     */
    public static WorkflowEmailPlaceHolderResolver getInstance() {
        return Instance.WORKFLOW_EMAIL_PLACE_HOLDER_RESOLVER;
    }

    /**
     * Resolves and replaces workflow email placeholders for the given {@link EntityThread},
     * using contextual information provided by the {@link TriggerSource} and {@link TlcModel}.
     * <p>
     * This method inspects the provided thread and trigger/model pair to populate
     * placeholder values (such as {@code {source}} or {@code {fieldId}})
     * in the workflow email templates.
     *
     * @param thread                 The entity thread in which placeholders should be resolved;
     *                               typically represents the workflow or process instance context.
     * @param triggerSourceModelPair a pair containing:
     *                               <ul>
     *                                 <li>{@link TriggerSource} – describes the origin of the workflow event
     *                                     (e.g., CRM, external API, or system event)</li>
     *                                 <li>{@link TlcModel} – the domain model carrying the actual data
     *                                     used for replacing placeholders</li>
     *                               </ul>
     */
    public void resolve(final EntityThread thread, final Pair<TriggerSource, TlcModel> triggerSourceModelPair) {
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final EntityMailTemplate template = new EntityMailTemplate();
            final long orgId = thread.orgId();

            template.setOrgId(orgId);
            template.setSubject(mail.getSubject());
            template.setContent(mail.getContent());
            template.setSource(Source.get(triggerSourceModelPair.getKey().getName()));
            template.setEntityId(triggerSourceModelPair.getValue().id());

            if (template.hasPlaceHolders()) {
                final Collection<MailPlaceHolder> placeHolders = placeHolderExtractor.getPlaceHolders(template);

                if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {
                    final Collection<PlaceHolder> placeHolderList = new HashSet<>();

                    for (final MailPlaceHolder placeHolder : placeHolders) {

                        if (placeHolder.isOwnerField()) {
                            final Long iamUserId = placeHolder.getLong();

                            if (Objects.nonNull(iamUserId)) {
                                final Long userId = crmUserService.getUserId(orgId, iamUserId);

                                placeHolder.setValue(crmUserService.getUser(orgId, userId).getFullName());
                            }
                        }
                        placeHolderList.add(placeHolder);
                    }
                    mail.setPlaceHolders(placeHolderList);
                    resolvePlaceHolders(thread);
                } else {
                    resolvePlaceHolders(thread);
                }
            }
        }
    }

    /**
     * Resolves and replaces workflow email placeholders for the given {@link EntityThread}.
     *
     * @param thread Refers the {@code EntityThread}.
     */
    private void resolvePlaceHolders(final EntityThread thread) {
        try {
            final EntityEmail mail = thread.getComposedEmail();
            final String subject = mail.getSubject();
            final String content = mail.getContent();
            final Collection<PlaceHolder> placeHolders = mail.placeHolders();

            if (Objects.nonNull(subject)) {
                mail.setSubject(placeHolderResolver.resolve(subject,
                        placeHolderExtractor.getSubjectPlaceHolders(placeHolders)));
            }

            if (Objects.nonNull(content)) {
                mail.setContent(placeHolderResolver.resolve(content,
                        placeHolderExtractor.getContentPlaceHolders(placeHolders)));
            }
        } catch (Exception exception) {
            throw ErrorCode.get(WorkflowErrorCodes.FAILED_TO_RESOLVE_EMAIL_PLACEHOLDER);
        }
    }

}
