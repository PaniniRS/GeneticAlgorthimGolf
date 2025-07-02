package project;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

import static project.Config.*;
import static util.Helper.multiGenetic;

import util.GeneticFunction;

import util.Helper;
import util.LogLevel;
import util.Logger;
import mpi.MPI;

import javax.swing.*;

public class GeneticGolf {
    public static void main(String[] args) throws Exception {

        // Safely check args[0] before accessing
        if (args.length > 0) {
            Logger.log("Main arguments: " + args[0], LogLevel.Warn);
        } else {
            Logger.log("Main arguments: (none provided to application)", LogLevel.Warn);
        }
        Logger.log("Running main");
//        RunSingleThreaded();
        Logger.log("--------------------------------");
//        RunMultiThreaded();
        Logger.log("--------------------------------");

        MPI.Init(args);
        RunDistributed();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void RunSingleThreaded(){
        Random r = new Random(SEED);
        long startTime = System.currentTimeMillis();

        new SingleThreaded(r).run();
        optimalToggle();

        Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Info);
    }

    private static void RunMultiThreaded() throws Exception {
        Random r = new Random(SEED);
        long startTime = System.currentTimeMillis();
        ArrayList<Ball> population = Helper.generatePopulation(r);
        int indexCut = (int) Math.floor(population.size() / THREADS); //TODO: Check if it rounds the cuts
        ExecutorService THREADPOOL = Executors.newFixedThreadPool(THREADS);

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> Helper.createAndShowGUI(panel));
        }

        for (int i = 0; i < GENERATIONS; i++) {
            //Fitness Multithreaded
            multiGenetic(indexCut, THREADPOOL, population, null, GeneticFunction.Fitness, i);
            BARRIER.await(1, TimeUnit.MINUTES);


            //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness())
            );
            ArrayList<Ball> newPop = new ArrayList<>(POPSIZE-BEST_POP_TO_GET);
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);

            // Get the best chromosomes/balls
            for (int j = 0; j < Math.min(BEST_POP_TO_GET, population.size()); j++) {
                Ball tempBall = population.get(j);
                if(tempBall.getFitness() >= 0.95) {
                    Logger.log("GEN["+i+"] "+"!!!! Reached optimal after " + i + " generations !!!! \n Final fitness of "  + j +" th best: " + tempBall.getFitness(), LogLevel.Success);

                    //part below can be optimized I think, since code repeats with below check
                    newBestPop.add(tempBall.copy()); //only needed if visualization is on if not we can skip it
                    optimalToggle();
                    break;
                }
                newBestPop.add(tempBall.copy());
            }

            if(getOptimalReached() == 1) {
                Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                if(GUI_TOGGLE) {
                    assert panel != null;
                    panel.updateVisualization(population, newBestPop, i);
                }
                THREADPOOL.shutdownNow();

                Logger.log("Thread pool shutdown success", LogLevel.Status);

                Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
                return;
            }

            //crossover -> TODO: multithread w/ return
            multiGenetic(indexCut, THREADPOOL, population, newPop, GeneticFunction.Crossover,i);

            //Mutation multithread
            multiGenetic(indexCut, THREADPOOL, population, newPop, GeneticFunction.Mutation, i);
            BARRIER.await(2, TimeUnit.MINUTES);

            //Adding ELITE chromosome to population
            population = newPop;
            population.addAll(newBestPop);
            if (population.size() != POPSIZE){
                throw new Exception("POPSIZE ISN'T THE SAME" + population.size() + "!=" + POPSIZE);
            }

            if (GUI_TOGGLE && i % GUI_DRAW_STEPS == 0 && panel != null) {
                panel.updateVisualization(population, newBestPop, i);
            }
        }
    }

    private static void RunDistributed() throws Exception {
        long startTime = System.currentTimeMillis();
        final int ROOT = 0;
        int me = MPI.COMM_WORLD.Rank();
        int nodes = MPI.COMM_WORLD.Size();

        ///
        //TODO 21-6 - 20:29 : DO ARRAY BUFFERS FOR bcasting the population array
        ///
        Random r = new Random(SEED);

        //Makes sure only one node sets the population, in our case it's always root
        ArrayList<Ball> population = null;
        Object[] populationArray = new Object[POPSIZE];
        if (me == ROOT ) {
            population = Helper.generatePopulation(r);
            populationArray = population.toArray();
        }
        //BUG TODO: Cannot pass arraylist into Bcast so make func that converts population
        MPI.COMM_WORLD.Bcast(populationArray, 0, POPSIZE, MPI.OBJECT, ROOT);
        MPI.COMM_WORLD.Barrier();
        Logger.log("Passed Barrier post pop init,");
        //Check if root populated the population array
        if (population == null) {throw new Exception("POPULATION IS NULL (pop not set by root)");}
        if (me == ROOT) {Helper.printPopulation(population);};
        int indexCut = (int) Math.floor(population.size() / THREADS); //TODO: Check if it rounds the cuts
        int indexStart = me * indexCut;
        int indexEnd = (indexCut*me+indexCut != population.size()-BEST_POP_TO_GET && me == THREADS-1) ? population.size()-BEST_POP_TO_GET : me * indexCut + indexCut;

//        ExecutorService THREADPOOL = Executors.newFixedThreadPool(THREADS);
//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

        //GUI setup TODO: Maybe check if it works so that root can only call/access it
        for (int i = 0; i < GENERATIONS; i++){
        //Run Fitness
        // Selection

        //Sort array on fitness

        //Extract elite pop
        //Check local optimality
        //Crossover
        //Mutation
        //Push elite pop to new pop
        //Refresh GUI NOTE: Optional on distributed

        }//§: end GenLoop
//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////
        Logger.log("Hello from " + (me+1) + " out of " + nodes);

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> Helper.createAndShowGUI(panel));
        }
        Logger.log("GUI queued " + (me+1));


        //Generations
        for (int i = 0; i < GENERATIONS; i++) {

        }





        //CLose MPI protocol
        MPI.Finalize();

        Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Info);
    }
}//class
