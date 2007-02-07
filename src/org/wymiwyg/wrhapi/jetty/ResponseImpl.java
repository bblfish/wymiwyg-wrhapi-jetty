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

import org.wymiwyg.wrhapi.HandlerException;
import org.wymiwyg.wrhapi.HeaderName;
import org.wymiwyg.wrhapi.MessageBody;
import org.wymiwyg.wrhapi.ResponseStatus;
import org.wymiwyg.wrhapi.util.ResponseBase;

import java.io.IOException;

import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;


/**
 * @author reto
 */
public class ResponseImpl extends ResponseBase {
    private static final Log log = LogFactory.getLog(ResponseImpl.class);
    private ResponseStatus status = null;
    private HttpServletResponse servletResponse;
    private boolean committed;

    // private boolean bodyWriting = false;
    private Thread bodyWriter = null;

    // private Exception bodyWriteException;

    /**
     * @param servletResponse
     */
    public ResponseImpl(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    /**
     * @see org.wymiwyg.rwcf.Response#setBody(java.lang.Object)
     */
    public void setBody(MessageBody body) throws HandlerException {
        commitHeader();

        try {
            //servletResponse.getOutputStream().write("hello\n".getBytes());
            //servletResponse.flushBuffer();
            WritableByteChannel out = Channels.newChannel(servletResponse.getOutputStream());
            body.writeTo(out);
            out.close();
        } catch (IOException e1) {
            throw new HandlerException(e1);
        }
    }

    /**
     *
     */
    private void commitHeader() throws HandlerException {
        synchronized (this) {
            if (committed == true) {
                throw new HandlerException("Response already committed");
            }

            committed = true;
        }

        if (status == null) {
            status = ResponseStatus.SUCCESS;
        }

        servletResponse.setStatus(status.getCode());
        writeHeaders();
    }

    void commitIfNeeded() throws HandlerException {
        if (!committed) {
            synchronized (this) {
                if (!committed) {
                    commitHeader();

                    try {
                        servletResponse.getOutputStream().close();
                    } catch (IOException e) {
                        throw new HandlerException(e);
                    }
                }
            }
        }
    }

    /**
     *
     */
    private void writeHeaders() {
        Iterator keyIter = headerMap.keySet().iterator();

        while (keyIter.hasNext()) {
            HeaderName current = (HeaderName) keyIter.next();
            String[] values = (String[]) headerMap.get(current);

            if (current.equals(HeaderName.SET_COOKIE)) {
                /*
                 * found on
                 * http://search.cpan.org/~gaas/libwww-perl-5.803/lib/HTTP/Headers.pm
                 * The HTTP spec (RFC 2616) promise that joining multiple values
                 * in this way will not change the semantic of a header field,
                 * but in practice there are cases like old-style Netscape
                 * cookies (see HTTP::Cookies) where "," is used as part of the
                 * syntax of a single field value.
                 *
                 * So the expririenced IE behavious seems to go back to netscape
                 */
                for (int i = 0; i < values.length; i++) {
                    String string = values[i];
                    servletResponse.addHeader(current.toString(), string);
                }
            } else {
                String currentStringValue = getHeaderStringValue(values);
                servletResponse.setHeader(current.toString(), currentStringValue);
            }
        }
    }

    /**
     * @param values
     * @return
     */
    private String getHeaderStringValue(String[] values) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;

        for (int i = 0; i < values.length; i++) {
            if (first) {
                first = false;
            } else {
                buffer.append(", ");
            }

            buffer.append(values[i]);
        }

        return buffer.toString();
    }

    /**
     * @see org.wymiwyg.rwcf.Response#setHeader(java.lang.String,
     *      java.lang.Object)
     */
    public void setHeader(HeaderName headerName, Object value) {
        if (value instanceof String[]) {
            headerMap.put(headerName, (String[]) value);
        } else {
            if (!(value instanceof Object[])) {
                String[] values = new String[1];
                values[0] = value.toString();
                headerMap.put(headerName, values);
            } else {
                Object[] array = (Object[]) value;
                String[] values = new String[array.length];

                for (int i = 0; i < array.length; i++) {
                    values[i] = array[i].toString();
                }
                headerMap.put(headerName, values);
            }
        }
    }

    /**
     * @see org.wymiwyg.rwcf.Response#setResponseStatus(org.wymiwyg.rwcf.modeler.ResponseStatus)
     */
    public void setResponseStatus(ResponseStatus status) {
        this.status = status;
    }

    synchronized void waitTillBodyWritten() throws HandlerException {
        if (!committed) {
            commitHeader();
        }

        if (log.isDebugEnabled()) {
            log.debug("waiting till body-written");
        }

        if (bodyWriter != null) {
            try {
                int i = 0;

                while (bodyWriter.isAlive()) {
                    bodyWriter.join(30000);

                    if (bodyWriter.isAlive()) {
                        log.info(
                            "body hasn't finished writing even after waiting " +
                            (i++ * 30) + "s");
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("joined body-writer");
                }

                bodyWriter = null;
            } catch (InterruptedException e) {
                log.warn(e.toString(), e);
            }
        }

        /*
         * if (bodyWriteException != null) { throw new
         * HandlerException("Exception writting body", bodyWriteException); }
         */
    }
}
