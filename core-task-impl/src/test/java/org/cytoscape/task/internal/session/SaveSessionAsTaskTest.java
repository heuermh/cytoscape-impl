package org.cytoscape.task.internal.session;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.io.util.RecentlyOpenedTracker;
import org.cytoscape.io.write.CySessionWriterManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SaveSessionAsTaskTest {
	
	@Mock private TaskMonitor tm;
	@Mock private CySessionManager mgr;
	@Mock private CySessionWriterManager writerMgr;
	@Mock private RecentlyOpenedTracker tracker;
	@Mock private CyEventHelper cyEventHelper;
	
	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}


	@Test(expected=NullPointerException.class)
	public void testSaveSessionAsTask() throws Exception {
		final SaveSessionAsTask t = new SaveSessionAsTask(writerMgr, mgr, tracker, cyEventHelper);
		t.setTaskIterator(new TaskIterator(t));
		
		t.run(tm);
		verify(mgr, times(1)).getCurrentSession();
	}
}
