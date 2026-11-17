package com.tlc.queue.test;

import com.tlc.commons.executor.Executor;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import java.util.List;

public class TestManualCommitTask extends Task {

    private final BackgroundConsumer backgroundConsumer;

    public TestManualCommitTask(final String consumerName, final TaskController controller, final Executor testManualExecutor) {
        super(controller);

        final ConsumerConfig consumerConfig = new KafkaConsumerConfig("test-manual-commit", consumerName, false,
                List.of(SubscriptionTopicHolder.getInstance().getSubscriptionTopic(QueueModel.Type.KAFKA, "TEST_MANUAL_COMMIT")),
                new TestManualCommitProcessor(testManualExecutor), new ConsumerConfig.PollConfig(5, 10000, 5), false);
        this.backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
    }

    @Override
    public void execute(final TaskController taskController) {
        backgroundConsumer.run();
    }

    public static class TestManualCommitTaskController extends TaskController {

        public TestManualCommitTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return The value of next interval.
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}