/*
 * Copyright (c) 2016, Timothy Aitken
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.caldfir.importcoords;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.Exceptions;
import org.gephi.ui.propertyeditor.NodeColumnNumbersEditor;

/**
 *
 * @author Tim
 */
public class ImportCartesianLayout implements Layout {
    
    private static final float DEFAULT_SCALE = 1000;
    private static final float DEFAULT_COORD = 0;
    
    // gephi layout admin
    private final ImportCartesianLayoutBuilder builder;
    private GraphModel graphModel;
    private boolean cancel;
    
    // properties
    private double scale = DEFAULT_SCALE;
    private Column xColumn;
    private Column yColumn;
    
    ImportCartesianLayout( ImportCartesianLayoutBuilder builder ){
        this.builder = builder;
    }
    
    @Override
    public void initAlgo() {
        cancel = false;
    }

    @Override
    public void setGraphModel(GraphModel graphModel) {
        this.graphModel = graphModel;
    }

    @Override
    public void goAlgo() {
        
        getGraph().readLock();
        
        // load, shift, and re-scale the coordinates
        List<Float> xScaledCoords = reScaleCoords(readCoords(xColumn));
        List<Float> yScaledCoords = reScaleCoords(readCoords(yColumn));
        
        // go back over the nodes and update them
        Iterator<Node> ni = getNodes().iterator();
        Iterator<Float> xi = xScaledCoords.iterator();
        Iterator<Float> yi = yScaledCoords.iterator();
        while( ni.hasNext() ){
            Node node = ni.next();
            node.setX(xi.next());
            node.setY(yi.next());
        }
        
        getGraph().readUnlock();
        
        cancel = true;
    }
    
    private List<Float> readCoords(Column column){
        
        // alright, the attributes could literally be anything, but we hope that 
        // they are numeric so we can do stuff with them
        List<Float> coordList = new LinkedList<Float>();
        NodeIterable nodeList = getNodes();
        for( Node node : nodeList ){
            Object obj = node.getAttribute(column);
            if( obj instanceof Number ){
                Number num = (Number) obj;
                coordList.add(num.floatValue());
            }
            else {
                coordList.add(DEFAULT_COORD);
            }
        }
        
        return coordList;
    }
    
    private List<Float> reScaleCoords(List<Float> coords){ 
        
        // get the max/min of the data
        float min = Collections.min(coords);
        float max = Collections.max(coords);
        
        // calculate some intermediate values
        float span = max - min;
        float av = min + (span/2);
        float rescale = (float) (scale/span);
        
        // iterate over the elements 
        List<Float> reScaledList = new LinkedList<Float>();
        for( float coord : coords ){
            reScaledList.add(rescale*(coord - av));
        }
        
        return reScaledList;
    }

    @Override
    public boolean canAlgo() {
        return !cancel && xColumn != null && yColumn != null;
    }

    @Override
    public void endAlgo() {
        // no-op
    }

    @Override
    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new LinkedList<LayoutProperty>();
        
        try {
            properties.add( LayoutProperty.createProperty(
                    this,
                    Double.class,
                    "scale",
                    builder.getName(),
                    "points will be re-scaled to fit within a bounding region of this size",
                    "getScale",
                    "setScale"));
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        try {
            properties.add( LayoutProperty.createProperty(
                    this,
                    Column.class,
                    "xColumn",
                    builder.getName(),
                    "column containing x-coordinates",
                    "getXColumn",
                    "setXColumn",
                    NodeColumnNumbersEditor.class));
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        try {
            properties.add( LayoutProperty.createProperty(
                    this,
                    Column.class,
                    "yColumn",
                    builder.getName(),
                    "column containing y-coordinates",
                    "getYColumn",
                    "setYColumn",
                    NodeColumnNumbersEditor.class));
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return properties.toArray(new LayoutProperty[0]);
    }

    @Override
    public void resetPropertiesValues() {
        this.scale = DEFAULT_SCALE;
        this.xColumn = null;
        this.yColumn = null;
    }

    @Override
    public LayoutBuilder getBuilder() {
        return builder;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public Column getXColumn() {
        return xColumn;
    }

    public void setXColumn(Column xColumn) {
        this.xColumn = xColumn;
    }

    public Column getYColumn() {
        return yColumn;
    }

    public void setYColumn(Column yColumn) {
        this.yColumn = yColumn;
    }
    
    private Graph getGraph(){
        return graphModel.getGraphVisible();
    }
    
    private NodeIterable getNodes(){
        return getGraph().getNodes();
    }
}
