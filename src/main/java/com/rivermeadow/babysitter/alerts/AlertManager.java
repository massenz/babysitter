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
    long maxDelayMsec;

    public AlertManager(long maxDelayMsec) {
        this.maxDelayMsec = maxDelayMsec;
    }

    public void addPager(Pager pager) {
        pagers.add(pager);
    }

    @Override
    public Status deregister(Server server) {
        if (registeredServers.remove(server)) {
            // TODO: refactor this into the alerting strategy outlined in Issue #3
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
            return Status.createErrorStatus(String.format("Server %s was already registered",
                    server.getName()));
        }
    }

    @Override
    public Set<Server> getRegisteredServers() {
        return registeredServers;
    }

    /**
     * Tries to claim ownership for the alert, by attempting to create a ZK node with the same
     * name as the server, in the ``alert sub-tree`` (see {@link com.rivermeadow.babysitter
     * .zookeper.ZookeeperConfiguration#getAlertsPath()}).
     *
     * <p>This may, or may not, succeed, as other babysitter servers may already have gotten
     * there before this one: if it does succeed, then this servers will go ahead and activate
     * all the ``pagers`` (plugins).
     *
     * <p>At some point we will need to remove this "silencing" alert node,
     * as the server may be restarted and not doing so would effectively prevent any future
     * failures to be alerted: we currently do this when a new server {@link #register(Server)
     * registers}, which means that, potentially, the size of the children of this sub-tree may
     * grow very large; realistically, we will eventually add some "pruning" background thread if
     * this proves to be an issue.
     *
     * @param unregisteredServer the server who just lost connectivity to ZK and we must assume
     *                           in need of some care and attention
     */
    private void tryOwnAlert(final Server unregisteredServer) {

    }
}
