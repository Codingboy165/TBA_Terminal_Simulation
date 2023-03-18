package com.tba.terminal_simulation.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class Gate {
    private int inBoundLanes;
    private int outBoundLanes;
    private int handlingLocations;
    private CopyOnWriteArrayList<Truck> trucksAtInboundLanes = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Truck> trucksAtOutboundLanes = new CopyOnWriteArrayList<>();

    public Gate(int inBoundLanes, int outBoundLanes, int handlingLocations) {
        this.inBoundLanes = inBoundLanes;
        this.outBoundLanes = outBoundLanes;
        this.handlingLocations = handlingLocations;
    }
}
