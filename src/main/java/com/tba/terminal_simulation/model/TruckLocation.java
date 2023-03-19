package com.tba.terminal_simulation.model;

//Here I made an enum because a truck can have maximum 9 location in all his travel
//from start gate to exit gate
public enum TruckLocation {

    PARKING_PLACE,
    AT_THE_START_GATE,
    ON_THE_WAY_TO_THE_STACK,
    WAITING_FOR_FREE_PLACE_AT_THE_STACK,
    AT_THE_STACK,
    RETURNING_FROM_THE_STACK,
    WAITING_FOR_FREE_PLACE_AT_THE_EXIT_GATE,
    AT_EXIT_GATE,
    PASSED_THE_EXIT_GATE
}
