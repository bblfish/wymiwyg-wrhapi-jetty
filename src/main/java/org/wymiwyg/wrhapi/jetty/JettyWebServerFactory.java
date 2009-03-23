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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.wymiwyg.wrhapi.Handler;
import org.wymiwyg.wrhapi.HandlerException;
import org.wymiwyg.wrhapi.MessageBody;
import org.wymiwyg.wrhapi.ServerBinding;
import org.wymiwyg.wrhapi.WebServer;
import org.wymiwyg.wrhapi.WebServerFactory;
import org.wymiwyg.wrhapi.util.MessageBody2Write;

/**
 * @author reto
 * @scr.component
 * @scr.service interface="org.wymiwyg.wrhapi.WebServerFactory"
 */
public class JettyWebServerFactory extends WebServerFactory {

	private static final Log log = LogFactory.getLog(JettyWebServerFactory.class);

	/* (non-Javadoc)
	 * @see org.wymiwyg.wrhapi.WebServerFactory#startNewWebServer(org.wymiwyg.wrhapi.Handler, org.wymiwyg.wrhapi.ServerBinding)
	 */
	public WebServer startNewWebServer(final Handler handler,
			final ServerBinding configuration) throws IOException {
		Server server = new Server() {
			/*
			 * public void handle(HttpConnection connection) {
			 * System.out.println("hi"); }
			 */
		};

		server.addHandler(new AbstractHandler() {

			public void handle(String arg0,
					HttpServletRequest servletRequest,
					final HttpServletResponse servletResponse, int arg3)
					throws IOException, ServletException {
				final ResponseImpl responseImpl = new ResponseImpl();
				try {

					handler.handle(new RequestImpl(servletRequest, configuration.getPort()),
							responseImpl);

				} catch (final HandlerException e) {
					responseImpl.setResponseStatus(e.getStatus());
					log.warn("Exception handling request", e);
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
				}
				final boolean[] headersWritten = new boolean[1];
				OutputStream out = servletResponse.getOutputStream();
				final MessageBody body = responseImpl.getBody();
				if (body != null) {
					
					WritableByteChannel outChannel = Channels.newChannel(out);
					FirstWriteOrCloseActionChannel fwOut = new FirstWriteOrCloseActionChannel(
							outChannel, new Runnable() {

						public void run() {
							commitStatusAndHeaders(servletResponse,
										responseImpl);
								headersWritten[0] = true;
						}
					});
					body.writeTo(fwOut);
				}

				if (!headersWritten[0]) {
					commitStatusAndHeaders(servletResponse, responseImpl);
				}
				out.close();

			}

			private void commitStatusAndHeaders(
					HttpServletResponse servletResponse,
					ResponseImpl responseImpl) {
				if (responseImpl.getStatus() != null) {
					servletResponse.setStatus(responseImpl.getStatus().getCode());
				} else {
					servletResponse.setStatus(200);
				}
				responseImpl.writeHeaders(servletResponse);
			}
		});

		Connector connector = new SelectChannelConnector();//BlockingChannelConnector();
		connector.setPort(configuration.getPort());
		server.addConnector(connector);

		try {
			server.start();
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}

		return new JettyWebServer(server);
	}
}
