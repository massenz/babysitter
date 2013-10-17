package com.rivermeadow.babysitter.zookeper;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Manages the ZK zkConfiguration options, by reading them from a file
 *
 * @author marco
 *
 */
public class ZookeeperConfiguration {
    private static final Logger logger = Logger.getLogger(ZookeeperConfiguration.class
            .getCanonicalName());

    private static final String ZK_CONFIG_FILE = "/zookeeper_config.properties";
    private static final String ZK_HOSTS = "zookeper.hosts";
    public static final String ZK_TIMEOUT = "zookeeper.session_timeout";
    private static final String ZK_ROOT = "zookeeper.base_path";

    Properties zkProps = new Properties();

    public ZookeeperConfiguration() {
        try {
            zkProps.load(getClass().getResourceAsStream(ZK_CONFIG_FILE));
        } catch (IOException e) {
            logger.severe("Could not find ZK zkConfiguration properties, " +
                    "this means nothing here will probably work at all. The error was: " +
                    e.getLocalizedMessage());
        }
    }

    public String hosts() {
        return zkProps.getProperty(ZK_HOSTS);
    }

    public int timeout() {
        return Integer.parseInt(zkProps.getProperty(ZK_TIMEOUT));
    }

    public String base_path() {
        return zkProps.getProperty(ZK_ROOT);
    }
}
