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

package org.apache.log4j.chainsaw;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Chainsaw's LoggingReceiver only deserializes the classes a LoggingEvent is made of (CVE-2022-23307).
 */
public class LoggingReceiverAllowListTest extends TestCase {

  public LoggingReceiverAllowListTest(final String name) {
    super(name);
  }

  static final class NotAllowListed implements Serializable {
    private static final long serialVersionUID = 1L;
    final String value;

    NotAllowListed(final String value) {
      this.value = value;
    }
  }

  static final class CountingModel extends MyTableModel {
    final Vector events = new Vector();

    public void addEvent(final EventDetails event) {
      events.addElement(event);
    }
  }

  private static LoggingEvent event(final String message, final Object propertyValue) {
    Hashtable properties = new Hashtable();
    properties.put("key", propertyValue);
    return new LoggingEvent(Logger.class.getName(),
        Logger.getLogger("allowlist.source"), System.currentTimeMillis(),
        Level.INFO, message, "main", null, null, null, properties);
  }

  private Vector deliver(final Object[] events) throws Exception {
    ServerSocket probe = new ServerSocket(0);
    int port = probe.getLocalPort();
    probe.close();

    CountingModel model = new CountingModel();
    new LoggingReceiver(model, port).start();

    Socket client = new Socket("127.0.0.1", port);
    try {
      ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
      for (int i = 0; i < events.length; i++) {
        oos.writeObject(events[i]);
      }
      oos.flush();
      client.shutdownOutput();
      // the receiver closing with unread input resets the connection
      try {
        assertEquals(-1, client.getInputStream().read());
      } catch (SocketException e) {
      }
    } finally {
      client.close();
    }
    return model.events;
  }

  public void testEventsWithStringPropertiesAreReceived() throws Exception {
    Vector received = deliver(new Object[] {
        event("first", "text"), event("second", "text") });
    assertEquals(2, received.size());
  }

  public void testConnectionStopsAtNonAllowListedProperty() throws Exception {
    Vector received = deliver(new Object[] {
        event("first", "text"), event("second", new NotAllowListed("x")), event("third", "text") });
    assertEquals(1, received.size());
    assertEquals("first", ((EventDetails) received.elementAt(0)).getMessage());
  }
}
