package com.tlc.cluster.service;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.NodeManager;
import com.tlc.cluster.listener.LeaderElectionController;
import com.tlc.cluster.listener.LeaderNotifier;
import com.tlc.commons.service.Service;

/**
 * @author Abishek
 * @version 1.0
 */
public interface ClusterService extends Service
{
	NodeManager nodeManager();

	LeaderElectionController startLeaderElection(String name, LeaderNotifier notifier) throws Exception;

	DistributedLock getDistributedLock(String... name) throws Exception;
}
