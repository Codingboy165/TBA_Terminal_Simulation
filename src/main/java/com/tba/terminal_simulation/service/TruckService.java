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
    private static final Queue<Truck> parkingPlace = new LinkedList<>();
    private static final Queue<Truck> trucksWaitingAtExit = new LinkedList<>();
    private static final Queue<Truck> trucksWaitingForFreePlaceAtHandlingLocation = new LinkedList<>();
    private static final CopyOnWriteArrayList<Truck> trucksAtHandlingLocations = new CopyOnWriteArrayList<>();
    private final List<Truck> allTheTruckLocation = new LinkedList<>();

    /**
     * The method check if the user didn't pass negative integers. If everything is fine, then the method
     * will create a gate with the input data
     * @param inBoundLanes the number of inbound lanes
     * @param outBoundLanes the number of outbound lanes
     * @param handlingLocations the number of handling location
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
     * @param trucks a list with trucks
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
     * This method ensures that the truck is added to the Parking place. The method which is annotated with @Scheduled
     * that method will ensure that the truck will start from the parking place
     * @param truck a truck object
     */
    public void addTruckToTheInBoundLane(Truck truck) {
        if (gate != null) {
            Truck newTruckToBeAdded = new Truck(truck.getType());
            System.out.println("The truck with id #" + newTruckToBeAdded.getId() + " is at the parking area");
            newTruckToBeAdded.setTruckLocation(TruckLocation.PARKING_PLACE);
            allTheTruckLocation.add(newTruckToBeAdded);
            parkingPlace.add(newTruckToBeAdded);
        } else throw new GateIsNotCreated("ERROR", "No gate has been created");
    }


    /**
     * This method will add the truck to the handling location waiting list which is a queue. If the queue head is the truck
     * that we passed as param than removes the queue head, and we add to the handling location list. Finally, the method returns
     * true if all these things happens, and let the truck leave this method and continue his tasks
     * @param truck a truck object
     * @return true if is free place at handling location, false if the truck is needed to wait
     */
    public synchronized static boolean handlingLocationChecker(Truck truck) {
        if (!trucksWaitingForFreePlaceAtHandlingLocation.contains(truck)) {
            trucksWaitingForFreePlaceAtHandlingLocation.add(truck);
        }
        int handlingLocations = (gate != null) ? gate.getHandlingLocations() : 0;
        if (trucksAtHandlingLocations.size() < handlingLocations) {
            if(trucksWaitingForFreePlaceAtHandlingLocation.peek() == truck){
            trucksAtHandlingLocations.add(trucksWaitingForFreePlaceAtHandlingLocation.poll());
            return true;
            }
        }
        return false;
    }

    /**
     * The method remove the truck from the handling location list
     * @param truck a truck object
     */
    public static void handlingLocationRemover(Truck truck) {
        trucksAtHandlingLocations.remove(truck);
    }


    /**
     * First of all clears the outbound lane if the trucks on it didn't have location AT_EXIT_GATE and then,
     * the method it will add the truck to the waiting list, which is a queue, if it doesn't in it. Then checks if
     * the queue head is the truck that we passed as param, if it is than removes the queue head, and it adds to the
     * gates outbound lane. Finally, the method returns true if all these things happens, and let the truck leave this
     * method and continue his tasks
     * @param truck a truck object
     * @return true if the truck can be added to the outbound-lane, false if the truck
     * can't be added to the outbound-lane
     */
    public static boolean endGateChecker(Truck truck) {
        if (gate != null) {
            clearTheOutBoundLane();
            if (!trucksWaitingAtExit.contains(truck)) {
                trucksWaitingAtExit.add(truck);
            }
            int outboundLanes = gate.getOutBoundLanes();
            if (gate.getTrucksAtOutboundLanes().size() < outboundLanes) {
                if (trucksWaitingAtExit.peek() == truck) {
                    gate.getTrucksAtOutboundLanes().add(trucksWaitingAtExit.poll());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method removes the truck form the gate outbound_lane
     * @param truck a truck object
     */
    public synchronized static void endGateRemover(Truck truck) {
        gate.getTrucksAtOutboundLanes().remove(truck);
    }


    /**
     * The method checks if the gate has free outbound lane but in the same time removes all the trucks that are in it
     * the location is no more AT_EXIT_GATE. The method checks only one time without a loop.
     */
    private static void clearTheOutBoundLane() {
        synchronized (gate.getTrucksAtOutboundLanes()) {
            List<Truck> trucks = new ArrayList<>(gate.getTrucksAtOutboundLanes());
            trucks.removeIf(truck -> truck.getTruckLocation() != TruckLocation.AT_EXIT_GATE);
                gate.getTrucksAtOutboundLanes().clear();
                gate.getTrucksAtOutboundLanes().addAll(trucks);
            }
        }

    /**
     * This method will run in every 100 milliseconds, and it will start the trucks from the parking place
     * if the gate has free place, and in the same time clear the inbound lanes if the truck in it doesn't have location
     * AT_START_GATE
     */
    @Scheduled(fixedDelay = 100)
    public void startCheckingFreePlacesAtStartGate() {
        checkIfHaveFreePlaceAtTheGate();
    }

    /**
     * This method is only executed by the method which is annotated with @Scheduled
     */
    public synchronized void checkIfHaveFreePlaceAtTheGate() {
        if (gate != null) {
            synchronized (gate.getTrucksAtInboundLanes()) {
                List<Truck> trucks = new ArrayList<>(gate.getTrucksAtInboundLanes());
                if (trucks.size() < gate.getInBoundLanes()) {
                    checkTheParkingPlace();
                } else {
                    trucks.removeIf(truck -> truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE);
                    gate.getTrucksAtInboundLanes().clear();
                    gate.getTrucksAtInboundLanes().addAll(trucks);
                }
            }
        }
    }


    /**
     * If the parking place is not empty then send a truck for the parking place, else
     * if it is empty, than try to empty the inbound lane if it is truck on it, and doesn't
     * have location at AT_START_GATE
     */
    private void checkTheParkingPlace() {
        if (!parkingPlace.isEmpty()) {
            Truck truck = parkingPlace.poll();
            Thread truckIsReadyToGo = new Thread(truck);
            gate.getTrucksAtInboundLanes().add(truck);
            truckIsReadyToGo.start();
        } else {
            gate.getTrucksAtInboundLanes().removeIf(truck ->
                    truck.getTruckLocation() != TruckLocation.AT_THE_START_GATE);
        }
    }


    /**
     * This method return all the trucks (the number, how many) which are at start gate grouped by truck type
     * @return map with two key values, trucks that delivers and trucks that receive, and there values, how many are
     * per delivery and per receive.
     */
    public Map<TruckType, Long> getAllTheTrucksFromTheInboundLanes() {
        if (gate != null) {
            Map<TruckType, Long> trucks = new HashMap<>();
            long trucksThatDeliver = allTheTruckLocation.stream().filter(truck -> truck.getTruckLocation() == TruckLocation.AT_THE_START_GATE).filter(truck -> truck.getType() == TruckType.DELIVER).count();
            long trucksThatReceive = allTheTruckLocation.stream().filter(truck -> truck.getTruckLocation() == TruckLocation.AT_THE_START_GATE).filter(truck -> truck.getType() == TruckType.RECEIVE).count();
            trucks.put(TruckType.DELIVER, trucksThatDeliver);
            trucks.put(TruckType.RECEIVE, trucksThatReceive);
            return trucks;
        } else return new HashMap<>();
    }

    /**
     * @return a list with all the trucks. In front end I pick it out the trucks id and the locations
     */
    public List<Truck> getAllTheTruckLocation() {
        return allTheTruckLocation;
    }

}