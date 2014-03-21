package com.rivermeadow.babysitter.plugins.mandrill;

import com.rivermeadow.babysitter.alerts.AlertPlugin;
import com.rivermeadow.babysitter.alerts.Context;
import com.rivermeadow.babysitter.alerts.Pager;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class MandrillEmailPlugin implements AlertPlugin {

    // TODO: this should be configurable via a system property
    public static final String CONFIG_FILE = "mandrill-email.cfg";
    public static final String NAME = "Mandrill Email Alert plugin";
    public static final String DESC = "Sends an email alert via the Mandrill (http://mandrill" +
            ".com) email service using the REST API.";

    private static final Logger log = Logger.getLogger(MandrillEmailPlugin.class);
    private static final String API_KEY = "mandrill.api_key";
    private static final String TEMPLATE_LOCATION = "mandrill.email_template.location";

    Pager pager;
    Context ctx;

    @Override
    public void startup(Context pluginContext) {
        this.ctx = pluginContext;
        Path configFilepath = this.ctx.getConfigAbsPath().resolve(CONFIG_FILE);
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFilepath.toFile()));
            pager = new MandrillEmailAlertPager(props.getProperty(API_KEY),
                    props.getProperty(TEMPLATE_LOCATION));
        } catch (IOException e) {
            log.error("Could not load configuration file for " + getName(), e);
        }
    }

    @Override
    public Pager activate() {
        return pager;
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void shutdown() {
        pager = null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }
}
