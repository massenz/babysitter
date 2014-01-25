package com.rivermeadow.babysitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetAddress;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class Server {

    @JsonProperty("server_address")
    ServerAddress serverAddress;

    int port;

    @JsonProperty(value = "ttl")
    int ttlSeconds;

    @JsonProperty("max_missed")
    int maxMissed;

    @JsonProperty("type")
    String serverType;

    @JsonProperty("desc")
    String description;

    @JsonProperty(value = "payload", required = false)
    Object data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (port != server.port) return false;
        if (!serverAddress.equals(server.serverAddress)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serverAddress.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return getName() + " :: " + getDescription();
    }

    public String getName() {
        return serverAddress.getHostname();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerAddress getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(ServerAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public int getPort() {
        return port;
    }


    public int getMaxMissed() {

        return maxMissed;
    }

    public void setMaxMissed(int maxMissed) {
        this.maxMissed = maxMissed;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Default constructor, only useful for the JSON deserializer.
     * Should not be used, but may be useful for serializers, injections, etc.
     */
    Server() {
        this(new ServerAddress(), 0, 0);
    }

    /**
     * Constructs a server, listening on a given port, requiring a ping every @code{ttlSeconds}
     *
     * @param serverAddress server's hostname and address
     * @param port where the server is operating
     * @param ttlSeconds interval between pings
     */
    public Server(ServerAddress serverAddress, int port, int ttlSeconds) {
        this.port = port;
        this.serverAddress = serverAddress;
        this.ttlSeconds = ttlSeconds;
    }
}
