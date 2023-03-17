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
    private List<Truck> trucksAtInboundLanes = new LinkedList<>();
    private List<Truck> trucksAtOutboundLanes = new LinkedList<>();

    public Gate(int inBoundLanes, int outBoundLanes, int handlingLocations) {
        this.inBoundLanes = inBoundLanes;
        this.outBoundLanes = outBoundLanes;
        this.handlingLocations = handlingLocations;
    }
}
