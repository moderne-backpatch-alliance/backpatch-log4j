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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * In-memory Context that records every name it is asked to look up and answers each with one fixed object.
 */
public class RecordingContext implements Context {

  private final Object found;
  private final List lookups = new ArrayList();

  public RecordingContext(final Object found) {
    this.found = found;
  }

  public List getLookups() {
    return lookups;
  }

  public Object lookup(final String name) {
    lookups.add(name);
    return found;
  }

  public Object lookup(final Name name) {
    return lookup(name.toString());
  }

  public void bind(final Name name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void bind(final String name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void rebind(final Name name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void rebind(final String name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void unbind(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void unbind(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void rename(final Name oldName, final Name newName) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void rename(final String oldName, final String newName) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NamingEnumeration list(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NamingEnumeration list(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NamingEnumeration listBindings(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NamingEnumeration listBindings(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Context createSubcontext(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Context createSubcontext(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Object lookupLink(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Object lookupLink(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NameParser getNameParser(final Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public NameParser getNameParser(final String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Name composeName(final Name name, final Name prefix) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public String composeName(final String name, final String prefix) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Object addToEnvironment(final String propName, final Object propVal) {
    return null;
  }

  public Object removeFromEnvironment(final String propName) {
    return null;
  }

  public Hashtable getEnvironment() {
    return new Hashtable();
  }

  public void close() {
  }

  public String getNameInNamespace() throws NamingException {
    throw new OperationNotSupportedException();
  }
}
