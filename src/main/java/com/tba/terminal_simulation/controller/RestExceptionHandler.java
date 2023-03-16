package com.tba.terminal_simulation.controller;

import com.tba.terminal_simulation.service.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(GateIsNotCreated.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response handleRuntimeException(GateIsNotCreated exception){
        return new Response(exception.getStatus(),exception.getStatusMsg());
    }

}
