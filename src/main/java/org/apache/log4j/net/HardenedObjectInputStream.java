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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HardenedObjectInputStream restricts the set of classes that can be
 * deserialized to a set of explicitly whitelisted classes. This prevents
 * certain type of attacks from being successful.
 * 
 * <p>
 * It is assumed that classes in the "java.lang" and "java.util" packages are
 * always authorized.
 * </p>
 * 
 * <p>
 * Objects nested more than 16 levels deep are refused.
 * </p>
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.2.18
 * 
 *        === Copied from the logback project with permission ==
 */
public class HardenedObjectInputStream extends ObjectInputStream {

	static final String ARRAY_CLASS_PREFIX = "[L";
	final List whitelistedClassNames;
	final static String[] JAVA_PACKAGES = new String[] { "java.lang", "java.util", ARRAY_CLASS_PREFIX + "java.lang" };

	static final int DEPTH_LIMIT = 16;

	public HardenedObjectInputStream(InputStream in, String[] whilelist) throws IOException {
		super(new DepthLimitedInputStream(in));

		this.whitelistedClassNames = new ArrayList();
		if (whilelist != null) {
			for (int i = 0; i < whilelist.length; i++) {
				this.whitelistedClassNames.add(whilelist[i]);
			}
		}
	}

	public HardenedObjectInputStream(InputStream in, List whitelist) throws IOException {
		super(new DepthLimitedInputStream(in));

		this.whitelistedClassNames = new ArrayList();
		this.whitelistedClassNames.addAll(whitelist);
	}

	protected Class resolveClass(ObjectStreamClass anObjectStreamClass) throws IOException, ClassNotFoundException {

		String incomingClassName = anObjectStreamClass.getName();

		if (!isWhitelisted(incomingClassName)) {
			throw new InvalidClassException("Unauthorized deserialization attempt", incomingClassName);
		}

		return super.resolveClass(anObjectStreamClass);
	}

	private boolean isWhitelisted(String incomingClassName) {
		for (int i = 0; i < JAVA_PACKAGES.length; i++) {
			if (incomingClassName.startsWith(JAVA_PACKAGES[i]))
				return true;
		}
		for (Iterator it = whitelistedClassNames.iterator(); it.hasNext();) {
			String whiteListed = (String) it.next();
			if (incomingClassName.equals(whiteListed))
				return true;
		}
		return false;
	}

	protected void addToWhitelist(List additionalAuthorizedClasses) {
		whitelistedClassNames.addAll(additionalAuthorizedClasses);
	}

	// readObject() is final and ObjectInputFilter needs Java 8u121, so depth is counted as ObjectInputStream.readObject0
	// frames. Every type code arrives through read(), and each nesting level takes at least two of them (object and
	// class descriptor), so after measuring depth d the limit cannot be passed before 2 * (DEPTH_LIMIT - d) more reads.
	static final class DepthLimitedInputStream extends FilterInputStream {

		private int readsBeforeCheck;

		DepthLimitedInputStream(InputStream in) {
			super(in);
		}

		public int read() throws IOException {
			if (readsBeforeCheck-- <= 0) {
				int depth = currentDepth();
				if (depth > DEPTH_LIMIT) {
					throw new InvalidObjectException("Unauthorized deserialization attempt: objects nested deeper than "
							+ DEPTH_LIMIT);
				}
				readsBeforeCheck = 2 * (DEPTH_LIMIT - depth);
			}
			return in.read();
		}

		private static int currentDepth() {
			StackTraceElement[] frames = new Throwable().getStackTrace();
			int depth = 0;
			for (int i = 0; i < frames.length; i++) {
				if ("readObject0".equals(frames[i].getMethodName())
						&& "java.io.ObjectInputStream".equals(frames[i].getClassName())) {
					depth++;
				}
			}
			return depth;
		}
	}
}
