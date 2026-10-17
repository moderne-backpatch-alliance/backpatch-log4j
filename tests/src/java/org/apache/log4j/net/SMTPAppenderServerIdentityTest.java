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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import javax.mail.Session;

import junit.framework.TestCase;

/**
 * Tests that the mail session SMTPAppender creates asks JavaMail to check
 * the server's host name against its certificate. Only the session's
 * properties are read; nothing connects.
 */
public class SMTPAppenderServerIdentityTest extends TestCase {
    public SMTPAppenderServerIdentityTest(final String testName) {
        super(testName);
    }

    private static Session session(final String protocol) {
        SMTPAppender appender = new SMTPAppender();
        appender.setSMTPHost("mail.example.com");
        if (protocol != null) {
            appender.setSMTPProtocol(protocol);
        }
        return appender.createSession();
    }

    public void testSmtpsSessionChecksServerIdentityByDefault() {
        assertEquals("true", session("smtps").getProperty("mail.smtps.ssl.checkserveridentity"));
    }

    public void testSmtpSessionChecksServerIdentityByDefault() {
        assertEquals("true", session(null).getProperty("mail.smtp.ssl.checkserveridentity"));
    }

    /**
     * The configurators set appender options through the bean property of
     * that name, as here.
     */
    public void testCheckServerIdentityPropertyTurnsTheCheckOff() throws Exception {
        SMTPAppender appender = new SMTPAppender();
        appender.setSMTPProtocol("smtps");
        PropertyDescriptor[] props = Introspector.getBeanInfo(SMTPAppender.class).getPropertyDescriptors();
        PropertyDescriptor prop = null;
        for (int i = 0; i < props.length; i++) {
            if ("checkServerIdentity".equals(props[i].getName())) {
                prop = props[i];
            }
        }
        assertNotNull("no checkServerIdentity property", prop);
        assertNotNull("checkServerIdentity cannot be set", prop.getWriteMethod());
        assertNotNull("checkServerIdentity cannot be read", prop.getReadMethod());
        assertEquals(Boolean.TRUE, prop.getReadMethod().invoke(appender, new Object[0]));
        prop.getWriteMethod().invoke(appender, new Object[] { Boolean.FALSE });
        assertEquals(Boolean.FALSE, prop.getReadMethod().invoke(appender, new Object[0]));
        assertNull(appender.createSession().getProperty("mail.smtps.ssl.checkserveridentity"));
    }
}
