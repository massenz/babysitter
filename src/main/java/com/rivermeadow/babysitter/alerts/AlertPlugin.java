package com.rivermeadow.babysitter.alerts;

/**
 * Main lifecycle interface for plugins.
 *
 * @author marco
 */
public interface AlertPlugin {
    /**
     * Sets up the plugin before activating it.
     *
     * <p>This is where expensive resources and other initialization activities should be conducted: this method is
     * guaranteed to be called once (and only once) for the plugin and always before the plugin is @link{started up
     * #startup()}.
     *
     * @param pluginContext provides a means by which the plugin can access system properties, as well as stash objects
     *      that may later be used by the Pager; it is left to the plugin implementation how to ensure the Context is
     *      reachable from the Pager implementation.
     */
    public void startup(Context pluginContext);

    /**
     * Activates the plugin to respond to incoming alerts.
     * <p>The returned Pager will be registered with the @link{AlertManager} so that it gets triggered
     *
     * <p>Here the plugin can re-activate suspended resources.
     *
     * @return a Pager to respond to alerts
     */
    public Pager activate();

    /**
     * Deactivates the plugin: while the plugin is not active, no alerts will be sent.
     * The plugin may be reactivated at a later time, at which point alerts will be sent again.
     *
     * Only inactive plugins can be shut down (see @link{#shutdown()}).
     *
     * This is an opportunity for the plugin to pause (and/or dispose of) expensive resources, that can be
     * re-acquired during the @link{#activate()} method.
     */
    public void deactivate();

    /**
     * Terminates the plugin
     * A terminated plugin cannot be re-activated: this is where the plugin ought to release any resources that
     * will no longer be needed.
     */
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
