package com.tba.terminal_simulation.service;

public class CheckerThreadForStartGate implements Runnable {


    private final TruckService truckService;

    public CheckerThreadForStartGate(TruckService truckService) {
        this.truckService = truckService;
    }

    @Override
    public void run() {
            if (TruckService.getGate().getTrucksAtInboundLanes().size() == TruckService.getGate().getInBoundLanes()) {
                if(truckService.checkIfHaveFreePlaceAtTheGate()){
                    truckService.stopCheckingFreePlacesAtStartGate();
                }
            }
    }
}
