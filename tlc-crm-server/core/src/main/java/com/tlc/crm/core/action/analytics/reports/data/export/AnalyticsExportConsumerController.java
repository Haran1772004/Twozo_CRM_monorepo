package com.tlc.crm.core.action.analytics.reports.data.export;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Controller class for managing the Analytics Export Consumer.
 * <p>
 * This service is responsible for scheduling a task that processes
 * analytics export requests from a queue using Kafka.
 * <p>
 * Implements {@link Service} to integrate with application lifecycle.
 *
 * @author Saravanan
 * @version 1.0
 */
public final class AnalyticsExportConsumerController implements Service 
{

    private static final Logger LOGGER = LoggerFactory.getLogger( AnalyticsExportConsumerController.class );

    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-analytics-export-request-consumer";
    private static final String THREAD_POOL_NAME = "analytics-export-pool";
    private static final String TASK_SCHEDULER_NAME = "analytics-export-processor";
    private static final String EXECUTOR_NAME = "analytics-export-executor";

    private static final int MAX_POOL_SIZE = 2;
    private static final int POLL_DURATION = 1000;
    private static final int MAX_SIZE = 10;

    private Long schedulerId;

    /**
     * Starts the analytics export consumer service.
     *
     * @param configurations the application configurations
     */
    @Override
    public void start( final Configurations configurations ) 
    {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if ( Objects.isNull( schedulerId ) && Objects.nonNull( schedulerService ) ) 
        {
            final Executor taskExecutor = ExecutorsHandler.createTaskExecutor( MAX_POOL_SIZE, EXECUTOR_NAME, false );

            this.schedulerId = schedulerService.schedule( THREAD_POOL_NAME,
                                                          new AnalyticsExportTask( new AnalyticsExportTaskController(),
                                                          taskExecutor,
                                                          configurations ) );

            LOGGER.info( "Analytics export consumer service started, Scheduler ID : {}", schedulerId );
        }
    }

    /**
     * Stops the analytics export consumer service.
     */
    @Override
    public void stop()
    {
        if ( Objects.nonNull( schedulerId ) ) 
        {
            final SchedulerService schedulerService = SchedulerServiceProvider.get();

            if ( Objects.nonNull( schedulerService ) ) 
            {
                schedulerService.removeTask( schedulerId );
                LOGGER.info( "Analytics export consumer service stopped, Scheduler ID : {}", schedulerId );
            }
        }
    }

    /**
     * Internal task responsible for consuming analytics export requests.
     */
    private static class AnalyticsExportTask extends Task 
    {
        private final BackgroundConsumer analyticsExportRequestConsumer;

        /**
         * Constructs the task with the required controller, executor, and config.
         *
         * @param controller      the task controller
         * @param executor        the thread pool executor
         * @param configurations  configuration object
         */
        private AnalyticsExportTask( final TaskController controller,
                                     final Executor executor,
                                     final Configurations configurations )
        {
            super( controller );

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig( REQUEST_CONSUMER_NAME,
                                                                           false,
                                                                           List.of( CoreQueueTopics.getAnalyticsExporterTopic( QueueModel.Type.KAFKA ) ),
                                                                           new AnalyticsExportProcessor( executor ),
                                                                           new ConsumerConfig.PollConfig( MAX_SIZE, POLL_DURATION, MAX_SIZE ),
                                                                           false );

            this.analyticsExportRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer( consumerConfig );
        }

        /**
         * Executes the analytics export consumer.
         *
         * @param taskController the task controller
         */
        @Override
        public void execute( final TaskController taskController )
        {
            analyticsExportRequestConsumer.run();
        }

        /**
         * Stops the analytics export consumer.
         *
         * @return true if stopped successfully
         */
        @Override
        public boolean stopTask()
        {
            analyticsExportRequestConsumer.stop();
            return true;
        }
    }

    /**
     * Internal task controller for scheduling analytics export tasks.
     */
    private static class AnalyticsExportTaskController extends TaskController
    {
        private AnalyticsExportTaskController()
        {
            super( TASK_SCHEDULER_NAME );
        }

        /**
         * Returns the next interval for scheduling. Zero implies immediate re-run.
         *
         * @return next interval in milliseconds
         */
        @Override
        public long getNextInterval()
        {
            return 0;
        }
    }
}
