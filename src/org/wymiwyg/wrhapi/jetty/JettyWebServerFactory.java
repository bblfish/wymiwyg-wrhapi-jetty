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
package org.wymiwyg.wrhapi.jetty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.BlockingChannelConnector;

import org.wymiwyg.wrhapi.Handler;
import org.wymiwyg.wrhapi.HandlerException;
import org.wymiwyg.wrhapi.ResponseStatus;
import org.wymiwyg.wrhapi.ServerBinding;
import org.wymiwyg.wrhapi.WebServer;
import org.wymiwyg.wrhapi.WebServerFactory;
import org.wymiwyg.wrhapi.util.MessageBody2Write;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author reto
 *
 */
public class JettyWebServerFactory extends WebServerFactory {
	
	private static final Log log = LogFactory.getLog(JettyWebServerFactory.class);
	
    /* (non-Javadoc)
     * @see org.wymiwyg.wrhapi.WebServerFactory#startNewWebServer(org.wymiwyg.wrhapi.Handler, org.wymiwyg.wrhapi.ServerBinding)
     */
    public WebServer startNewWebServer(final Handler handler,
        ServerBinding configuration) throws IOException {
        Server server = new Server() {
                /*
                 * public void handle(HttpConnection connection) {
                 * System.out.println("hi"); }
                 */
            };

        server.addHandler(new AbstractHandler() {
                public void handle(String arg0,
                    HttpServletRequest servletRequest,
                    HttpServletResponse servletResponse, int arg3)
                    throws IOException, ServletException {
                	ResponseImpl responseImpl = new ResponseImpl(servletResponse);
                    try {
                        
                        handler.handle(new RequestImpl(servletRequest),
                            responseImpl);
                        
                    } catch (final HandlerException e) {
                    	responseImpl.setResponseStatus(e.getStatus());
                    	try {
							responseImpl.setBody(new MessageBody2Write() {
								public void writeTo(WritableByteChannel out) throws IOException {
									PrintWriter printWriter = new PrintWriter(Channels.newWriter(out, "utf-8"));
									printWriter.println(e.getMessage());
									printWriter.close();
								}
								
							});
						} catch (HandlerException e1) {
							throw new RuntimeException(e1);
						}
                    } /*catch  (final RuntimeException e) {
                    	log.error("Runtime exception handling request", e);
                    	responseImpl.setResponseStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                    	try {
							responseImpl.setBody(new MessageBody2Write() {
								public void writeTo(WritableByteChannel out) throws IOException {
									PrintWriter printWriter = new PrintWriter(Channels.newWriter(out, "utf-8"));
									printWriter.println("A runtime exception occured (see logs for details)");
									printWriter.close();
								}
								
							});
						} catch (HandlerException e1) {
							throw new RuntimeException(e1);
						}
                    } */
                    try {
						responseImpl.commitIfNeeded();
					} catch (HandlerException e) {
						throw new RuntimeException(e);
					}
                }
            });

        Connector connector = new BlockingChannelConnector();
        connector.setPort(configuration.getPort());
        server.addConnector(connector);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new JettyWebServer(server);
    }
}
