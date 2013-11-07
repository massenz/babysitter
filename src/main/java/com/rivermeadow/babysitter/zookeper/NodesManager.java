package com.rivermeadow.babysitter.zookeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.rivermeadow.babysitter.model.Server;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

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

    Logger logger = Logger.getLogger(NodesManager.class);

    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    EvictionListener evictionListener;

    @Autowired
    RegistrationListener registrationListener;

    @Autowired
    ZookeeperConfiguration zkConfiguration;


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
                    Map<String, Set<Server>> diffs = computeDiff(servers, true);
                    logger.debug(String.format("Server modified. Watched servers now: %s",
                            servers.toString()));
                    logger.debug(String.format("Removed Servers: %s", diffs.get(REMOVED)));
                    logger.debug(String.format("Added servers: %s", diffs.get(ADDED)));
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

    public List<String> getMonitoredServers() throws KeeperException, InterruptedException {
        return zk.getChildren(zkConfiguration.getBasePath(), this);
    }

    public Server getServerInfo(String name) throws KeeperException, InterruptedException {
        // TODO: make a 'pluggable' servers state manager
        // Right now, it just goes to ZK and gets the data out
        String fullPath = zkConfiguration.getBasePath() + File.separatorChar + name;
        Stat stat = new Stat();
        byte[] data = zk.getData(fullPath, false, stat);
        String serverInfo = new String(data);
        logger.debug("Version: " + stat.getVersion());
        try {
            Server server = mapper.readValue(data, Server.class);
            logger.debug(String.format("%s :: %s", server.getName(), server.getDescription()));
            return server;
        } catch (IOException e) {
            logger.error(String.format("Could not convert data [%s] into a valid Server object "
                    + "(%s)", new String(data), e.getLocalizedMessage()), e);
            return null;
        }
    }

    public void createServer(String name, Server server) {
        try {
            zk.create(zkConfiguration.getBasePath() + '/' + name, mapper.writeValueAsBytes(server),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL);
            registrationListener.register(server);
        } catch (KeeperException | InterruptedException | JsonProcessingException e) {
            logger.error(String.format("Cannot create server %s entry: %s", name,
                    e.getLocalizedMessage()), e);
        }
    }

    public void removeServer(String id) {
        try {
            String path = zkConfiguration.getBasePath() + "/" + id;
            Stat stat = zk.exists(path, false);
            if (stat == null){
                throw new IllegalStateException(String.format("The server %s does not exist", id));
            }
            zk.delete(path, stat.getVersion());
        } catch (KeeperException | InterruptedException e) {
            logger.error(String.format("Cannot remove server %s: %s", id, e.getLocalizedMessage()
            ), e);
        }
    }

    /**
     * Computes the diff between the currently registered servers and the @code{latest} set as
     * reported by Zookeeper (typically, following a change in the monitored node's childrend --
     * see @link{NodesManager#process(WatchedEvent)}.
     *
     * <p>The returned Map contains exactly two elements (either, or both,
     * of which MAY be empty): a DELETED Set, containing any servers that was in the currently
     * kept list, and is no longer in @code{latest}, and an ADDED Set,
     * containing any newly discovered servers.
     *
     * <p>This method will also call the Listeners, if the @code{}alertListeners} flag
     * is @code{true}
     *
     * @param latest a List of servers as obtained, for example, from Zookeeper
     * @param alertListeners a flag that, if true, will cause the listeners to be alerted for
     *                       each added and removed server
     *
     * @return a ``pair`` of Sets, either, or both of which may be empty,
     * containing the ADDED and DELETED servers
     */
    public Map<String, Set<Server>> computeDiff(List<String> latest, boolean alertListeners)
            throws KeeperException, InterruptedException {
        Set<Server> knownServers = registrationListener.getRegisteredServers();
        Set<String> latestServersNames = Sets.newHashSet(latest);
        Set<Server> evictedServers = Sets.newHashSet();
        for (Server server : knownServers) {
            if (!latestServersNames.remove(server.getName())) {
                // if we couldn't remove the name, it means it wasn't there to start with, so
                // it must be a removed server:
                logger.info("Server " + server.getName() + " has been removed");
                // we can't quite alert yet, as eviction may change the knownServers Set and
                // that would cause a CollectionModifiedException to be thrown
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
        if (alertListeners) {
            for (Server server : evictedServers) {
                logger.debug("Reporting eviction to listener: " +
                        server.getServerAddress().getHostname());
                evictionListener.deregister(server);
            }
            for (Server server : newlyAddedServers) {
                logger.debug("Reporting addition to listener: " +
                        server.getServerAddress().getHostname());
                registrationListener.register(server);
            }
        }
        return diffs;
    }
}
