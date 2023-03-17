package com.tba.terminal_simulation.service;

import com.tba.terminal_simulation.controller.GateIsNotCreated;
import com.tba.terminal_simulation.model.Gate;
import com.tba.terminal_simulation.model.Truck;
import com.tba.terminal_simulation.model.TruckLocation;
import com.tba.terminal_simulation.model.TruckType;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Here is the Service class. This class is responsible for everything
//Here we can instantiate trucks, lanes.
@Service
@Getter
public class TruckService {


    //I created a lists because I need to store somewhere all the trucks at different place that the user instantiate
    //I didn't use a database because this is a small application and with simple lists I can store the trucks an anything
    private static Gate gate;
    private static Queue<Truck> parkingPlace = new LinkedList<>();
    private static List<Truck> trucksThatArePassedTheStartGate = new LinkedList<>();
    private static Queue<Truck> trucksWaitingAtExit = new LinkedList<>();
    private static List<Truck> trucksAtHandlingLocations = new LinkedList<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean alreadyWorkingAtCleaningTheOutboundLane = false;
    private boolean havePlaceAtTheHandlingLocation = false;
    private boolean alreadyWorkingAtCleaningTheInboundLane = false;

    public static Gate getGate() {
        return gate;
    }

    //Here is the method with which the User can instantiate a gate with handling locations and inbound and outbound lanes


    /**
     * The method check if the user didn't pass negative integers. If everything is fine, then the method
     * will create a gate with the input data
     *
     * @param inBoundLanes
     * @param outBoundLanes
     * @param handlingLocations
     * @return a nice response to the REST API
     */
    public ResponseEntity<Response> creatingAGate(int inBoundLanes, int outBoundLanes, int handlingLocations) {
        Response response = new Response();
        if (inBoundLanes > 0 && outBoundLanes > 0 && handlingLocations > 0) {
            gate = new Gate(inBoundLanes, outBoundLanes, handlingLocations);
            response.setStatus("OK");
            response.setStatusMsg("You just created a gate successfully");
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        } else {
            response.setStatus("ERROR");
            response.setStatusMsg("The gate can't be created with negative values");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    /**
     * @return true if is free place at handling location, false if the truck is needed to wait
     */
    public static boolean handlingLocationChecker() {

        return trucksAtHandlingLocations.size() == 0 || trucksAtHandlingLocations.size() < gate.getHandlingLocations();
    }


    /**
     * This method add a truck to the handling location list. If at the handling location didn't is any place than
     * not will add the truck to the list
     *
     * @param truck
     */
    public static void handlingLocationAdder(Truck truck) {
            trucksAtHandlingLocations.add(truck);
    }


    /**
     * The method remove the truck from the handling location list
     *
     * @param truck
     */
    public static void handlingLocationRemover(Truck truck) {
        trucksAtHandlingLocations.remove(truck);
    }


    /**
     * @param truck
     * @return true if the truck can be added to the outbound-lane, false if the truck
     * can't be added to the outbound-lane
     */
    public static boolean endGateChecker(Truck truck) {
        if (!trucksWaitingAtExit.contains(truck)) {
            trucksWaitingAtExit.add(truck);
        }
            if (checkIfHaveFreePlaceAtExitWithoutThreadLoop()) {
                 if(gate.getTrucksAtOutboundLanes().contains(truck) && !trucksWaitingAtExit.contains(truck)){
                     return true;
                 }
        }
        return false;
    }

    /**
     * This method removes the truck form the outbound_lane
     *
     * @param truck
     */
    public static void endGateRemover(Truck truck) {
        gate.getTrucksAtOutboundLanes().remove(truck);
    }

    /**
     * This method accept a List of trucks and everytime and call the addTruckToTheInBoundLane()
     * that many times as truck much as is in the List that is passed by the user
     *
     * @param truck
     * @return a nice response to the REST API
     */
    public ResponseEntity<Response> addTrucksToTheInBoundLane(List<Truck> truck) {
        int i = 0;
        while (i < truck.size()) {
            addTruckToTheInBoundLane(truck.get(i));
            i++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Response response = new Response();
        response.setStatus("OK");
        response.setStatusMsg("All truck are sent successfully");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * The truck is added to the Parking place and then if it gate has free place at the inBoundLane then
     * added to the Inbound Lane and then start the thread
     *
     * @param truck
     * @return The truck that is added to the Parking place
     */
    //Here I made a method with which we can add a Truck to the lanes
    public Truck addTruckToTheInBoundLane(Truck truck) {
        //I created a new truck and I added in the constructor all the behaviours that a truck has
        //I created a truck with the "new" operator because I want to call the constructor and the
        // id to be incremented
        if (gate != null) {
            Truck newTruckToBeAdded = new Truck(truck.getType());
            Thread truckIsReadyToGo = new Thread(newTruckToBeAdded);
            System.out.println("The truck with id #" + newTruckToBeAdded.getId() + " is at the parking area");
            newTruckToBeAdded.setTruckLocation(TruckLocation.PARKING_PLACE);
            parkingPlace.add(newTruckToBeAdded);
            if (checkIfHaveFreePlaceWithoutThreadLoop()) {
                //Here I add the truck to the List
                parkingPlace.poll();
                gate.getTrucksAtInboundLanes().add(newTruckToBeAdded);
                truckIsReadyToGo.start();
                return newTruckToBeAdded;
            }
            startCheckingFreePlacesAtStartGate();
            return newTruckToBeAdded;
        } else throw new GateIsNotCreated("ERROR", "No gate has been created");
        //Here I return it
    }

    /**
     * \
     * The truck is added
     *
     * @param threadTruck
     */
    public static void addTruckToTheOutBoundLane(Truck threadTruck) {
        //I created a new truck and I added in the constructor all the behaviours that a truck has
        //I created a truck with the "new" operator because I want to call the constructor and the
        //id to be incremented
        trucksWaitingAtExit.add(threadTruck);
        if (gate.getTrucksAtOutboundLanes().size() < gate.getOutBoundLanes()) {
            gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
        } else {
            if (checkIfHaveFreePlaceAtExitWithoutThreadLoop()) {
                trucksWaitingAtExit.poll();
                gate.getTrucksAtOutboundLanes().add(threadTruck);
            }
        }
    }


    /**
     * The method checks if the gate has free outbound lane but in the same time removes all the trucks that are in it
     * the location is no more AT_EXIT_GATE. The method checks only one time without a loop.
     *
     * @return True if the exit gate has free outbound lane. False if all the outbound lane is occupied
     */
    private static boolean checkIfHaveFreePlaceAtExitWithoutThreadLoop() {

        boolean hasFreePlaces = false;

        Iterator<Truck> iterator = gate.getTrucksAtOutboundLanes().iterator();
        while (iterator.hasNext()) {
            Truck truck = iterator.next();
            if (truck.getTruckLocation() != TruckLocation.AT_EXIT_GATE) {
                if (trucksWaitingAtExit.size() == 1 && (gate.getTrucksAtOutboundLanes().size() == 0 || gate.getOutBoundLanes() != gate.getTrucksAtOutboundLanes().size())) {
                    gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
                    hasFreePlaces = true;
                } else {
                    iterator.remove();
                    gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
                    trucksThatArePassedTheStartGate.remove(truck);
                }
            }
        }
        return hasFreePlaces;
    }


    /**
     * The method starts an executor for searching free places at the start gate. If it founds a truck which is the location
     * is not at the start gate, it will remove from the inbound-lane of the Gate
     */
    public void startCheckingFreePlacesAtStartGate() {
        scheduler.scheduleAtFixedRate(new CheckerThreadForStartGate(this), 0, 3, TimeUnit.SECONDS); // run every minute
    }


    /**
     * The method shutdown the executor that is searching for free places at the start gate. But first checks
     * in a loop the inbound lanes. With just one executor service, because the variable alreadyWorkingAtCleaningTheInboundLane
     * ensures that only one executor-service cleaning the inbound-lane. If executor service A goes to the emptyTheInboundLane() but
     * another executor service with name B is there than A comes back to this method and shutdown
     */
    public void stopCheckingFreePlacesAtStartGate() {
        emptyTheInboundLane();
        scheduler.shutdown();
    }


    /**
     * Allows only one executor service to execute this method. Cleans up the inbound-lane, in form that if
     * a truck location is not PARKING_PLACE and AT_START_GATE than remove from the gate trucksAtInboundLane list
     * ITT MÉG KELL NÉZNI HOGY BIZTOS UGY MÜKÖDIK AHOGY GONDOLOM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private void emptyTheInboundLane() {
        if (!alreadyWorkingAtCleaningTheInboundLane && parkingPlace.isEmpty()) {
            while (!gate.getTrucksAtInboundLanes().isEmpty()) {
                checkTheParkingSlotsAndThenTheGate();
            }
            alreadyWorkingAtCleaningTheInboundLane = false;
            return;
        }
        alreadyWorkingAtCleaningTheInboundLane = true;
    }


    /**
     * Here is the upper method continue method. If the truck location is not PARKING_PLACE or AT_THE_START_GATE
     * then remove from the gate Inbound lanes and added to the list trucksThatArePassedTheStartGate
     */
    private void checkTheParkingSlotsAndThenTheGate() {
        for (int i = 0; i < gate.getTrucksAtInboundLanes().size(); i++) {
            if (gate.getTrucksAtInboundLanes().get(i).getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
                trucksThatArePassedTheStartGate.add(gate.getTrucksAtInboundLanes().remove(i));
            }
        }
    }

    /**
     * @return check if the gate has free place at Inbound-lane and at the same time removes all the trucks from gate InBound-lane
     * that location is not equals with AT_THE_START_GATE and it is in the gate.getTrucksAtInboundLanes()
     */
    public static boolean checkIfHaveFreePlaceWithoutThreadLoop() {

        boolean hasFreePlaces = false;

        if (gate != null) {
            if (gate.getTrucksAtInboundLanes().size() == 0 || gate.getInBoundLanes() != gate.getTrucksAtInboundLanes().size()) {
                return true;
            }
            Iterator<Truck> iterator = gate.getTrucksAtInboundLanes().iterator();
            while (iterator.hasNext()) {
                Truck truck = iterator.next();
                if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
                    trucksThatArePassedTheStartGate.add(truck);
                    iterator.remove();
                    hasFreePlaces = true;
                }
            }
        } else throw new GateIsNotCreated("ERROR", "No gate has been created");
        return hasFreePlaces;
    }

    /**
     * This method is only executed by executor services which are searching for free places
     * at start gate
     *
     * @return true if the start gate has free place at the inbound lanes, false if
     * the start gate didn't ha any free place
     */
    public boolean checkIfHaveFreePlaceAtTheGate() {
        boolean hasFreePlaces = false;
        Iterator<Truck> iterator = gate.getTrucksAtInboundLanes().iterator();
        while (iterator.hasNext()) {
            Truck truck = iterator.next();
            if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
                trucksThatArePassedTheStartGate.add(truck);
                iterator.remove();
                checkTheParkingPlace();
                hasFreePlaces = true;
            }
        }
        return hasFreePlaces;
    }


    /**
     * If the parking place is not empty then send a truck for the parking place
     */
    private void checkTheParkingPlace() {
        if (!parkingPlace.isEmpty()) {
            Truck truck = parkingPlace.poll();
            Thread truckIsReadyToGo = new Thread(truck);
            gate.getTrucksAtInboundLanes().add(truck);
            truckIsReadyToGo.start();
        }
    }


    //Here we just got how many trucks are at the gate

    /**
     * @return map with two key values, trucks that delivers and trucks that receive, and there values, how many are
     * per delivery and per receive.
     */
    public Map<TruckType, Integer> getAllTheTrucksFromTheInboundLanes() {
        if (gate != null) {
            int trucksThatDeliver = 0;
            int trucksThatReceive = 0;
            checkTheParkingSlotsAndThenTheGate();
            for (Truck truck : gate.getTrucksAtInboundLanes()) {
                if (truck.getType() == TruckType.DELIVER) {
                    trucksThatDeliver++;
                } else if (truck.getType() == TruckType.RECEIVE) {
                    trucksThatReceive++;
                }
            }
            Map<TruckType, Integer> trucks = new HashMap<>();
            trucks.put(TruckType.DELIVER, trucksThatDeliver);
            trucks.put(TruckType.RECEIVE, trucksThatReceive);
            return trucks;
        } else return new HashMap<>();
    }

    /**
     * @return Map with the trucks id-s and there location
     */
    public Map<Long, TruckLocation> getAllTheTruckLocation() {
        Map<Long, TruckLocation> result = new HashMap<>();
        List<Truck> allTheTrucks = new LinkedList<>();
        allTheTrucks.addAll(gate.getTrucksAtInboundLanes());
        allTheTrucks.addAll(gate.getTrucksAtOutboundLanes());
        allTheTrucks.addAll(parkingPlace);
        allTheTrucks.addAll(trucksThatArePassedTheStartGate);
        allTheTrucks.addAll(trucksAtHandlingLocations);
        allTheTrucks.addAll(trucksWaitingAtExit);
        int i = 0;
        while (i < allTheTrucks.size()) {
            result.put(allTheTrucks.get(i).getId(), allTheTrucks.get(i).getTruckLocation());
            i++;
        }
        return result;
    }


    //A new functionality. With this method the user can restart the simulation with default values
    public ResponseEntity<Response> restartTheSimulation() {

        //Here we instantiate the response class, which is class and with it, we can send much better REST APIs
        Response response = new Response();

        //This is just an empty truck. I created only to modify the static variable, the nextId variable,
        //because if I modify the in one object all the other object will see that modification
        Truck tempTruck = new Truck(null);

        //Here I empty the List by instantiating the list with a new LinkedList
        //trucks = new LinkedList<>();


        Field field;

        String fieldName = "nextId";

        //Here I manipulate the nextId field with reflection because in different way I can't.
        //The class didn't offer us public field,and we don't want to make setters for the nextId field
        //because we don't want to let other developers to manipulate, the nextId field with no reason
        try {
            field = Truck.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("You have a problem in the application on the reflection part." +
                    " In the truck class you don't have a field with name: " + fieldName);
        }

        //With this we transform the private field to be public for a little time as long as we change his value
        field.setAccessible(true);

        //Here we change the value of the nextId value to be 1. We change to 1 because if we restart the simulation
        //every value we want to be the default value. In our situation if we set the nextId field to 1 in the next step when
        //the user instantiating a truck his id will be one, the next one is 2 etc.
        try {
            field.set(tempTruck, 1L);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("You don't have access to modify " + fieldName + " field.");
        }

        //I reset all the fields to the default
        resetFields();

        //Here we create a great json to the front end. The result will be
        //        {
        //           "status": "OK",
        //           "statusMsg": "Simulation restarted successfully"
        //        }
        response.setStatus("OK");
        response.setStatusMsg("Simulation restarted successfully");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private void resetFields() {
        parkingPlace = new LinkedList<>();
        trucksThatArePassedTheStartGate = new LinkedList<>();
        trucksWaitingAtExit = new LinkedList<>();
        trucksAtHandlingLocations = new LinkedList<>();
        scheduler.shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        gate = null;
        alreadyWorkingAtCleaningTheOutboundLane = false;
        alreadyWorkingAtCleaningTheInboundLane = false;
        havePlaceAtTheHandlingLocation = false;
    }

}