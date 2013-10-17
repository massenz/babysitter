package com.rivermeadow.babysitter.spring;

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

    @Bean
    @Scope("singleton")
    NodesManager getNodesManager() {
        return new NodesManager();
    }

    @Bean
    @Scope("singleton")
    EvictionListener evictionListener() {
        return null;
    }

    @Bean
    @Scope("singleton")
    RegistrationListener registrationListener() {
        return null;
    }

    @Bean
    @Scope("singleton")
    ZookeeperConfiguration configuration() {
        return new ZookeeperConfiguration();
    }

}
