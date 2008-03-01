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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wymiwyg.wrhapi.Handler;
import org.wymiwyg.wrhapi.HandlerException;
import org.wymiwyg.wrhapi.Request;
import org.wymiwyg.wrhapi.Response;
import org.wymiwyg.wrhapi.ServerBinding;
import org.wymiwyg.wrhapi.WebServer;
import org.wymiwyg.wrhapi.util.MessageBody2Read;

/**
 * @author reto
 * 
 */
public class Activator implements BundleActivator {

	private WebServer webServer;

	public void start(BundleContext context) throws Exception {
		if (webServer == null) {
			webServer = new JettyWebServerFactory().startNewWebServer(
					new Handler() {

						public void handle(Request request, Response response)
								throws HandlerException {
							response.setBody(new MessageBody2Read() {

								public ReadableByteChannel read()
										throws IOException {
									return Channels
											.newChannel(new ByteArrayInputStream(
													"Hello".getBytes()));
								}

							});
						}

					}, new ServerBinding() {

						public InetAddress getInetAddress() {
							// TODO Auto-generated method stub
							return null;
						}

						public int getPort() {
							// TODO Auto-generated method stub
							return 8989;
						}

					});
		}

	}

	public void stop(BundleContext context) throws Exception {
		webServer.stop();

	}

}
