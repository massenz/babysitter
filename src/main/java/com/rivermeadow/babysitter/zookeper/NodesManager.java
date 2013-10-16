package com.rivermeadow.babysitter.zookeper;

import com.google.common.collect.Maps;
import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class NodesManager implements Watcher, EvictionListener, RegistrationListener {

    private static final int SESSION_TIMEOUT = 5000;
    private static final String HOSTS_PATH = "/monitor/hosts";
    Logger logger = Logger.getLogger("NodesManager");
    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private Map<String, Server> serverPool = Maps.newHashMap();

    public NodesManager() {
        try {
            // TODO: get the list of zookeper servers from a configuration class
            connect("localhost");
        } catch (IOException | InterruptedException | KeeperException e) {
            logger.severe("Could not connect to Zookeper instance: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.fine(String.format("NodeManager watched event [%s] for %s",
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
                    List<String> servers = zk.getChildren(HOSTS_PATH, this);
                    // TODO: go through the list of servers and add the ones that we don't know yet about
                    logger.info(String.format("These are the watched servers: %s", servers.toString()));
                } catch (KeeperException | InterruptedException e) {
                    logger.throwing(this.getClass().getCanonicalName(), "process", e);
                }
                break;
            default:
                logger.warning(String.format("Not an expected event: %s; for %s",
                        watchedEvent.getType(), watchedEvent.getPath()));
        }
    }

    private void connect(String hosts) throws IOException, InterruptedException, KeeperException {
        zk = new ZooKeeper(hosts, SESSION_TIMEOUT, this);
        connectedSignal.await();
    }

    public List<String> getMonitoredServers() throws KeeperException, InterruptedException {
        return zk.getChildren(HOSTS_PATH, this);
    }

    public void createServer(String name) {
        try {
            zk.create(HOSTS_PATH + '/' + name, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void startup() throws KeeperException, InterruptedException {
        List<String> servers = getMonitoredServers();
        // TODO: go through the list of servers and add the ones that we don't know yet about
        logger.info(String.format("These are the watched servers: %s", servers.toString()));
    }

    public void shutdown() throws InterruptedException {
        if (zk != null) {
            zk.close();
        }
    }

    @Override
    public Status register(Server server) {
        // TODO: we're simply adding this server to the list of monitored severs
        serverPool.put(server.getName(), server);
        logger.info(String.format("Server %s registered", server.getName()));
        return Status.createStatus(String.format("Server %s registered",
                server.getName()));
    }

    @Override
    public Status deregister(Server server) {
        // TODO: we're just removing from the pool, we should alert too
        if (serverPool.containsKey(server.getName())) {
            serverPool.remove(server.getName());
            return Status.createStatus(String.format("Server %s removed from pool - TODO raise "
                    + "alert!", server.getName()));
        }
        return Status.createErrorStatus(String.format("Server %s unknown", server.getName()));
    }

    public void removeServer(String id) {
        try {
            String path = HOSTS_PATH + "/" + id;
            Stat stat = zk.exists(path, false);
            if (stat == null){
                throw new IllegalStateException(String.format("The server %s does not exist", id));
            }
            zk.delete(path, stat.getVersion());
        } catch (KeeperException | InterruptedException e) {
            logger.severe(String.format("Cannot remove server %s: %s", id,
                    e.getLocalizedMessage()));
        }
    }
}
