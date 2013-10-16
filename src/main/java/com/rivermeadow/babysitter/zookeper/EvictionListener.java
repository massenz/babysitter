package com.rivermeadow.babysitter.zookeper;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;

/**
 * An `eviction listener` will receive regular updates from ZK and will manage keeping the
 * node's status up-to-date in our AlertManager.
 *
 * @author marco
 *
 */
public interface EvictionListener {

    /**
     * Invoked when a server is removed from the pool of monitored servers
     *
     * @param server the server being removed
     * @return the outcome of this call
     */
    public Status deregister(Server server);

}
