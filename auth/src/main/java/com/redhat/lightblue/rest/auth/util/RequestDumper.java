/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.auth.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestDumper extends HttpServlet implements Servlet {

    private static final long serialVersionUID = 1L;

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<html><head><title>" + "Request Dumper</title></head>");
        out.println("<body><pre>");

        out.println(dump(req));
        out.println("</pre></body></html>");
    }

    public static String dump(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder("\n\n");

        buf.append("REQUEST:\n--------\n");
        if (request.getUserPrincipal() != null) {
            buf.append("Principal name: [").append(request.getUserPrincipal().getName()).append("]\n");
        } else {
            buf.append("Principal is [null]\n");
        }

        buf.append("AuthType: [").append(request.getAuthType()).append("]\n");
        buf.append("request URI: [").append(request.getRequestURI()).append("]\n");
        buf.append("request URL: [").append(request.getRequestURL().toString()).append("]\n");
        buf.append("isRequestedSessionIdFromCookie: [").append(request.isRequestedSessionIdFromCookie()).append("]\n");
        buf.append("isRequestedSessionIdFromURL: [").append(request.isRequestedSessionIdFromURL()).append("]\n");
        buf.append("isRequestedSessionIdValid: [").append(request.isRequestedSessionIdValid()).append("]\n");
        buf.append("isSecure: [").append(request.isSecure()).append("]\n");
        buf.append("In authenticated role?: [").append(request.isUserInRole("authenticated")).append("]\n");
        buf.append("In lightblue-user role?: [").append(request.isUserInRole("lightblue-user")).append("]\n");
        buf.append("In user-admin role?: [").append(request.isUserInRole("user-admin")).append("]\n");
        buf.append("In readonly role?: [").append(request.isUserInRole("readonly")).append("]\n");
        buf.append("In updater role?: [").append(request.isUserInRole("updater")).append("]\n");
        buf.append("In nonexistant role?: [").append(request.isUserInRole("nonexistant")).append("]\n");

        buf.append("\n\n");

        buf.append("BODY: \n------\n");

        StringBuilder requestBuffer = new StringBuilder();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                requestBuffer.append(line.trim());
            }
        } catch (Exception e) { /*report an error*/ }

        buf.append(requestBuffer.toString());

        buf.append("\n\n");

        buf.append("HEADERS: \n------\n");

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            buf.append("    ");
            buf.append(name);
            buf.append("=");
            buf.append(value);
            buf.append("\n");
        }

        buf.append("COOKIES:\n-------\n");
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cooky : cookies) {
                buf.append("Cookie: [").append(cooky.getName()).append("] Value: [").append(cooky.getValue()).append("]\n");
                buf.append("    comment: [").append(cooky.getComment()).append("]\n");
                buf.append("    domain: [").append(cooky.getDomain()).append("]\n");
                buf.append("    maxAge: [").append(cooky.getMaxAge()).append("]\n");
                buf.append("    path: [").append(cooky.getPath()).append("]\n");
                buf.append("    secure?: [").append(cooky.getSecure()).append("]\n");
                buf.append("    version: [").append(cooky.getVersion()).append("]\n");
            }
        }

        return (buf.toString());
    }
}
