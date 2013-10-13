package com.rivermeadow.babysitter.spring;

import com.rivermeadow.babysitter.zookeper.BeatListener;
import com.rivermeadow.babysitter.zookeper.NodesManager;
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
    BeatListener getBeatListener() {
        return new NodesManager();
    }
}
