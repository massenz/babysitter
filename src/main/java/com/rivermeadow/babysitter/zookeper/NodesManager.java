package com.rivermeadow.babysitter.zookeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivermeadow.babysitter.model.Server;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
    Logger logger = Logger.getLogger(NodesManager.class);

    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);

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
        List<String> servers = getMonitoredServers();
        // TODO: go through the list of servers and add the ones that we don't know yet about
        logger.info(String.format("Nodes Manager Started successfully. There are currently %d " +
                "servers: %s", servers.size(), servers.toString()));
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
                    // TODO: go through the list of servers and add the ones that we don't know yet about
                    logger.debug(String.format("Server modified. Watched servers now: %s",
                            servers.toString()));
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

    public String getServerInfo(String name) throws KeeperException, InterruptedException {
        // TODO: make a 'pluggable' servers state manager
        // Right now, it just goes to ZK and gets the data out
        String fullPath = zkConfiguration.getBasePath() + File.separatorChar + name;
        Stat stat = new Stat();
        byte[] data = zk.getData(fullPath, false, stat);
        String serverInfo = new String(data);
        logger.debug(String.format("%s :: %s", name, serverInfo));
        logger.debug("Version: " + stat.getVersion());
        // TODO: de-serialize JSON to Server object
        return serverInfo;
    }

    public void createServer(String name, Server server) {
        try {
            ObjectMapper mapper = new ObjectMapper();
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
}
