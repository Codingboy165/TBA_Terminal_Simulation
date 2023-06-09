package com.tba.terminal_simulation.controller;

import com.tba.terminal_simulation.model.Gate;
import com.tba.terminal_simulation.model.Truck;
import com.tba.terminal_simulation.model.TruckType;
import com.tba.terminal_simulation.service.TruckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TerminalController {

    private final TruckService truckService;

    public TerminalController(TruckService truckService){
        this.truckService = truckService;
    }

    @PostMapping("api/gate/create")
    public ResponseEntity<Response> createAGate(@RequestBody Gate gate){
        return truckService.createAGate(gate.getInBoundLanes(), gate.getOutBoundLanes(), gate.getHandlingLocations());
    }
    @PostMapping("api/trucks/send")
    public ResponseEntity<Response> createATruck(@RequestBody List<Truck> truck){
        return truckService.addTrucksToTheInBoundLane(truck);
    }
    @GetMapping("api/trucks/gate")
    public Map<TruckType,Long> getAllTruckPerReceiveAnDeliverAtTheGate(){
        return truckService.getAllTheTrucksFromTheInboundLanes();
    }
    @GetMapping("api/trucks/location")
    public List<Truck> getAllTrucksLocation(){
        return truckService.getAllTheTruckLocation();
    }

}
