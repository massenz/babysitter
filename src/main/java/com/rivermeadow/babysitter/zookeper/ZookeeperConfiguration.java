package com.rivermeadow.babysitter.zookeper;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manages the ZK zkConfiguration options, by reading them from a file
 *
 * @author marco
 *
 */
@Component
public class ZookeeperConfiguration {
    private static final Logger logger = Logger.getLogger(ZookeeperConfiguration.class);

    @Value("${zookeeper.hosts}")
    String hosts;

    @Value("${zookeeper.base_path}")
    String basePath;

    @Value("${zookeeper.session_timeout}")
    Integer sessionTimeout;

    public ZookeeperConfiguration() {
        logger.debug("Zookeeper configs: " + hosts + " :: " + basePath);
    }

    public String hosts() {
        return hosts;
    }

    public int timeout() {
        return sessionTimeout;
    }

    public String base_path() {
        return basePath;
    }
}
