package com.rivermeadow.babysitter.zookeper;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;

/**
 * A `beat listener` will receive regular updates from a node and will manage keeping the node's
 * status up-to-date in ZooKeeper.
 *
 * @author marco
 *
 */
public interface BeatListener {


    public Status register(Server server);

    public Status updateHeartbeat(Server server);

    public Status deregister(Server server);

}
