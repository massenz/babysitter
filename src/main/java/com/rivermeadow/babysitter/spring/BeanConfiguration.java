package com.rivermeadow.babysitter.spring;

import com.rivermeadow.babysitter.alerts.AlertManager;
import com.rivermeadow.babysitter.alerts.Context;
import com.rivermeadow.babysitter.alerts.PluginRegistry;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import com.rivermeadow.babysitter.zookeper.RegistrationListener;
import com.rivermeadow.babysitter.zookeper.ZookeeperConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.xeustechnologies.jcl.JarClassLoader;

/**
 * TODO: enter class description here
 *
 * @author marco
 */

@Configuration
@EnableConfigurationProperties
public class BeanConfiguration {

    @Value("${plugin.config_path}")
    String configPath;

    AlertManager alertManager = new AlertManager();
    Context pluginsContext;

    @Bean
    @Scope("singleton")
    ZookeeperConfiguration zkConfiguration() {
        return new ZookeeperConfiguration();
    }

    @Bean
    @Scope("singleton")
    NodesManager nodesManager() {
        return new NodesManager();
    }

    @Bean
    @Scope("singleton")
    Context pluginsContext() {
        if (pluginsContext == null) {
            pluginsContext = new Context(configPath);
        }
        return pluginsContext;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    @Bean
    @Scope("singleton")
    PluginRegistry registry() {
        return new PluginRegistry(getAlertManager(), pluginsContext());
    }

    @Bean
    @Scope("singleton")
    EvictionListener evictionListener() {
        return getAlertManager();
    }

    @Bean
    @Scope("singleton")
    RegistrationListener registrationListener() {
        return getAlertManager();
    }

    @Bean
    @Scope("singleton")
    JarClassLoader jcl() {
        return new JarClassLoader();
    }
}
