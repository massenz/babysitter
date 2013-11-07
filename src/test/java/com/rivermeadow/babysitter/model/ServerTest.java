package com.rivermeadow.babysitter.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class ServerTest {
    private static String JSON = "src/test/resources/server.json";
    private static String JSON_NO_DATA = "src/test/resources/server-no-data.json";
    private static String JSON_EMPTY = "src/test/resources/server-empty-payload.json";
    String json, json_no_data, json_empty_data;

    ObjectMapper mapper = new ObjectMapper();

    String readJsonFile(String file) {
        Path filepath = Paths.get(file);
        assertTrue("File " + filepath.toAbsolutePath() + " does not " +
                "exist", Files.exists(filepath));
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filepath, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line.trim());
            }
        } catch (IOException e) {
            fail("Server JSON file could not be opened: " + e.getLocalizedMessage());
        }
        assertFalse("No data read from JSON file " + filepath.toAbsolutePath(), sb.length() == 0);
        return sb.toString();
    }

    @Before
    public void setupData() {
        // TODO: make it more flexible!
        json = readJsonFile(JSON);
        json_no_data = readJsonFile(JSON_NO_DATA);
        json_empty_data = readJsonFile(JSON_EMPTY);
    }

    @Test
     public  void testParseJson() throws Exception {
        Server server = mapper.readValue(new File(JSON), Server.class);
        assertNotNull(server);
        assertEquals("simpleserver", server.getServerType());
        assertEquals("192.168.1.61", server.getServerAddress().getIp());
    }

    @Test
    public  void testParseJsonNoData() throws Exception {
        Server server = mapper.readValue(new File(JSON_NO_DATA), Server.class);
        assertNotNull(server);
        assertEquals("no_payload", server.getServerType());
        assertEquals("10.10.121.235", server.getServerAddress().getIp());
        assertNull(server.getData());
    }

    @Test
    public  void testParseJsonEmptyData() throws Exception {
        Server server = mapper.readValue(new File(JSON_EMPTY), Server.class);
        assertNotNull(server);
        assertEquals("empty", server.getServerType());
        assertEquals("72.125.63.10", server.getServerAddress().getIp());
        assertTrue(server.getData() instanceof Map);
        Map<String, ?> payload = (Map<String, ?>) server.getData();
        assertTrue(payload.isEmpty());
    }

    // TODO: we should really test equals() and hashCode() correctness
}
