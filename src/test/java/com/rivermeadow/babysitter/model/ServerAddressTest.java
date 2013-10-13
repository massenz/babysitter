package com.rivermeadow.babysitter.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class ServerAddressTest {

    @Test
    public void testDeserializer() throws IOException {
        String address = "{\"ip\":\"192.168.1.50\",\"hostname\":\"mordor\"}";
        String json =
                "{\"server_address\": {" +
                        "    \"ip\": \"192.168.2.53\"," +
                        "    \"hostname\": \"foo\" " +
                        "}," +
                        "\"ttl\": 60, " +
                        "\"max_missed\": 5," +
                        "\"type\": \"simpleserver\", " +
                        "\"desc\": \"A simple heartbeat server\"," +
                        "\"port\": 9099}";

        ObjectMapper mapper = new ObjectMapper();
        ServerAddress a2 = new ServerAddress("mordor", "192.168.1.50");
        mapper.writeValue(System.out, a2);
        ServerAddress addr = mapper.readValue(address, ServerAddress.class);
        assertNotNull(addr);
        Server server = mapper.readValue(json, Server.class);
        assertNotNull(server);

    }
}
