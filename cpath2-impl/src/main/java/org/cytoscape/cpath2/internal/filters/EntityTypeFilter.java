package org.cytoscape.cpath2.internal.filters;

/*
 * #%L
 * Cytoscape CPath2 Impl (cpath2-impl)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cytoscape.cpath2.internal.schemas.summary_response.BasicRecordType;
import org.cytoscape.cpath2.internal.util.BioPaxEntityTypeMap;

/**
 * EntityType Filter.
 *
 * @author Ethan Cerami
 */
public class EntityTypeFilter implements Filter {
    Set<String> entityTypeSet;

    /**
     * Constructor.
     *
     * @param entityTypeSet Set of Entity Types we want to keep.
     */
    public EntityTypeFilter(Set<String> entityTypeSet) {
        this.entityTypeSet = entityTypeSet;
    }

    /**
     * Filters the record list.  Those items which pass the filter
     * are included in the returned list.
     *
     * @param recordList List of RecordType Objects.
     * @return List of RecordType Objects.
     */
    public List<BasicRecordType> filter(List<BasicRecordType> recordList) {
        BioPaxEntityTypeMap bpMap = BioPaxEntityTypeMap.getInstance();
        ArrayList<BasicRecordType> passedList = new ArrayList<BasicRecordType>();
        for (BasicRecordType record : recordList) {
            String type = record.getEntityType();
            if (type != null) {
            	String bioPaxType = (String) bpMap.get(type);
                if (bioPaxType != null) {
                    type = bioPaxType;
                }
                if (entityTypeSet.contains(type)) {
                    passedList.add(record);
                }
            }
        }
        return passedList;
    }
}