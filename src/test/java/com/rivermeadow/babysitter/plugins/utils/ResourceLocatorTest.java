package com.rivermeadow.babysitter.plugins.utils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import junit.framework.Assert;

import static com.rivermeadow.babysitter.plugins.utils.ResourceLocator.*;
import static org.junit.Assert.*;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
public class ResourceLocatorTest {

    @BeforeClass
    public static void setupClass() {
        try {
            File temp = File.createTempFile("tmp-", ".tmp");
            System.out.println(temp.getAbsolutePath());
            temp.deleteOnExit();
            assertTrue(temp.exists());
            System.setProperty("test.resource.location", "file://" + temp.getAbsolutePath());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        }
    }
    @Test
    public void testGetResourceFromUri() throws Exception {
        String testFileLoc = "classpath:/locator/foo.txt";
        InputStream is = getResourceFromUri(new URI(testFileLoc));
        assertNotNull(is);
    }

    @Test
    public void testGetResourceFromSystemProperty() throws Exception {
        InputStream is = ResourceLocator.getResourceFromSystemProperty("test.resource.location");
        assertNotNull(is);
        assertEquals(FileInputStream.class, is.getClass());
    }

    @Test
    public void testGetFromHttpThrows() {
        try {
            getResourceFromUri(new URI("http://example.com/myTest.xml"));
            fail("HTTP method not supported, should have thrown");
        } catch (Exception ex) {
            assertEquals(UnsupportedOperationException.class, ex.getClass());
        }
    }
}
