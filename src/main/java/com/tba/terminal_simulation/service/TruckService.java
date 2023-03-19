package com.tba.terminal_simulation.service;

import com.tba.terminal_simulation.controller.GateIsNotCreated;
import com.tba.terminal_simulation.model.Gate;
import com.tba.terminal_simulation.model.Truck;
import com.tba.terminal_simulation.model.TruckLocation;
import com.tba.terminal_simulation.model.TruckType;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

//Here is the Service class. This class is responsible for everything
//Here we can instantiate trucks, lanes.
@Service
@Getter
public class TruckService {


    //I created a lists because I need to store somewhere all the trucks at different place that the user instantiate
    //I didn't use a database because this is a small application and with simple lists I can store the trucks an anything
    private static Gate gate;
    private static Object lock = new Object();
    private static Queue<Truck> parkingPlace = new LinkedList<>();
    //    private static List<Truck> trucksThatArePassedTheStartGate = new LinkedList<>();
    private static Queue<Truck> trucksWaitingAtExit = new LinkedList<>();
    private static Queue<Truck> trucksWaitingForFreePlaceAtHandlingLocation = new LinkedList<>();
    private static CopyOnWriteArrayList<Truck> trucksAtHandlingLocations = new CopyOnWriteArrayList<>();
    //    private static Queue<Truck> trucksThatAreFinishedHerWork = new LinkedList<>();
//    private boolean havePlaceAtTheHandlingLocation = false;
    private List<Truck> allTheTruckLocation = new LinkedList<>();

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
     * This method accept a List of trucks and everytime and call the addTruckToTheInBoundLane()
     * that many times as truck much as is in the List that is passed by the user
     *
     * @param trucks
     * @return a nice response to the REST API
     */
    public ResponseEntity<Response> addTrucksToTheInBoundLane(List<Truck> trucks) {
        int i = 0;

        while (i < trucks.size()) {
            Truck truck = trucks.get(i);
            addTruckToTheInBoundLane(truck);
            i++;
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
    public void addTruckToTheInBoundLane(Truck truck) {
        //I created a new truck and I added in the constructor all the behaviours that a truck has
        //I created a truck with the "new" operator because I want to call the constructor and the
        // id to be incremented

        if (gate != null) {
            Truck newTruckToBeAdded = new Truck(truck.getType());
            System.out.println("The truck with id #" + newTruckToBeAdded.getId() + " is at the parking area");
            newTruckToBeAdded.setTruckLocation(TruckLocation.PARKING_PLACE);
            allTheTruckLocation.add(newTruckToBeAdded);
            parkingPlace.add(newTruckToBeAdded);
        } else throw new GateIsNotCreated("ERROR", "No gate has been created");
        //Here I return it
    }


    /**
     * @return true if is free place at handling location, false if the truck is needed to wait
     */
    public synchronized static boolean handlingLocationChecker(Truck truck) {
        if (!trucksWaitingForFreePlaceAtHandlingLocation.contains(truck)) {
            trucksWaitingForFreePlaceAtHandlingLocation.add(truck);
        }
        if (trucksAtHandlingLocations != null) {
            int handlingLocations = (gate != null) ? gate.getHandlingLocations() : 0;
            if (trucksAtHandlingLocations.size() < handlingLocations) {
                if(trucksWaitingForFreePlaceAtHandlingLocation.peek() == truck){
                trucksAtHandlingLocations.add(trucksWaitingForFreePlaceAtHandlingLocation.poll());
                return true;
                }
            }
        }
        return false;
    }


//    /**
//     * This method add a truck to the handling location list. If at the handling location didn't is any place than
//     * not will add the truck to the list
//     *
//     * @param truck
//     */
//    public synchronized static void handlingLocationAdder(Truck truck) {
//        trucksAtHandlingLocations.add(truck);
//    }


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

        if (gate != null) {
            clearTheOutBoundLane();
            if (!trucksWaitingAtExit.contains(truck)) {
                trucksWaitingAtExit.add(truck);
            }
            if (trucksWaitingAtExit != null) {
                int outboundLanes = (gate != null) ? gate.getOutBoundLanes() : 0;
                if (gate != null && gate.getTrucksAtOutboundLanes().size() < outboundLanes) {
                    if (trucksWaitingAtExit.peek() == truck) {
                        gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This method removes the truck form the outbound_lane
     *
     * @param truck
     */
    public synchronized static void endGateRemover(Truck truck) {
//        trucksThatArePassedTheStartGate
//        trucksThatAreFinishedHerWork.add(truck);
        gate.getTrucksAtOutboundLanes().remove(truck);
    }


    /**
     * The method checks if the gate has free outbound lane but in the same time removes all the trucks that are in it
     * the location is no more AT_EXIT_GATE. The method checks only one time without a loop.
     */
    private static void clearTheOutBoundLane() {


        synchronized (gate.getTrucksAtOutboundLanes()) {
            List<Truck> trucks = new ArrayList<>(gate.getTrucksAtOutboundLanes());
                Iterator<Truck> truckIterator = trucks.iterator();
                while (truckIterator.hasNext()) {
                    Truck truck = truckIterator.next();
                    if (truck.getTruckLocation() != TruckLocation.AT_EXIT_GATE) {
//                            trucksThatArePassedTheStartGate.add(truck);
                        truckIterator.remove();
                    }
                }
                gate.getTrucksAtOutboundLanes().clear();
                gate.getTrucksAtOutboundLanes().addAll(trucks);
            }
        }



////        boolean hasFreePlaces = false;
//        synchronized (gate.getTrucksAtOutboundLanes()) {
////            if (gate.getOutBoundLanes() != gate.getTrucksAtOutboundLanes().size()) {
////                gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
////                return true;
////            }
//            Iterator<Truck> iterator = gate.getTrucksAtOutboundLanes().iterator();
//            while (iterator.hasNext()) {
//                try {
//                    Truck truck = iterator.next();
//                    if (truck.getTruckLocation() != TruckLocation.AT_EXIT_GATE) {
////                        trucksThatAreFinishedHerWork.add(truck);
//                        iterator.remove();
////                        gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
////                        trucksThatArePassedTheStartGate.remove(truck);
////                        hasFreePlaces = true;
//                    }
////                    } else if (gate.getOutBoundLanes() != gate.getTrucksAtOutboundLanes().size()) {
////                        gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
////                        hasFreePlaces = true;
////                    }
//                } catch (ConcurrentModificationException e) {
//                    break;
//                }
//            }
////
//        }
//    }

    /**
     * The method starts an executor for searching free places at the start gate. If it founds a truck which is the location
     * is not at the start gate, it will remove from the inbound-lane of the Gate
     */
    @Scheduled(fixedDelay = 200)
    public void startCheckingFreePlacesAtStartGate() {
        checkIfHaveFreePlaceAtTheGate();
    }

//    /**
//     * Allows only one executor service to execute this method. Cleans up the inbound-lane, in form that if
//     * a truck location is not PARKING_PLACE and AT_START_GATE than remove from the gate trucksAtInboundLane list
//     * ITT MÉG KELL NÉZNI HOGY BIZTOS UGY MÜKÖDIK AHOGY GONDOLOM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//     */
//    private void emptyTheInboundLane() {
//        if (gate != null) {
//            if (parkingPlace.isEmpty()) {
//                checkTheParkingSlotsAndThenTheGate();
//            }
//        }
//    }

    /**
     * Here is the upper method continue method. If the truck location is not PARKING_PLACE or AT_THE_START_GATE
     * then remove from the gate Inbound lanes and added to the list trucksThatArePassedTheStartGate
     */
    private void checkTheParkingSlotsAndThenTheGate() {
        if(parkingPlace.isEmpty()) {
            gate.getTrucksAtInboundLanes().removeIf(truck -> {
                if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
//                trucksThatArePassedTheStartGate.add(truck);
                    return true; // Remove the element from the list
                } else {
                    return false; // Keep the element in the list
                }
            });
        }
    }


    /**
     * @return check if the gate has free place at Inbound-lane and at the same time removes all the trucks from gate InBound-lane
     * that location is not equals with AT_THE_START_GATE and it is in the gate.getTrucksAtInboundLanes()
     */

    public static boolean checkIfHaveFreePlaceWithoutThreadLoop() {
        synchronized (lock) {
            boolean hasFreePlaces = false;

            if (gate != null) {
                if (gate.getInBoundLanes() != gate.getTrucksAtInboundLanes().size()) {
                    return true;
                }
                List<Truck> trucksAtInboundLanes = new ArrayList<>(gate.getTrucksAtInboundLanes());
                Iterator<Truck> iterator = trucksAtInboundLanes.iterator();
                while (iterator.hasNext()) {
                    Truck truck = iterator.next();
                    if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
//                        trucksThatArePassedTheStartGate.add(truck);
                        iterator.remove();
                        hasFreePlaces = true;
                    }
                }
                gate.getTrucksAtInboundLanes().clear();
                gate.getTrucksAtInboundLanes().addAll(trucksAtInboundLanes);
            } else throw new GateIsNotCreated("ERROR", "No gate has been created");
            return hasFreePlaces;
        }
    }


    /**
     * This method is only executed by executor services which are searching for free places
     * at start gate
     *
     * @return true if the start gate has free place at the inbound lanes, false if
     * the start gate didn't ha any free place
     */
    public synchronized void checkIfHaveFreePlaceAtTheGate() {
        if (gate != null) {
            synchronized (gate.getTrucksAtInboundLanes()) {
                List<Truck> trucks = new ArrayList<>(gate.getTrucksAtInboundLanes());
                if (trucks.size() < gate.getInBoundLanes()) {
                    checkTheParkingPlace();
                } else {
                    Iterator<Truck> truckIterator = trucks.iterator();
                    while (truckIterator.hasNext()) {
                        Truck truck = truckIterator.next();
                        if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
//                            trucksThatArePassedTheStartGate.add(truck);
                            truckIterator.remove();
//                            checkTheParkingPlace();
                        }
                    }
                    gate.getTrucksAtInboundLanes().clear();
                    gate.getTrucksAtInboundLanes().addAll(trucks);
                }
            }
        }
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
        } else {
            gate.getTrucksAtInboundLanes().removeIf(truck -> {
                if (truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE) {
//                    trucksThatArePassedTheStartGate.add(truck);
                    return true; // Remove the element from the list
                } else {
                    return false; // Keep the element in the list
                }
            });
        }
    }


    /**
     * @return map with two key values, trucks that delivers and trucks that receive, and there values, how many are
     * per delivery and per receive.
     */
    public Map<TruckType, Long> getAllTheTrucksFromTheInboundLanes() {
        if (gate != null) {
            Map<TruckType, Long> trucks = new HashMap<>();
//            checkTheParkingSlotsAndThenTheGate();
            long trucksThatDeliver = allTheTruckLocation.stream().filter(truck -> truck.getTruckLocation() == TruckLocation.AT_THE_START_GATE).filter(truck -> truck.getType() == TruckType.DELIVER).count();
            long trucksThatReceive = allTheTruckLocation.stream().filter(truck -> truck.getTruckLocation() == TruckLocation.AT_THE_START_GATE).filter(truck -> truck.getType() == TruckType.RECEIVE).count();
            trucks.put(TruckType.DELIVER, trucksThatDeliver);
            trucks.put(TruckType.RECEIVE, trucksThatReceive);
            return trucks;
        } else return new HashMap<>();
    }

    /**
     * @return Map with the trucks id-s and there location
     */
    public List<Truck> getAllTheTruckLocation() {
        return allTheTruckLocation;
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
        gate = null;
        lock = new Object();
        parkingPlace = new LinkedList<>();
//        trucksThatArePassedTheStartGate = new LinkedList<>();
        trucksWaitingAtExit = new LinkedList<>();
        trucksAtHandlingLocations = new CopyOnWriteArrayList<>();
//        trucksThatAreFinishedHerWork = new LinkedList<>();
//        havePlaceAtTheHandlingLocation = false;
        allTheTruckLocation = new LinkedList<>();
    }

}