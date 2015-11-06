/*
 * Facade.java
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.InputSource;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.ErrorMessageRegistry;
import com.xmlcalabash.util.Input;
import com.xmlcalabash.util.URIUtils;

/**
 * <p>
 * Simple API to Calabash. Note that this class is not thread-safe, much like
 * many of the underlying Calabash classes. Thus, these classes should not be
 * shared between multiple threads in a multi-threaded environment.
 * </p>
 *
 * <p>
 * Steps to use Calabash:
 * </p>
 * <xmp> try { // 1a. Create the configuration. boolean schemaAware = false;
 * final XProcConfiguration config = new XProcConfiguration(schemaAware);
 * 
 * // 1b. Set configuration options (optional) config.logOpt = LogOptions.PLAIN;
 * config.safeMode = true; config.debug = true;
 * 
 * // 2. Create the facade. final Facade facade = new Facade(config); // OR
 * final Facade facade2 = Facade.configure(config, "config.xml"); // OR final
 * Facade facade3 = Facade.configure(config, new SAXSource(new
 * InputSource(getClass().getResourceAsStream("/config.xml"))));
 * 
 * // 3. Create the pipeline to execute. XPipeline pipeline =
 * facade.createPipeline(); // OR XPipeline pipeline2 =
 * facade.createPipeline("pipeline.xpl"); // OR XPipeline pipeline3 =
 * facade.createPipelineFromLibrary("http://somewhere.com/library.xpl"); // OR
 * XPipeline pipeline4 =
 * facade.createPipelineFromLibrary("http://somewhere.com/library.xpl", new
 * QName("http://somewhere.com/foo", "foobar"));
 * 
 * // 4. Execute the pipeline with the given inputs, parameters, and options.
 * Map<String,List<XdmNode>> inputs = null; Map<QName,String> parameters = null;
 * Map<QName,String> options = null; Map<String,List<XdmNode>> outputs =
 * Facade.runPipeline(pipeline2, inputs, parameters, options);
 * 
 * // 5. use the output // ...
 * 
 * } catch (XProcException e) { System.err.println(
 * "Error executing the pipeline: " + facade.errorCodeAndMessage(e));
 * e.printStackTrace(); } catch (SaxonApiException e) { System.err.println(
 * "Parse error"); e.printStackTrace(); } } </xmp>
 *
 * @author Jonathan Cranford
 */
public final class Facade {

	private final XProcConfiguration config;
	private XProcRuntime runtime;
	private final ErrorMessageRegistry errorRegistry;

	/**
	 * Creates a new facade with the given configuration.
	 *
	 * @param config
	 *            this XProcConfiguration will be used in subsequent facade
	 *            operations
	 */
	public Facade(final XProcConfiguration config) {
		this.config = config;
		errorRegistry = new ErrorMessageRegistry(config.getProcessor().newDocumentBuilder());
	}

	/**
	 * Creates a new facade configured with the given XProcConfiguration and
	 * config file. Parses and loads the configuration from the given filename.
	 *
	 * @param config
	 *            this XProcConfiguration will be used in subsequent facade
	 *            operations
	 * @param configFile
	 *            calabash configuration file
	 * @return new Facade object
	 * @throws SaxonApiException
	 *             if the given file can't be parsed
	 */
	public static Facade configure(final XProcConfiguration config, final String configFile) throws SaxonApiException {
		if (configFile == null) {
			return new Facade(config);
		} else {
			// Make this absolute because sometimes it fails from the command
			// line otherwise.
			String cfgURI = URIUtils.cwdAsURI().resolve(configFile).toASCIIString();
			return configure(config, new SAXSource(new InputSource(cfgURI)));
		}
	}

	/**
	 * Creates a new facade configured with the given XProcConfiguration and
	 * config file. Parses and loads the configuration file from the given SAX
	 * InputSource. This method can be used to configure the xproc engine from
	 * an arbitrary input stream.
	 *
	 * @param config
	 *            this XProcConfiguration will be used in subsequent facade
	 *            operations
	 * @param source
	 *            JAXP Source for the config file
	 * @return new Facade object
	 * @throws SaxonApiException
	 *             if the given inputSource can't be parsed
	 */
	public static Facade configure(final XProcConfiguration config, final Source source) throws SaxonApiException {
		final Facade facade = new Facade(config);
		if (source == null) {
			return facade;
		} else {
			final XdmNode doc = facade.buildDocument(source);
			config.parse(doc);
			return facade;
		}
	}

	/**
	 * <p>
	 * Utility method that parses and builds a document from the given
	 * InputStream.
	 * </p>
	 *
	 * <p>
	 * <em>Note</em>: For multiple calls,
	 * {@link #buildDocument(DocumentBuilder, InputStream)} should be used
	 * instead in order to reuse the same DocumentBuilder.
	 * </p>
	 *
	 * @param in
	 *            input stream containing the xml document
	 * @return document node
	 * @throws SaxonApiException
	 *             if the input steam can't be parsed
	 */
	public XdmNode buildDocument(final InputStream in) throws SaxonApiException {
		return buildDocument(new SAXSource(new InputSource(in)));
	}

	/**
	 * Utility method that parses and builds a document from the given
	 * InputStream using the given DocumentBuilder.
	 *
	 * @param builder
	 *            DocumentBuilder to use in parsing the document
	 * @param in
	 *            input stream containing the xml document
	 * @return document node
	 * @throws SaxonApiException
	 *             if the input steam can't be parsed
	 */
	public static XdmNode buildDocument(final DocumentBuilder builder, final InputStream in) throws SaxonApiException {
		return builder.build(new SAXSource(new InputSource(in)));
	}

	/**
	 * <p>
	 * Utility method that parses and builds a document from the given JAXP
	 * source.
	 * </p>
	 *
	 * <p>
	 * <em>Note</em>: For multiple calls, {@link DocumentBuilder#build(Source)}
	 * should be called directly in order to reuse the same DocumentBuilder.
	 * </p>
	 *
	 * @param source
	 *            source document
	 * @return document node
	 * @throws SaxonApiException
	 *             if the source can't be parsed
	 */
	public XdmNode buildDocument(final Source source) throws SaxonApiException {
		DocumentBuilder builder = config.getProcessor().newDocumentBuilder();
		return builder.build(source);
	}

	/**
	 * <p>
	 * Utility method that builds each of the given sources into a new document,
	 * returning a list of built documents.
	 * </p>
	 *
	 * <p>
	 * <em>Note</em>: For multiple calls,
	 * {@link #buildDocuments(DocumentBuilder, List)} should be used instead in
	 * order to reuse the same DocumentBuilder.
	 * </p>
	 *
	 * @param sources
	 *            list of source documents
	 * @return list of document nodes
	 * @throws SaxonApiException
	 *             if any of the sources can't be parsed
	 */
	public List<XdmNode> buildDocuments(final List<Source> sources) throws SaxonApiException {
		DocumentBuilder builder = config.getProcessor().newDocumentBuilder();
		return buildDocuments(builder, sources);
	}

	/**
	 * Utility method that builds each of the given sources into a new document,
	 * returning a list of built documents.
	 *
	 * @param builder
	 *            DocumentBuilder to use in parsing each document
	 * @param sources
	 *            list of source documents
	 * @return list of document nodes
	 * @throws SaxonApiException
	 *             if any of the sources can't be parsed
	 */
	public List<XdmNode> buildDocuments(final DocumentBuilder builder, final List<Source> sources)
			throws SaxonApiException {
		final List<XdmNode> docs = new LinkedList<XdmNode>();
		for (Source s : sources) {
			docs.add(builder.build(s));
		}
		return docs;
	}

	/**
	 * Creates an XPipeline from the internal configuration.
	 *
	 * @return the pipeline loaded from the configuration.
	 * @throws IllegalStateException
	 *             if the given configuration doesn't specify a pipeline.
	 * @throws SaxonApiException
	 *             if there's an error reading or using the pipeline from the
	 *             configuration.
	 */
	public XPipeline createPipeline() throws SaxonApiException {
		if (config.pipeline == null) {
			throw new IllegalStateException("No pipeline specified");
		} else {
			XdmNode doc = config.pipeline.read();
			return getRuntime().use(doc);
		}
	}

	/**
	 * Returns the current runtime object, lazily creating one if necessary.
	 *
	 * @return current runtime object, created from the internal configuration.
	 */
	public XProcRuntime getRuntime() {
		if (runtime == null) {
			runtime = new XProcRuntime(config);
		}
		return runtime;
	}

	/**
	 * Returns the current configuration.
	 *
	 * @return current internal configuration
	 */
	public XProcConfiguration getConfig() {
		return config;
	}

	/**
	 * Creates an XPipeline from the given JAXP Source.
	 *
	 * @param pipelineSource
	 *            the JAXP Source containing the pipeline
	 * @return XPipeline
	 * @throws SaxonApiException
	 *             if there's an error parsing or using the given source as a
	 *             pipeline
	 */
	public XPipeline createPipeline(final Source pipelineSource) throws SaxonApiException {
		return getRuntime().use(buildDocument(pipelineSource));
	}

	/**
	 * Creates an XPipeline from the given URI.
	 *
	 * @param pipelineURI
	 *            URI from which to load the pipeline.
	 * @return the pipeline loaded from the given URI.
	 * @throws SaxonApiException
	 *             if there's an error reading or using the pipeline from the
	 *             given URI.
	 */
	public XPipeline createPipeline(final String pipelineURI) throws SaxonApiException {
		return getRuntime().load(new Input(pipelineURI));
	}

	/**
	 * Loads the library at the specified URI and returns a pipeline containing
	 * the first step in the library.
	 *
	 * @param libraryURI
	 *            the URI of the library to load
	 * @return a pipeline containing the first step in the specified library
	 * @throws SaxonApiException
	 *             if there's an error loading the library.
	 */
	public XPipeline createPipelineFromLibrary(final String libraryURI) throws SaxonApiException {
		return getRuntime().loadLibrary(new Input(libraryURI)).getFirstPipeline();
	}

	/**
	 * Loads the library from the specified URI and returns a pipeline
	 * containing the named step.
	 *
	 * @param libraryURI
	 *            the URI of the library to load
	 * @param stepName
	 *            namespace-qualified name of the step to put into the pipeline
	 * @return a pipeline containing the named step
	 * @throws SaxonApiException
	 *             if there's an error loading the library.
	 */
	public XPipeline createPipelineFromLibrary(final String libraryURI, final QName stepName) throws SaxonApiException {
		return getRuntime().loadLibrary(new Input(libraryURI)).getPipeline(stepName);
	}

	/**
	 * Serializes the outputs to System.out.
	 *
	 * @param pipeline
	 *            used to serialize the output documents
	 * @param outputs
	 *            as returned from
	 *            {@link #runPipeline(XPipeline, Map, Map, Map)}
	 */
	public void printOutputs(final XPipeline pipeline, final Map<String, List<XdmNode>> outputs) {
		for (String port : outputs.keySet()) {
			System.out.println("Output Port '" + port + "'");
			System.out.println("================");
			final Serialization serial = pipeline.getSerialization(port);
			final WritableDocument wd = new WritableDocument(getRuntime(), null, // forces
																					// use
																					// of
																					// System.out
					serial);
			for (XdmNode doc : outputs.get(port)) {
				wd.write(doc);
				System.out.println();
			}
			wd.close();
			System.out.println();
		}
	}

	/**
	 * Writes the given sequence of documents to the specified URI, based on the
	 * serialization settings of the given port.
	 *
	 * @param pipeline
	 *            Serialization settings corresponding to the given port in this
	 *            pipeline will be used.
	 * @param port
	 *            Serialization settings for this port in the given pipeline
	 *            will be used.
	 * @param outputDocs
	 *            the documents to serialize
	 * @param uri
	 *            where to write the documents. If null, document will be
	 *            written to System.out.
	 */
	public void writeOutput(final XPipeline pipeline, final String port, final List<XdmNode> outputDocs,
			final String uri) {
		final Serialization serial = pipeline.getSerialization(port);
		final WritableDocument wd = new WritableDocument(getRuntime(), uri, serial);
		for (XdmNode doc : outputDocs) {
			wd.write(doc);
		}
		wd.close();
	}

	/**
	 * Writes the outputs from the given pipeline to the URIs specified by port
	 * in the given port map.
	 *
	 * @param pipeline
	 *            used to serialize the output documents
	 * @param outputs
	 *            as returned from
	 *            {@link #runPipeline(XPipeline, Map, Map, Map)}
	 * @param outputPortMap
	 *            map of port names to URIs. As a special case, if the URI for a
	 *            particular port is null, then the corresponding documents will
	 *            be output to System.out.
	 */
	public void writeOutputs(final XPipeline pipeline, final Map<String, List<XdmNode>> outputs,
			final Map<String, String> outputPortMap) {
		for (String port : pipeline.getOutputs()) {
			if (outputPortMap.containsKey(port)) {
				writeOutput(pipeline, port, outputs.get(port), outputPortMap.get(port));
			}
		}
	}

	/**
	 * Returns the error message that matches the given exception.
	 *
	 * @param e
	 *            XProcException to look up
	 * @return matching error message, or an empty string if nothing matches
	 */
	public String errorMessage(final XProcException e) {
		return errorRegistry.lookup(e.getErrorCode());
	}

	/**
	 * Returns the error code and corresponding error message.
	 *
	 * @param e
	 *            XProcException to look up
	 * @return formatted string containing the error code and message
	 */
	public String errorCodeAndMessage(final XProcException e) {
		final QName code = e.getErrorCode();
		final String msg = errorRegistry.lookup(code);
		if (code == null) {
			return "";
		}
		final String localName = code.getLocalName();
		if (empty(msg)) {
			return localName;
		}
		return localName + ": " + msg;
	}

	/**
	 * Returns a formatted exception message containing the exception message,
	 * the error code, and the error message corresponding to the error code.
	 *
	 * @param e
	 *            exception to lookup
	 * @return formatted error message
	 */
	public String formattedErrorMessage(final XProcException e) {
		final String msg = e.getMessage();
		final String errorCodeAndMessage = errorCodeAndMessage(e);
		if (empty(msg)) {
			return errorCodeAndMessage;
		}
		if (empty(errorCodeAndMessage)) {
			return msg;
		}
		return String.format("%s (%s)", msg, errorCodeAndMessage);
	}

	// utility method
	private boolean empty(final String s) {
		return s == null || s.length() == 0;
	}

	/**
	 * Executes the given pipeline with the given named inputs, parameters, and
	 * options.
	 *
	 * @param pipeline
	 *            pipeline to execute
	 * @param inputs
	 *            map of port name to input documents.
	 * @param parameters
	 *            map of namespace-qualified parameter names to values
	 * @param options
	 *            map of namespace-qualified option names to values
	 * @return map of output port names to output documents
	 * @throws XProcException
	 *             on an error executing the pipeline
	 * @throws SaxonApiException
	 *             on a parse error while executing the pipeline
	 */
	public static Map<String, List<XdmNode>> runPipeline(final XPipeline pipeline,
			final Map<String, List<XdmNode>> inputs, final Map<QName, String> parameters,
			final Map<QName, String> options) throws XProcException, SaxonApiException {
		writeToInputPorts(pipeline, inputs);
		setParameters(pipeline, parameters);
		passOptions(pipeline, options);
		pipeline.run();
		return getPipelineOutputs(pipeline);
	}

	private static Map<String, List<XdmNode>> getPipelineOutputs(final XPipeline pipeline) throws SaxonApiException {
		Map<String, List<XdmNode>> outputs = new HashMap<String, List<XdmNode>>();
		for (String port : pipeline.getOutputs()) {
			final ReadablePipe rpipe = pipeline.readFrom(port);
			final List<XdmNode> outputDocs = new LinkedList<XdmNode>();
			while (rpipe.moreDocuments()) {
				outputDocs.add(rpipe.read());
			}
			outputs.put(port, outputDocs);
		}
		return outputs;
	}

	private static void passOptions(final XPipeline pipeline, final Map<QName, String> options) {
		if (options != null) {
			for (QName optname : options.keySet()) {
				pipeline.passOption(optname, new RuntimeValue(options.get(optname)));
			}
		}
	}

	private static void setParameters(final XPipeline pipeline, final Map<QName, String> parameters) {
		if (parameters != null) {
			for (QName name : parameters.keySet()) {
				pipeline.setParameter(name, new RuntimeValue(parameters.get(name)));
			}
		}
	}

	private static void writeToInputPorts(final XPipeline pipeline, final Map<String, List<XdmNode>> inputs) {
		if (inputs != null) {
			Set<String> pipelineInputPorts = pipeline.getInputs();
			for (String port : inputs.keySet()) {
				if (!pipelineInputPorts.contains(port)) {
					throw new XProcException(
							"There is a binding for the port '" + port + "' but the pipeline declares no such port.");
				}
				bindInputPort(pipeline, port, inputs.get(port));
			}
		}
	}

	private static void bindInputPort(final XPipeline pipeline, String port, final List<XdmNode> docs) {
		pipeline.clearInputs(port);
		if (docs != null) {
			for (XdmNode doc : docs) {
				pipeline.writeTo(port, doc);
			}
		}
	}

}
