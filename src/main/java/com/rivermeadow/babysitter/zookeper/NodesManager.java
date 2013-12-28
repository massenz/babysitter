package com.rivermeadow.babysitter.zookeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;

import static com.rivermeadow.babysitter.plugins.utils.ResourceLocator.*;

/**
 * This is the main interface towards the ZK service and manages the registration and eviction of
 * servers from the ZK point of view, by registering itself as the 'watcher' for the children of
 * a "well-known" master node (@link{#HOSTS_PATH}).
 *
 * This class does @strong{not} manage the internally-maintained list of servers,
 * or the alerts if any of the monitored nodes disappears: this is entirely delegated to the
 * @link{EvictionListener} and @link{RegistrationListener}.
 *
 * This class also encapsulates the necessary logic to determine if a change in the watched
 * children nodes was due to a server being added or removed.
 *
 * @author marco
 */
public class NodesManager implements Watcher {
    private static final String REMOVED = "REMOVED";
    private static final String ADDED = "ADDED";
    public static final String BOOTSTRAP_LOCATION = "bootstrap.location";
    Logger logger = Logger.getLogger(NodesManager.class);

    private ZooKeeper zk;

    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private ObjectMapper mapper = new ObjectMapper();
    private final long maxDelayMsec;

    @Autowired
    EvictionListener evictionListener;

    @Autowired
    RegistrationListener registrationListener;

    @Autowired
    ZookeeperConfiguration zkConfiguration;

    public NodesManager(long maxDelaysMsec) {
        this.maxDelayMsec = maxDelaysMsec;
    }

    @PostConstruct
    public void connectZookeeper() {
        logger.debug("Node Manager started, connecting to ZK hosts: " + zkConfiguration.getHosts());
        try {
            connect(zkConfiguration.getHosts());
            logger.debug("Connected - Session ID: " + zk.getSessionId());
        } catch (IOException | InterruptedException | KeeperException e) {
            logger.error("Could not connect to Zookeper instance", e);
        }
    }

    private void connect(String hosts) throws IOException, InterruptedException, KeeperException {
        zk = new ZooKeeper(hosts, zkConfiguration.getSessionTimeout(), this);
        connectedSignal.await();
    }

    public void startup() throws KeeperException, InterruptedException {
        bootstrap();
        List<String> serverNames = getMonitoredServers();
        for (String name : serverNames) {
            // TODO: add some guard here, as one (or more) of the servers may have gone since the
            //       previous call to obtain the list
            Server server = getServerInfo(name);
            registrationListener.register(server);
        }
        logger.info(String.format("Nodes Manager Started successfully. There are currently %d " +
                "servers: %s", serverNames.size(), serverNames.toString()));
    }

    private void bootstrap() {
        try {
            InputStream bootstrapFile = getResourceFromSystemProperty(BOOTSTRAP_LOCATION);
            List<String> nodes = (List<String>)
                    mapper.readValue(bootstrapFile, Map.class).get("paths");
            logger.info("Paths to create: " + nodes);
            for (String node : nodes) {
                createNode(node);
            }
        } catch (IOException | URISyntaxException e) {
            logger.error(String.format("Could not retrieve a valid list of 'bootstrap nodes' " +
                    "for the application; this may not be fatal, but it's possible the server " +
                    "will misbehave.  Failure was: %s, while trying to access %s (based on " +
                    "system property %s)", e.getLocalizedMessage(),
                    System.getProperty(BOOTSTRAP_LOCATION),
                    BOOTSTRAP_LOCATION));
        }
    }

    private void createNode(final String node) {
        zk.create(node, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch (KeeperException.Code.get(rc)) {
                            case CONNECTIONLOSS:
                                createNode(path);
                                break;
                            case OK:
                                logger.info("Node " + path + " created");
                                break;
                            case NODEEXISTS:
                                // safe to ignore
                                logger.debug("Path " + path + " already exists");
                                break;
                            default:
                                logger.error("Unexpected result while trying to create node " +
                                        node + ": " + KeeperException.create(KeeperException.Code
                                        .get(rc), path));
                        }
                    }
                },
                null    // nothing useful to pass to the ctx callback parameter
        );
    }

    public void shutdown() throws InterruptedException {
        if (zk != null) {
            zk.close();
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.trace(String.format("NodeManager watched event [%s] for %s",
                watchedEvent.getType(), watchedEvent.getPath()));
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    logger.info("ZooKeeper client connected to server");
                    connectedSignal.countDown();
                }
                break;
            case NodeChildrenChanged:
                try {
                    List<String> servers = zk.getChildren(zkConfiguration.getBasePath(), this);
                    Map<String, Set<Server>> diffs = computeDiff(servers);
                    logger.debug(String.format("Server modified. Watched servers now: %s",
                            servers.toString()));
                    logger.debug(String.format("Removed Servers: %s", diffs.get(REMOVED)));
                    logger.debug(String.format("Added servers: %s", diffs.get(ADDED)));
                    processDiffs(diffs);
                } catch (KeeperException | InterruptedException e) {
                    logger.error(String.format("There was an error retrieving the list of " +
                            "servers [%s]", e.getLocalizedMessage()), e);
                }
                break;
            default:
                logger.warn(String.format("Not an expected event: %s; for %s",
                        watchedEvent.getType(), watchedEvent.getPath()));
        }
    }

    private void processDiffs(Map<String, Set<Server>> diffs) {
        // this can be done without further ado
        for (Server server : diffs.get(ADDED)) {
            logger.debug("Reporting addition to listener: " +
                    server.getServerAddress().getHostname());
            registrationListener.register(server);
            // if the server had been silenced, we re-enable monitoring:
            removeSilence(server);
        }
        // before we jump to trigger alerts, we need to wait around a bit,
        // so that other servers have the option to do that too.
        long msecs = (long)(Math.random() * maxDelayMsec);

        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            // never quite understood the point of this, but so be it...
            logger.error(String.format("Interrupted while waiting to start alerting (%s)",
                    e.getLocalizedMessage()));
            return;
        }

        for (Server server : diffs.get(REMOVED)) {
            logger.debug("Reporting eviction to listener: " +
                    server.getServerAddress().getHostname());
            // by 'silencing' the server, if we are successful, we will also trigger the alerts
            // TODO: this is not ideal, should be re-considered
            silence(server, false);
        }
    }

    public List<String> getMonitoredServers() throws KeeperException, InterruptedException {
        return zk.getChildren(zkConfiguration.getBasePath(), this);
    }

    /**
     * Computes the diff between the currently registered servers and the @code{latest} set as
     * reported by Zookeeper (typically, following a change in the monitored node's children --
     * see {@link NodesManager#process(WatchedEvent)}).
     *
     * <p>The returned Map contains exactly two elements (either, or both,
     * of which MAY be empty): a DELETED Set, containing any servers that was in the currently
     * kept list, and is no longer in {@code latest}, and an ADDED Set,
     * containing any newly discovered servers.
     *
     * @param latest a List of servers as obtained, for example, from Zookeeper
     * @return  a ``pair`` of Sets, either, or both of which may be empty,
     *          containing the ADDED and DELETED servers
     */
    public Map<String, Set<Server>> computeDiff(List<String> latest)
            throws KeeperException, InterruptedException {
        Set<Server> knownServers = registrationListener.getRegisteredServers();
        Set<String> latestServersNames = Sets.newHashSet(latest);
        Set<Server> evictedServers = Sets.newHashSet();
        for (Server server : knownServers) {
            if (!latestServersNames.remove(server.getName())) {
                // if we can't remove the name of a known server from the most recent list we got
                // from ZK, it means it's no longer there, so it must be a removed server:
                logger.info("Server " + server.getName() + " has been removed");
                evictedServers.add(server);
            }
        }
        // at this point, latestServerNames contains ONLY the names of the servers that were not
        // already in knownServers, hence the newly added ones: we need to build the Set by
        // querying ZK to retrieve the data associated with the new servers
        Set<Server> newlyAddedServers = Sets.newHashSet();
        for (String name : latestServersNames) {
            logger.info("Server " + name + " has joined the monitored pool");
            newlyAddedServers.add(getServerInfo(name));
        }
        Map<String, Set<Server>> diffs = Maps.newHashMapWithExpectedSize(2);
        diffs.put(ADDED, newlyAddedServers);
        diffs.put(REMOVED, evictedServers);
        return diffs;
    }

    /**
     * Retrieves info about the given server, by interrogating ZK
     *
     * @param name the server's hostname
     * @return the parsed {@link Server} object, from the JSON data sent to ZK by the server itself
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    public Server getServerInfo(String name) throws KeeperException, InterruptedException {
        String fullPath = zkConfiguration.getBasePath() + File.separatorChar + name;
        Stat stat = new Stat();
        byte[] data = zk.getData(fullPath, false, stat);
        try {
            Server server = mapper.readValue(data, Server.class);
            logger.debug(String.format("%s :: %s", server.getName(), server.getDescription()));
            return server;
        } catch (IOException e) {
            logger.error(String.format("Could not convert data [%s] into a valid Server object " +
                    "(%s): %s",
                    new String(data), fullPath, e.getLocalizedMessage()), e);
            return null;
        }
    }

    /**
     * By adding a node with the server's hostname in the `alerts subtree`,
     * we are effectively preventing any alerts to be triggered on this server.
     *
     * <p>This is useful when processing an alert, and wanting to avoid that more than one
     * monitor triggers the alerting plugins; or it could be set (via an API call) to silence a
     * "flaky" or "experimental" server that keeps triggering alerts.
     *
     * @param server the server that will be 'silenced'
     * @param persistent whether the silence should survive this server's session
     *                   failure/termination
     */
    synchronized public Status silence(Server server, boolean persistent) {
        String path = buildAlertPathForServer(server);
        try {
            Stat stat = zk.exists(path, false);
            if (stat == null) {
                CreateMode createMode = persistent ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
                zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode,
                        createSilenceCallback, server);
            }
        } catch (KeeperException.SessionExpiredException ex) {
            // this is a pretty severe situation, as we've lost contact with ZK and we can't
            // really know what's going on, and at the same time we know a monitored server has
            // triggered an alert.
            // Unintuitively, we cannot panic and just trigger alerts,
            // as the system is in an unstable state: we should just fail, fast and noisily
            throw new RuntimeException("ZK Session Expired, probably caused by a connectivity " +
                    "loss or, more worryingly, because we've lost all connectivity to the ZK " +
                    "ensemble.");
        } catch (KeeperException | InterruptedException e) {
            // this is probably safe to ignore, as it's thrown by the exists() call
            // which is a an expected event, when there are more than one monitor server running
            String msg = String.format("Caught an exception while trying to silence %s (%s)",
                    server, e.getLocalizedMessage());
            logger.error(msg);
            return Status.createErrorStatus(msg);
        }
        return Status.createStatus("Server " + server.getName() + " silenced");
    }

    // NOTE: ``name`` is not documented, however, it's the full name of the path being created
    //      this is useful when creating 'sequence' nodes, but clearly pointless here,
    //      as it's the same as ``path``
    AsyncCallback.StringCallback createSilenceCallback = new AsyncCallback.StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            Server svr = (Server) ctx;
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    silence(svr, false);
                    break;
                case NODEEXISTS:
                    // This is to be expected, as there may be several monitoring servers all
                    // competing to owning the alert by 'silencing' the server
                    // TODO: remove this trace if it proves to be too 'noisy'
                    logger.trace("Trying to silence an already silenced server [" + name + "]");
                    break;
                case OK:
                    logger.info(String.format("Server %s silenced (%s)", svr.getName(),
                            svr.getServerAddress().getHostname()));
                    // we got here first, we can now trigger the alerts on this server:
                    // TODO: this will have unintended consequences if all we wanted to do was to
                    //      silence this server
                    evictionListener.deregister(svr);
                    break;
                default:
                    logger.debug(String.format("[%s] Unexpected result for silencing %s (%s)",
                            KeeperException.Code.get(rc), svr.getName(), path));
            }
        }
    };

    /**
     * Removes the silence for the server, for example, when the server re-starts after an
     * unexpected termination (that triggered an alert and a subsequent 'silence' to be set).
     *
     * @param server the server that will be 'unsilenced'
     */
    synchronized public void removeSilence(Server server) {
        String path = buildAlertPathForServer(server);
        try {
            Stat stat = zk.exists(path, false);
            if (stat != null) {
                zk.delete(path, stat.getVersion(), removeSilenceCallback, server);
            }
        } catch (KeeperException | InterruptedException kex) {
            // by and large this is safe to ignore: it just means another server got notified and
            // got there first to remove this 'silence'
            // we are just debug logging this in case weird errors are encountered:
            logger.debug(String.format("Exception encountered ('%s') while pruning the %s " +
                    "branch, upon registration of %s - this is probably safe to ignore, " +
                    "unless other unexplained errors start to crop up",
                    kex.getLocalizedMessage(), path, server.getName()));
        }
    }

    AsyncCallback.VoidCallback removeSilenceCallback = new AsyncCallback.VoidCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx) {
            Server svr = (Server) ctx;
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    // it is important to retry, as we don't know whether the original call
                    // succeeded and failing to remove the node will cause this server to be
                    // ignored, which is not acceptable
                    // As the removeSilence() operation is idempotent, it's safe to retry
                    removeSilence(svr);
                    break;
                case OK:
                    logger.info(String.format("Server %s re-enabled (%s)", svr.getName(),
                            svr.getServerAddress().getHostname()));
                    break;
                default:
                    logger.debug(String.format("[%s] Unexpected result for silencing %s (%s)",
                            KeeperException.Code.get(rc), svr.getName(), path));
            }
        }
    };

    private String buildAlertPathForServer(Server server) {
        return zkConfiguration.getAlertsPath() + File.separator +
                server.getServerAddress().getHostname();
    }
}
