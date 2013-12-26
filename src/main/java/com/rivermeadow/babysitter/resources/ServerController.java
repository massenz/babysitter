package com.rivermeadow.babysitter.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.rivermeadow.babysitter.alerts.AlertPlugin;
import com.rivermeadow.babysitter.alerts.Context;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.alerts.PluginRegistry;
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

import java.util.List;
import java.util.Map;
import java.util.Set;


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

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    PluginRegistry registry;

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

    @RequestMapping(value = "/servers", method = {RequestMethod.GET},
            produces = "application/json")
    @ResponseBody
    String getAllServers() {
        Map<String, Object> response = Maps.newHashMapWithExpectedSize(2);
        try {
            List<String> servers = nodesManager.getMonitoredServers();
            logger.debug("Got these servers: " + servers);
            response.put("servers", servers);
            response.put("status", "OK");
            return mapper.writeValueAsString(response);
        } catch (KeeperException | InterruptedException | JsonProcessingException e) {
            return String.format("{ \"Error\": \"%s\"}", e.getLocalizedMessage());
        }
    }

    // NOTE: the RegEx pattern is necessary to cope with Spring's PathVariable stupidity
    //   without it, it would "swallow" anything after a `.` (perfectly valid in a URL param)
    //   See: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
    @RequestMapping(value = "/servers/{id:.+}", method = {RequestMethod.GET},
            produces = "application/json")
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

    @RequestMapping(value = "/plugins/{fqn:.+}", method = {RequestMethod.GET},
            produces = "application/json")
    @ResponseBody
    String getPlugin(@PathVariable String fqn) {
        PluginRegistry.PluginBundle bundle = registry.getBundle(fqn);
        if (bundle != null) {
            try {
                return mapper.writeValueAsString(bundle);
            } catch (JsonProcessingException e) {
                logger.error(String.format("Could not build proper JSON for %s (%s)",
                        bundle.getMetadata(), e.getLocalizedMessage()));
                return String.format("{\"error\": \"%s\"," +
                        "\"plugin\": \"%s\"}", e.getLocalizedMessage(), fqn);
            }
        }
        // TODO: construct error string properly, escaping quotes etc.
        return String.format("{\"error\": \"%s not found\"," +
                "\"plugin\": \"%s\"}", fqn, fqn);
    }

    @RequestMapping(value = "/plugins", method = {RequestMethod.GET}, produces = "application/json")
    @ResponseBody
    String getAllPlugins() throws JsonProcessingException {
        Set<PluginRegistry.PluginBundle> bundles = registry.getAllBundles();
        List<PluginRegistry.PluginMetadata> metadata = Lists.newArrayListWithCapacity(bundles
                .size());
        for (PluginRegistry.PluginBundle bundle : bundles) {
            metadata.add(bundle.getMetadata());
        }
        return mapper.writeValueAsString(metadata);
    }


    public static void main(String[] args) throws Exception {
        // TODO: inject correct major.minor version and generated build no.
        logger.debug(String.format("Starting Babysitter Server - rev. %d.%d.%d", 0, 2, 0));
        SpringApplication.run(new Object[]{
                ServerController.class,
                BeanConfiguration.class
        }, args);
    }
}
