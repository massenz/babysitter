package com.rivermeadow.babysitter.spring;

import com.rivermeadow.babysitter.alerts.AlertManager;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.alerts.mandrill.MandrillEmailAlertPager;
import com.rivermeadow.babysitter.zookeper.EvictionListener;
import com.rivermeadow.babysitter.zookeper.NodesManager;
import com.rivermeadow.babysitter.zookeper.RegistrationListener;
import com.rivermeadow.babysitter.zookeper.ZookeeperConfiguration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import java.util.concurrent.TimeUnit;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */

@Configuration
@EnableConfigurationProperties
public class BeanConfiguration {

    AlertManager alertManager;

    @Value("${mandrill.api_key}")
    String apiKey;

    @Value("${mandrill.email_template.location}")
    String templateLocation;

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

    public AlertManager getAlertManager() {
        if (alertManager == null) {
            alertManager = new AlertManager();
            // TODO: this should actually be driven by a configuration file or even auto-discovery
            alertManager.addPager(new MandrillEmailAlertPager(apiKey, templateLocation));
        }
        return alertManager;
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
}
