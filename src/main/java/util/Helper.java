package util;

import mpi.MPI;
import project.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static project.Config.*;
import static project.Config.POSX_INIT_BOUND;

public class Helper {

    public static Ball selectRandom(ArrayList<Ball> population, Random r) {
        return population.get(r.nextInt(population.size())); //nextInt is upper exclusive
    }//selectRandom


    public static void createAndShowGUI(GUI panel) {
        JFrame frame = new JFrame("Genetic Golf Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(GUIWidth, GUIHeight);
        frame.setVisible(true);
    }

    /**
     * Returns a populated ArrayList with random objects of the type Ball, takes random object to ensure deterministic result.
    */
    public static ArrayList<Ball> generatePopulation(Random r){
        ArrayList<Ball> pop = new ArrayList<>();
        for (int i = 0; i < POPSIZE; i++) {
            double angle = r.nextDouble(ANGLE_BOUND);
            double velocity = r.nextDouble(VELOCITY_BOUND);
            double posX = r.nextDouble(POSX_INIT_BOUND);
            pop.add(new Ball(posX, 0, velocity, angle));
        }
        return pop;
    }//GeneratePopulationMethod


    public static void printPopulation(ArrayList<Ball> pop){
        Logger.log("\n\n\n\n\nPOPULATION:");
        for (int i = 0; i < pop.size(); i++) {
            Logger.log("\t ("+i+")" + pop.get(i).getFitness(), LogLevel.Status);
        }
        Logger.log("\n\n\n\n\nPOPULATION:");
    }
    public static void printPopulation(ArrayList<Ball> pop, int numberOfEleToPrint) throws Exception {
        System.out.println("\n\n\n\n\nPOPULATION:");

        if (numberOfEleToPrint > pop.size()) {
            Logger.log("number of elements in array printing cannot be more than population size", LogLevel.Warn);
            numberOfEleToPrint = pop.size();
        }
        //Print from behind if negative
        if (numberOfEleToPrint < 0 ){
            int start = Math.max(pop.size() + numberOfEleToPrint, 0);
            for (int i = start; i < pop.size(); i++) {
                Logger.log("\t ("+i+")" + pop.get(i).getFitness(), LogLevel.Status);
            }
        }else {
        for (int i = 0; i < numberOfEleToPrint; i++) {
            Logger.log("\t ("+i+")" + pop.get(i).getFitness(), LogLevel.Status);
        }
        }
        System.out.println("\n\n\n\n\nPOPULATION:");
    }
    public static void crossover(ArrayList<Ball> population, ArrayList<Ball> newPop) {
        for (int i = 0; i < POPSIZE-BEST_POP_TO_GET; i++) {
            Random r = new Random(SEED + i);
            Ball b = population.get(i);
            if (r.nextDouble() < CROSSOVER_RATE) {
                newPop.add(b.crossover(Helper.selectRandom(population, r), r));
            }else{
                newPop.add(b);
            }
        }
    }
    public static void mutate (ArrayList<Ball> newPop, int generation) {
        Random r = new Random(SEED + generation);
        for (int i = 0; i < newPop.size(); i++) {
            Ball b = newPop.get(i);
            double tempDouble = r.nextDouble();
            if (tempDouble < MUTATION_RATE) {
                b.mutate(tempDouble * 10, r);
            }
        }
    }
    public static void multiGenetic(int indexCut, ExecutorService THREADPOOL, ArrayList<Ball> population, ArrayList<Ball> newPop, GeneticFunction funcType, int generation) throws ExecutionException, InterruptedException {
        if (funcType == GeneticFunction.Crossover){
            ArrayList<MultiThreadedCrossover> tasks = new ArrayList<>(THREADS-1);
            //Splitting the work to Threads
            for (int k = 0; k < THREADS; k++){
                int indexStart = k* indexCut;
                int indexEnd = (indexCut*k+indexCut != population.size()-BEST_POP_TO_GET && k == THREADS-1) ? population.size()-BEST_POP_TO_GET : k * indexCut + indexCut;
                tasks.add(new MultiThreadedCrossover(indexStart, indexEnd, population));
            }
            //Running all Threads and waiting to finish
            List<Future<List<Ball>>> futures = THREADPOOL.invokeAll(tasks);
            //Joining the results into the newPop array
            for (Future<List<Ball>> future : futures) {
                newPop.addAll(future.get());
            }
        }else{
            for (int j = 0; j < THREADS; j++) {
                int indexStart = j* indexCut;
                int indexEnd = (indexCut*j+indexCut != population.size() && j == THREADS-1) ? population.size() : j * indexCut + indexCut;
                switch (funcType){
                    case Mutation -> THREADPOOL.submit(new MultiThreadedMutation(indexStart, indexEnd, population, generation));
                    case Fitness -> THREADPOOL.submit(new MultiThreadedFitness(indexStart, indexEnd, population));
                }
            }
        }
    }

    /**
     *Calculates the displacements and offsets within the function the splits the scatteringArray into chunk and sends it to the collectionArray
     */
    public enum MPIOPERATION {
        SCATTER,
        GATHER
    }

    public static void MPI_POPULATION_GENERIC (MPIOPERATION type, int totalSizeToScatter, Object[] sendArray, Object[] destArray, int ROOT){
        int me = MPI.COMM_WORLD.Rank();
        int nodeCounts = MPI.COMM_WORLD.Size();

        int baseChunk = totalSizeToScatter / nodeCounts;
        int remainder = totalSizeToScatter % nodeCounts;

        int[] receiveCounts = new int[nodeCounts];
        int[] displacements = new int[nodeCounts];

            int offset = 0;
            for (int i = 0; i < nodeCounts; i++) {
                receiveCounts[i] = baseChunk + (i < remainder ? 1 : 0);
                displacements[i] = offset;
                offset += receiveCounts[i];
            }

        if (me == ROOT) {
            int totalSend = Arrays.stream(receiveCounts).sum();
            if (totalSend != totalSizeToScatter) {
                throw new RuntimeException("Total sending size doesn't match the provided array size to scatter: " + totalSend + " vs " + totalSizeToScatter);
            }
        }

        // This is the receiving count for the current process
        int mySendCount = baseChunk + (me < remainder ? 1 : 0);

        if (type == MPIOPERATION.GATHER) {
            MPI.COMM_WORLD.Gatherv(sendArray, 0, mySendCount, MPI.OBJECT,
                    destArray, 0, receiveCounts, displacements, MPI.OBJECT, ROOT);

        }else if (type == MPIOPERATION.SCATTER) {
            MPI.COMM_WORLD.Scatterv(sendArray, 0, receiveCounts, displacements, MPI.OBJECT,
                    destArray, 0, mySendCount, MPI.OBJECT, ROOT);
        }else{
            throw new RuntimeException("Unsupported MPI operation: " + type);
        }
    }



}
