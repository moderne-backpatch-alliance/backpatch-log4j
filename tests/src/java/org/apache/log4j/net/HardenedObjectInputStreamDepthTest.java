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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * HardenedObjectInputStream refuses objects nested more than 16 levels deep (CVE-2023-26464).
 */
public class HardenedObjectInputStreamDepthTest extends TestCase {

  private static final int LIMIT = 16;

  public HardenedObjectInputStreamDepthTest(final String name) {
    super(name);
  }

  /** A chain of Hashtables, depth levels deep counting the outermost as 1. */
  private static Hashtable nestedHashtables(final int depth) {
    Hashtable table = new Hashtable();
    for (int i = 1; i < depth; i++) {
      Hashtable outer = new Hashtable();
      outer.put("k", table);
      table = outer;
    }
    return table;
  }

  private static Object[] nestedArrays(final int depth) {
    Object[] array = new Object[0];
    for (int i = 1; i < depth; i++) {
      array = new Object[] {array};
    }
    return array;
  }

  private static int depthOf(Hashtable table) {
    int depth = 1;
    while (!table.isEmpty()) {
      table = (Hashtable) table.get("k");
      depth++;
    }
    return depth;
  }

  private static int depthOf(Object[] array) {
    int depth = 1;
    while (array.length > 0) {
      array = (Object[]) array[0];
      depth++;
    }
    return depth;
  }

  private static byte[] serialize(final Object object) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bytes);
    oos.writeObject(object);
    oos.close();
    return bytes.toByteArray();
  }

  private static Object readHardened(final Object object) throws Exception {
    ObjectInputStream ois = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(object)), new String[0]);
    try {
      return ois.readObject();
    } finally {
      ois.close();
    }
  }

  public void testHashtablesNestedToTheLimitAreRead() throws Exception {
    assertEquals(LIMIT, depthOf((Hashtable) readHardened(nestedHashtables(LIMIT))));
  }

  public void testArraysNestedToTheLimitAreRead() throws Exception {
    assertEquals(LIMIT, depthOf((Object[]) readHardened(nestedArrays(LIMIT))));
  }

  private static final int[] BEYOND_LIMIT = {LIMIT + 2, LIMIT + 10};

  public void testHashtablesNestedBeyondTheLimitAreRefused() throws Exception {
    for (int i = 0; i < BEYOND_LIMIT.length; i++) {
      try {
        readHardened(nestedHashtables(BEYOND_LIMIT[i]));
        fail("Hashtables nested " + BEYOND_LIMIT[i] + " deep were read");
      } catch (InvalidObjectException expected) {
      }
    }
  }

  public void testArraysNestedBeyondTheLimitAreRefused() throws Exception {
    for (int i = 0; i < BEYOND_LIMIT.length; i++) {
      try {
        readHardened(nestedArrays(BEYOND_LIMIT[i]));
        fail("arrays nested " + BEYOND_LIMIT[i] + " deep were read");
      } catch (InvalidObjectException expected) {
      }
    }
  }

  public void testLoggingEventWithNestedPropertyIsRead() throws Exception {
    Hashtable properties = new Hashtable();
    properties.put("key", nestedHashtables(4));
    LoggingEvent event = new LoggingEvent(Logger.class.getName(), Logger.getLogger("depth.source"),
        System.currentTimeMillis(), Level.INFO, "message", "main",
        new ThrowableInformation(new Exception("failure")), "ndc", null, properties);
    event.getLocationInformation();
    ObjectInputStream ois = new HardenedLoggingEventInputStream(new ByteArrayInputStream(serialize(event)));
    try {
      LoggingEvent read = (LoggingEvent) ois.readObject();
      assertEquals("message", read.getMessage());
      assertEquals("java.lang.Exception: failure", read.getThrowableStrRep()[0]);
      assertEquals(4, depthOf((Hashtable) read.getMDC("key")));
    } finally {
      ois.close();
    }
  }
}
