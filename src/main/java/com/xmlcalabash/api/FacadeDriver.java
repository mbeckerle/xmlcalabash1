/*
 * FacadeDriver.java
 *
 * Copyright 2010 The MITRE Corporation.
 * Portions Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */
package com.xmlcalabash.api;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.LogOptions;

public final class FacadeDriver {

	public static void main(String[] args) {

		if (args == null || args.length < 1) {
			System.err.println("Usage: java " + FacadeDriver.class.getName()
					+ " [-d] [-o port=uri] ... pipeline.xpl [source.xml]");
			System.exit(1);
		}

		// process args
		final Map<String, String> outputPortMap = new HashMap<String, String>();
		boolean debug = false;
		int argi = 0;
		if ("-d".equals(args[argi])) {
			argi++;
			debug = true;
		}
		while ("-o".equals(args[argi]) && argi + 1 < args.length) {
			argi++;
			final String opt = args[argi++];
			final String[] parts = opt.split("=");
			outputPortMap.put(parts[0], parts[1]);
		}
		final String filename = args[argi++];
		String inputfile = null;
		if (argi < args.length) {
			inputfile = args[argi++];
		}

		// 1a. Create the configuration.
		boolean schemaAware = false;
		final XProcConfiguration config = new XProcConfiguration(schemaAware);

		// 1b. Set configuration options (optional)
		config.logOpt = LogOptions.OFF;
		config.debug = true;

		// 2. Create the facade.
		final Facade facade = new Facade(config);
		// facade.getRuntime().setPhoneHome(false);

		// 3. Create the pipeline to execute.
		try {
			XPipeline pipeline2 = facade.createPipeline(filename);

			// 4. Execute the pipeline with the given inputs, parameters, and
			// options.
			Map<String, List<XdmNode>> inputs = null;
			Map<QName, String> parameters = null;
			Map<QName, String> options = null;
			if (inputfile != null) {
				final XdmNode doc = facade.buildDocument(new BufferedInputStream(new FileInputStream(inputfile)));
				inputs = Collections.singletonMap("source", Collections.singletonList(doc));
			}
			Map<String, List<XdmNode>> outputs = Facade.runPipeline(pipeline2, inputs, parameters, options);

			if (outputPortMap.isEmpty()) {
				facade.printOutputs(pipeline2, outputs);
			} else {
				facade.writeOutputs(pipeline2, outputs, outputPortMap);
			}

		} catch (XProcException e) {
			System.err.println("Error executing the pipeline: " + facade.formattedErrorMessage(e));
			if (debug) {
				e.printStackTrace();
			}
		} catch (SaxonApiException e) {
			System.err.println("Parse error: " + e.toString());
			if (debug) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
