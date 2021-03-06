package org.cytoscape.view.vizmap.gui.internal;

/*
 * #%L
 * Cytoscape VizMap GUI Impl (vizmap-gui-impl)
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualLexiconNode;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.events.SetCurrentVisualStyleEvent;
import org.cytoscape.view.vizmap.events.SetCurrentVisualStyleListener;
import org.cytoscape.view.vizmap.gui.DefaultViewEditor;
import org.cytoscape.view.vizmap.gui.editor.EditorManager;
import org.cytoscape.view.vizmap.gui.event.LexiconStateChangedEvent;
import org.cytoscape.view.vizmap.gui.event.LexiconStateChangedListener;
import org.cytoscape.view.vizmap.gui.internal.util.VizMapperUtil;
import org.cytoscape.view.vizmap.gui.util.PropertySheetUtil;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledPanel;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.swing.plaf.blue.BlueishButtonUI;

/**
 * Dialog for editing default visual property values.<br>
 * This is a modal dialog.
 * 
 * <p>
 * Basic idea is the following:
 * <ul>
 * <li>Build dummy network with 2 nodes and 1 edge.</li>
 * <li>Edit the default appearence of the dummy network</li>
 * <li>Create a image from the dummy.</li>
 * </ul>
 * </p>
 * 
 */
public class DefaultViewEditorImpl extends JDialog implements DefaultViewEditor, SetCurrentVisualStyleListener,
		LexiconStateChangedListener, PropertyChangeListener {

	private final static long serialVersionUID = 1202339876675416L;

	private static final Logger logger = LoggerFactory.getLogger(DefaultViewEditorImpl.class);

	private static final int ICON_WIDTH = 48;
	private static final int ICON_HEIGHT = 48;

	private final Map<Class<? extends CyIdentifiable>, Set<VisualProperty<?>>> vpSets;
	private final Map<Class<? extends CyIdentifiable>, JList> listMap;

	private final CyApplicationManager cyApplicationManager;

	private final EditorManager editorFactory;
	private final VisualMappingManager vmm;
	private final VizMapperUtil util;
	private final DefaultViewPanelImpl mainView;
	private final CyEventHelper cyEventHelper;
	private final SetViewModeAction viewModeAction;

	private DependencyTable depTable;

	public DefaultViewEditorImpl(final DefaultViewPanelImpl mainView,
								 final EditorManager editorFactory,
								 final CyApplicationManager cyApplicationManager,
								 final VisualMappingManager vmm,
								 final VizMapperUtil util,
								 final CyEventHelper cyEventHelper,
								 final SetViewModeAction viewModeAction) {
		super();

		if (mainView == null)
			throw new NullPointerException("DefaultViewPanel is null.");

		if (vmm == null)
			throw new NullPointerException("Visual Mapping Manager is null.");

		this.cyEventHelper = cyEventHelper;

		this.vmm = vmm;
		this.util = util;
		vpSets = new HashMap<Class<? extends CyIdentifiable>, Set<VisualProperty<?>>>();
		listMap = new HashMap<Class<? extends CyIdentifiable>, JList>();

		this.cyApplicationManager = cyApplicationManager;
		this.setModal(true);
		this.mainView = mainView;
		this.editorFactory = editorFactory;
		
		this.viewModeAction = viewModeAction;
		viewModeAction.addPropertyChangeListener(this);

		updateVisualPropertyLists();

		initComponents();
		buildList();

		// Listening to resize event.
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				defaultObjectTabbedPane.repaint();
				mainView.updateView();
			}
		});
	}

	private void updateVisualPropertyLists() {
		vpSets.clear();

		vpSets.put(CyNode.class, getLeafNodes(util.getVisualPropertySet(CyNode.class)));
		vpSets.put(CyEdge.class, getLeafNodes(util.getVisualPropertySet(CyEdge.class)));
		vpSets.put(CyNetwork.class, getNetworkLeafNodes(util.getVisualPropertySet(CyNetwork.class)));
	}

	private Set<VisualProperty<?>> getLeafNodes(final Collection<VisualProperty<?>> props) {

		final Set<VisualLexicon> lexSet = vmm.getAllVisualLexicon();

		final Set<VisualProperty<?>> propSet = new TreeSet<VisualProperty<?>>(new VisualPropertyComparator());

		for (VisualLexicon lexicon : lexSet) {
			for (VisualProperty<?> vp : props) {
				if (lexicon.getVisualLexiconNode(vp).getChildren().size() == 0)
					propSet.add(vp);
			}
		}
		return propSet;

	}

	private Set<VisualProperty<?>> getNetworkLeafNodes(final Collection<VisualProperty<?>> props) {
		final Set<VisualLexicon> lexSet = vmm.getAllVisualLexicon();

		final Set<VisualProperty<?>> propSet = new TreeSet<VisualProperty<?>>(new VisualPropertyComparator());

		for (VisualLexicon lexicon : lexSet) {
			for (VisualProperty<?> vp : props) {
				if (lexicon.getVisualLexiconNode(vp).getChildren().size() == 0
						&& lexicon.getVisualLexiconNode(vp).getParent().getVisualProperty() == BasicVisualLexicon.NETWORK)
					propSet.add(vp);
			}
		}
		return propSet;

	}

	@Override
	public void showEditor(Component parent) {
		updateVisualPropertyLists();
		buildList();

		updateDependencyTable();

		mainView.updateView();
		setSize(900, 450);
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	private void updateDependencyTable() {
		final VisualStyle selectedStyle = vmm.getCurrentVisualStyle();
		final Set<VisualPropertyDependency<?>> dependencies = selectedStyle.getAllVisualPropertyDependencies();
		final DependencyTableModel depTableModel = new DependencyTableModel();

		for (VisualPropertyDependency<?> dep : dependencies) {
			final Object[] newRow = new Object[2];
			newRow[0] = dep.isDependencyEnabled();
			newRow[1] = dep.getDisplayName();
			depTableModel.addRow(newRow);
		}

		depTable = new DependencyTable(cyApplicationManager, cyEventHelper, depTableModel, dependencies);
		dependencyScrollPane.setViewportView(depTable);
		depTable.repaint();
	}

	private void initComponents() {
		jXPanel1 = new JXPanel();
		jXTitledPanel1 = new JXTitledPanel();
		defaultObjectTabbedPane = new JTabbedPane();
		nodeScrollPane = new JScrollPane();
		dependencyScrollPane = new JScrollPane();

		nodeList = new JXList();
		edgeList = new JXList();
		edgeScrollPane = new JScrollPane();
		globalScrollPane = new JScrollPane();
		applyButton = new JButton();

		networkList = new JXList();

		listMap.put(CyNode.class, nodeList);
		listMap.put(CyEdge.class, edgeList);
		listMap.put(CyNetwork.class, networkList);

		cancelButton = new JButton();
		cancelButton.setText("Cancel");
		cancelButton.setVisible(false);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});

		showAllVPButton = new JToggleButton();
		showAllVPButton.setText("Show All");
		showAllVPButton.setToolTipText("Show all Visual Properties");
		showAllVPButton.setUI(new BlueishButtonUI());
		showAllVPButton.setSelected(PropertySheetUtil.isAdvancedMode());
		showAllVPButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				viewModeAction.menuSelected(null);
				viewModeAction.actionPerformed(null);
			}
		});

		nodeList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				listActionPerformed(e);
			}
		});

		edgeList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				listActionPerformed(e);
			}
		});

		networkList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				listActionPerformed(e);
			}
		});

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		jXTitledPanel1.setTitle("Default Visual Properties");
		jXTitledPanel1.setTitleFont(new Font("SansSerif", 1, 12));
		jXTitledPanel1.setMinimumSize(new Dimension(300, 27));
		jXTitledPanel1.setPreferredSize(new Dimension(300, 27));
		defaultObjectTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

		nodeScrollPane.setViewportView(nodeList);
		edgeScrollPane.setViewportView(edgeList);
		globalScrollPane.setViewportView(networkList);
		dependencyScrollPane.setViewportView(depTable);

		defaultObjectTabbedPane.addTab("Node", nodeScrollPane);
		defaultObjectTabbedPane.addTab("Edge", edgeScrollPane);
		defaultObjectTabbedPane.addTab("Network", globalScrollPane);
		defaultObjectTabbedPane.addTab("Dependency", dependencyScrollPane);

		GroupLayout jXTitledPanel1Layout = new GroupLayout(jXTitledPanel1.getContentContainer());
		jXTitledPanel1.getContentContainer().setLayout(jXTitledPanel1Layout);
		jXTitledPanel1Layout.setHorizontalGroup(jXTitledPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(defaultObjectTabbedPane, GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE));
		jXTitledPanel1Layout.setVerticalGroup(jXTitledPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(defaultObjectTabbedPane, GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE));

		applyButton.setText("Apply");
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final CyNetworkView view = cyApplicationManager.getCurrentNetworkView();
				if (view != null)
					applyNewStyle(view);
				dispose();
			}
		});

		GroupLayout jXPanel1Layout = new GroupLayout(jXPanel1);
		jXPanel1.setLayout(jXPanel1Layout);
		jXPanel1Layout.setHorizontalGroup(jXPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
				jXPanel1Layout
						.createSequentialGroup()
						.addContainerGap()
						.addGroup(
								jXPanel1Layout
										.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addGroup(
												jXPanel1Layout.createSequentialGroup()
														.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(cancelButton)
														.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(applyButton))
														.addGroup(
																GroupLayout.Alignment.TRAILING,
																jXPanel1Layout.createSequentialGroup()
																	.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
																	.addComponent(showAllVPButton))
										.addComponent(mainView, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jXTitledPanel1, GroupLayout.PREFERRED_SIZE, 198, Short.MAX_VALUE)
						.addGap(12, 12, 12)));
		jXPanel1Layout
				.setVerticalGroup(jXPanel1Layout
						.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(
								GroupLayout.Alignment.TRAILING,
								jXPanel1Layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												jXPanel1Layout
														.createParallelGroup(GroupLayout.Alignment.TRAILING)
														.addComponent(jXTitledPanel1, GroupLayout.Alignment.LEADING,
																GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
														.addGroup(
																jXPanel1Layout
																		.createSequentialGroup()
																		.addComponent(mainView,
																				GroupLayout.DEFAULT_SIZE,
																				GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addPreferredGap(
																				LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(
																				jXPanel1Layout
																						.createParallelGroup(
																								GroupLayout.Alignment.BASELINE)
																						.addComponent(cancelButton)
																						.addComponent(applyButton)
																						.addComponent(showAllVPButton))))
										.addContainerGap()));

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jXPanel1,
				GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jXPanel1,
				GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		pack();
	} // </editor-fold>

	private <V> void listActionPerformed(MouseEvent e) {
		final Object source = e.getSource();
		final JList list;

		if (source instanceof JList)
			list = (JList) source;
		else
			return;

		V newValue = null;

		@SuppressWarnings("unchecked")
		final VisualProperty<V> vp = (VisualProperty<V>) list.getSelectedValue();

		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
			final VisualStyle selectedStyle = vmm.getCurrentVisualStyle();
			final V defaultVal = selectedStyle.getDefaultValue(vp);
			
			try {
				if (defaultVal != null)
					newValue = editorFactory.showVisualPropertyValueEditor(this, vp, defaultVal);
				else
					newValue = editorFactory.showVisualPropertyValueEditor(this, vp, vp.getDefault());
			} catch (Exception ex) {
				logger.error("Error opening Visual Property values editor for: " + vp, ex);
			}

			if (newValue != null) {
				// Got new value. Apply to the dummy view.
				selectedStyle.setDefaultValue(vp, newValue);
				mainView.updateView();
				mainView.repaint();
			}

			repaint();
		}
	}

	private void applyNewStyle(final CyNetworkView view) {
		final VisualStyle selectedStyle = vmm.getCurrentVisualStyle();
		vmm.setVisualStyle(selectedStyle, view);
		selectedStyle.apply(view);
		view.updateView();
	}

	// Variables declaration - do not modify
	private JButton applyButton;
	private JButton cancelButton;
	private JToggleButton showAllVPButton;
	private JScrollPane nodeScrollPane;
	private JScrollPane edgeScrollPane;
	private JScrollPane globalScrollPane;

	// New from 3.0
	private JScrollPane dependencyScrollPane;

	private JTabbedPane defaultObjectTabbedPane;
	private JXList nodeList;
	private JXList edgeList;
	private JXList networkList;
	private JXPanel jXPanel1;

	private JXTitledPanel jXTitledPanel1;

	// End of variables declaration

	/**
	 * Populate the list model based on current lexicon tree structure.
	 */
	private void buildList() {
		final VisualPropCellRenderer renderer = new VisualPropCellRenderer();
		final RenderingEngine<CyNetwork> engine = mainView.getRenderingEngine();
		
		if (engine == null)
			return;
		
		final VisualLexicon lex = engine.getVisualLexicon();
		final VisualStyle selectedStyle = vmm.getCurrentVisualStyle();

		for (Class<? extends CyIdentifiable> key : vpSets.keySet()) {
			final DefaultListModel model = new DefaultListModel();
			final JList list = listMap.get(key);

			list.setModel(model);
			final Set<VisualProperty<?>> vps = vpSets.get(key);
			for (final VisualProperty<?> vp : vps) {

				// Check supported or not.
				if (PropertySheetUtil.isCompatible(vp) == false)
					continue;

				// Filter based on mode
				if (PropertySheetUtil.isAdvancedMode() == false) {
					if (PropertySheetUtil.isBasic(vp) == false)
						continue;
				}

				// Do not allow editing of the following two VP
				if (vp.getDisplayName().contains("Edge Target Arrow Selected Paint")
						|| vp.getDisplayName().contains("Edge Source Arrow Selected Paint")) {
					continue;
				}

				// Filter based on dependency:
				final VisualLexiconNode treeNode = lex.getVisualLexiconNode(vp);
				if (treeNode != null)
					model.addElement(vp);
				
				// Override dependency
				final Set<VisualPropertyDependency<?>> dependencies = selectedStyle.getAllVisualPropertyDependencies();
				for (VisualPropertyDependency<?> dep : dependencies) {
					if (dep.isDependencyEnabled()) {
						final VisualProperty<?> parentVP = dep.getParentVisualProperty();
						
						if (parentVP.getTargetDataType() == key) {
							if (model.contains(parentVP) == false)
								model.addElement(parentVP);
							
							Set<VisualProperty<?>> props = dep.getVisualProperties();
							for (VisualProperty<?> prop : props)
								model.removeElement(prop);
						}
					}
				}
			}
			
			list.setCellRenderer(renderer);
		}
	}

	private final class VisualPropCellRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = -1325179272895141114L;

		private final Font SELECTED_FONT = new Font("SansSerif", Font.ITALIC, 14);
		private final Font NORMAL_FONT = new Font("SansSerif", Font.BOLD, 12);
		private final Color SELECTED_COLOR = new Color(10, 50, 180, 20);
		private final Color SELECTED_FONT_COLOR = new Color(0, 150, 255, 150);

		private final int ICON_GAP = 55;

		VisualPropCellRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			final VisualStyle selectedStyle = vmm.getCurrentVisualStyle();

			Icon icon = null;
			VisualProperty<Object> vp = null;

			if (value instanceof VisualProperty<?>) {
				vp = (VisualProperty<Object>) value;

				final RenderingEngine<?> presentation = mainView.getRenderingEngine();

				if (presentation != null) {
					final Object defValue = selectedStyle.getDefaultValue(vp);
					icon = presentation.createIcon(vp, selectedStyle.getDefaultValue(vp), ICON_WIDTH, ICON_HEIGHT);

					if (defValue != null)
						setToolTipText(defValue.toString());
				}
			}

			setText(vp.getDisplayName());

			setIcon(icon);
			setFont(isSelected ? SELECTED_FONT : NORMAL_FONT);

			this.setVerticalTextPosition(CENTER);
			this.setVerticalAlignment(CENTER);
			this.setIconTextGap(ICON_GAP);

			setBackground(isSelected ? SELECTED_COLOR : list.getBackground());
			setForeground(isSelected ? SELECTED_FONT_COLOR : list.getForeground());

			if (icon != null) {
				setPreferredSize(new Dimension(250, icon.getIconHeight() + 24));
			} else {
				setPreferredSize(new Dimension(250, 55));
			}

			this.setBorder(new DropShadowBorder());

			return this;
		}
	}

	@Override
	public Component getDefaultView(final VisualStyle vs) {
		mainView.updateView(vs);
		return mainView;
	}

	@Override
	public void handleEvent(final SetCurrentVisualStyleEvent e) {
		final VisualStyle selectedStyle = e.getVisualStyle();
		setTitle("Default Appearance for " + selectedStyle.getTitle());
	}

	/**
	 * Handles local property change event. This will be used to switch view mode: show all VPs or basic VPs only.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent fromSetViewMode) {
		// Need to update property sheet.
		if (fromSetViewMode.getPropertyName().equals(SetViewModeAction.VIEW_MODE_CHANGED)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showAllVPButton.setSelected(PropertySheetUtil.isAdvancedMode());
				}
			});
			
			buildList();
		}
	}
	
	@Override
	public void handleEvent(LexiconStateChangedEvent e) {
		logger.debug("Def editor got Lexicon update event.");
		buildList();

		mainView.updateView();
		mainView.repaint();
	}
	
	private static class VisualPropertyComparator implements Comparator<VisualProperty<?>> {

		@Override
		public int compare(VisualProperty<?> vp1, VisualProperty<?> vp2) {
			String name1 = vp1.getDisplayName();
			String name2 = vp2.getDisplayName();

			return name1.compareTo(name2);
		}
	}
}
