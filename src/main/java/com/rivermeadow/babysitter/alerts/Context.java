package com.rivermeadow.babysitter.alerts;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 */
@Component
@ConfigurationProperties(name = "plugin")
public class Context {

    @Value("${plugin.config_path}")
    String configPath;

    Map<String, Object> properties = Maps.newHashMap();

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
