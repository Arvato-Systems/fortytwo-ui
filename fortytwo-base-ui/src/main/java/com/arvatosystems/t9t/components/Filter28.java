/*
 * Copyright (c) 2012 - 2018 Arvato Systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arvatosystems.t9t.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.arvatosystems.t9t.tfi.web.ApplicationSession;
import com.arvatosystems.t9t.base.CrudViewModel;
import com.arvatosystems.t9t.base.FieldMappers;
import com.arvatosystems.t9t.component.ext.IGridIdOwner;
import com.arvatosystems.t9t.component.fields.FieldFactory;
import com.arvatosystems.t9t.component.fields.IField;
import com.arvatosystems.t9t.components.tools.ILeanGridConfigResolver;
import com.arvatosystems.t9t.components.tools.LeanGridConfigResolver;

import de.jpaw.bonaparte.api.SearchFilters;
import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.pojos.api.SearchFilter;
import de.jpaw.bonaparte.pojos.api.TrackingBase;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.ui.UIFilter;
import de.jpaw.bonaparte.util.ToStringHelper;

/** Creates a grid with filters based on a grid definition.
 * Emits a search event when the search button has been pushed.
 * The event data is populated with the correct search request. */
public class Filter28 extends Grid implements IGridIdOwner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Filter28.class);
    private static final long serialVersionUID = 234233457567L;

    private final ApplicationSession session = ApplicationSession.get();
    private Button28 resetButton;
    private Button28 searchButton;
    private Button28 toggleButton;
    private final List<IField> filters = new ArrayList<IField>(15);

    private int columnLength = 4;
    private String gridId;
    private String viewModelId;
    private ILeanGridConfigResolver leanGridConfigResolver;
    private CrudViewModel<BonaPortable, TrackingBase> crudViewModel;  // set when gridId is defined

    public Filter28() {
        super();
        LOGGER.debug("new Filter28() created");
        setVflex("min");
        setSclass("grid");
    }

    @Override
    public void setGridId(String gridId) {
        LOGGER.debug("Filter28() assigned grid ID {}", gridId);
        this.gridId = gridId;
        setViewModelId(GridIdTools.getViewModelIdByGridId(gridId));

        addColumns();
        populateFilters();
    }

    public void resetSearchFilters() {
        super.removeChild(super.getRows());
        populateFilters();
    }

    private void populateFilters() {
        leanGridConfigResolver = new LeanGridConfigResolver(gridId, session);
        addRows();
        resetButton.addEventListener (Events.ON_CLICK, (MouseEvent ev) -> { performReset(); });
        searchButton.addEventListener(Events.ON_CLICK, (MouseEvent ev) -> { performSearch("onClick"); });
        this.addEventListener(Events.ON_OK, (KeyEvent ev) -> { performSearch("onOk"); });
        if (toggleButton != null) {
            toggleButton.addEventListener(Events.ON_CLICK, (MouseEvent ev) -> {
                Events.postEvent("onToSOLR", this, null);
            });
        }
        registerFields();
    }
    // the purpose of the next method is unclear....
    void registerFields() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("filters", filters);
        BindUtils.postGlobalCommand(null, null, "registerFields", args);
    }

    private void performSearch(String cause) {
        SearchFilter current = null;
        for (IField f : filters) {
            SearchFilter sf = f.getSearchFilter();
            if (sf != null)
                current = SearchFilters.and(current, f.isNegated() ? SearchFilters.not(sf) : sf);
        }
        LOGGER.debug("Filter triggered by {}, running {}", cause, ToStringHelper.toStringML(current));

        Events.postEvent("onSearch", this, current);
    }

    private void performReset() {
        for (IField<?> e: filters) {
            e.clear();
        }
    }


    private void addColumns() {
        Columns cols = new Columns();
        for (int count = 0; count < columnLength; count++) {
            Column col = new Column();
            col.setLabel("");
            col.setHflex("1");
            cols.appendChild(col);
        }
        super.appendChild(cols);
    }

    private void addRows() {
        filters.clear();
        Rows rows = new Rows();

        // compose each component
        Row eachRow = new Row();

        FieldFactory factory = new FieldFactory(crudViewModel, gridId, session);

        int index = 0;
        for (UIFilter filter : leanGridConfigResolver.getFilters()) {
            String fieldname = filter.getFieldName();
            FieldDefinition fieldDef = FieldMappers.getFieldDefinitionForPath(fieldname, crudViewModel);
            // nope we need the list of components here, similar but different
            IField field = factory.createField(fieldname, filter, fieldDef);

            if (field != null) {
                filters.add(field);
                // add each component
                List<Component> components = field.getComponents();
                for (Component c : components) {
                    Cell eachCell = new Cell();
                    eachCell.appendChild(c);
                    eachRow.appendChild(eachCell);
                    // append and create new row when it has reached the column length
                    if ((index + 1) % columnLength == 0) {
                        rows.appendChild(eachRow);
                        eachRow = new Row();
                        eachRow.setVflex("min");
                    }
                    index++;
                }
            }
        }

        // calculate the cols span
        int colsSpan = columnLength - (index % columnLength);

        // add buttons
        Cell lastCell = new Cell();
        lastCell.setColspan(colsSpan);
        lastCell.setAlign("right");

        // add search button
        searchButton = new Button28();
        searchButton.setId("searchButton");
        //searchButton.setAutodisable("searchButton,newButton,saveButton,deleteButton,resetButton");
        lastCell.appendChild(searchButton);

        // add reset button
        resetButton = new Button28();
        resetButton.setId("resetButton");
        //resetButton.setAutodisable("searchButton,newButton,saveButton,deleteButton,resetButton");
        lastCell.appendChild(resetButton);

        if (crudViewModel.searchClass.getProperty("isSolr") != null) {
            // is SOLR
            toggleButton = new Button28();
            toggleButton.setId("toggleButton");
            //resetButton.setAutodisable("searchButton,newButton,saveButton,deleteButton,resetButton");
            lastCell.appendChild(toggleButton);
        } else {
            toggleButton = null;
        }
        // append row into rows
        eachRow.appendChild(lastCell);
        rows.appendChild(eachRow);

        super.appendChild(rows);
    }

    @Override
    public String getGridId() {
        return gridId;
    }

    @Listen("onCreate")
    public void onCreate() {
        LOGGER.debug("Filter28.onCreate()");
        GridIdTools.enforceGridId(this);
    }

    @Override
    public CrudViewModel<BonaPortable, TrackingBase> getCrudViewModel() {
        GridIdTools.enforceGridId(this);
        return crudViewModel;
    }

    @Override
    public String getViewModelId() {
        return viewModelId;
    }

    @Override
    public void setViewModelId(String viewModelId) {
        this.viewModelId = viewModelId;
        crudViewModel = GridIdTools.getViewModelByViewModelId(viewModelId);
    }

    @Override
    public ApplicationSession getSession() {
        return session;
    }
}
