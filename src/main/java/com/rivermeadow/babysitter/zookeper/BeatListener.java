package com.rivermeadow.babysitter.zookeper;

import com.rivermeadow.babysitter.model.Server;

/**
 * A `beat listener` will receive regular updates from a node and will manage keeping the node's
 * status up-to-date in ZooKeeper.
 *
 * @author marco
 *
 */
public interface BeatListener {


    public void register(Server server);

    public void updateHeartbeat(Server server);

    public void deregister(Server server);

}
