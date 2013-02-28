package org.cytoscape.task.internal.loadvizmap;

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

import java.net.URL;

import java.util.HashSet;
import java.util.Set;

import org.cytoscape.task.read.LoadVizmapURLTaskFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoadVizmapURLTaskFactoryImpl extends AbstractTaskFactory implements LoadVizmapURLTaskFactory {

    @Override
    public TaskIterator createTaskIterator() {
        task = new LoadVizmapURLTask();
        return new TaskIterator(task);
    }

    @Override
    public Set<VisualStyle> loadStyles(final URL url) {
        return new HashSet<VisualStyle>();
    }

    @Override
    public TaskIterator createTaskIterator(final URL url) {
        return createTaskIterator();
    }
}