package com.rivermeadow.babysitter.resources;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.spring.BeanConfiguration;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import com.rivermeadow.babysitter.zookeper.ZookeeperConfiguration;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;


/**
 * The Main server controller class to start the application and dispatch requests
 *
 * @author marco
 */
@Controller
@EnableAutoConfiguration
public class ServerController {
    Logger logger = Logger.getLogger("ServerController");

    @Autowired
    private NodesManager nodesManager;
    private boolean managerStarted = false;

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.POST},
            consumes = "application/json", produces = "text/plain")
    @ResponseBody
    String registerServer(@PathVariable String id, @RequestBody Server server) {
        if (managerStarted) {
            nodesManager.createServer(id, server);
            logger.info(String.format("Server %s registered", id));
            return "ok";
        } else {
            return "Service not started yet, cannot register server: obtain list of servers " +
                    "first" + " (use the /servers endpoint)";
        }
    }

    @RequestMapping(value = "/servers", method = {RequestMethod.GET}, produces = "text/plain")
    @ResponseBody
    String getAllServers() {
        StringBuilder response = new StringBuilder();
        try {
            if (!managerStarted) {
                nodesManager.startup();
                response.append("Server started\n");
                managerStarted = true;
            }
            // TODO: use JSON to return a list of registered servers
            response.append(nodesManager.getMonitoredServers().toString());
            response.append('\n').append("\nStatus: OK");
            return response.toString();
        } catch (KeeperException | InterruptedException e) {
            return "[Error] " + e.getLocalizedMessage();
        }
    }

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.DELETE})
    @ResponseBody
    String deleteServer(@PathVariable String id) {
        nodesManager.removeServer(id);
        return "Server " + id + " removed";
    }



    public static void main(String[] args) throws Exception {
        SpringApplication.run(new Object[] {ServerController.class, BeanConfiguration.class},
                args);
    }
}
