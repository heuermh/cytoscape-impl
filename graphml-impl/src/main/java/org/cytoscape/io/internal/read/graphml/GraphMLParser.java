package org.cytoscape.io.internal.read.graphml;

/*
 * #%L
 * Cytoscape GraphML Impl (graphml-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.cytoscape.io.internal.read.graphml.GraphMLToken.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Parser for GraphML data file.
 * @author Kozo.Nishida
 *
 */
public class GraphMLParser extends DefaultHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GraphMLParser.class);

	// Map of data type to nodes or edges
	private Map<String, String> datatypeMap;
	private Map<String, String> datanameMap;

	private CyIdentifiable currentObject = null;

	// Attribute values
	private String currentAttributeID = null;
	private String currentAttributeKey = null;
	private String currentAttributeData = null;
	private String currentAttributeType = null;
	private String currentEdgeSourceName = null;
	private String currentEdgeTargetName = null;
	private String currentObjectTarget = null;

	private final CyNetworkFactory networkFactory;
	private final CyRootNetworkManager rootNetworkManager;

	private boolean directed = true;

	private final Stack<CyNetwork> networkStack;
	private final List<CyNetwork> cyNetworks;

	// Current CyNetwork. GraphML can have multiple networks in a file.
	private CyNetwork currentNetwork;

	private final Map<String, CyIdentifiable> nodeid2CyNodeMap;
	
	private String lastTag;
	private CyNode lastNode;

	/**
	 * Main constructor for our parser. Initialize any local arrays. Note that
	 * this parser is designed to be as memory efficient as possible. As a
	 * result, a minimum number of local data structures
	 */
	GraphMLParser(final TaskMonitor tm, final CyNetworkFactory networkFactory,
			final CyRootNetworkManager rootNetworkManager) {
		this.networkFactory = networkFactory;
		this.rootNetworkManager = rootNetworkManager;

		networkStack = new Stack<CyNetwork>();
		cyNetworks = new ArrayList<CyNetwork>();

		datatypeMap = new HashMap<String, String>();
		datanameMap = new HashMap<String, String>();

		this.nodeid2CyNodeMap = new HashMap<String, CyIdentifiable>();
	}

	CyNetwork[] getCyNetworks() {
		return cyNetworks.toArray(new CyNetwork[0]);
	}

	/********************************************************************
	 * Handler routines. The following routines are called directly from the SAX
	 * parser.
	 *******************************************************************/

	@Override
	public void startDocument() {
	}

	@Override
	public void endDocument() throws SAXException {
		// Clear
		datatypeMap.clear();
		datatypeMap = null;
	}

	@Override
	public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
		if (qName.equals(GRAPH.getTag()))
			createGraph(atts);
		else if (qName.equals(KEY.getTag())) {
			// Attribute definition found:
			datatypeMap.put(atts.getValue(ID.getTag()), atts.getValue(ATTRTYPE.getTag()));
			datanameMap.put(atts.getValue(ID.getTag()), atts.getValue(ATTRNAME.getTag()));
		} else if (qName.equals(NODE.getTag()))
			createNode(atts);
		else if (qName.equals(EDGE.getTag()))
			createEdge(atts);
		else if (qName.equals(DATA.getTag())) {
			currentAttributeKey = atts.getValue(KEY.getTag());
			currentAttributeType = datatypeMap.get(currentAttributeKey);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		lastTag = qName;

		if (qName != DATA.getTag())
			currentObjectTarget = null;

		// Check nest
		if (networkStack.size() > 1 && qName == GRAPH.getTag()) {
			networkStack.pop();
			currentNetwork = networkStack.peek();
		}
	}

	private void createGraph(final Attributes atts) {
		final String networkID = atts.getValue(ID.getTag());
		
		if (networkStack.size() == 0) {
			// Root network.
			currentNetwork = networkFactory.createNetwork();
		} else {
			final CyNetwork parentNetwork = networkStack.peek();
			currentNetwork = rootNetworkManager.getRootNetwork(parentNetwork).addSubNetwork();
			if(lastTag != null && lastTag.equals(NODE.getTag())) {
				Collection<CyNode> toBeRemoved = new ArrayList<CyNode>();
				toBeRemoved.add(lastNode);
				
				for(CyNetwork network: networkStack)
					network.removeNodes(toBeRemoved);
			}
		}
		currentNetwork.getRow(currentNetwork).set(CyNetwork.NAME, networkID);
		
		networkStack.push(currentNetwork);
		cyNetworks.add(currentNetwork);
		// parse directed or undirected
		String edef = atts.getValue(EDGEDEFAULT.getTag());
		directed = DIRECTED.getTag().equalsIgnoreCase(edef);
	}

	private void createNode(final Attributes atts) {
		// Parse node entry.
		currentObjectTarget = NODE.getTag();
		currentAttributeID = atts.getValue(ID.getTag());

		// If nested, add them to all parent networks.
		if (networkStack.size() > 1) {
			final CyNetwork rootNetwork = networkStack.get(0);
			currentObject = nodeid2CyNodeMap.get(currentAttributeID);
			if (currentObject == null) {
				currentObject = rootNetwork.addNode();
				rootNetwork.getRow(currentObject).set(CyNetwork.NAME, currentAttributeID);
				nodeid2CyNodeMap.put(currentAttributeID, currentObject);
			}

			for (CyNetwork network : networkStack) {
				if (network != rootNetwork) {
					((CySubNetwork) network).addNode((CyNode) currentObject);
				}
			}
		} else {
			// Root
			currentObject = nodeid2CyNodeMap.get(currentAttributeID);
			if (currentObject == null) {
				currentObject = currentNetwork.addNode();
				currentNetwork.getRow(currentObject).set(CyNetwork.NAME, currentAttributeID);
				nodeid2CyNodeMap.put(currentAttributeID, currentObject);
			}
		}

		lastNode = (CyNode) currentObject;
	}

	private void createEdge(final Attributes atts) {
		// Parse edge entry
		currentObjectTarget = EDGE.getTag();
		currentEdgeSourceName = atts.getValue(SOURCE.getTag());
		currentEdgeTargetName = atts.getValue(TARGET.getTag());
		final CyNode sourceNode = (CyNode) nodeid2CyNodeMap.get(currentEdgeSourceName);
		final CyNode targetNode = (CyNode) nodeid2CyNodeMap.get(currentEdgeTargetName);

		if (networkStack.size() > 1) {
			final CyNetwork rootNetwork = networkStack.get(0);
			currentObject = rootNetwork.addEdge(sourceNode, targetNode, directed);
			rootNetwork.getRow(currentObject).set(CyNetwork.NAME, currentEdgeSourceName + " (-) " + currentEdgeTargetName);
			rootNetwork.getRow(currentObject).set(CyEdge.INTERACTION, "-");

			for (CyNetwork network : networkStack) {
				if (network != rootNetwork) {
					((CySubNetwork) network).addEdge((CyEdge) currentObject);
				}
			}
		} else {
			try {
				currentObject = currentNetwork.addEdge(sourceNode, targetNode, directed);
				currentNetwork.getRow(currentObject).set(CyNetwork.NAME, currentEdgeSourceName + " (-) " + currentEdgeTargetName);
				currentNetwork.getRow(currentObject).set(CyEdge.INTERACTION, "-");
			} catch (Exception e) {
				logger.warn("Edge entry ignored: " + currentEdgeSourceName + " (-) " + currentEdgeTargetName, e);
			}
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) {
		currentAttributeData = new String(ch, start, length);

		if (currentObjectTarget != null) {
			if (currentObjectTarget.equals(NODE.getTag()) || currentObjectTarget.equals(EDGE.getTag()) || currentObjectTarget.equals(GRAPH.getTag())) {
				if (currentAttributeType != null && currentAttributeData.trim().length() != 0) {
					final String columnName = datanameMap.get(currentAttributeKey);
					final CyColumn column = currentNetwork.getRow(currentObject).getTable().getColumn(columnName);
					final GraphMLToken attrTag = GraphMLToken.getType(currentAttributeType);
					if (attrTag != null && attrTag.getDataType() != null) {
						if (column == null)
							currentNetwork.getRow(currentObject).getTable().createColumn(columnName, attrTag.getDataType(), false);
						
						currentNetwork.getRow(currentObject).set(columnName, attrTag.getObjectValue(currentAttributeData));
					}
				}
			}
		}
	}
}
