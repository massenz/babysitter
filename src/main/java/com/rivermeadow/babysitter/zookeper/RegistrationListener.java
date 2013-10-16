package com.rivermeadow.babysitter.zookeper;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.Status;

/**
 * This listener is invoked when a new server joins the pool of monitored servers
 *
 * @author marco
 *
 */
public interface RegistrationListener {

    public Status register(Server server);
}
