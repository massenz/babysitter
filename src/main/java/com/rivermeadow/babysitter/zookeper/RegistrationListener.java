package com.rivermeadow.babysitter.zookeper;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;

import java.util.Set;

/**
 * This listener is invoked when a new server joins the pool of monitored servers, or new
 * information is updated to the server.
 *
 * @author marco
 *
 */
public interface RegistrationListener {

    public Status register(Server server);

    public Set<Server> getRegisteredServers();

    public Status updateServer(Server server);
}
