package com.tlc.crm.core.action.analytics.internal;

import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.trash.CRMDashboardTrashManager;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A service that is responsible to trigger and control the dashboard trash related actions
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public class DashboardTrashController implements Service
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DashboardTrashController.class );
    private static final String THREAD_POOL_NAME = "dashboard-trash-pool";
    private static final String TASK_SCHEDULER_NAME = "dashboard-trash";
    private final SchedulerService schedulerService;

    private Long schedulerId;

    private DashboardTrashController()
    {
        schedulerService = SchedulerServiceProvider.get();
    }

    private static class Instance
    {
        private static final DashboardTrashController DASHBOARD_TRASH_CONTROLLER = new DashboardTrashController();
    }

    public static DashboardTrashController getInstance()
    {
        return Instance.DASHBOARD_TRASH_CONTROLLER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start( final Configurations configurations )
    {
        if( Objects.isNull( schedulerId ) && Objects.nonNull( schedulerService ) )
        {
            this.schedulerId = schedulerService.schedule( THREAD_POOL_NAME, new DashboardTrashTask( new DashboardTrashTaskController() ) );

            LOGGER.info( "CRM Analytics: Dashboard trash service started, Scheduler ID : {}", schedulerId );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if( Objects.nonNull( schedulerId ) && Objects.nonNull( schedulerService ) )
        {
            schedulerService.removeTask( schedulerId );
            LOGGER.info( "CRM Analytics: Dashboard trash status update service stopped Scheduler ID : {}", schedulerId );
        }
    }

    /**
     * <p>
     * A task which is responsible for dashboard trash activity
     * </p>
     *
     * @author pandiarajkumar G
     * @version 1.0
     */
    private static class DashboardTrashTask extends Task
    {
        private final CRMDashboardTrashManager dashboardTrashManager;
        private final ClusterService clusterService;

        private DashboardTrashTask( final TaskController controller )
        {
            super( controller );
            dashboardTrashManager = CRMDashboardTrashManager.getInstance();
            clusterService = ClusterServiceProvider.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute( final TaskController taskController )
        {
            try
            {
                dashboardTrashManager.deleteDashboard();
            } catch ( Throwable throwable )
            {
                LOGGER.info( "Dashboard Delete action failed : {}", throwable.getMessage() );
                LOGGER.debug( "Stack", throwable );
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask()
        {
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls trash task {@link DashboardTrashTaskController}.
     * </p>
     *
     * @author pandiarajkumar G
     * @version 1.0
     */
    public static class DashboardTrashTaskController extends TaskController
    {
        public DashboardTrashTaskController()
        {
            super( TASK_SCHEDULER_NAME );
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval()
        {
            return TimeUnit.MINUTES.toMillis( 1 );
        }
    }
}
