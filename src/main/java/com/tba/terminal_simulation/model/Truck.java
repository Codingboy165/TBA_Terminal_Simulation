package com.tba.terminal_simulation.model;

import com.tba.terminal_simulation.model.TruckLocation;
import com.tba.terminal_simulation.service.TruckService;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tba.terminal_simulation.service.TruckService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.distribution.GammaDistribution;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class Truck implements Runnable {

    //Here I created a static variable to increment the id automatically for every truck object
    @JsonIgnore
    private static Long nextId = 1L;
    //Here is the real id
    private Long id;
    //This type it means if a truck is delivering or receive
    private TruckType type;
    private TruckLocation truckLocation;
    @JsonIgnore
    private int allExerciseTimeBeforeExit;

    //In the constructor the User just instantiate a truck with a type. The id will be incrementing automatically
    public Truck(TruckType type) {
        this.id = nextId++;
        this.type = type;
    }

    //I made a private truck constructor only for spring and I made private because
    // I don't want to let the User instantiate a truck without telling me if the truck is delivers or receive
    private Truck() {

    }

    public double getHandlingTime() {
        double alpha = 9.0; // shape parameter
        double beta = 3.0; // rate parameter
        GammaDistribution gammaDist = new GammaDistribution(alpha, beta);
        return gammaDist.sample();
    }

    //I made this class to be a Thread because of the multithreading. I want in one point if al lanes are occupied the other
    //Trucks/threads to sleep or to wait until a lane is free
    @Override
    public void run() {

        Random random = new Random();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        int atGateDeliver = random.nextInt(240000) + 120000;
        int atGateReceiver = random.nextInt(420000) + 180000;
        int toTheStack = random.nextInt(61000) + 150000;
        int atTheStackT = (int) getHandlingTime();
        int returningFromTheStack = random.nextInt(61000) + 150000;
        int atExitGateDeliver = random.nextInt(240000) + 120000;
        int atExitGateReceiver = random.nextInt(420000) + 180000;


        boolean theTruckCanBeAtTheStackImmediately = false;
        boolean theTruckCanBeAtTheExitGateImmediately = false;

        if (this.type == TruckType.RECEIVE) {

            truckLocation = TruckLocation.AT_THE_START_GATE;

            System.out.println("The truck with id #" + id + " which receive is at the gate...");
            //Schedule task1 with a delay between 3 minutes and 9 minutes
            Runnable task = () -> System.out.println("The truck with id #" + id + " passed the gate, and now it is going to the stack");
            executor.schedule(task, atGateReceiver, TimeUnit.MILLISECONDS);
            executor.shutdown();

            try {
                if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.ON_THE_WAY_TO_THE_STACK;


            ScheduledExecutorService goingToStackArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task7 = () -> System.out.println("The truck with id #" + id + " is at the stack area...");
            goingToStackArea.schedule(task7, toTheStack, TimeUnit.MILLISECONDS);
            goingToStackArea.shutdown();
            try {
                if (!goingToStackArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    goingToStackArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                goingToStackArea.shutdownNow();
                Thread.currentThread().interrupt();
            }


            while (!TruckService.handlingLocationChecker()) {
                try {
                    Thread.sleep(random.nextInt(3000));
                    truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_STACK;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


            //Here I add this truck to the handlingLocation list. If the user set the handling locations smaller than the
            //inbound lanes, or some trucks are faster and coming to the handling locations and for example the terminal
            //has only 3 handling location but the trucks which are coming are 4, one is needed to wait to others to terminate
            //his work at the handling location
            TruckService.handlingLocationAdder(this);
            // Create a new executor instance, which will handle the time for that trucks which are going to the stack
            ScheduledExecutorService goingToStack = Executors.newSingleThreadScheduledExecutor();
            Runnable task2 = () -> System.out.println("The truck with id #" + id + " is at the stack...");
            //If the truck is at the handling location but needs to wait because other truck is already at the handling location
            //after that truck is going away this truck don't need to wait, the truck can go to the handling location right now
            goingToStack.schedule(task2, 1, TimeUnit.MILLISECONDS);
            goingToStack.shutdown();
            try {
                if (!goingToStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    goingToStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                goingToStack.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.AT_THE_STACK;

            // Create a new executor instance, which will handle the time at the stack
            ScheduledExecutorService atTheStack = Executors.newSingleThreadScheduledExecutor();
            // Schedule task3 with a delay by the Gamma value
            Runnable task3 = () -> System.out.println("The truck with id #" + id + " returning from the stack...");
            atTheStack.schedule(task3, atTheStackT, TimeUnit.MINUTES);
            atTheStack.shutdown();
            try {
                if (!atTheStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atTheStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                atTheStack.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.RETURNING_FROM_THE_STACK;

            //Here we call this static method to remove the truck from the handlingLocation list, and other trucks if the list size
            //not equals with gate.getHandlingLocations() which is returning an int that means other truck can go to the handling location
            TruckService.handlingLocationRemover(this);

            ScheduledExecutorService atExitGateArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task6 = () -> System.out.println("The truck with id #" + id + " is near at the exit gate...");
            atExitGateArea.schedule(task6, returningFromTheStack, TimeUnit.MILLISECONDS);
            atExitGateArea.shutdown();

            try {
                if (!atExitGateArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atExitGateArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                atExitGateArea.shutdownNow();
                Thread.currentThread().interrupt();
            }


            //Here I check if the User has entered different inbound and outbound lanes than only that number
            //of trucks to go to the exit lane. If for example 3 truck is returning form the handling location, from the stack
            //and the gate has only 2 outbound lane than 1 truck needs to wait until 1 of 2 trucks passed the exit gate
            while (!TruckService.endGateChecker(this)) {
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_EXIT_GATE;
                System.out.println("The truck with id #" + id + " is waiting at exit lane " +
                        "because every lane is already in use");
                try {
                    Thread.sleep(random.nextInt(3000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


            // Create a new executor instance, which will handle the time for that trucks which are coming back from the stack
            ScheduledExecutorService comingBackFromTheStack = Executors.newSingleThreadScheduledExecutor();
            // Schedule task4 with a delay between 2 minutes 30 sec and 3 minutes 30 sec
            Runnable task4 = () -> System.out.println("The truck with id #" + id + " is at the exit gate...");
            comingBackFromTheStack.schedule(task4, 1, TimeUnit.MILLISECONDS);
            comingBackFromTheStack.shutdown();
            try {
                if (!comingBackFromTheStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    comingBackFromTheStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                comingBackFromTheStack.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.AT_EXIT_GATE;


            // Create a new executor instance which is handle the time at the exit gate
            ScheduledExecutorService outBoundLaneExecutor = Executors.newSingleThreadScheduledExecutor();

            // Schedule task5 with a delay between 3 and 9 minute
            Runnable task5 = () -> System.out.println("The truck with id #" + id + " passed the exit gate");
            outBoundLaneExecutor.schedule(task5, atExitGateReceiver, TimeUnit.MILLISECONDS);
            outBoundLaneExecutor.shutdown();
            try {
                if (!outBoundLaneExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    outBoundLaneExecutor.shutdownNow();
                } // wait for the task to complete
            } catch (InterruptedException e) {
                outBoundLaneExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            TruckService.endGateRemover(this);

            truckLocation = TruckLocation.PASSED_THE_EXIT_GATE;

            //I use this variable because I check with a thread if this truck ha passed the end gate. If it passed
            //I remove from the list


        } else if (this.type == TruckType.DELIVER) {

            allExerciseTimeBeforeExit = atGateDeliver + toTheStack + atTheStackT + returningFromTheStack + atExitGateDeliver;

            truckLocation = TruckLocation.AT_THE_START_GATE;

            System.out.println("The truck with id #" + id + " which delivers, is at the gate...");
            // Schedule task1 with a delay between 2 minutes and 5 minutes
            Runnable task = () -> System.out.println("The truck with id #" + id + " passed the gate, and now it is going to the stack");
            executor.schedule(task, atGateDeliver, TimeUnit.MILLISECONDS);
            executor.shutdown();

            try {
                if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.ON_THE_WAY_TO_THE_STACK;


            ScheduledExecutorService goingToStackArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task7 = () -> System.out.println("The truck with id #" + id + " is at the stack area...");
            goingToStackArea.schedule(task7, toTheStack, TimeUnit.MILLISECONDS);
            goingToStackArea.shutdown();
            try {
                if (!goingToStackArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    goingToStackArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                goingToStackArea.shutdownNow();
                Thread.currentThread().interrupt();
            }


            while (!TruckService.handlingLocationChecker()) {
                try {
                    Thread.sleep(random.nextInt(3000));
                    truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_STACK;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            TruckService.handlingLocationAdder(this);

            // Create a new executor instance, which will handle the time for that trucks which are going to the stack
            ScheduledExecutorService goingToStack = Executors.newSingleThreadScheduledExecutor();
            Runnable task2 = () -> System.out.println("The truck with id #" + id + " is at the stack...");

            goingToStack.schedule(task2, 1, TimeUnit.MILLISECONDS);

            goingToStack.shutdown();
            try {
                if (!goingToStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    goingToStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                goingToStack.shutdownNow();
                Thread.currentThread().interrupt();
            }

            truckLocation = TruckLocation.AT_THE_STACK;

            // Create a new executor instance, which will handle the time at the stack
            ScheduledExecutorService atTheStack = Executors.newSingleThreadScheduledExecutor();
            // Schedule task3 with a delay between by the Gamma value
            Runnable task3 = () -> System.out.println("The truck with id #" + id + " returning from the stack...");
            atTheStack.schedule(task3, atTheStackT, TimeUnit.MINUTES);
            atTheStack.shutdown();
            try {
                if (!atTheStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atTheStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                atTheStack.shutdownNow();
                Thread.currentThread().interrupt();
                // handle InterruptedException
            }

            truckLocation = TruckLocation.RETURNING_FROM_THE_STACK;

            TruckService.handlingLocationRemover(this);

            // Create a new executor instance, which will handle the time for that trucks which are coming back from the stack
            ScheduledExecutorService atExitGateArea = Executors.newSingleThreadScheduledExecutor();
            // Schedule task4 with a delay between 2 minutes 30 sec and 3 minutes 30 sec
            Runnable task6 = () -> System.out.println("The truck with id #" + id + " is near at the exit gate...");
            atExitGateArea.schedule(task6, atGateDeliver, TimeUnit.MILLISECONDS);

            atExitGateArea.shutdown();
            try {
                if (!atExitGateArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atExitGateArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                atExitGateArea.shutdownNow();
                Thread.currentThread().interrupt();
            }


            while (!TruckService.endGateChecker(this)) {
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_EXIT_GATE;
                try {
                    Thread.sleep(random.nextInt(3000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


            // Create a new executor instance, which will handle the time for that trucks which are coming back from the stack
            ScheduledExecutorService comingBackFromTheStack = Executors.newSingleThreadScheduledExecutor();
            // Schedule task4 with a delay between 2 minutes 30 sec and 3 minutes 30 sec
            Runnable task4 = () -> System.out.println("The truck with id #" + id + " is at the exit gate...");
            comingBackFromTheStack.schedule(task4, 1, TimeUnit.NANOSECONDS);
            comingBackFromTheStack.shutdown();
            try {
                if (!comingBackFromTheStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    comingBackFromTheStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                comingBackFromTheStack.shutdownNow();
                Thread.currentThread().interrupt();
            }


            truckLocation = TruckLocation.AT_EXIT_GATE;


            // Create a new executor instance which is handle the time at the exit gate
            ScheduledExecutorService outBoundLaneExecutor = Executors.newSingleThreadScheduledExecutor();

            // Schedule task2 with a delay of 5 seconds
            Runnable task5 = () -> System.out.println("The truck with id #" + id + " passed the exit gate");
            outBoundLaneExecutor.schedule(task5, atExitGateDeliver, TimeUnit.MILLISECONDS);
            outBoundLaneExecutor.shutdown();
            try {
                if (!outBoundLaneExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    outBoundLaneExecutor.shutdownNow();
                } // wait for the task to complete
            } catch (InterruptedException e) {
                outBoundLaneExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            TruckService.endGateRemover(this);
            truckLocation = TruckLocation.PASSED_THE_EXIT_GATE;

        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Truck truck = (Truck) o;
        return id.equals(truck.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
