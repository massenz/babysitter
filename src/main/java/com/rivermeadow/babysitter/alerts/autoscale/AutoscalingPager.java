package com.rivermeadow.babysitter.alerts.autoscale;

import com.google.common.collect.Lists;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.model.Server;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * This pager attempts to re-spawn a processAndArgs that has been unexpectedly terminated.
 *
 * @author marco
 *
 */
public class AutoscalingPager implements Pager {

    public static final Logger logger = Logger.getLogger(AutoscalingPager.class);

    private final List<String> processAndArgs;

    public AutoscalingPager(String process) {
        this.processAndArgs = Lists.newArrayList(process.split(" "));
        this.processAndArgs.add("--desc");
        this.processAndArgs.add("Process respawned by the Awesome Respawn Plugin");
    }

    @Override
    public void page(Server server) {
        // TODO: this should actually attempt to launch a remote server of the same ``type`` as server
        // TODO: how do we handle SSH keys?
        logger.info(String.format("Re-spawning processAndArgs: %s on host: %s [%s]", processAndArgs,
                server.getName(), server.getServerAddress().getIp()));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process respawn = Runtime.getRuntime().exec(processAndArgs.toArray(
                            new String [0]));
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
                    logger.info(String.format("Process %s terminated with exit code %d", processAndArgs, respawn.exitValue()));
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
}
