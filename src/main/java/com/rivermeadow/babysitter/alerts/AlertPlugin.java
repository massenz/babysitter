package com.rivermeadow.babysitter.alerts;

/**
 * Main lifecycle interface for plugins.
 *
 * @author marco
 */
public interface AlertPlugin {
    // TODO: add AlertManager to the constructor signature, or a Context object of some sort
    public void startup();

    // TODO: add Pager as the return type for this
    public Pager activate();
    public void deactivate();
    public void shutdown();

    /**
     * A plugin should have a meaningful (and, hopefully, unique) name
     *
     * @return the plugin's name
     */
    public String getName();


    /**
     * A brief description of what this plugin does
     *
     * @return a brief description of what the plugin does
     */
    public String getDescription();
}
