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

import java.util.Collections;

import javax.naming.NamingException;

import junit.framework.TestCase;

/**
 * JMSAppender only looks up JNDI names in the java: namespace (CVE-2021-4104).
 */
public class JMSAppenderJndiNameTest extends TestCase {

  private static final Object FOUND = new Object();

  public JMSAppenderJndiNameTest(final String name) {
    super(name);
  }

  public void testNamesOutsideJavaNamespaceAreRefusedBeforeLookup() {
    String[] names = {"jms/TopicConnectionFactory", "ldap://example.invalid/TopicConnectionFactory"};
    for (int i = 0; i < names.length; i++) {
      RecordingContext ctx = new RecordingContext(FOUND);
      try {
        new JMSAppender().lookup(ctx, names[i]);
        fail("lookup of " + names[i] + " was not refused");
      } catch (NamingException expected) {
      }
      assertEquals(names[i], Collections.EMPTY_LIST, ctx.getLookups());
    }
  }

  public void testJavaNameIsLookedUp() throws NamingException {
    RecordingContext ctx = new RecordingContext(FOUND);
    assertSame(FOUND, new JMSAppender().lookup(ctx, "java:comp/env/jms/TopicConnectionFactory"));
    assertEquals(Collections.singletonList("java:comp/env/jms/TopicConnectionFactory"), ctx.getLookups());
  }
}
