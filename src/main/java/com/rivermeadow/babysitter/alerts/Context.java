package com.rivermeadow.babysitter.alerts;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 */
public class Context {

    Map<String, Object> properties = Maps.newHashMap();

    public Object getNamedProperty(String name) {
        return properties.get(name);
    }

    public void setNamedProperty(String name, Object value) {
        properties.put(name, value);
    }
}
