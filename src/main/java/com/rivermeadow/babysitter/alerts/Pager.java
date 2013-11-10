package com.rivermeadow.babysitter.alerts;

import com.rivermeadow.babysitter.model.Server;

/**
 * Event management interface for pluggable alert pagers.
 *
 * All registered @link{AlertPlugin}s need to provide a @code{Pager} implementation when
 * @link{activated AlertPlugin#activate()} which will be registered with the @link{AlertManager}
 * and triggered (`paged`) when a server unexpectedly terminates connection with ZooKeeper.
 *
 * @author marco
 *
 */
public interface Pager {
    public void page(Server server);
}
