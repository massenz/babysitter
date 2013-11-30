package com.rivermeadow.babysitter.alerts;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 */
@Component
public class Context {

    String configPath;

    Map<String, Object> properties = Maps.newHashMap();

    public Context(String configPath) {
        this.configPath = configPath;
    }

    public Object getNamedProperty(String name) {
        return properties.get(name);
    }

    public void setNamedProperty(String name, Object value) {
        properties.put(name, value);
    }

    public String getConfigPath() {
        return configPath;
    }
}
