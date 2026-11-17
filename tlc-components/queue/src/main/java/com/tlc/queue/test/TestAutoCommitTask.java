package com.tlc.queue.test;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import java.util.List;

public class TestAutoCommitTask extends Task {

    private final BackgroundConsumer backgroundConsumer;

    public TestAutoCommitTask(final TaskController controller) {
        super(controller);

        final ConsumerConfig consumerConfig = new KafkaConsumerConfig("tlc-queue-test-auto-commit-consumer", false,
                List.of(SubscriptionTopicHolder.getInstance().getSubscriptionTopic(QueueModel.Type.KAFKA, "TEST_AUTO_COMMIT")),
                new TestAutoCommitProcessor(), new ConsumerConfig.PollConfig(5, 10000, 5), true);
        this.backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
    }

    @Override
    public void execute(final TaskController taskController) {
        backgroundConsumer.run();
    }

    public static class TestAutoCommitTaskController extends TaskController {

        public TestAutoCommitTaskController(final String scheduleName) {
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