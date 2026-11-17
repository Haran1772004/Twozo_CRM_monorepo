package com.tlc.cluster.listener;

import java.io.IOException;

/**
 * @author Abishek
 * @version 1.0
 */
public interface LeaderElectionController
{
    void stop() throws IOException;

    boolean isRunning();

    void start() throws Exception;
}
