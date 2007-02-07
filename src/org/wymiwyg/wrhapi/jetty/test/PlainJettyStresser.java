/*
 * Copyright  2002-2006 WYMIWYG (http://wymiwyg.org)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.wymiwyg.wrhapi.jetty.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author reto
 *
 */
public class PlainJettyStresser {
    private final static Log log = LogFactory.getLog(PlainJettyStresser.class);

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100; i++) {
            log.info("round " + i);

            Server server = new Server();
            server.addHandler(new AbstractHandler() {
                    public void handle(String arg0, HttpServletRequest request,
                        HttpServletResponse response, int arg3)
                        throws IOException, ServletException {
                        response.setStatus(200);
                        response.setHeader("Content-Type", "text/plain");

                        final OutputStream out = response.getOutputStream();
                        out.write("Hello".getBytes());
                        out.close();
                    }
                });

            //Connector connector = new BlockingChannelConnector(); //works
            Connector connector = new SelectChannelConnector(); //works for very few rounds
            connector.setPort(8181);
            server.addConnector(connector);
            server.start();
            Thread.sleep(100);

            URL serverURL = new URL("http://localhost:8181/");
            InputStream in = serverURL.openStream();

            for (int ch = in.read(); ch != -1; ch = in.read()) {
                System.out.write(ch);
            }

            System.out.println();
            in.close();
            server.stop();

            while (server.isStopping()) {
                log.info("waiting for jetty to stop");
                Thread.sleep(100);
            }
        }
    }
}
