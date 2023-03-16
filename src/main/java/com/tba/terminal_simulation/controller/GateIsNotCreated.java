package com.tba.terminal_simulation.controller;

import lombok.Getter;

@Getter
public class GateIsNotCreated extends RuntimeException {

    private final String statusMsg;
    private final String status;
    public GateIsNotCreated(String status, String statusMsg) {
        this.status=status;
        this.statusMsg=statusMsg;
    }

}
