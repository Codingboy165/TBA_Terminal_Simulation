package com.tba.terminal_simulation.service;

public class CheckerThreadForStartGate implements Runnable {


    private final TruckService truckService;

    public CheckerThreadForStartGate(TruckService truckService) {
        this.truckService = truckService;
    }

    @Override
    public void run() {
            if (TruckService.getGate().getTrucksAtInboundLanes().size() == TruckService.getGate().getInBoundLanes()) {
                try {
                    if(truckService.checkIfHaveFreePlaceAtTheGate()){
                        truckService.stopCheckingFreePlacesAtStartGate();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }
    }
}
