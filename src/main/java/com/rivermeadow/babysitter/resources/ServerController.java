package com.rivermeadow.babysitter.resources;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.spring.BeanConfiguration;
import com.rivermeadow.babysitter.zookeper.BeatListener;
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
    private BeatListener listener;

    @RequestMapping(value = "/register/{id}", method = {RequestMethod.POST},
            consumes = "application/json", produces = "text/plain")
    @ResponseBody
    String register(@PathVariable String id, @RequestBody Server serverData) {
        // TODO: use Jackson to create the Server object from the JSON body
        listener.register(serverData);
        logger.info(String.format("Server %s registered", id));
        return "ok";
    }

    @RequestMapping(value = "/beat", method = {RequestMethod.POST},
            consumes = "application/json", produces = "text/plain")
    @ResponseBody
    String beat(@RequestBody Server server) {
        logger.info(String.format("POST heartbeat from %s [%s]", server.getServerType(),
                server.getServerAddress().getHostname()));
        listener.updateHeartbeat(server);
        return "ok";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(new Object[] {ServerController.class, BeanConfiguration.class},
                args);
    }
}
