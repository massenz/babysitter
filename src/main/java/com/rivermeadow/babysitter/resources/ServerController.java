package com.rivermeadow.babysitter.resources;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

@Controller
@EnableAutoConfiguration
public class ServerController {

    Logger logger = Logger.getLogger("ServerController");

    @RequestMapping("/register/{id}")
    @ResponseBody
    String home(@PathVariable String id) {
        return String.format("Server %s registered", id);
    }

    @RequestMapping("/beat")
    @ResponseBody
    String beat(Map<String, ?> body) {
        logger.info("Received heartbeat from [IP]");
        if (body != null) {
            logger.info(body.toString());
        }
        return "ok";
    }

    @RequestMapping(value = "/beat", method = {RequestMethod.POST})
    @ResponseBody
    String beat_with_data(@RequestBody Map<String, ?> body) {
        if (body != null) {
            logger.info(String.format("POST heartbeat from [%s]", body.get("ip").toString()));
            logger.info(String.format("Hostname: %s", body.get("hostname")));
        }
        return "ok";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ServerController.class, args);
    }
}
