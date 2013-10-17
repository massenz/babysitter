package com.rivermeadow.babysitter.spring;

import com.rivermeadow.babysitter.alerts.AlertManager;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import com.rivermeadow.babysitter.zookeper.RegistrationListener;
import com.rivermeadow.babysitter.zookeper.ZookeeperConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */

@Configuration
public class BeanConfiguration {

    AlertManager alertManager;

    @Bean
    @Scope("singleton")
    ZookeeperConfiguration zkConfiguration() {
        return new ZookeeperConfiguration();
    }

    @Bean
    @Scope("singleton")
    NodesManager nodesManager() {
        return new NodesManager(zkConfiguration());
    }

    @Bean
    @Scope("singleton")
    EvictionListener evictionListener() {
        if (alertManager == null) {
            alertManager = new AlertManager();
        }
        return alertManager;
    }

    @Bean
    @Scope("singleton")
    RegistrationListener registrationListener() {
        if (alertManager == null) {
            alertManager = new AlertManager();
        }
        return alertManager;
    }
}
