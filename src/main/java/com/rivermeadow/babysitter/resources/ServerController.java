package com.rivermeadow.babysitter.resources;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.spring.BeanConfiguration;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@Controller
@EnableAutoConfiguration
public class ServerController {

    private static final int SESSION_TIMEOUT = 5000;
    Logger logger = Logger.getLogger("ServerController");

    @Autowired
    private NodesManager manager;
    private boolean managerStarted = false;

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.POST},
            consumes = "application/json", produces = "text/plain")
    @ResponseBody
    String registerServer(@PathVariable String id, @RequestBody Server server) {
        // TODO: use Jackson to create the Server object from the JSON body
        manager.createServer(id);
        logger.info(String.format("Server %s registered", id));
        return "ok";
    }

    @RequestMapping(value = "/servers", method = {RequestMethod.GET}, produces = "text/plain")
    @ResponseBody
    String getAllServers() {
        StringBuilder response = new StringBuilder();
        try {
            if (!managerStarted) {
                manager.startup();
                response.append("Server started\n");
            }
            // TODO: use JSON to return a list of registered servers
            response.append(manager.getMonitoredServers().toString());
            response.append('\n').append("\nStatus: OK");
            return response.toString();
        } catch (KeeperException | InterruptedException e) {
            return "[Error] " + e.getLocalizedMessage();
        }
    }

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.DELETE})
    @ResponseBody
    String deleteServer(@PathVariable String id) {
        manager.removeServer(id);
        return "Server " + id + " removed";
    }



    public static void main(String[] args) throws Exception {
        SpringApplication.run(new Object[] {ServerController.class, BeanConfiguration.class},
                args);
    }
}
