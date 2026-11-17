package com.tlc.cluster.listener;

/**
 * @author Abishek
 * @version 1.0
 */
public interface LeaderNotifier
{
    void takeLeadership();

    void revokeLeadership();
}
