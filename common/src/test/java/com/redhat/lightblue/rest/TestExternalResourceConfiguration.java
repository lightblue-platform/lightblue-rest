package com.redhat.lightblue.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.redhat.lightblue.util.JsonUtils;

public class TestExternalResourceConfiguration {

    private void assertUrlEquals(URL url, String fileName, String protocol) {
        assertNotNull(url);
        assertEquals(protocol, url.getProtocol());
        String[] parts = url.getPath().split("/");
        assertEquals(fileName, parts[parts.length - 1]);
    }

    @Test
    public void testHttpURL() throws Exception {
        ExternalResourceConfiguration config = new ExternalResourceConfiguration(JsonUtils.json(
                "[\"http://www.somesite.com/my.jar\"]"));

        Set<URL> urls = config.getExternalUrls();
        assertNotNull(urls);
        assertEquals(1, urls.size());

        assertUrlEquals(urls.iterator().next(), "my.jar", "http");
    }

    @Test
    public void testFileURL() throws Exception {
        URL dirUrl = getClass().getResource(
                "/externalResourcesConfiguration");

        ExternalResourceConfiguration config = new ExternalResourceConfiguration(JsonUtils.json(
                "[\"file:///" + dirUrl.getPath() + "\"]"));

        Set<URL> urls = config.getExternalUrls();
        assertNotNull(urls);
        assertEquals(2, urls.size());

        //Clunky, but guarantees the order.
        List<URL> sortedUrls = new ArrayList<URL>(urls);
        Collections.sort(sortedUrls, new Comparator<URL>(){

            @Override
            public int compare(URL o1, URL o2) {
                return o1.getPath().compareTo(o2.getPath());
            }

        });

        assertUrlEquals(sortedUrls.get(0), "not_really_a_jar.jar", "file");
        assertUrlEquals(sortedUrls.get(1), "also_not_really_a_jar.jar", "file");
    }

}
