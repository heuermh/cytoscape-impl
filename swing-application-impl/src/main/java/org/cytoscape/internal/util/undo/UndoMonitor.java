package org.cytoscape.internal.util.undo;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
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

import java.util.Properties;

import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewDestroyedListener;
import org.cytoscape.work.swing.undo.SwingUndoSupport;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.property.CyProperty;

/**
 * This class monitors the undoable edit stack and implements whatever
 * discard policy we might have. Currently, we discard all edits if
 * the network view focus changes.
 */
public class UndoMonitor implements SetCurrentNetworkViewListener, NetworkDestroyedListener, NetworkViewDestroyedListener {

	private SwingUndoSupport undo;
	private Properties props;

	public UndoMonitor(SwingUndoSupport undo,CyProperty<Properties> cyProps) {

		this.undo = undo;
		this.props = cyProps.getProperties();

		undo.getUndoManager().setLimit( getLimit() );
	}

    private int getLimit() {
        int lim;
        try {
            lim = Integer.parseInt( props.getProperty("undo.limit") );
        } catch ( Exception e ) {
            e.printStackTrace();
            lim = 10;
        }

        if ( lim < 0 )
            lim = 10;

        return lim;
    }


	/**
	 * This method listens for changes to the current network and discards all
	 * edits when the network changes.
	 * 
	 * @param e The change event.
	 */
	public void handleEvent(final SetCurrentNetworkViewEvent e) {
		if (e.getNetworkView() != null)
			undo.getUndoManager().discardAllEdits();
	}


    /**
 	 * This method listens for a network destroy event. If the network being destroyed 
 	 * is the only available network, it discards all of its edits. Hence, when more
 	 * than one network is available, destruction of an inactive network (which does not
 	 * have the current network view) will not discard the useful edits of the active 
 	 * network.
 	 * Moreover, if the edits are related to the destroyed network, the
 	 * SetCurrentNetwrkViewEvent will handle the discard of edits.
	 *
	 * @param e The change event.
	 */
	@Override
	public void handleEvent(NetworkDestroyedEvent e) {
		if (e.getSource().getNetworkSet().isEmpty())
			undo.getUndoManager().discardAllEdits();
	}

	
	/**
 	 * This method listens for a network view destroy event. If the network view being destroyed 
 	 * is the only visible view, it discards all of its edits. In case more
 	 * than one network view is available, the SetCurrentNetworkView handles
 	 * the discard of the edits. 
	 * @param e The change event.
	 */
	@Override
	public void handleEvent(NetworkViewDestroyedEvent e) {

		if (e.getSource().getNetworkViewSet().isEmpty())
			undo.reset();
	}
}

