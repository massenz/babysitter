package com.rivermeadow.babysitter.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivermeadow.babysitter.Constants;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rivermeadow.babysitter.plugins.utils.ResourceLocator
        .getResourceFromSystemProperty;

/**
 * Builds an in-memory map of Users from a bootstrap configuration file (JSON) and looks them up.
 */
@Service
@Profile("test")
public class BootstrapUserService implements UserDetailsService {

    Logger logger = Logger.getLogger(BootstrapUserService.class);
    Map<String, Map<String, ?>> users = new HashMap<>();


    public BootstrapUserService() {
        logger.debug(String.format("Bootstrapping User Details Service with file from %s",
                System.getProperty(Constants.BOOTSTRAP_LOCATION)));
        ObjectMapper mapper = new ObjectMapper();
        try {
            InputStream bootstrapFile = getResourceFromSystemProperty(Constants.BOOTSTRAP_LOCATION);
            List<Map<String, ?>> usersObjects = (List<Map<String, ?>>)
                    mapper.readValue(bootstrapFile, Map.class).get("users");
            for (Map<String, ?> userObj : usersObjects) {
                String username = (String) userObj.get("name");
                logger.debug(String.format("Adding bootstrap user %s", username));

                // Simply store the raw Map object in the map, indexed by username; it will
                // de-serialized upon request in the `loadUserByUsername()` method:
                users.put(username, userObj);
            }
        } catch (Exception ex) {
            logger.error("Could not initialize user lists from bootstrap file %s", ex);
            // We cannot progress from here, as most likely nothing will work from now on
            throw new RuntimeException("Error while loading users from bootstrap file", ex);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Map<String, ?> userDetails = users.get(username);
        if (userDetails != null) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : (List<String>) userDetails.get("roles")) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
            return new User(
                    username,
                    (String) userDetails.get("password"),
                    authorities
            );
        }
        return null;
    }
}
