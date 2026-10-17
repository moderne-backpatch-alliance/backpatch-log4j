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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import junit.framework.TestCase;

/**
 * JMSSink only looks up JNDI names in the java: namespace (CVE-2022-23302).
 */
public class JMSSinkJndiNameTest extends TestCase {

  private static final String TCF = "java:comp/env/jms/TopicConnectionFactory";
  private static final String TOPIC = "java:comp/env/jms/Topic";

  static RecordingContext context;

  /** Hands JMSSink's InitialContext the test's RecordingContext. */
  public static final class RecordingContextFactory implements InitialContextFactory {
    public Context getInitialContext(final Hashtable environment) {
      return context;
    }
  }

  /** Every JMS call returns another stub, so the sink gets from one lookup to the next. */
  private static final class StubHandler implements InvocationHandler {
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
      if (method.getName().equals("equals")) {
        return Boolean.valueOf(proxy == args[0]);
      }
      if (method.getName().equals("hashCode")) {
        return new Integer(System.identityHashCode(proxy));
      }
      if (method.getName().equals("toString")) {
        return "stub " + method.getDeclaringClass().getName();
      }
      Class type = method.getReturnType();
      return type.isInterface() ? stub(new Class[] {type}) : null;
    }
  }

  private static Object stub(final Class[] interfaces) {
    return Proxy.newProxyInstance(JMSSinkJndiNameTest.class.getClassLoader(), interfaces, new StubHandler());
  }

  private String previousFactory;

  public JMSSinkJndiNameTest(final String name) {
    super(name);
  }

  protected void setUp() {
    context = new RecordingContext(stub(new Class[] {TopicConnectionFactory.class, Topic.class}));
    previousFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, RecordingContextFactory.class.getName());
  }

  protected void tearDown() {
    if (previousFactory == null) {
      System.getProperties().remove(Context.INITIAL_CONTEXT_FACTORY);
    } else {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, previousFactory);
    }
    context = null;
  }

  public void testJavaNamesAreLookedUp() {
    new JMSSink(TCF, TOPIC, "user", "password");
    assertEquals(Arrays.asList(new String[] {TCF, TOPIC}), context.getLookups());
  }

  public void testConnectionFactoryNameOutsideJavaNamespaceIsNotLookedUp() {
    new JMSSink("jms/TopicConnectionFactory", TOPIC, "user", "password");
    assertEquals(Collections.EMPTY_LIST, context.getLookups());
  }

  public void testTopicNameOutsideJavaNamespaceIsNotLookedUp() {
    new JMSSink(TCF, "jms/Topic", "user", "password");
    assertEquals(Collections.singletonList(TCF), context.getLookups());
  }

  public void testLookupFindsJavaName() throws NamingException {
    assertNotNull(JMSSink.lookup(context, TOPIC));
    assertEquals(Collections.singletonList(TOPIC), context.getLookups());
  }

  public void testLookupRefusesNameOutsideJavaNamespace() {
    try {
      JMSSink.lookup(context, "jms/Topic");
      fail("JMSSink.lookup looked up a name outside java:");
    } catch (NamingException expected) {
    }
    assertEquals(Collections.EMPTY_LIST, context.getLookups());
  }
}
