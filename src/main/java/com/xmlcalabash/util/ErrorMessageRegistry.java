/*
 * ErrorMessageRegistry.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * Portions Copyright 2010 The MITRE Corporation.
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

package com.xmlcalabash.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.xml.sax.InputSource;

import com.xmlcalabash.core.XProcException;

/**
 * Registry that ties error codes to error messages.
 *
 * @author Jonathan Cranford
 */
public final class ErrorMessageRegistry {

    private static final String ERROR_LIST_FILE = "/etc/error-list.xml";
    private static final QName ERROR_QNAME = new QName("error");
    private static final QName CODE_QNAME = new QName("code");
    private static final Logger logger = Logger.getLogger(ErrorMessageRegistry.class.getName());
   
    private final Map<String, String> registry;
   
   
    /**
     * Initializes the registry using the given DocumentBuilder to read in the error list.
     * Note that if there is an error loading the error list, then calls to
     * {@link #lookup(QName)} will always return an empty string.
     * 
     * @param builder The Saxon DocumentBuilder used to parse the error list file.
     */
    public ErrorMessageRegistry(final DocumentBuilder builder) {
        registry = new HashMap<String,String>();
        final InputStream instream = getClass().getResourceAsStream(ERROR_LIST_FILE);
        if (instream != null) {
            try {
                final XdmNode doc = builder.build(new SAXSource(new InputSource(instream)));
                final XdmSequenceIterator iter = doc.axisIterator(Axis.DESCENDANT, ERROR_QNAME);
                while (iter.hasNext()) {
                    final XdmNode error = (XdmNode) iter.next();
                    registry.put(error.getAttributeValue(CODE_QNAME), error.getStringValue());
                }
            } catch (SaxonApiException sae) {
                logger.log(Level.CONFIG, "Error parsing error-list.xml", sae);
            }
        }
    }
   
   
    /**
     * Returns the error message from the XProc spec that matches the given code.
     *
     * @param code usually retrieved via {@link XProcException#getErrorCode()}
     * @return matching error message, or an empty string if there isn't a matching code. 
     * @see #ErrorMessageRegistry(DocumentBuilder)
     */
    public String lookup(final QName code) {
        if (code != null) {
            final String msg = registry.get(code.getLocalName());
            if (msg != null) {
                return msg;
            }
        }
        return "";
    }

}
