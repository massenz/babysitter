package com.rivermeadow.babysitter.alerts;

import com.google.common.collect.Sets;
import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.RegistrationListener;

import java.util.Set;

/**
 * TODO: this class will be an abstract class
 *
 * Encapsulates
 *
 * @author marco
 *
 */
public class AlertManager implements EvictionListener, RegistrationListener {

    Set<Server> registeredServers = Sets.newHashSet();

    @Override
    public Status deregister(Server server) {
        if (registeredServers.remove(server)) {
            return Status.createStatus(String.format("Server %s removed", server.getName()));
            // TODO: raise an alert (if applicable)
        } else {
            return Status.createErrorStatus(String.format("Server %s was not a registered " +
                    "server", server.getName()));
        }
    }

    @Override
    public Status register(Server server) {
        if (registeredServers.add(server)) {

            return Status.createStatus(String.format("Server %s added", server.getName()));
        } else {
            return Status.createErrorStatus(String.format("Server %s was already registered " +
                    "server", server.getName()));
        }
    }

    @Override
    public Set<Server> getRegisteredServers() {
        return registeredServers;
    }
}
