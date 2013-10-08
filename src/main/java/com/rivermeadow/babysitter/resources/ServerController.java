package com.rivermeadow.babysitter.resources;

import org.apache.zookeeper.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

@Controller
@EnableAutoConfiguration
public class ServerController implements Watcher {

    private static final int SESSION_TIMEOUT = 5000;
    Logger logger = Logger.getLogger("ServerController");
    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);

    @RequestMapping(value = "/register/{id}", method = {RequestMethod.POST})
    @ResponseBody
    String register(@PathVariable String id) {
        if (zk == null) {
            try {
                connect("localhost");
                logger.info("Connected to Zookeper on localhost");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return "failed: " + e.getLocalizedMessage();
            }
        }
        try {
            create(id);
        } catch (KeeperException | InterruptedException e) {
            String msg = String.format("Could not register %s, cause was: %s", id,
                    e.getLocalizedMessage());
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        logger.info(String.format("Server %s registered", id));
        return "ok";
    }

    @RequestMapping(value = "/beat", method = {RequestMethod.POST})
    @ResponseBody
    String beat(@RequestBody Map<String, ?> body) {
        if (zk == null) {
            // We got a heartbeat from a server that never registered,
            // this is not a valid situation
            throw new IllegalStateException(String.format("Server %s never registered and we are " +
                    "not ready yet to accept heartbeats", body.get("hostname")));
        }
        if (body != null) {
            logger.info(String.format("POST heartbeat from [%s]", body.get("ip").toString()));
            logger.info(String.format("Hostname: %s", body.get("hostname")));
        }
        return "ok";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ServerController.class, args);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
    }

    public void connect(String hosts) throws IOException, InterruptedException {
        zk = new ZooKeeper(hosts, SESSION_TIMEOUT, this);
        connectedSignal.await();
    }

    public void create(String name) throws KeeperException, InterruptedException {
        String path = File.separator + name;
        String createdPath = zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
        logger.info(String.format("Created %s", createdPath));
    }
}
