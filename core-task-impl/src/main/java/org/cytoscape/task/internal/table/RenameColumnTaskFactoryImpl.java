/*
 Copyright (c) 2010-2011, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.task.internal.table;


import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.task.AbstractTableColumnTaskFactory;
import org.cytoscape.task.table.RenameColumnTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;


public final class RenameColumnTaskFactoryImpl extends AbstractTableColumnTaskFactory implements RenameColumnTaskFactory{
	private final UndoSupport undoSupport;

	private final TunableSetter tunableSetter; 

	public RenameColumnTaskFactoryImpl(final UndoSupport undoSupport, TunableSetter tunableSetter) {
		this.undoSupport = undoSupport;
		this.tunableSetter = tunableSetter;
	}
	@Override
	public TaskIterator createTaskIterator(CyColumn column) {
		if (column == null)
			throw new IllegalStateException("you forgot to set the CyColumn on this task factory!");
		return new TaskIterator(new RenameColumnTask(undoSupport, column));
	}
	@Override
	public TaskIterator createTaskIterator(CyColumn column, String newColumnName) {
		final Map<String, Object> m = new HashMap<String, Object>();
		m.put("newColumnName", newColumnName);

		return tunableSetter.createTaskIterator(this.createTaskIterator(column), m); 
	}
}