package com.rivermeadow.babysitter.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.xeustechnologies.jcl.JarClassLoader;

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
    JarClassLoader jcl() {
        return new JarClassLoader();
    }
}
