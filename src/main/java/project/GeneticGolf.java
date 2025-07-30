package project;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

import static project.Config.*;
import static util.Helper.*;

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
//        Logger.log("Running main");
//        RunSingleThreaded();
//        Logger.log("--------------------------------");
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
        int indexCut = (int) Math.floor((double) population.size() / THREADS); //TODO: Check if it rounds the cuts
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
        int nodeCounts = MPI.COMM_WORLD.Size();

        Object[] globalPopulationArray = new Object[POPSIZE];
        ArrayList<Ball> globalPopulationArrayList = null;

        int localNodeCountWorkSize = POPSIZE/nodeCounts + (me < (POPSIZE%nodeCounts) ? 1 : 0);
        ArrayList<Ball> localPopulationArrayList = new ArrayList<>(localNodeCountWorkSize);

        //Root initializes global population
        if (me == ROOT) {
            globalPopulationArrayList = Helper.generatePopulation(new Random(SEED)); // Same seed as sequential
            globalPopulationArray = globalPopulationArrayList.toArray();
        }//!!! Root code block !!!

        // Scatter initial population to all nodes
        Object[] localPopArray = new Object[localNodeCountWorkSize];

        //GUI setup (Only Root)
        GUI panel = (GUI_TOGGLE && me==ROOT) ? new GUI() : null;
        if (me == ROOT && panel != null) {
            SwingUtilities.invokeLater(() -> Helper.createAndShowGUI(panel));
        } // !!! ROOT CODE BLOCK !!!

/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
////////////////////////GEN LOOP/////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
        for (int i = 0; i < GENERATIONS; i++){

            if (me == ROOT) {
                Logger.log("\n\n\n\n\n\n ----------GEN START-----------\n\n\n\n\n");
            }

            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE, globalPopulationArray, localPopArray, ROOT);

            // Convert to ArrayList
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

        //Run Fitness (All nodes)
            for (Ball ball : localPopulationArrayList) {
                ball.setFitness(ball.evaluateFitness());
            }

            // Gather fitness results
            Object[] fullPopulationArray = new Object[POPSIZE];
            //Getting back the calculated fitness results
            Object[] localDataForGather = localPopulationArrayList.toArray();
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, POPSIZE, localDataForGather, fullPopulationArray, ROOT);


            //Exists everywhere for elite selection
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);
            Object[] newBestPopArray = new Object[BEST_POP_TO_GET];

            if (me == ROOT) {
            //Converting the full population array so we can extract elites
                ArrayList<Ball> fullPopulationArList = new ArrayList<>();
                ConvertArrayToArrayList(fullPopulationArList, fullPopulationArray);

        // Selection (Root)
                //Sort array on fitness
                fullPopulationArList.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));



        //Extract elite pop **ROOT ONLY**
                boolean optimalFound = false;
                for (int j = 0; j < Math.min(BEST_POP_TO_GET, fullPopulationArList.size()); j++) {
                    final int indexToExtract = fullPopulationArList.size()-1;
                    Ball tempBall = fullPopulationArList.get(0);

                    //If optimal ball is found
                    if (tempBall.getFitness() >= 0.95 && !optimalFound) {
                        Logger.log("GEN[" + i + "] " + "!!!! Reached optimal after " + i + " generations !!!! \n Final fitness of " + j + " th best: " + tempBall.getFitness(), LogLevel.Success);
                        newBestPop.add(tempBall.copy()); //only needed if visualization is on if not we can skip it
                        fullPopulationArList.remove(indexToExtract);
                        optimalFound = true;
                        optimalToggle();
                    }

                    //If it's not optimal just add the ball to the array
                    newBestPop.add(tempBall.copy());
                    fullPopulationArList.remove(indexToExtract);

                }


                // Update globalPopulationArrayList to be the working population only
                globalPopulationArrayList = fullPopulationArList; //working pop
                globalPopulationArray = globalPopulationArrayList.toArray();
                newBestPopArray = newBestPop.toArray();

            }


        ///Broadcast elite population to all nodes
            MPI.COMM_WORLD.Bcast(newBestPopArray, 0, BEST_POP_TO_GET, MPI.OBJECT, ROOT);
        // Makes sure the nodes get the new best pop and there aren't any leftovers
                newBestPop.clear();
                ConvertArrayToArrayList(newBestPop, newBestPopArray);


        //Check local optimality
            if(getOptimalReached() == 1) {
                Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                if(GUI_TOGGLE && me==ROOT && panel != null) {

                    //Visualizing the whole population on complete
                    ArrayList<Ball> completePopulation = new ArrayList<>();
                    completePopulation.addAll(newBestPop);
                    panel.updateVisualization(completePopulation, newBestPop, i);
                }
                if (me == ROOT){
                Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
                }
                MPI.COMM_WORLD.Barrier();
                MPI.Finalize();
                return;}

            //Scattering working population for manipulation
            int workingPopSize = POPSIZE - BEST_POP_TO_GET;
            int newLocalSize = workingPopSize/nodeCounts + (me < (workingPopSize%nodeCounts) ? 1 : 0);
            Object[] workingLocalArray = new Object[newLocalSize];
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE-BEST_POP_TO_GET, globalPopulationArray, workingLocalArray, ROOT);


            //Crossover
            ArrayList<Ball> modifiedLocalPopArrList = new ArrayList<>();
            Random re = new Random(SEED); // More deterministic seed

            //Update local population after scatter
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            for (Ball ball : localPopulationArrayList) {
                if (re.nextDouble() < CROSSOVER_RATE && localPopulationArrayList.size() > 1) {
                    Ball partner = Helper.selectRandom(localPopulationArrayList, re);
                    modifiedLocalPopArrList.add(ball.crossover(partner, re));
                } else {
                    modifiedLocalPopArrList.add(ball.copy());
                }
            }

        //Mutation
            for (Ball ball : modifiedLocalPopArrList) {
                if (re.nextDouble() < MUTATION_RATE) {
                    ball.mutate(re.nextDouble(), re);
                }
            }

// Gather modified population (only ROOT needs the destination array)
            Object[] gatheredWorkingPop = null;
            if (me == ROOT) {
                gatheredWorkingPop = new Object[workingPopSize];
            }
            Object[] localModifiedData = modifiedLocalPopArrList.toArray();
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, workingPopSize, localModifiedData, gatheredWorkingPop, ROOT);


            if (me == ROOT) {

                //Convert the array into an array list for manipulation
                ConvertArrayToArrayList(globalPopulationArrayList, gatheredWorkingPop);

                // Add elite population
                globalPopulationArrayList.addAll(newBestPop);

                // Verify population size
                Logger.log("Working population size: " + globalPopulationArrayList.size() +
                        ", Elite population size: " + newBestPop.size() +
                        ", Total should be: " + POPSIZE, LogLevel.Debug);
                if (globalPopulationArrayList.size() != POPSIZE) {
                    throw new Exception("Population size mismatch: " + globalPopulationArrayList.size() + " != " + POPSIZE);
                }

                //Set up GlobalPopArray for the next iteration
                globalPopulationArray = globalPopulationArrayList.toArray();

                // Update GUI
                if (panel != null && GUI_TOGGLE && i % GUI_DRAW_STEPS == 0) {
                    panel.updateVisualization(globalPopulationArrayList, newBestPop, i);
                }

//                // Reduced logging frequency [REMOVE ON FINAL]
//                if (i % 100 == 0) {
//                    Logger.log("Gen " + i + " | Best fitness: " +
//                            (globalPopulationArrayList.isEmpty() ? "N/A" : globalPopulationArrayList.get(0).getFitness() + "Population size: " + globalPopulationArrayList.size()), LogLevel.Status);
//                }

            }
        }//=-=-=-=-=-=-=-=§: end GenLoop

        //CLose MPI protocol
        MPI.COMM_WORLD.Barrier();
        MPI.Finalize();
    }
/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////xsx
/////////////////////////////////////////////////////////////////////////////
    /**
    Clears array list if it's not null, otherwise it transfers the elements from the array into the array list by .add
     */
    private static void ConvertArrayToArrayList(ArrayList<Ball> ArrList, Object[] Arr) {
        if (ArrList != null)ArrList.clear();
        for (Object obj : Arr) {
            if (obj != null) {
                assert ArrList != null;
                ArrList.add((Ball) obj);
            }else{
                    throw new IllegalStateException("Null element during population conversion. Check MPI Scatterv source.");
            }
        }
    }
}//class
