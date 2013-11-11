package com.rivermeadow.babysitter.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivermeadow.babysitter.alerts.AlertPlugin;
import com.rivermeadow.babysitter.alerts.Context;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.ServerAddress;
import com.rivermeadow.babysitter.spring.BeanConfiguration;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.JclUtils;


/**
 * The Main server controller class to start the application and dispatch requests
 *
 * @author marco
 */
@Controller
@EnableAutoConfiguration
public class ServerController {
    private static final Logger logger = Logger.getLogger(ServerController.class);

    NodesManager nodesManager;

    @Autowired
    JarClassLoader jarClassLoader;
    JclObjectFactory factory = JclObjectFactory.getInstance();
    Context ctx = new Context();

    @Autowired
    public ServerController(NodesManager nodesManager) {
        this.nodesManager = nodesManager;
        try {
            this.nodesManager.startup();
        } catch (KeeperException | InterruptedException e) {
            logger.error("Could not start NodesManager: " + e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.POST},
            consumes = "application/json", produces = "text/plain")
    @ResponseBody
    String registerServer(@PathVariable String id, @RequestBody Server server) {
        nodesManager.createServer(id, server);
        logger.info(String.format("Server %s registered", id));
        return "ok";
    }

    @RequestMapping(value = "/servers", method = {RequestMethod.GET}, produces = "text/plain")
    @ResponseBody
    String getAllServers() {
        StringBuilder response = new StringBuilder();
        try {
            String content = nodesManager.getMonitoredServers().toString();
            logger.debug("Getting all servers: " + content);
            response.append(content);
            response.append('\n').append("\nStatus: OK");
            return response.toString();
        } catch (KeeperException | InterruptedException e) {
            return "[Error] " + e.getLocalizedMessage();
        }
    }

    // NOTE: the RegEx pattern is necessary to cope with Spring's PathVariable stupidity
    //   without it, it would "swallow" anything after a `.` (perfectly valid in a URL param)
    //   See: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
    @RequestMapping(value = "/servers/{id:.+}", method = {RequestMethod.GET})
    @ResponseBody
    String getServer(@PathVariable String id) {
        logger.debug("Retrieving data for server " + id);
        try {
            Server server = nodesManager.getServerInfo(id);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(server);
        } catch (KeeperException | InterruptedException | JsonProcessingException e) {
            String msg = String.format("Error occurred retrieving server %s: %s", id,
                    e.getLocalizedMessage());
            logger.error(msg, e);
            return msg;
        }
    }

    // TODO: add POST method to register plugin, and GET should only provide info about plugin
    @RequestMapping(value = "/plugins/{fqn:.+}", method = {RequestMethod.GET})
    @ResponseBody
    String getPlugin(@PathVariable String fqn) {
        logger.debug("Activating plugin " + fqn);
        if (jarClassLoader != null) {
            try {
                // TODO: obviously this would be POSTed as a file upload
                jarClassLoader.add("/tmp/jcl-test.jar");

                //Create object of loaded class
                Object obj = factory.create(jarClassLoader, fqn);
                AlertPlugin plugin = JclUtils.cast(obj, AlertPlugin.class);
                logger.info("Loaded valid AlertPlugin: " + plugin.getName() + " :: " + plugin.getDescription());
                plugin.startup(ctx);
                Pager pager = JclUtils.cast(plugin.activate(), Pager.class);
                logger.info("Plugin activated, obtained Pager: " + pager.getClass().getName());
                Server fakeServer = new Server(new ServerAddress("test", "10.10.121.100"), 80, 30);
                fakeServer.setDescription("This is a fake server");
                pager.page(fakeServer);
                return "Loaded valid AlertPlugin: " + plugin.getName() + " :: " + plugin.getDescription();
            } catch (Exception ex) {
                // TODO: this catch casts too wide a net, see if it can be reduced
                logger.error("Could not instantiate plugin " + fqn + "; error was: " + ex.getLocalizedMessage(), ex);
                return "[ERROR] Plugin " + fqn + " cannot be loaded: " + ex.getLocalizedMessage();
            }
        } else {
            return "[ERROR] JAR Class loader not initialized, please check Spring configuration";
        }
    }

    @RequestMapping(value = "/servers/{id}", method = {RequestMethod.DELETE})
    @ResponseBody
    String deleteServer(@PathVariable String id) {
        nodesManager.removeServer(id);
        return "Server " + id + " removed";
    }

    public static void main(String[] args) throws Exception {
        // TODO: inject correct major.minor version and generated build no.
        logger.debug(String.format("Starting Babysitter Server - rev. %d.%d.%d", 0, 1, 1234));
        SpringApplication.run(new Object[]{
                ServerController.class,
                BeanConfiguration.class
        }, args);
    }
}
