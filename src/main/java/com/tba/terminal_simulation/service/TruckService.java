package com.tba.terminal_simulation.service;

import com.tba.terminal_simulation.controller.GateIsNotCreated;
import com.tba.terminal_simulation.model.Gate;
import com.tba.terminal_simulation.model.Truck;
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


    public static Gate getGate() {
        return gate;
    }

    //I created a list because I need to store somewhere all the trucks that the user instantiate
    //I didn't use a database because this is a small application and with a simple list I can store the trucks
    private static Queue<Truck> parkingPlace = new LinkedList<>();
    private static List<Truck> trucksThatArePassedTheStartGate = new LinkedList<>();
    private static Queue<Truck> trucksWaitingAtExit = new LinkedList<>();
    private static List<Truck> trucksAtHandlingLocations = new LinkedList<>();
    private static Gate gate;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean alreadyWorkingAtCleaningTheOutboundLane = false;
    private boolean havePlaceAtTheHandlingLocation = false;
    private boolean alreadyWorkingAtCleaningTheInboundLane = false;


    //Here is the method with which the User can instantiate a gate with handling locations and inbound and outbound lanes
    public ResponseEntity<Response> creatingAGate(int inBoundLanes, int outBoundLanes, int handlingLocations) {
        gate = new Gate(inBoundLanes, outBoundLanes, handlingLocations);
        Response response = new Response();
        response.setStatus("OK");
        response.setStatusMsg("You just created a gate successfully");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    public static boolean handlingLocationChecker() {
        return trucksAtHandlingLocations.size() == 0 || trucksAtHandlingLocations.size() < gate.getHandlingLocations();
    }

    public static void handlingLocationAdder(Truck truck) {
        if (trucksAtHandlingLocations.size() < gate.getHandlingLocations()) {
            trucksAtHandlingLocations.add(truck);
        }
    }

    public static void handlingLocationRemover(Truck truck) {
        trucksAtHandlingLocations.remove(truck);
    }

    public static boolean endGateChecker(Truck truck) {
        if (gate.getTrucksAtOutboundLanes().size() == 0) {
            addTruckToTheOutBoundLane(truck);
            return true;
        } else {
            checkIfHaveFreePlaceAtExitWithoutThreadLoop();
            if (gate.getTrucksAtOutboundLanes().size() == gate.getOutBoundLanes()) {
                return false;
            } else {
                addTruckToTheOutBoundLane(truck);
                return true;
            }
        }
    }

    public static void endGateRemover(Truck truck) {
        gate.getTrucksAtOutboundLanes().remove(truck);
    }

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

    //Here I made a method with which we can add a Truck to the lanes
    public Truck addTruckToTheInBoundLane(Truck truck) {
        //I created a new truck and I added in the constructor all the behaviours that a truck has
        //I created a truck with the "new" operator because I want to call the constructor and the
        // id to be incremented
        if(gate!=null) {
            Truck newTruckToBeAdded = new Truck(truck.getType());
            Thread truckIsReadyToGo = new Thread(newTruckToBeAdded);
            System.out.println("The truck with id #" + newTruckToBeAdded.getId() + " is at the parking area");
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
        }else throw new GateIsNotCreated("ERROR", "No gate has been created");
        //Here I return it
    }

    public static void addTruckToTheOutBoundLane(Truck threadTruck) {
        //I created a new truck and I added in the constructor all the behaviours that a truck has
        //I created a truck with the "new" operator because I want to call the constructor and the
        //id to be incremented
        if (gate.getTrucksAtOutboundLanes().size() == 0) {
            gate.getTrucksAtOutboundLanes().add(threadTruck);
        } else {
            trucksWaitingAtExit.add(threadTruck);
            if (checkIfHaveFreePlaceAtExitWithoutThreadLoop()) {
                //Here I add the truck to the List
                trucksWaitingAtExit.poll();
                gate.getTrucksAtOutboundLanes().add(threadTruck);
            }
        }
//      startCheckingFreePlacesAtEndGate();
    }

    private static boolean checkIfHaveFreePlaceAtExitWithoutThreadLoop() {
        boolean hasFreePlaces = false;

        if (gate.getTrucksAtInboundLanes().size() == 0 || gate.getInBoundLanes() != gate.getTrucksAtInboundLanes().size()) {
            return true;
        }
        Iterator<Truck> iterator = gate.getTrucksAtOutboundLanes().iterator();
        while (iterator.hasNext()) {
            Truck truck = iterator.next();
            if (truck.hasPassedTheExitGate()) {
                parkingPlace.add(truck);
                iterator.remove();
                hasFreePlaces = true;
            }
        }
        return hasFreePlaces;
    }

    public void startCheckingFreePlacesAtStartGate() {
        scheduler.scheduleAtFixedRate(new CheckerThreadForStartGate(this), 0, 3, TimeUnit.SECONDS); // run every minute
    }

    public void stopCheckingFreePlacesAtStartGate() {
        emptyTheInboundLane();
        scheduler.shutdown();
    }

//    public boolean checkIfHaveFreePlaceAtTheEndGate() {
//        boolean hasFreePlaces = false;
//        Iterator<Truck> iterator = gate.getTrucksAtOutboundLanes().iterator();
//        while (iterator.hasNext()) {
//            Truck truck = iterator.next();
//            if (truck.hasPassedTheExitGate()) {
//                iterator.remove();
//                lettingTheTrucksInTheQueueGo();
//                hasFreePlaces = true;
//            }
//        }
//        return hasFreePlaces;
//    }

//    private void lettingTheTrucksInTheQueueGo() {
//        if (!trucksWaitingAtExit.isEmpty()) {
//            Truck truck = trucksWaitingAtExit.poll();
//            gate.getTrucksAtOutboundLanes().add(truck);
//        }
//    }

    private void emptyTheOutboundLane() {
        if (alreadyWorkingAtCleaningTheOutboundLane && trucksWaitingAtExit.isEmpty()) {
            while (!gate.getTrucksAtOutboundLanes().isEmpty()) {
                checkTheLaneAtExitAndThenTheGate();
            }
            alreadyWorkingAtCleaningTheOutboundLane = true;
            return;
        }
        alreadyWorkingAtCleaningTheOutboundLane = false;
    }

    private void checkTheLaneAtExitAndThenTheGate() {

        for (int i = 0; i < gate.getTrucksAtOutboundLanes().size(); i++) {
            if (gate.getTrucksAtOutboundLanes().get(i).hasPassedTheExitGate()) {
                parkingPlace.add(gate.getTrucksAtOutboundLanes().remove(i));
            }
        }

    }

    private void emptyTheInboundLane() {
        if (alreadyWorkingAtCleaningTheInboundLane && parkingPlace.isEmpty()) {
            while (!gate.getTrucksAtInboundLanes().isEmpty()) {
                checkTheParkingSlotsAndThenTheGate();
            }
            alreadyWorkingAtCleaningTheInboundLane = true;
            return;
        }
        alreadyWorkingAtCleaningTheInboundLane = false;
    }


    public static boolean checkIfHaveFreePlaceWithoutThreadLoop() {

        boolean hasFreePlaces = false;
        if(gate != null){
        if (gate.getTrucksAtInboundLanes().size() == 0 || gate.getInBoundLanes() != gate.getTrucksAtInboundLanes().size()) {
            return true;
        }
        Iterator<Truck> iterator = gate.getTrucksAtInboundLanes().iterator();
        while (iterator.hasNext()) {
            Truck truck = iterator.next();
            if (truck.hasPassedTheStartGate()) {
                trucksThatArePassedTheStartGate.add(truck);
                iterator.remove();
                hasFreePlaces = true;
            }
        }} else throw new GateIsNotCreated("ERROR","No gate has been created");
        return hasFreePlaces;
    }

    public boolean checkIfHaveFreePlaceAtTheGate() throws InterruptedException {
        boolean hasFreePlaces = false;
        Iterator<Truck> iterator = gate.getTrucksAtInboundLanes().iterator();
        while (iterator.hasNext()) {
            Truck truck = iterator.next();
            if (truck.hasPassedTheStartGate()) {
                trucksThatArePassedTheStartGate.add(truck);
                iterator.remove();
                checkTheParkingPlace();
                hasFreePlaces = true;
            }
        }
        return hasFreePlaces;
    }

    private void checkTheParkingPlace() {
        if (!parkingPlace.isEmpty()) {
            Truck truck = parkingPlace.poll();
            Thread truckIsReadyToGo = new Thread(truck);
            gate.getTrucksAtInboundLanes().add(truck);
            truckIsReadyToGo.start();
        }
    }

    public void checkTheParkingSlotsAndThenTheGate() {
        for (int i = 0; i < gate.getTrucksAtInboundLanes().size(); i++) {
            if (gate.getTrucksAtInboundLanes().get(i).hasPassedTheStartGate()) {
                trucksThatArePassedTheStartGate.add(gate.getTrucksAtInboundLanes().remove(i));
            }
        }
    }


    //Here we just got how many trucks are at the gate
    public Map<TruckType, Integer> getAllTheTrucksFromTheQueue() {
        if (gate != null) {
            int trucksThatDeliver = 0;
            int trucksThatReceive = 0;
            List<Truck> allTheTrucks = new LinkedList<>();
            allTheTrucks.addAll(gate.getTrucksAtInboundLanes());
            allTheTrucks.addAll(gate.getTrucksAtOutboundLanes());
            for (Truck allTheTruck : allTheTrucks) {
                if (allTheTruck.getType() == TruckType.DELIVER) {
                    trucksThatDeliver++;
                } else if (allTheTruck.getType() == TruckType.RECEIVE) {
                    trucksThatReceive++;
                }
            }
            Map<TruckType, Integer> trucks = new HashMap<>();
            trucks.put(TruckType.DELIVER,trucksThatDeliver);
            trucks.put(TruckType.RECEIVE,trucksThatReceive);
            return trucks;
        }else return new HashMap<>();
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
        scheduler = Executors.newSingleThreadScheduledExecutor();
        gate=null;
        alreadyWorkingAtCleaningTheOutboundLane = false;
        alreadyWorkingAtCleaningTheInboundLane = false;
        havePlaceAtTheHandlingLocation = false;
    }

}