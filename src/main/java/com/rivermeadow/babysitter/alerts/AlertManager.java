package com.rivermeadow.babysitter.alerts;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.RegistrationListener;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * Encapsulates the behavior of an alert manager to respond to event such as a monitored
 * server unexpectedly de-registering from ZK.
 *
 * A number of (pluggable) adapters will be alerted when such an event occurs
 *
 * @author marco
 *
 */
public class AlertManager implements EvictionListener, RegistrationListener {
    Logger logger = Logger.getLogger(AlertManager.class);

    Set<Server> registeredServers = Sets.newHashSet();
    List<Pager> pagers = Lists.newArrayList();

    public void addPager(Pager pager) {
        pagers.add(pager);
    }

    @Override
    public Status deregister(Server server) {
        if (registeredServers.remove(server)) {
            for (Pager pager : pagers) {
                pager.page(server);
            }
            return Status.createStatus(String.format("Server %s removed", server.getName()));
        } else {
            logger.error(String.format("Attempt to remove non-monitored server: %s [%s] :: %s",
                    server.getName(), server.getServerAddress().getIp(), server.getDescription()));
            // TODO: add a Metric here to keep track, and possibly raise an alert if > threshold
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
