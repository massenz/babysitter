package com.rivermeadow.babysitter.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivermeadow.babysitter.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders
        .AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning
        .InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration
        .WebSecurityConfigurerAdapter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import static com.rivermeadow.babysitter.plugins.utils.ResourceLocator
        .getResourceFromSystemProperty;

/**
 * Spring Security configuration class.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    Logger logger = Logger.getLogger(SecurityConfig.class);


    public static class User {
        public String name;
        public String password;
        public List<String> roles = new ArrayList<>();
    }

    private List<User> bootstrapUsers() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            InputStream bootstrapFile = getResourceFromSystemProperty(Constants.BOOTSTRAP_LOCATION);
            List<Map<String, ?>> usersObjects = (List<Map<String, ?>>)
                    mapper.readValue(bootstrapFile, Map.class).get("users");
            List<User> users = new ArrayList<>(usersObjects.size());
            for (Map<String, ?> userObj : usersObjects) {
                User u = new User();
                u.name = (String) userObj.get("name");
                u.password = (String) userObj.get("password");
                for (String role : (List<String>) userObj.get("roles")) {
                    u.roles.add(role);
                }
                users.add(u);
            }
            return users;
        } catch (Exception ex) {
            logger.error("Could not initialize user lists from bootstrap file %s", ex);
            // We cannot progress from here, as most likely nothing will work from now on
            throw new RuntimeException("Error while loading users from bootstrap file", ex);
        }
    }

    @Override
    protected void configure(AuthenticationManagerBuilder builder) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> configurer =
            builder.inMemoryAuthentication();
        List<User> users = bootstrapUsers();
        for (User user : users) {
            logger.debug(String.format("Adding bootstrap user %s", user.name));
            configurer.withUser(user.name)
                    .password(user.password)
                    .roles(user.roles.toArray(new String[user.roles.size()]));
        }
    }
}
