package com.redhat.lightblue.rest.crud.testsupport;

import com.redhat.lightblue.rest.crud.RestApplication;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;

public abstract class CrudWebXmls {
    private static final String RESTEASY_SERVLET_NAME = "RESTEasy";

    private static DocumentBuilder documentBuilder;

    /**
     * Uses the production web.xml, but enriches it with standalone Weld and RESTEasy so it may run
     * more portably in non full EE profile containers like Jetty or Tomcat.
     */
    public static Document forNonEE6Container() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder docBuilder = documentBuilder();
        Document webXml = docBuilder.parse(Paths.get("src/main/webapp/WEB-INF/web.xml").toFile());
        Node webApp = webXml.getFirstChild();

        webApp.appendChild(webXml.createElement("listener"))
                .appendChild(webXml.createElement("listener-class"))
                .appendChild(webXml.createTextNode("org.jboss.weld.environment.servlet.Listener"));

        Node restEasy = webApp.appendChild(webXml.createElement("servlet"));
        restEasy.appendChild(webXml.createElement("servlet-name"))
                .appendChild(webXml.createTextNode(RESTEASY_SERVLET_NAME));
        restEasy.appendChild(webXml.createElement("servlet-class"))
                .appendChild(webXml.createTextNode(HttpServletDispatcher.class.getName()));
        Node restEasyParams = restEasy.appendChild(webXml.createElement("init-param"));
        restEasyParams.appendChild(webXml.createElement("param-name")
                .appendChild(webXml.createTextNode("javax.ws.rs.Application")));
        restEasyParams.appendChild(webXml.createElement("param-value")
                .appendChild(webXml.createTextNode(RestApplication.class.getName())));

        Node restEasyMapping = webApp.appendChild(webXml.createElement("servlet-mapping"));
        restEasyMapping.appendChild(webXml.createElement("servlet-name"))
                .appendChild(webXml.createTextNode(RESTEASY_SERVLET_NAME));
        restEasyMapping.appendChild(webXml.createElement("url-pattern"))
                .appendChild(webXml.createTextNode("/*"));

        return webXml;
    }

    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        if (documentBuilder == null) {
            synchronized (CrudWebXmls.class) {
                if (documentBuilder == null) {
                    documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                }
            }
        }

        return documentBuilder;
    }
}
