package com.rivermeadow.babysitter.zookeper;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Manages the ZK zkConfiguration options, by reading them from the application
 * configuration file (see {@code config/application.properties})
 *
 * @author marco
 *
 */
@Component
@ConfigurationProperties(name = "zookeeper")
public class ZookeeperConfiguration {

    String hosts;
    String basePath;
    String alertsPath;
    Integer sessionTimeout;
    String configPath;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getAlertsPath() {
        return alertsPath;
    }

    public void setAlertsPath(String alertsPath) {
        this.alertsPath = alertsPath;
    }
}
