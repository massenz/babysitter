package com.rivermeadow.babysitter.zookeper;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import com.rivermeadow.babysitter.model.Server;
import com.rivermeadow.babysitter.model.ServerAddress;

import static junit.framework.Assert.assertEquals;

/**
 * TODO: learn about unit testing ZK
 *
 * @author marco
 *
 */
public class NodesManagerTest {

    public static final String BASE_PATH = "/tests/monitor";
    public static final String ALERTS_PATH = "/tests/alerts";
    NodesManager instance = new NodesManager();

    @Before
    public void setup() {
        ZookeeperConfiguration config = new ZookeeperConfiguration();
        config.setBasePath(BASE_PATH);
        config.setAlertsPath(ALERTS_PATH);
        instance.zkConfiguration = config;
    }

    @Test
    public void testProcess() throws Exception {

    }

    @Test
    public void testGetMonitoredServers() throws Exception {

    }

    @Test
    public void testComputeDiff() throws Exception {

    }

    @Test
    public void testGetServerInfo() throws Exception {

    }

    @Test
    public void testSilence() throws Exception {

    }

    @Test
    public void testRemoveSilence() throws Exception {

    }

    @Test
    public void testBuildAlertPathForServer() throws Exception {
        String name = "my_server-1234";
        assertEquals(ALERTS_PATH + File.separator + name, instance.buildAlertPathForServer(name));

        ServerAddress address = new ServerAddress(name, "");
        Server server = new Server(address, 80, 30);
        assertEquals(ALERTS_PATH + File.separator + name, instance.buildAlertPathForServer(server));
    }

    @Test
    public void testBuildMonitorPathForServer() throws Exception {
        String serverHostName = "foo";
        assertEquals(BASE_PATH + "/foo", instance.buildMonitorPathForServer(serverHostName));
        serverHostName = ALERTS_PATH + "/foo";
        assertEquals(BASE_PATH + "/foo", instance.buildMonitorPathForServer(serverHostName));
        serverHostName = BASE_PATH + "/foo";
        assertEquals(BASE_PATH + "/foo", instance.buildMonitorPathForServer(serverHostName));
    }
}
