package com.tlc.crm.plan.and.billing.internal.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.plan.and.billing.api.models.CRMPayerLocalDetails;
import com.tlc.crm.plan.and.billing.fields.PaymentFields;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;
import com.tlc.crm.plan.and.billing.queue.SubscriptionQueueTopics;

import com.tlc.iam.common.models.User;

import com.tlc.l10n.model.number.PhoneNumber;
import com.tlc.l10n.model.number.PhoneNumberUtil;

import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.service.PaymentService;
import com.tlc.payment.gateway.service.provider.PaymentServiceProvider;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A listener that is responsible to trigger and control the payer create consumer actions {@link PayerCreateEventHandler}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PayerCreateEventListener implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayerCreateEventListener.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-plan-and-billings-payer-create-consumer";
    private static final String THREAD_POOL_NAME = "payer-create-pool";
    private static final String TASK_SCHEDULER_NAME = "payer-create";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public PayerCreateEventListener() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of payer creation
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new PayerCreateTask(new PayerCreationTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("Payer creation service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Payer creation service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to create payer in the provider.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    private static class PayerCreateTask extends Task {

        private final BackgroundConsumer payerCreateConsumer;

        private PayerCreateTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(SubscriptionQueueTopics.getPayerCreateEventTopic(QueueModel.Type.KAFKA)), new PayerCreateEventHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            payerCreateConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            payerCreateConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            payerCreateConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link PayerCreateTask}.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    private static class PayerCreationTaskController extends TaskController {

        private PayerCreationTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }

    /**
     * <p>
     * An event handler that process the payer create requests and triggers the payer create action based on the request parameters
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    private static class PayerCreateEventHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(PayerCreateEventHandler.class);

        private final PaymentService paymentService;
        private final OrganizationService organizationService;
        private final UserService crmUserService;
        private final com.tlc.iam.client.UserService userService;

        private PayerCreateEventHandler() {
            paymentService = PaymentServiceProvider.get();
            organizationService = OrganizationService.getInstance();
            crmUserService = UserService.getInstance();
            userService = com.tlc.iam.client.UserService.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param streamMessage Represents the {@link StreamMessage}
         */
        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final long orgId = requestParameters.getLong(PaymentFields.ORG_ID);
            final JsonArray userIds = requestParameters.getJsonArray(PaymentFields.USER_IDS);
            final Collection<CrmUser> crmUsers = new ArrayList<>();
            final Collection<Long> iamUserIds = new HashSet<>();
            final int size = userIds.size();

            for (int index = 0; index < size; index++) {
                final long userId = userIds.getLong(index);
                final CrmUser crmUser = crmUserService.getUser(orgId, userId);

                crmUsers.add(crmUser);
                iamUserIds.add(crmUser.getIamId());
            }

            if (iamUserIds.isEmpty()) {
                return;
            }

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (final CrmUser crmUser : crmUsers) {
                        final User user = userMap.get(crmUser.getIamId());

                        crmUser.setUser(user);
                    }
                    createPayer(orgId, crmUsers);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LoggerUtil.logError(LOGGER, ErrorCode.get(SubscriptionErrorCodes.USER_RETRIEVAL_FAILED, throwable), "i18n.common.user.retrieval.failed");
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }

        /**
         * <p>
         * Creates the payer in the payment gateway provider.
         * </p>
         *
         * @param orgId    Represents the organisation ID.
         * @param crmUsers Holds the details of CRM user.
         */
        private void createPayer(final long orgId, final Collection<CrmUser> crmUsers) {

            for (final CrmUser crmUser : crmUsers) {
                final Payer payer = new Payer();
                final Long userId = crmUser.id();
                final PhoneNumber phoneNumber = PhoneNumberUtil.parse(crmUser.getMobile());

                payer.setFirstName(crmUser.getFirstName());
                payer.setLastName(crmUser.getLastName());
                payer.setEmail(crmUser.getEmail());
                payer.setPhone(phoneNumber.getNumber());
                payer.setPhoneCountryCode(phoneNumber.getCountry().getCode());
                payer.setLocalDetails(new CRMPayerLocalDetails(orgId, userId));

                try {
                    paymentService.createPayer(payer);
                    LOGGER.info("Payment Gateway: Payer created Successfully, Org ID: {}, User ID: {}, Reference ID: {}", orgId, userId, payer.getRefenceId());
                } catch (final Exception exception) {
                    LOGGER.warn("Payment Gateway: Payer creation failed", exception);
                }
            }
        }
    }
}