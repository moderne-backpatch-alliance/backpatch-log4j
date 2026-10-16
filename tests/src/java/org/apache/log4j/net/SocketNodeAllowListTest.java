/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.net;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.VectorAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;

/**
 * SocketNode only deserializes the classes a LoggingEvent is made of (CVE-2019-17571).
 */
public class SocketNodeAllowListTest extends TestCase {

  public SocketNodeAllowListTest(final String name) {
    super(name);
  }

  static final class NotAllowListed implements Serializable {
    private static final long serialVersionUID = 1L;
    final String value;

    NotAllowListed(final String value) {
      this.value = value;
    }
  }

  private Vector deliver(final Object propertyValue) throws Exception {
    Hashtable properties = new Hashtable();
    properties.put("key", propertyValue);
    LoggingEvent event = new LoggingEvent(Logger.class.getName(),
        Logger.getLogger("allowlist.source"), System.currentTimeMillis(),
        Level.INFO, "message", "main", null, null, null, properties);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bytes);
    oos.writeObject(event);
    oos.close();

    Hierarchy hierarchy = new Hierarchy(new RootLogger(Level.ALL));
    VectorAppender appender = new VectorAppender();
    hierarchy.getRootLogger().addAppender(appender);

    ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    try {
      Socket client = new Socket(server.getInetAddress(), server.getLocalPort());
      try {
        Socket accepted = server.accept();
        client.getOutputStream().write(bytes.toByteArray());
        client.getOutputStream().flush();
        client.shutdownOutput();
        new SocketNode(accepted, hierarchy).run();
      } finally {
        client.close();
      }
    } finally {
      server.close();
    }
    return appender.getVector();
  }

  public void testEventWithStringPropertyIsLogged() throws Exception {
    Vector received = deliver("text");
    assertEquals(1, received.size());
    assertEquals("text", ((LoggingEvent) received.elementAt(0)).getMDC("key"));
  }

  public void testEventWithNonAllowListedPropertyIsRejected() throws Exception {
    assertEquals(0, deliver(new NotAllowListed("x")).size());
  }
}
