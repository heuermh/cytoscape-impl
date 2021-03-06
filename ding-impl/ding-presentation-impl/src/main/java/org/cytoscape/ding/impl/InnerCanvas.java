package org.cytoscape.ding.impl;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
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


import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import org.cytoscape.ding.EdgeView;
import org.cytoscape.ding.GraphViewChangeListener;
import org.cytoscape.ding.NodeView;
import org.cytoscape.ding.ViewChangeEdit;
import org.cytoscape.ding.impl.events.GraphViewEdgesSelectedEvent;
import org.cytoscape.ding.impl.events.GraphViewEdgesUnselectedEvent;
import org.cytoscape.ding.impl.events.GraphViewNodesSelectedEvent;
import org.cytoscape.ding.impl.events.GraphViewNodesUnselectedEvent;
import org.cytoscape.ding.impl.events.ViewportChangeListener;
import org.cytoscape.graph.render.export.ImageImposter;
import org.cytoscape.graph.render.immed.EdgeAnchors;
import org.cytoscape.graph.render.immed.GraphGraphics;
import org.cytoscape.graph.render.stateful.GraphLOD;
import org.cytoscape.graph.render.stateful.GraphRenderer;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.intr.LongEnumerator;
import org.cytoscape.util.intr.LongHash;
import org.cytoscape.util.intr.LongStack;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.work.undo.UndoSupport;

/**
 * Canvas to be used for drawing actual network visualization
 */
public class InnerCanvas extends DingCanvas implements MouseListener, MouseMotionListener,
		KeyListener, MouseWheelListener {

	private final static long serialVersionUID = 1202416511420671L;

	// TODO This is public because BirdsEyeView needs to ensure that it isn't null
	// and that is ridiculous. 
	public GraphGraphics m_grafx;

	final double[] m_ptBuff = new double[2];
	final float[] m_extentsBuff2 = new float[4];
	final float[] m_floatBuff1 = new float[2];
	final float[] m_floatBuff2 = new float[2];
	final Line2D.Float m_line = new Line2D.Float();
	final GeneralPath m_path = new GeneralPath();
	final GeneralPath m_path2 = new GeneralPath();
	final LongStack m_stack = new LongStack();
	final LongStack m_stack2 = new LongStack();
	final Object m_lock;
	DGraphView m_view;
	final GraphLOD[] m_lod = new GraphLOD[1];
	final LongHash m_hash;
	double m_xCenter;
	double m_yCenter;
	double m_scaleFactor;
	private int m_lastRenderDetail = 0;
	private Rectangle m_selectionRect = null;
	private ViewChangeEdit m_undoable_edit;
	private boolean isPrinting = false;
	private PopupMenuHelper popup;

	FontMetrics m_fontMetrics = null;
	
	private boolean NodeMovement = true;

	//  for turning selection rectangle on and off
	private boolean selecting = true;

	private boolean enablePopupMenu = true;

	private UndoSupport m_undo;

	private int m_currMouseButton = 0;
	private int m_lastXMousePos = 0;
	private int m_lastYMousePos = 0;
	private boolean m_button1NodeDrag = false;
	
	private final MousePressedDelegator mousePressedDelegator;
	private final MouseReleasedDelegator mouseReleasedDelegator;
	private final MouseDraggedDelegator mouseDraggedDelegator;
	private final AddEdgeMousePressedDelegator addEdgeMousePressedDelegator;

	private AddEdgeStateMonitor addEdgeMode;

	InnerCanvas(Object lock, DGraphView view, UndoSupport undo) {
		super();
		m_lock = lock;
		m_view = view;
		m_undo = undo;
		m_lod[0] = new GraphLOD(); // Default LOD.
		m_hash = new LongHash();
		m_backgroundColor = Color.white;
		m_isVisible = true;
		m_isOpaque = false;
		m_xCenter = 0.0d;
		m_yCenter = 0.0d;
		m_scaleFactor = 1.0d;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
		popup = new PopupMenuHelper(m_view, this);

		mousePressedDelegator = new MousePressedDelegator();
		mouseReleasedDelegator = new MouseReleasedDelegator();
		mouseDraggedDelegator = new MouseDraggedDelegator();
		addEdgeMousePressedDelegator = new AddEdgeMousePressedDelegator();

		addEdgeMode = new AddEdgeStateMonitor(this,m_view);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);

		if ((width > 0) && (height > 0)) {
			final Image img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			GraphGraphics grafx = new GraphGraphics(img, false);

			synchronized (m_lock) {
				m_img = img;
				m_grafx = grafx;
				m_view.m_viewportChanged = true;
			}
		}
	}


	@Override
	public void update(Graphics g) {
		if (m_grafx == null || m_view == null)
			return;

		// This is the magical portion of code that transfers what is in the
		// visual data structures into what's on the image.
		boolean contentChanged = false;
		boolean viewportChanged = false;
		double xCenter = 0.0d;
		double yCenter = 0.0d;
		double scaleFactor = 1.0d;

		m_fontMetrics = g.getFontMetrics();

		synchronized (m_lock) {
			if (m_view.m_contentChanged || m_view.m_viewportChanged) {
				renderGraph(m_grafx,/* setLastRenderDetail = */ true, m_lod[0]);
				contentChanged = m_view.m_contentChanged;
				m_view.m_contentChanged = false;
				viewportChanged = m_view.m_viewportChanged;
				xCenter = m_xCenter;
				yCenter = m_yCenter;
				scaleFactor = m_scaleFactor;
				m_view.m_viewportChanged = false;
			}
		}

		// if canvas is visible, draw it (could be made invisible via DingCanvas api)
		if (m_isVisible) {
			g.drawImage(m_img, 0, 0, null);
		}

		if ((m_selectionRect != null) && (this.isSelecting())) {
			final Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.red);
			g2.draw(m_selectionRect);
		}

		if (contentChanged) {
			final ContentChangeListener lis = m_view.m_cLis[0];

			if (lis != null)
				lis.contentChanged();
		}

		if (viewportChanged) {
			final ViewportChangeListener lis = m_view.m_vLis[0];

			if (lis != null)
				lis.viewportChanged(getWidth(), getHeight(), xCenter, yCenter, scaleFactor);
		}
	}


	@Override
	public void paint(Graphics g) {
		update(g);
	}


	@Override
	public void print(Graphics g) {
		isPrinting = true;
		renderGraph(new GraphGraphics(
				new ImageImposter(g, getWidth(), getHeight()), false), 
				/* setLastRenderDetail = */ false, m_view.m_printLOD);
		isPrinting = false;
	}


	@Override
	public void printNoImposter(Graphics g) {
		isPrinting = true;
		final Image img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		renderGraph(new GraphGraphics(img, false), /* setLastRenderDetail = */ false, m_view.m_printLOD);
		isPrinting = false;
	}

	/**
 	 * Return true if this view is curerntly being printed (as opposed to painted on the screen)
 	 * @return true if we're currently being printed, false otherwise
 	 */
	public boolean isPrinting() { 
		return isPrinting; 
	}

	/**
	 * This method exposes the JComponent processMouseEvent so that canvases on
	 * top of us can pass events they don't want down.
	 * 
	 * @param e the MouseEvent to process
	 */
	@Override
	public void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (!m_view.isValueLocked(BasicVisualLexicon.NETWORK_SCALE_FACTOR))
			adjustZoom(e.getWheelRotation());
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		mouseDraggedDelegator.delegateMouseDragEvent(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (addEdgeMode.addingEdge())
			addEdgeMode.drawRubberBand(e);
		else {
			final String tooltipText = getToolTipText(e.getPoint());
			final Component[] components = this.getParent().getComponents();
			for (Component comp : components) {
				if (comp instanceof JComponent)
					((JComponent) comp).setToolTipText(tooltipText);
			}
		}
	}


	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) {
		mouseReleasedDelegator.delegateMouseEvent(e);
	}

	public void mousePressed(MouseEvent e) {
		if ( addEdgeMode.addingEdge() )
			addEdgeMousePressedDelegator.delegateMouseEvent(e);
		else
			mousePressedDelegator.delegateMouseEvent(e);
		requestFocusInWindow();
	}


	/**
	 * Handles key press events. Currently used with the up/down, left/right arrow
	 * keys. Pressing any of the listed keys will move the selected nodes one pixel
	 * in that direction.
	 * @param k The key event that we're listening for.
	 */
	public void keyPressed(KeyEvent k) {
		final int code = k.getKeyCode();
		if ( (code == KeyEvent.VK_UP) || (code == KeyEvent.VK_DOWN) || 
		     (code == KeyEvent.VK_LEFT) || (code == KeyEvent.VK_RIGHT)) {
			handleArrowKeys(k);
		} else if ( code == KeyEvent.VK_ESCAPE ) {
			handleEscapeKey();
		}
	}

	/**
	 * Currently not used.
	 * @param k The key event that we're listening for.
	 */
	public void keyReleased(KeyEvent k) { }

	/**
	 * Currently not used.
	 * @param k The key event that we're listening for.
	 */
	public void keyTyped(KeyEvent k) { }


	private long getChosenNode() {
		m_ptBuff[0] = m_lastXMousePos;
		m_ptBuff[1] = m_lastYMousePos;
		m_view.xformComponentToNodeCoords(m_ptBuff);
		m_stack.empty();
		m_view.getNodesIntersectingRectangle((float) m_ptBuff[0], (float) m_ptBuff[1],
		                                     (float) m_ptBuff[0], (float) m_ptBuff[1],
		                                     (m_lastRenderDetail
		                                     & GraphRenderer.LOD_HIGH_DETAIL) == 0,
		                                     m_stack);
		long chosenNode = (m_stack.size() > 0) ? m_stack.peek() : -1;
		return chosenNode;
	}

	private long getChosenAnchor() {
		m_ptBuff[0] = m_lastXMousePos;
		m_ptBuff[1] = m_lastYMousePos;
		m_view.xformComponentToNodeCoords(m_ptBuff);

		final LongEnumerator hits = m_view.m_spacialA.queryOverlap((float) m_ptBuff[0],
		                                                          (float) m_ptBuff[1],
		                                                          (float) m_ptBuff[0],
		                                                          (float) m_ptBuff[1],
		                                                          null, 0, false);
		long chosenAnchor = (hits.numRemaining() > 0) ? hits.nextLong() : (-1);
		return chosenAnchor;
	}
	
	private long getChosenEdge() {
		computeEdgesIntersecting(m_lastXMousePos - 1, m_lastYMousePos - 1,
                m_lastXMousePos + 1, m_lastYMousePos + 1, m_stack2);
        long chosenEdge = (m_stack2.size() > 0) ? m_stack2.peek() : -1;
        return chosenEdge;
	}
	
	/**
	 * @return an array of indices of unselected nodes
	 */
	private long[] getUnselectedNodes() {
		long [] unselectedNodes;
		if (m_view.m_nodeSelection) { // Unselect all selected nodes.
			unselectedNodes = m_view.getSelectedNodeIndices();

			// Adding this line to speed things up from O(n*log(n)) to O(n).
			m_view.m_selectedNodes.empty();

			for (int i = 0; i < unselectedNodes.length; i++)
				((DNodeView) m_view.getDNodeView(unselectedNodes[i])).unselectInternal();
		} else
			unselectedNodes = new long[0];
		return unselectedNodes;

	}
	
	private long[] getUnselectedEdges() {
		long[] unselectedEdges;
		if (m_view.m_edgeSelection) { // Unselect all selected edges.
			unselectedEdges = m_view.getSelectedEdgeIndices();

			// Adding this line to speed things up from O(n*log(n)) to O(n).
			m_view.m_selectedEdges.empty();

			for (int i = 0; i < unselectedEdges.length; i++)
				((DEdgeView) m_view.getDEdgeView(unselectedEdges[i])).unselectInternal();
		} else
			unselectedEdges = new long[0];
		return unselectedEdges;
	}
	
	private int toggleSelectedNode(long chosenNode, MouseEvent e) {
		int chosenNodeSelected = 0;
		final boolean wasSelected = m_view.getDNodeView(chosenNode).isSelected();

		if (wasSelected && e.isShiftDown()) {
			((DNodeView) m_view.getDNodeView(chosenNode)).unselectInternal();
			chosenNodeSelected = -1;
		} else if (!wasSelected) {
			((DNodeView) m_view.getDNodeView(chosenNode)).selectInternal();
			chosenNodeSelected = 1;
		}

		m_button1NodeDrag = true;
		m_view.m_contentChanged = true;	
		return chosenNodeSelected;
	}
	
	
	private void toggleChosenAnchor (long chosenAnchor, MouseEvent e) {		
		if (e.isAltDown() || e.isMetaDown()) {
			// Remove handle
			final long edge = chosenAnchor >>> 6;
			final int anchorInx = (int)(chosenAnchor & 0x000000000000003f);
			// Save remove handle
			m_undoable_edit = new ViewChangeEdit(m_view,ViewChangeEdit.SavedObjs.SELECTED_EDGES,"Remove Edge Handle",m_undo);
			m_view.getDEdgeView(edge).removeHandle(anchorInx);
			m_button1NodeDrag = false;
		} else {
			final boolean wasSelected = m_view.m_selectedAnchors.count(chosenAnchor) > 0;

			if (wasSelected && e.isShiftDown())
				m_view.m_selectedAnchors.delete(chosenAnchor);
			else if (!wasSelected) {
				if (!e.isShiftDown())
					m_view.m_selectedAnchors.empty();

				m_view.m_selectedAnchors.insert(chosenAnchor);
			}

			m_button1NodeDrag = true;
		}

		m_view.m_contentChanged = true;	
	}
	
	private int toggleSelectedEdge(long chosenEdge, MouseEvent e) {
		int chosenEdgeSelected = 0;
		
		// Add new Handle for Edge Bend.
		if ((e.isAltDown() || e.isMetaDown()) && ((m_lastRenderDetail & GraphRenderer.LOD_EDGE_ANCHORS) != 0)) {
			
			m_view.m_selectedAnchors.empty();
			m_ptBuff[0] = m_lastXMousePos;
			m_ptBuff[1] = m_lastYMousePos;
			m_view.xformComponentToNodeCoords(m_ptBuff);
			// Store current handle list
			m_undoable_edit = new ViewChangeEdit(m_view, ViewChangeEdit.SavedObjs.SELECTED_EDGES, "Add Edge Handle", m_undo);
			final Point2D newHandlePoint = new Point2D.Float((float) m_ptBuff[0], (float) m_ptBuff[1]);
			final int chosenInx = m_view.getDEdgeView(chosenEdge).addHandlePoint(newHandlePoint);
			
			m_view.m_selectedAnchors.insert(((chosenEdge) << 6) | chosenInx);
		}

		final boolean wasSelected = m_view.getDEdgeView(chosenEdge).isSelected();

		if (wasSelected && e.isShiftDown()) {
			((DEdgeView) m_view.getDEdgeView(chosenEdge)).unselectInternal();
			chosenEdgeSelected = -1;
		} else if (!wasSelected) {
			((DEdgeView) m_view.getDEdgeView(chosenEdge)).selectInternal(false);
			chosenEdgeSelected = 1;

			if ((m_lastRenderDetail & GraphRenderer.LOD_EDGE_ANCHORS) != 0) {
				m_ptBuff[0] = m_lastXMousePos;
				m_ptBuff[1] = m_lastYMousePos;
				m_view.xformComponentToNodeCoords(m_ptBuff);

				final LongEnumerator hits = m_view.m_spacialA.queryOverlap((float) m_ptBuff[0], (float) m_ptBuff[1],
						(float) m_ptBuff[0], (float) m_ptBuff[1], null, 0, false);

				if (hits.numRemaining() > 0) {
					final long hit = hits.nextLong();

					if (m_view.m_selectedAnchors.count(hit) == 0)
						m_view.m_selectedAnchors.insert(hit);
				}
			}
		}

		m_button1NodeDrag = true;
		m_view.m_contentChanged = true;
		return chosenEdgeSelected;
	}
	
	private long[] setSelectedNodes() {
		long [] selectedNodes = null;
		
		m_ptBuff[0] = m_selectionRect.x;
		m_ptBuff[1] = m_selectionRect.y;
		m_view.xformComponentToNodeCoords(m_ptBuff);

		final double xMin = m_ptBuff[0];
		final double yMin = m_ptBuff[1];
		m_ptBuff[0] = m_selectionRect.x + m_selectionRect.width;
		m_ptBuff[1] = m_selectionRect.y + m_selectionRect.height;
		m_view.xformComponentToNodeCoords(m_ptBuff);

		final double xMax = m_ptBuff[0];
		final double yMax = m_ptBuff[1];
		m_stack.empty();
		m_view.getNodesIntersectingRectangle((float) xMin, (float) yMin,
		                                     (float) xMax, (float) yMax,
		                                     (m_lastRenderDetail
		                                     & GraphRenderer.LOD_HIGH_DETAIL) == 0,
		                                     m_stack);
		m_stack2.empty();

		final LongEnumerator nodesXSect = m_stack.elements();

		while (nodesXSect.numRemaining() > 0) {
			final long nodeXSect = nodesXSect.nextLong();

			if (m_view.m_selectedNodes.count(nodeXSect) == 0)
				m_stack2.push(nodeXSect);
		}

		selectedNodes = new long[m_stack2.size()];

		final LongEnumerator nodes = m_stack2.elements();

		for (int i = 0; i < selectedNodes.length; i++)
			selectedNodes[i] = nodes.nextLong();

		for (int i = 0; i < selectedNodes.length; i++)
			((DNodeView) m_view.getDNodeView(selectedNodes[i])) .selectInternal();

		if (selectedNodes.length > 0)
			m_view.m_contentChanged = true;	
		return selectedNodes;
	}
	
	
	private long [] setSelectedEdges() {
		long [] selectedEdges = null;
		if ((m_lastRenderDetail & GraphRenderer.LOD_EDGE_ANCHORS) != 0) {
			m_ptBuff[0] = m_selectionRect.x;
			m_ptBuff[1] = m_selectionRect.y;
			m_view.xformComponentToNodeCoords(m_ptBuff);

			final double xMin = m_ptBuff[0];
			final double yMin = m_ptBuff[1];
			m_ptBuff[0] = m_selectionRect.x + m_selectionRect.width;
			m_ptBuff[1] = m_selectionRect.y + m_selectionRect.height;
			m_view.xformComponentToNodeCoords(m_ptBuff);

			final double xMax = m_ptBuff[0];
			final double yMax = m_ptBuff[1];
			final LongEnumerator hits = m_view.m_spacialA.queryOverlap((float) xMin,
			                                                          (float) yMin,
			                                                          (float) xMax,
			                                                          (float) yMax,
			                                                          null,
			                                                          0,
			                                                          false);

			if (hits.numRemaining() > 0)
				m_view.m_contentChanged = true;

			while (hits.numRemaining() > 0) {
				final long hit = hits.nextLong();

				if (m_view.m_selectedAnchors.count(hit) == 0)
					m_view.m_selectedAnchors.insert(hit);
			}
		}

		computeEdgesIntersecting(m_selectionRect.x, m_selectionRect.y,
		                         m_selectionRect.x + m_selectionRect.width,
		                         m_selectionRect.y + m_selectionRect.height,
		                         m_stack2);
		m_stack.empty();

		final LongEnumerator edgesXSect = m_stack2.elements();

		while (edgesXSect.numRemaining() > 0) {
			final long edgeXSect = edgesXSect.nextLong();

			if (m_view.m_selectedEdges.count(edgeXSect) == 0)
				m_stack.push(edgeXSect);
		}

		selectedEdges = new long[m_stack.size()];

		final LongEnumerator edges = m_stack.elements();

		for (int i = 0; i < selectedEdges.length; i++)
			selectedEdges[i] = edges.nextLong();

		for (int i = 0; i < selectedEdges.length; i++)
			((DEdgeView) m_view.getDEdgeView(selectedEdges[i])).selectInternal(false);

		if (selectedEdges.length > 0)
			m_view.m_contentChanged = true;
		return selectedEdges;
	}

	/**
	 * Returns the tool tip text for the specified location if any exists first
	 * checking nodes, then edges, and then returns null if it's empty space.
	 */
	private String getToolTipText(final Point p) {
		// display tips for nodes before edges
		final DNodeView nv = (DNodeView) m_view.getPickedNodeView(p);
		if (nv != null)  {
			final String tooltip = nv.getToolTip();
			return tooltip;
		}
		// only display edge tool tips if the LOD is sufficient
		if ((m_lastRenderDetail & GraphRenderer.LOD_HIGH_DETAIL) != 0) {
				DEdgeView ev = (DEdgeView) m_view.getPickedEdgeView(p);
				if (ev != null) 
					return m_view.m_edgeDetails.getTooltipText(ev.getCyEdge(), 0);
		}

		return null;
	}

	// Puts [last drawn] edges intersecting onto stack; as RootGraph indices.
	// Depends on the state of several member variables, such as m_hash.
	// Clobbers m_stack and m_ptBuff.
	// The rectangle extents are in component coordinate space.
	// IMPORTANT: Code that calls this method should be holding m_lock.
	final void computeEdgesIntersecting(final int xMini, final int yMini, final int xMaxi,
	                                    final int yMaxi, final LongStack stack) {
		m_ptBuff[0] = xMini;
		m_ptBuff[1] = yMini;
		m_view.xformComponentToNodeCoords(m_ptBuff);

		final double xMin = m_ptBuff[0];
		final double yMin = m_ptBuff[1];
		m_ptBuff[0] = xMaxi;
		m_ptBuff[1] = yMaxi;
		m_view.xformComponentToNodeCoords(m_ptBuff);

		final double xMax = m_ptBuff[0];
		final double yMax = m_ptBuff[1];
		LongEnumerator edgeNodesEnum = m_hash.elements(); // Positive.
		m_stack.empty();

		final int edgeNodesCount = edgeNodesEnum.numRemaining();

		for (int i = 0; i < edgeNodesCount; i++)
			m_stack.push(edgeNodesEnum.nextLong());

		m_hash.empty();
		edgeNodesEnum = m_stack.elements();
		stack.empty();

		final CyNetwork graph = m_view.m_drawPersp;

		if ((m_lastRenderDetail & GraphRenderer.LOD_HIGH_DETAIL) == 0) {
			// We won't need to look up arrows and their sizes.
			for (int i = 0; i < edgeNodesCount; i++) {
				final long node = edgeNodesEnum.nextLong(); // Positive.
				final CyNode nodeObj = graph.getNode(node);

				if (!m_view.m_spacial.exists(node, m_view.m_extentsBuff, 0))
					continue; // Will happen if e.g. node was removed. 

				final float nodeX = (m_view.m_extentsBuff[0] + m_view.m_extentsBuff[2]) / 2;
				final float nodeY = (m_view.m_extentsBuff[1] + m_view.m_extentsBuff[3]) / 2;
				final Iterable<CyEdge> touchingEdges = graph.getAdjacentEdgeIterable(nodeObj, CyEdge.Type.ANY);

				for ( CyEdge e : touchingEdges ) {      
					final long edge = e.getSUID(); 
					final long otherNode = node ^ e.getSource().getSUID().longValue() ^ e.getTarget().getSUID().longValue(); 

					if (m_hash.get(otherNode) < 0) {
						m_view.m_spacial.exists(otherNode, m_view.m_extentsBuff, 0);

						final float otherNodeX = (m_view.m_extentsBuff[0] + m_view.m_extentsBuff[2]) / 2;
						final float otherNodeY = (m_view.m_extentsBuff[1] + m_view.m_extentsBuff[3]) / 2;
						m_line.setLine(nodeX, nodeY, otherNodeX, otherNodeY);

						if (m_line.intersects(xMin, yMin, xMax - xMin, yMax - yMin))
							stack.push(edge);
					}
				}

				m_hash.put(node);
			}
		} else { // Last render high detail.
			for (int i = 0; i < edgeNodesCount; i++) {
				final long node = edgeNodesEnum.nextLong(); // Positive.
				final CyNode nodeObj = graph.getNode(node);

				if (!m_view.m_spacial.exists(node, m_view.m_extentsBuff, 0))
					continue; /* Will happen if e.g. node was removed. */

				final byte nodeShape = m_view.m_nodeDetails.getShape(nodeObj);
				final Iterable<CyEdge> touchingEdges = graph.getAdjacentEdgeIterable(nodeObj, CyEdge.Type.ANY);
 
				for ( CyEdge edge : touchingEdges ) {      
//					final int edge = e.getIndex(); // Positive.
					final double segThicknessDiv2 = m_view.m_edgeDetails.getWidth(edge) / 2.0d;
					final long otherNode = node ^ edge.getSource().getSUID().longValue() ^ edge.getTarget().getSUID().longValue();
					final CyNode otherNodeObj = graph.getNode(otherNode);

					if (m_hash.get(otherNode) < 0) {
						m_view.m_spacial.exists(otherNode, m_extentsBuff2, 0);

						final byte otherNodeShape = m_view.m_nodeDetails.getShape(otherNodeObj);
						final byte srcShape;
						final byte trgShape;
						final float[] srcExtents;
						final float[] trgExtents;

						if (node == edge.getSource().getSUID().longValue()) {
							srcShape = nodeShape;
							trgShape = otherNodeShape;
							srcExtents = m_view.m_extentsBuff;
							trgExtents = m_extentsBuff2;
						} else { // node == graph.edgeTarget(edge).
							srcShape = otherNodeShape;
							trgShape = nodeShape;
							srcExtents = m_extentsBuff2;
							trgExtents = m_view.m_extentsBuff;
						}

						final byte srcArrow;
						final byte trgArrow;
						final float srcArrowSize;
						final float trgArrowSize;

						if ((m_lastRenderDetail & GraphRenderer.LOD_EDGE_ARROWS) == 0) {
							srcArrow = trgArrow = GraphGraphics.ARROW_NONE;
							srcArrowSize = trgArrowSize = 0.0f;
						} else {
							srcArrow = m_view.m_edgeDetails.getSourceArrowShape(edge);
							trgArrow = m_view.m_edgeDetails.getTargetArrowShape(edge);
							srcArrowSize = ((srcArrow == GraphGraphics.ARROW_NONE) 
							                ? 0.0f
							                : m_view.m_edgeDetails.getSourceArrowSize(edge));
							trgArrowSize = ((trgArrow == GraphGraphics.ARROW_NONE) 
							                ? 0.0f
							                : m_view.m_edgeDetails.getTargetArrowSize(edge));
						}

						final EdgeAnchors anchors = (((m_lastRenderDetail
						                              & GraphRenderer.LOD_EDGE_ANCHORS) == 0)
						                             ? null : m_view.m_edgeDetails.getAnchors(edge));

						if (!GraphRenderer.computeEdgeEndpoints(m_grafx, srcExtents, srcShape,
						                                        srcArrow, srcArrowSize, anchors,
						                                        trgExtents, trgShape, trgArrow,
						                                        trgArrowSize, m_floatBuff1,
						                                        m_floatBuff2))
							continue;

						m_grafx.getEdgePath(srcArrow, srcArrowSize, trgArrow, trgArrowSize,
						                    m_floatBuff1[0], m_floatBuff1[1], anchors,
						                    m_floatBuff2[0], m_floatBuff2[1], m_path);
						GraphRenderer.computeClosedPath(m_path.getPathIterator(null), m_path2);

						if (m_path2.intersects(xMin - segThicknessDiv2, yMin - segThicknessDiv2,
						                       (xMax - xMin) + (segThicknessDiv2 * 2),
						                       (yMax - yMin) + (segThicknessDiv2 * 2)))
							stack.push(edge.getSUID().longValue());
					}
				}

				m_hash.put(node);
			}
		}
	}

	
	private void adjustZoom(int notches) {
		final double factor;
		
		if (notches < 0)
			factor = 1.1; // scroll up, zoom in
		else
			factor = 0.9; // scroll down, zoom out

		synchronized (m_lock) {
			m_scaleFactor = m_scaleFactor * factor;
		}

		m_view.m_viewportChanged = true;
		
		// Update view model.
		m_view.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, m_scaleFactor);
		repaint();
	}


	public int getLastRenderDetail() {
		return m_lastRenderDetail;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param s DOCUMENT ME!
	 */
	public void setSelecting(boolean s) {
		selecting = s;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public boolean isSelecting() {
		return selecting;
	}

	// 

	/**
	 * Called to get the tranform matrix used by the inner canvas
	 * to move the nodes.
	 *
	 * @return AffineTransform
	 */
	public AffineTransform getAffineTransform() {
		return (m_grafx != null) ? m_grafx.getTransform() : null;
	}

	public void enableNodeMovement(){
		this.NodeMovement = true;
	}
	
	public void disableNodeMovement(){
		this.NodeMovement = false;
	}
	
	public boolean isNodeMovementDisabled(){
		return !(this.NodeMovement);
	}

	
	public void enablePopupMenu(){
		this.enablePopupMenu = true;
	}
	
	public void disablePopupMenu(){
		this.enablePopupMenu = false;
	}
	
	public boolean isPopupMenuDisabled(){
		return !(this.enablePopupMenu);
	}

	
	/**
	 *  @param setLastRenderDetail if true, "m_lastRenderDetail" will be updated, otherwise it will not be updated.
	 */
	private void renderGraph(GraphGraphics graphics, final boolean setLastRenderDetail, final GraphLOD lod) {
		// Set color alpha based on opacity setting
		final int alpha = (m_isOpaque) ? 255 : 0;

		final Color backgroundColor = new Color(m_backgroundColor.getRed(), m_backgroundColor.getGreen(),
							m_backgroundColor.getBlue(), alpha);

		synchronized (m_lock) {
			final int lastRenderDetail = GraphRenderer.renderGraph(m_view.m_drawPersp,
									       m_view.m_spacial, lod,
									       m_view.m_nodeDetails,
									       m_view.m_edgeDetails, m_hash,
									       graphics, backgroundColor, m_xCenter,
									       m_yCenter, m_scaleFactor);
			if (setLastRenderDetail)
				m_lastRenderDetail = lastRenderDetail;
		}
	}

	private void handleEscapeKey() {
		AddEdgeStateMonitor.reset(m_view);
		repaint();
	}

	/**
	 * Arrow key handler.
	 * They are used to pan and mode nodes/edge bend handles.
	 * @param k key event
	 */
	private void handleArrowKeys(KeyEvent k) {
		final int code = k.getKeyCode();
		double move = 1.0;

		// Adjust increment if Shift key is pressed
		if (k.isShiftDown())
			move = 15.0;

		// Pan if CTR is pressed.
		if(k.isControlDown()) {
			// Pan
			if (code == KeyEvent.VK_UP) {
				pan(0, move);
			} else if (code == KeyEvent.VK_DOWN) {
				pan(0, -move);
			} else if (code == KeyEvent.VK_LEFT) {
				pan(-move, 0);
			} else if (code == KeyEvent.VK_RIGHT) {
				pan(move, 0);
			}
			return;
		}
		
		if (m_view.m_nodeSelection) {
			// move nodes
			final long[] selectedNodes = m_view.getSelectedNodeIndices();
			for (int i = 0; i < selectedNodes.length; i++) {
				DNodeView nv = ((DNodeView) m_view.getDNodeView(selectedNodes[i]));
				double xPos = nv.getXPosition();
				double yPos = nv.getYPosition();

				if (code == KeyEvent.VK_UP) {
					yPos -= move;
				} else if (code == KeyEvent.VK_DOWN) {
					yPos += move;
				} else if (code == KeyEvent.VK_LEFT) {
					xPos -= move;
				} else if (code == KeyEvent.VK_RIGHT) {
					xPos += move;
				}

				nv.setOffset(xPos, yPos);
			}

			// move edge anchors
			LongEnumerator anchorsToMove = m_view.m_selectedAnchors.searchRange(Integer.MIN_VALUE,
			                                                                   Integer.MAX_VALUE,
			                                                                   false);

			while (anchorsToMove.numRemaining() > 0) {
				final long edgeAndAnchor = anchorsToMove.nextLong();
				final long edge = edgeAndAnchor >>> 6;
				final int anchorInx = (int)(edgeAndAnchor & 0x000000000000003f);
				final DEdgeView ev = (DEdgeView) m_view.getDEdgeView(edge);
				
				final Bend bend = ev.getBend();
				final Handle handle = bend.getAllHandles().get(anchorInx);
				final Point2D newPoint = handle.calculateHandleLocation(m_view.getViewModel(),ev);
				m_floatBuff1[0] = (float) newPoint.getX();
				m_floatBuff1[1] = (float) newPoint.getY();

				if (code == KeyEvent.VK_UP) {
					ev.moveHandleInternal(anchorInx, m_floatBuff1[0], m_floatBuff1[1] - move);
				} else if (code == KeyEvent.VK_DOWN) {
					ev.moveHandleInternal(anchorInx, m_floatBuff1[0], m_floatBuff1[1] + move);
				} else if (code == KeyEvent.VK_LEFT) {
					ev.moveHandleInternal(anchorInx, m_floatBuff1[0] - move, m_floatBuff1[1]);
				} else if (code == KeyEvent.VK_RIGHT) {
					ev.moveHandleInternal(anchorInx, m_floatBuff1[0] + move, m_floatBuff1[1]);
				}
			}
			repaint();
		}
	}
	
	private void pan(double deltaX, double deltaY) {
		synchronized (m_lock) {
			m_xCenter -= (deltaX / m_scaleFactor);
			m_yCenter -= (deltaY / m_scaleFactor);
		}
		m_view.m_viewportChanged = true;
		repaint();
	}

	private class AddEdgeMousePressedDelegator extends ButtonDelegator {
		@Override
		void singleLeftClick(MouseEvent e) {
			Point rawPt = e.getPoint();
			double[] loc = new double[2];
			loc[0] = rawPt.getX();
			loc[1] = rawPt.getY();
			m_view.xformComponentToNodeCoords(loc);
			Point xformPt = new Point();
			xformPt.setLocation(loc[0],loc[1]); 
			NodeView nview = m_view.getPickedNodeView(rawPt);
			if ( nview != null && !InnerCanvas.this.isPopupMenuDisabled()) 
				popup.createNodeViewMenu(nview, e.getX(), e.getY(), "Edge");
		}
	}

	private final class MousePressedDelegator extends ButtonDelegator {

		@Override
		void singleLeftClick(MouseEvent e) {
			m_undoable_edit = null;
		
			m_currMouseButton = 1;
			m_lastXMousePos = e.getX();
			m_lastYMousePos = e.getY();
		
			long[] unselectedNodes = null;
			long[] unselectedEdges = null;
			long chosenNode = -1;
			long chosenEdge = -1;
			long chosenAnchor = -1;
			long chosenNodeSelected = 0;
			long chosenEdgeSelected = 0;
	
			synchronized (m_lock) {
				if (m_view.m_nodeSelection) {
					chosenNode = getChosenNode();
				}
	
				if (m_view.m_edgeSelection && (chosenNode < 0)
				    && ((m_lastRenderDetail & GraphRenderer.LOD_EDGE_ANCHORS) != 0)) {
					chosenAnchor = getChosenAnchor();
				}
	
				if (m_view.m_edgeSelection && (chosenNode < 0) && (chosenAnchor < 0)) {
					chosenEdge = getChosenEdge();
				}
	
				if ((!e.isShiftDown()) // If shift is down never unselect.
				    && (((chosenNode < 0) && (chosenEdge < 0) && (chosenAnchor < 0)) // Mouse missed all.
				       // Not [we hit something but it was already selected].
				       || !( ((chosenNode >= 0) && m_view.getDNodeView(chosenNode).isSelected())
				             || (chosenAnchor >= 0) 
				             || ((chosenEdge >= 0) && m_view.getDEdgeView(chosenEdge).isSelected()) ))) {
				
						unselectedNodes = getUnselectedNodes();
						unselectedEdges = getUnselectedEdges();
	
					if ((unselectedNodes.length > 0) || (unselectedEdges.length > 0))
						m_view.m_contentChanged = true;
				}
				
	
				if (chosenNode >= 0) {
				    chosenNodeSelected = toggleSelectedNode(chosenNode, e);
				}
	
				if (chosenAnchor >= 0) {
					toggleChosenAnchor(chosenAnchor, e);
				}
	
				if (chosenEdge >= 0)
					chosenEdgeSelected = toggleSelectedEdge(chosenEdge, e);
	
				if ((chosenNode < 0) && (chosenEdge < 0) && (chosenAnchor < 0)) {
					m_selectionRect = new Rectangle(m_lastXMousePos, m_lastYMousePos, 0, 0);
					m_button1NodeDrag = false;
				}
			}
	
			final GraphViewChangeListener listener = m_view.m_lis[0];
	
			// delegating to listeners
			if (listener != null) {
				if ((unselectedNodes != null) && (unselectedNodes.length > 0))
					listener.graphViewChanged(new GraphViewNodesUnselectedEvent(m_view,
   	                                       DGraphView.makeNodeList(unselectedNodes,m_view)));
	
				if ((unselectedEdges != null) && (unselectedEdges.length > 0))
					listener.graphViewChanged(new GraphViewEdgesUnselectedEvent(m_view,
   	                                       DGraphView.makeEdgeList(unselectedEdges,m_view)));
	
				if (chosenNode >= 0) {
					if (chosenNodeSelected > 0)
						listener.graphViewChanged(new GraphViewNodesSelectedEvent(m_view,
							DGraphView.makeList(((DNodeView)m_view.getDNodeView(chosenNode)).getModel())));
					else if (chosenNodeSelected < 0)
						listener.graphViewChanged(new GraphViewNodesUnselectedEvent(m_view,
							DGraphView.makeList(((DNodeView)m_view.getDNodeView(chosenNode)).getModel())));
				}
	
				if (chosenEdge >= 0) {
					if (chosenEdgeSelected > 0)
						listener.graphViewChanged(new GraphViewEdgesSelectedEvent(m_view,
							DGraphView.makeList(m_view.getDEdgeView(chosenEdge).getCyEdge())));
					else if (chosenEdgeSelected < 0)
						listener.graphViewChanged(new GraphViewEdgesUnselectedEvent(m_view,
							DGraphView.makeList(m_view.getDEdgeView(chosenEdge).getCyEdge())));
				}
			}
	
			// Repaint after listener events are fired because listeners may change
			// something in the graph view.
			repaint();
		}
	
		@Override
		void singleLeftControlClick(MouseEvent e) {
			// Cascade
			this.singleLeftClick(e);
		}
	
		@Override
		void singleMiddleClick(MouseEvent e) {
			//System.out.println("MousePressed ----> singleMiddleClick");
			// Save all node positions
			m_undoable_edit = new ViewChangeEdit(m_view,ViewChangeEdit.SavedObjs.NODES,"Move",m_undo);
			m_currMouseButton = 2;
			m_lastXMousePos = e.getX();
			m_lastYMousePos = e.getY();
		}
	
	
		@Override
		void singleRightClick(MouseEvent e) {
			//System.out.println("MousePressed ----> singleRightClick");
			// Save all node positions
			m_undoable_edit = new ViewChangeEdit(m_view,ViewChangeEdit.SavedObjs.NODES,"Move",m_undo);
			m_currMouseButton = 3;
			m_lastXMousePos = e.getX();
			m_lastYMousePos = e.getY();
		
			NodeView nview = m_view.getPickedNodeView(e.getPoint());
			if (nview != null && !InnerCanvas.this.isPopupMenuDisabled()) {
				popup.createNodeViewMenu(nview,e.getX(),e.getY(),"NEW");
			} else {
				EdgeView edgeView = m_view.getPickedEdgeView(e.getPoint());
				if (edgeView != null && !InnerCanvas.this.isPopupMenuDisabled()) {
					popup.createEdgeViewMenu(edgeView, e.getX(), e.getY(), "NEW");
				} else {
					// Clicked on empty space...
					Point rawPt = e.getPoint();
					double[] loc = new double[2];
					loc[0] = rawPt.getX();
					loc[1] = rawPt.getY();
					m_view.xformComponentToNodeCoords(loc);
					Point xformPt = new Point();
					xformPt.setLocation(loc[0],loc[1]); 
					if (!InnerCanvas.this.isPopupMenuDisabled()){
						popup.createNetworkViewMenu(rawPt, xformPt, "NEW");						
					}
				}
			}
		}
	
		@Override
		void doubleLeftClick(MouseEvent e) {
			//System.out.println("MousePressed ----> doubleLeftClick");
			NodeView nview = m_view.getPickedNodeView(e.getPoint());
			if ( nview != null && !InnerCanvas.this.isPopupMenuDisabled())
				popup.createNodeViewMenu(nview,e.getX(), e.getY(), "OPEN");
			else {
				EdgeView edgeView = m_view.getPickedEdgeView(e.getPoint());
				if (edgeView != null && !InnerCanvas.this.isPopupMenuDisabled()) {
					popup.createEdgeViewMenu(edgeView,e.getX(),e.getY(),"OPEN");
				} else {
					Point rawPt = e.getPoint();
					double[] loc = new double[2];
					loc[0] = rawPt.getX();
					loc[1] = rawPt.getY();
					m_view.xformComponentToNodeCoords(loc);
					Point xformPt = new Point();
					xformPt.setLocation(loc[0],loc[1]); 
					if (!InnerCanvas.this.isPopupMenuDisabled()){
						popup.createNetworkViewMenu(rawPt, xformPt, "OPEN");
					}
				}
			}
		}
	}

	private final class MouseReleasedDelegator extends ButtonDelegator {

		@Override
		void singleLeftClick(MouseEvent e) {
			//System.out.println("1. MouseReleased ----> singleLeftClick");
			
			if (m_currMouseButton == 1) {
				m_currMouseButton = 0;
	
				if (m_selectionRect != null) {
					long[] selectedNodes = null;
					long[] selectedEdges = null;
	
					synchronized (m_lock) {
						if (m_view.m_nodeSelection || m_view.m_edgeSelection) {
							if (m_view.m_nodeSelection)
								selectedNodes = setSelectedNodes();	
							if (m_view.m_edgeSelection)
								selectedEdges = setSelectedEdges();
						}
					}
					
					m_selectionRect = null;

					// Update visual property value (x/y)
					if (selectedNodes != null){
						for (long node : selectedNodes) {
							final DNodeView dNodeView = (DNodeView) m_view.getDNodeView(node);
							dNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, dNodeView.getXPosition());
							dNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, dNodeView.getYPosition());
						}						
					}
	
					final GraphViewChangeListener listener = m_view.m_lis[0];
	
					if (listener != null) {
						if ((selectedNodes != null) && (selectedNodes.length > 0))
							listener.graphViewChanged(new GraphViewNodesSelectedEvent(m_view,
					                                DGraphView.makeNodeList(selectedNodes,m_view)));
	
						if ((selectedEdges != null) && (selectedEdges.length > 0))
							listener.graphViewChanged(new GraphViewEdgesSelectedEvent(m_view,
					                               DGraphView.makeEdgeList(selectedEdges,m_view)));
					}
					
					// Repaint after listener events are fired because listeners may
					// change something in the graph view.
					repaint();
				}
			}
			
	
			if (m_undoable_edit != null)
				m_undoable_edit.post();
		}
	
		@Override
		void singleMiddleClick(MouseEvent e) {
			//System.out.println("MouseReleased ----> singleMiddleClick");
			if (m_currMouseButton == 2)
				m_currMouseButton = 0;
	
			if (m_undoable_edit != null)
				m_undoable_edit.post();
		}
	
		@Override
		void singleRightClick(MouseEvent e) {
			//System.out.println("MouseReleased ----> singleRightClick");
			if (m_currMouseButton == 3)
				m_currMouseButton = 0;
	
			if (m_undoable_edit != null)
				m_undoable_edit.post();
		}
	}

	private final class MouseDraggedDelegator extends ButtonDelegator {

		// emulate right click and middle click with a left clic and a modifier
		// TODO: make sure to be consistent with other emulation inside cytoscape and on Mac OSX
		static final int FAKEMIDDLECLIC = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.META_DOWN_MASK;
		static final int FAKERIGHTCLIC  = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK;
		
		@Override
		void singleLeftClick(MouseEvent e) {
			//System.out.println("MouseDragged ----> singleLeftClick");
			if (m_button1NodeDrag) {
				// save selected node and edge positions
				if (m_undoable_edit == null) {
					m_undoable_edit = new ViewChangeEdit(m_view, ViewChangeEdit.SavedObjs.SELECTED, "Move",m_undo);
				}
				synchronized (m_lock) {
					m_ptBuff[0] = m_lastXMousePos;
					m_ptBuff[1] = m_lastYMousePos;
					m_view.xformComponentToNodeCoords(m_ptBuff);
	
					final double oldX = m_ptBuff[0];
					final double oldY = m_ptBuff[1];
					m_lastXMousePos = e.getX();
					m_lastYMousePos = e.getY();
					m_ptBuff[0] = m_lastXMousePos;
					m_ptBuff[1] = m_lastYMousePos;
					m_view.xformComponentToNodeCoords(m_ptBuff);
	
					final double newX = m_ptBuff[0];
					final double newY = m_ptBuff[1];
					double deltaX = newX - oldX;
					double deltaY = newY - oldY;
	
					// If the shift key is down, then only move horizontally,
					// vertically, or diagonally, depending on the slope.
					if (e.isShiftDown()) {
						final double slope = deltaY / deltaX;
	
						// slope of 2.41 ~ 67.5 degrees (halfway between 45 and 90)
						// slope of 0.41 ~ 22.5 degrees (halfway between 0 and 45)
						if ((slope > 2.41) || (slope < -2.41)) {
							deltaX = 0.0; // just move vertical
						} else if ((slope < 0.41) && (slope > -0.41)) {
							deltaY = 0.0; // just move horizontal
						} else {
							final double avg = (Math.abs(deltaX) + Math.abs(deltaY)) / 2.0;
							deltaX = (deltaX < 0) ? (-avg) : avg;
							deltaY = (deltaY < 0) ? (-avg) : avg;
						}
					}
	
					// TODO: Optimize to not instantiate new array on every call.
					final long[] selectedNodes = m_view.getSelectedNodeIndices();
	
					for (int i = 0; i < selectedNodes.length; i++) {
						final NodeView dNodeView = m_view.getDNodeView(selectedNodes[i]);
						final double oldXPos = dNodeView.getXPosition();
						final double oldYPos = dNodeView.getYPosition();
						dNodeView.setOffset(oldXPos + deltaX, oldYPos + deltaY);
					}
	
					final LongEnumerator anchorsToMove = m_view.m_selectedAnchors.searchRange(Integer.MIN_VALUE,
					                                                                         Integer.MAX_VALUE,
					                                                                         false);
	
					while (anchorsToMove.numRemaining() > 0) {
						final long edgeAndAnchor = anchorsToMove.nextLong();
						final long edge = edgeAndAnchor >>> 6;
						final int anchorInx = (int)(edgeAndAnchor & 0x000000000000003f);
						final DEdgeView ev = (DEdgeView) m_view.getDEdgeView(edge);
						
						final Bend bend = ev.getBend();
						final Handle handle = bend.getAllHandles().get(anchorInx);
						final Point2D newPoint = handle.calculateHandleLocation(m_view.getViewModel(),ev);
						m_floatBuff1[0] = (float) newPoint.getX();
						m_floatBuff1[1] = (float) newPoint.getY();
						
						ev.moveHandleInternal(anchorInx, m_floatBuff1[0] + deltaX, m_floatBuff1[1] + deltaY);
					}
	
					if ((selectedNodes.length > 0) || (m_view.m_selectedAnchors.size() > 0))
						m_view.m_contentChanged = true;
				}
			}
	
			if (m_selectionRect != null) {
				final int x = Math.min(m_lastXMousePos, e.getX());
				final int y = Math.min(m_lastYMousePos, e.getY());
				final int w = Math.abs(m_lastXMousePos - e.getX());
				final int h = Math.abs(m_lastYMousePos - e.getY());
				m_selectionRect.setBounds(x, y, w, h);
			}
	
			repaint();
		}

		public void delegateMouseDragEvent(MouseEvent e) {
			// Note: the fake clicks are here for OSX but I do not see a reason to disable them on other systems
			switch (e.getModifiersEx()) {
			case MouseEvent.BUTTON1_DOWN_MASK:
				singleLeftClick(e);
				break;
			case MouseEvent.BUTTON2_DOWN_MASK:
			case FAKEMIDDLECLIC:
				singleMiddleClick(e);
				break;
			case MouseEvent.BUTTON3_DOWN_MASK:
			case FAKERIGHTCLIC:
				singleRightClick(e);
				break;
			}
		}

		@Override
		void singleMiddleClick(MouseEvent e) {
			double deltaX = e.getX() - m_lastXMousePos;
			double deltaY = e.getY() - m_lastYMousePos;
			m_lastXMousePos = e.getX();
			m_lastYMousePos = e.getY();
	
			synchronized (m_lock) {
				m_xCenter -= (deltaX / m_scaleFactor);
				m_yCenter -= (deltaY / m_scaleFactor);
			}
	
			m_view.m_viewportChanged = true;
			m_view.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, m_xCenter);
			m_view.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, m_yCenter);
			
			repaint();
		}

		@Override
		void singleRightClick(MouseEvent e) {
			//System.out.println("MouseDragged ----> singleRightClick");
			double deltaY = e.getY() - m_lastYMousePos;
	
			synchronized (m_lock) {
				m_lastXMousePos = e.getX();
				m_lastYMousePos = e.getY();
				m_scaleFactor *= Math.pow(2, -deltaY / 300.0d);
			}
	
			m_view.m_viewportChanged = true;
			repaint();
		}
	}

	public double getScaleFactor(){
		return m_scaleFactor;
	}

	public void dispose() {
		m_view = null;
		m_undoable_edit = null;
		addEdgeMode = null;
		popup.dispose();
	}
	
	public void ensureInitialized() {
		if (!m_grafx.isInitialized()) {
			m_grafx.setTransform(m_xCenter, m_yCenter, m_scaleFactor);
		}
	}
}
