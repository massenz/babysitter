package com.rivermeadow.babysitter.plugins.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Utility class to extract a resource's data from its generic URI location.
 *
 * <p>This works by providing a URI to the desired resource and obtaining the
 * relative @link{InputStream} regardless of whether this is a file in the local
 * filesystem (@literal{file:}), a classpath resource (@literal{classpath:}) or a
 * resource on a remote server (eg a @literal{http:} URL).</p>
 *
 * @author marco
 *
 */
public class ResourceLocator {
    public enum Scheme {
        CLASSPATH,
        FILE,
        HTTP,
        HTTPS;

        @Override
        public String toString() { return this.name().toLowerCase(); }

        public static Scheme parse(String value) {
            return valueOf(value.toUpperCase());
        }

        public static Scheme parse(URI uri) {
            return parse(uri.getScheme());
        }
    }

    public static InputStream getResourceFromUri(URI resourceUri) throws FileNotFoundException {
        switch (Scheme.parse(resourceUri)) {
            case CLASSPATH:
                return ResourceLocator.class.getResourceAsStream(resourceUri.getPath());
            case FILE:
                return new FileInputStream(Paths.get(resourceUri).toFile());
            case HTTP:
            case HTTPS:
                throw new UnsupportedOperationException("Downloading resources via HTTP{S} not " +
                        "implemented yet");
            default:
                throw new IllegalArgumentException("Could not parse " + resourceUri + " into a " +
                        "valid resource location");
        }
    }

    public static InputStream getResourceFromSystemProperty(String propertyName)
            throws URISyntaxException, FileNotFoundException {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return getResourceFromUri(new URI(propertyValue));
        }
        throw new IllegalArgumentException("Not a valid system property: " + propertyName);
    }
}
