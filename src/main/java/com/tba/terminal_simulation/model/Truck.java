package com.tba.terminal_simulation.model;

import com.tba.terminal_simulation.service.TruckService;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.distribution.GammaDistribution;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


//This is a class which extends the Runnable interface, that means each truck is a thread and I can
//start from a parking place to do the tasks
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

    //The truckLocation hold  values that represents where the truck is. It is updated after every task, time
    private TruckLocation truckLocation;

    //In the constructor the User just instantiate a truck with a type. The id will be incrementing automatically
    public Truck(TruckType type) {
        this.id = nextId++;
        this.type = type;
    }


    /**
     * This method is needed for handling time, how long the truck will stay at the stack, at the handling location.
     *
     * @return the time which is needed the truck to stay at the stack
     */
    @JsonIgnore
    public double getHandlingTime() {
        double alpha = 9.0; // shape parameter
        double beta = 3.0; // rate parameter
        GammaDistribution gammaDist = new GammaDistribution(alpha, beta);
        return gammaDist.sample();
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

    @Override
    public void run() {

        //To generate random values for time I use the java.util.random class
        Random random = new Random();

        //These are the values which are used for the time for certain tasks
        //These are used like milliseconds
        //240000 = 4 minutes , 120000 = 2 minutes, 420000 = 7 minutes,  180000 = 3 minutes
        //60000 = 1 minute, 150000 = 2,5 minutes
        //I put to every random + 1 millisecond because the java random generate values 0 to
        //the number what I add to, EXCLUSIVE!! That means I need to add a number to generate
        //random values to that value inclusive

        int atGateDeliver = random.nextInt(240001) + 120000;
        int atGateReceiver = random.nextInt(420001) + 180000;
        int toTheStack = random.nextInt(60001) + 150000;
        int atTheStackT = (int) getHandlingTime();
        int returningFromTheStack = random.nextInt(60001) + 150000;
        int atExitGateDeliver = random.nextInt(240001) + 120000;
        int atExitGateReceiver = random.nextInt(420001) + 180000;

        //I check if the truck is receiving
        if (this.type == TruckType.RECEIVE) {

            //I update the truckLocation because if I start this thread that means the start gate has free place and
            //this truck can go from the parking place to the inbound lane
            truckLocation = TruckLocation.AT_THE_START_GATE;


            //I make a scheduledExecutorService to schedule the time that truck is spend at the start gate
            System.out.println("The truck with id #" + id + " which receive is at the gate...");
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            //Schedule task with a delay between 3 minutes and 9 minutes
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

            //I update the truckLocation because after the scheduledExecutorService shutdown that means
            //the truck spend a random time between 3 minutes and 9 minutes and passed the gate. That means
            //the truck is going to the stack
            truckLocation = TruckLocation.ON_THE_WAY_TO_THE_STACK;

            //I make another scheduledExecutorService to schedule the time that truck is spend by going to stack
            ScheduledExecutorService goingToStackArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task2 = () -> System.out.println("The truck with id #" + id + " is at the stack area...");
            goingToStackArea.schedule(task2, toTheStack, TimeUnit.MILLISECONDS);
            goingToStackArea.shutdown();
            try {
                if (!goingToStackArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    goingToStackArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                goingToStackArea.shutdownNow();
                Thread.currentThread().interrupt();
            }

            //Here is a checker if the truck can go to the handling location. I made this checker which is responsible
            //to not let more trucks that can not fit to the handling locations to go to the handling location.
            //For example if the user type to handling location 3 but coming 5 trucks this checker ensure that only 3 trucks
            //book place at the 3 handling location. Other 2 truck needs to wait until one truck go away from the handling
            //location and returning to the exit gate. If the trucks can go to the handling location than this method also
            //add to the that list
            while (!TruckService.handlingLocationChecker(this)) {
                //Also I update the location of this truck
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_STACK;
            }

            System.out.println("The truck with id #" + id + " is at the stack...");

            //If the truck pass the checker that means the truck is no need to wait anymore. This truck is at the stack now
            truckLocation = TruckLocation.AT_THE_STACK;

            // Create a new executor instance, which will handle the time at the stack
            ScheduledExecutorService atTheStack = Executors.newSingleThreadScheduledExecutor();
            // Schedule task3 with a delay by the Gamma value
            Runnable task4 = () -> System.out.println("The truck with id #" + id + " returning from the stack...");
            atTheStack.schedule(task4, atTheStackT, TimeUnit.MINUTES);
            atTheStack.shutdown();
            try {
                if (!atTheStack.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atTheStack.shutdownNow();
                }
            } catch (InterruptedException e) {
                atTheStack.shutdownNow();
                Thread.currentThread().interrupt();
            }

            //After the executor-service shutdown than we can update the truckLocation to RETURNING_FROM_THE_STACK
            truckLocation = TruckLocation.RETURNING_FROM_THE_STACK;

            //Here we call this static method to remove the truck from the handlingLocation list
            TruckService.handlingLocationRemover(this);

            //I create this executor service to handle the time for the truck which returning from the stack, from the
            //handling location
            ScheduledExecutorService atExitGateArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task5 = () -> System.out.println("The truck with id #" + id + " is near at the exit gate...");
            atExitGateArea.schedule(task5, returningFromTheStack, TimeUnit.MILLISECONDS);
            atExitGateArea.shutdown();

            try {
                if (!atExitGateArea.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    atExitGateArea.shutdownNow();
                }
            } catch (InterruptedException e) {
                atExitGateArea.shutdownNow();
                Thread.currentThread().interrupt();
            }


            //Here I check if the User has entered different inbound and outbound lanes, then only that number
            //of trucks to go to the exit lane. If for example 3 truck is returning form the handling location, from the stack
            //and the gate has only 2 outbound lane than 1 truck needs to wait until 1 of 2 trucks passed the exit gate
            while (!TruckService.endGateChecker(this)) {
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_EXIT_GATE;
            }

            System.out.println("The truck with id #" + id + " is at the exit gate...");

            //If the truck passed checker that means the truck it doesn't need to wait anymore, and we can update the truckLocation
            //to AT_EXIT_GATE
            truckLocation = TruckLocation.AT_EXIT_GATE;


            // Create a new executor instance which is handle the time that the truck is spending at the exit gate
            ScheduledExecutorService outBoundLaneExecutor = Executors.newSingleThreadScheduledExecutor();
            // Schedule task7 with a delay between 3 and 9 minute
            Runnable task7 = () -> System.out.println("The truck with id #" + id + " passed the exit gate");
            outBoundLaneExecutor.schedule(task7, atExitGateReceiver, TimeUnit.MILLISECONDS);
            outBoundLaneExecutor.shutdown();
            try {
                if (!outBoundLaneExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    outBoundLaneExecutor.shutdownNow();
                } // wait for the task to complete
            } catch (InterruptedException e) {
                outBoundLaneExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            //Here we remove the truck from the gate outbound lane
            TruckService.endGateRemover(this);

            //And finally we update the truckLocation with value PASSED_THE_EXIT_GATE
            truckLocation = TruckLocation.PASSED_THE_EXIT_GATE;

            //This truck has done his job :)
            //The thread will shut down

            //I check if the truck is delivers
        } else if (this.type == TruckType.DELIVER) {

            //I update the truckLocation because if I start this thread that means the start gate has free place and
            //this truck can go from the parking place to the inbound lane
            truckLocation = TruckLocation.AT_THE_START_GATE;

            System.out.println("The truck with id #" + id + " which delivers, is at the gate...");
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> System.out.println("The truck with id #" + id + " passed the gate, and now it is going to the stack");
            // Schedule task1 with a delay between 2 minutes and 5 minutes
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

            //I update the truckLocation because after the scheduledExecutorService shutdown that means
            //the truck spend a random time between 2 minutes and 5 minutes and passed the gate. That means
            //the truck is going to the stack
            truckLocation = TruckLocation.ON_THE_WAY_TO_THE_STACK;

            //I make another scheduledExecutorService to schedule the time that truck is spend by going to stack
            ScheduledExecutorService goingToStackArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task7 = () -> System.out.println("The truck with id #" + id + " is at the stack area...");
            //Schedule task2 with a delay between 2:30 and 3:30 minutes
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

            //Here is a checker if the truck can go to the handling location. I made this checker which is responsible
            //to not let more trucks that can not fit to the handling locations to go to the handling location.
            //For example if the user type to handling location 3 but coming 5 trucks this checker ensure that only 3 trucks
            //book place at the 3 handling location. Other 2 truck needs to wait until one truck go away from the handling
            //location and returning to the exit gate. If the trucks can go to the handling location than this method also
            //add to the that list
            while (!TruckService.handlingLocationChecker(this)) {
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_STACK;
            }

            System.out.println("The truck with id #" + id + " is at the stack...");

            //If the truck pass the checker that means the truck is no need to wait anymore. This truck is at the stack now
            truckLocation = TruckLocation.AT_THE_STACK;

            // Create a new executor instance, which will handle the time at the stack
            ScheduledExecutorService atTheStack = Executors.newSingleThreadScheduledExecutor();
            Runnable task3 = () -> System.out.println("The truck with id #" + id + " returning from the stack...");
            // Schedule task3 with a delay between by the Gamma value
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

            //After the executor-service shutdown than we can update the truckLocation to RETURNING_FROM_THE_STACK
            truckLocation = TruckLocation.RETURNING_FROM_THE_STACK;

            //Here we call this static method to remove the truck from the handlingLocation list
            TruckService.handlingLocationRemover(this);

            // Create a new executor instance, which will handle the time for that trucks which are coming back from the stack
            ScheduledExecutorService atExitGateArea = Executors.newSingleThreadScheduledExecutor();
            Runnable task6 = () -> System.out.println("The truck with id #" + id + " is near at the exit gate...");
            // Schedule task6 with a delay between 2 minutes 30 sec and 3 minutes 30 sec
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

            //Here I check if the User has entered different inbound and outbound lanes, then only that number
            //of trucks to go to the exit lane. If for example 3 truck is returning form the handling location, from the stack
            //and the gate has only 2 outbound lane than 1 truck needs to wait until 1 of 2 trucks passed the exit gate
            while (!TruckService.endGateChecker(this)) {
                truckLocation = TruckLocation.WAITING_FOR_FREE_PLACE_AT_THE_EXIT_GATE;
            }

            //If the truck passed checker that means the truck it doesn't need to wait anymore, and we can update the truckLocation
            //to AT_EXIT_GATE
            truckLocation = TruckLocation.AT_EXIT_GATE;


            // Create a new executor instance which is handle the time at the exit gate
            ScheduledExecutorService outBoundLaneExecutor = Executors.newSingleThreadScheduledExecutor();
            Runnable task5 = () -> System.out.println("The truck with id #" + id + " passed the exit gate");
            // Schedule task5 between 2 and 5 minutes
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

            //Here we remove the truck from the gate outbound lane
            TruckService.endGateRemover(this);

            //And finally we update the truckLocation with value PASSED_THE_EXIT_GATE
            truckLocation = TruckLocation.PASSED_THE_EXIT_GATE;

            //This truck has done his job :)
            //The thread will shut down
        }
    }
}
