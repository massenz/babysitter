package com.rivermeadow.babysitter.plugins.autoscale;

import com.google.common.collect.Lists;
import com.rivermeadow.babysitter.alerts.AlertPlugin;
import com.rivermeadow.babysitter.alerts.Context;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.model.Server;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * This pager attempts to re-spawn a process that has been unexpectedly terminated.
 *
 * It is just a demo plugin that should not be used in Production; it has no real use.
 *
 * @author marco
 *
 */
public class AutoscalingPager implements AlertPlugin, Pager {

    public static final Logger logger = Logger.getLogger(AutoscalingPager.class);
    public static final String PLUGIN_CONFIG = "autoscale.cfg";
    public static final String PROCESS_PROPERTY = "autoscale.process";
    private static final String PLUGIN_NAME = "Awesome Respawn Plugin";
    private static final String PLUGIN_DESCRIPTION = "A plugin that re-spawns a process that has " +
            "" + "been unexpectedly terminated";
    private List<String> processAndArgs;

    public AutoscalingPager() {
    }

    @Override
    public void page(Server server) {
        logger.info(String.format("Re-spawning: %s on host: %s [%s]", processAndArgs,
                server.getName(), server.getServerAddress().getIp()));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process respawn = Runtime.getRuntime().exec(processAndArgs.toArray(new
                            String[0]));
                    // Badly named method, this is actually the STDOUT of the processAndArgs
                    InputStream is = respawn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    char[] buffer = new char[1024];
                    int numBytes;
                    while ((numBytes = reader.read(buffer)) != -1) {
                        String msg = new String(buffer, 0, numBytes);
                        logger.info(String.format("[%s] STDOUT: %s", processAndArgs, msg));
                    }
                    respawn.waitFor();
                    logger.info(String.format("Process %s terminated with exit code %d",
                            processAndArgs, respawn.exitValue()));
                    if (respawn.exitValue() != 0) {
                        reader = new InputStreamReader(respawn.getErrorStream());
                        while ((numBytes = reader.read(buffer)) != -1) {
                            String msg = new String(buffer, 0, numBytes);
                            logger.info(String.format("[%s] STDERR: %s", processAndArgs, msg));
                        }
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }).start();
    }

    @Override
    public void startup(Context pluginContext) {
        Path configFile = Paths.get(pluginContext.getConfigPath(), PLUGIN_CONFIG);
        Properties configs = new Properties();
        try {
            configs.load(new FileInputStream(configFile.toFile()));
            this.processAndArgs = Lists.newArrayList(configs.getProperty(PROCESS_PROPERTY).split
                    (" "));
            this.processAndArgs.add("--desc");
            this.processAndArgs.add("Process respawned by the Awesome Respawn Plugin");
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public Pager activate() {
        return this;
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_DESCRIPTION;
    }
}
