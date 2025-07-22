package project;
import java.util.ArrayList;
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
        int nodeCounts = MPI.COMM_WORLD.Size();
        Random r = new Random(SEED + me);

        //Calculate displacement + send count
        int nodeCountWorkSize = (POPSIZE - BEST_POP_TO_GET) / nodeCounts;
        int remainderWorkSize = (POPSIZE - BEST_POP_TO_GET) % nodeCounts;

        //Makes sure only certain worker nodeCounts get the remaining non-assigned balls
        int localNodeCountWorkSize = nodeCountWorkSize + (me < remainderWorkSize ? 1 : 0);

        Object[] globalPopulationArray = new Object[POPSIZE];
        ArrayList<Ball> globalPopulationArrayList = null;

        // Local population for this nodeCount
        ArrayList<Ball> localPopulationArrayList = new ArrayList<>(localNodeCountWorkSize);

        //Root initializes global population
        if (me == ROOT) {
            globalPopulationArrayList = Helper.generatePopulation(new Random(SEED)); // Same seed as sequential
            globalPopulationArray = globalPopulationArrayList.toArray();
        }//!!! Root code block !!!

        // Scatter initial population to all nodes
        Object[] localPopArray = new Object[localNodeCountWorkSize];
        int[] nodeCountsSendCounts = new int[nodeCounts];
        int[] nodeCountsDisplacements = new int[nodeCounts];



//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

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

        // Calculate send counts and nodeCountsDisplacements
        Helper.MPI_SCATTER_POPULATION(nodeCounts, nodeCountWorkSize, remainderWorkSize, nodeCountsSendCounts, nodeCountsDisplacements, localPopArray, globalPopulationArray, localNodeCountWorkSize, ROOT);
        // Convert to ArrayList
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

        //Run Fitness (All)
            for (Ball ball : localPopulationArrayList) {
                ball.setFitness(ball.evaluateFitness());
            }

            //TODO: Check if properly sent args
            MPI.COMM_WORLD.Gatherv(localPopulationArrayList.toArray(), 0, localNodeCountWorkSize, MPI.OBJECT, globalPopulationArray, 0,nodeCountsSendCounts, nodeCountsDisplacements, MPI.OBJECT, ROOT);

            //Exists everywhere
            ArrayList<Ball> newPop = new ArrayList<>(POPSIZE-BEST_POP_TO_GET);
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);


            //Convert the array with calculated fitness to arraylist
            if (me == ROOT) {
                ConvertArrayToArrayList(globalPopulationArrayList, globalPopulationArray);
        // Selection (Root)
                //Sort array on fitness
                globalPopulationArrayList.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness())
                );

        //Extract elite pop **ROOT ONLY**
                for (int j = 0; j < Math.min(BEST_POP_TO_GET, globalPopulationArrayList.size()); j++) {
                    Ball tempBall = globalPopulationArrayList.get(j);
                    globalPopulationArrayList.remove(j);
                    if (tempBall.getFitness() >= 0.95) {
                        Logger.log("GEN[" + i + "] " + "!!!! Reached optimal after " + i + " generations !!!! \n Final fitness of " + j + " th best: " + tempBall.getFitness(), LogLevel.Success);

                        //part below can be optimized I think, since code repeats with below check
                        newBestPop.add(tempBall.copy()); //only needed if visualization is on if not we can skip it
                        optimalToggle();
                        break;
                    }
                    newBestPop.add(tempBall.copy());
                }

            //Remove extracted elite pop
                if (me == ROOT && i % 100 == 0) {Logger.log("Size of global array: " + globalPopulationArrayList.size());}
            }
        //Broadcast elite pop
        Object[] newBestPopArray = new Object[BEST_POP_TO_GET];
            if (me == ROOT) {
                newBestPopArray = newBestPop.toArray();
            }
            MPI.COMM_WORLD.Bcast(newBestPopArray, 0, BEST_POP_TO_GET, MPI.OBJECT, ROOT);
        // Makes sure the non root nodes get the new best pop
            if (me != ROOT) {
                ConvertArrayToArrayList(newBestPop, newBestPopArray);
            }


        //Check local optimality
            if(getOptimalReached() == 1) {
                Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                if(GUI_TOGGLE) {
                    assert panel != null;
                    panel.updateVisualization(globalPopulationArrayList, newBestPop, i);
                }
                if (me == ROOT){
                Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
                }
                MPI.COMM_WORLD.Barrier();
                return;}

            //Prep Array Chunks
            Helper.MPI_SCATTER_POPULATION(nodeCounts, nodeCountWorkSize, remainderWorkSize, nodeCountsSendCounts, nodeCountsDisplacements, localPopArray, globalPopulationArray, localNodeCountWorkSize, ROOT);

            //Crossover
            ArrayList<Ball> newLocalPopArrList = new ArrayList<>();
            Random re = new Random(SEED + me * GENERATIONS + i); // More deterministic seed

            // Update local population
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            for (Ball ball : localPopulationArrayList) {
                if (re.nextDouble() < CROSSOVER_RATE && localPopulationArrayList.size() > 1) {
                    Ball partner = Helper.selectRandom(localPopulationArrayList, re);
                    newLocalPopArrList.add(ball.crossover(partner, re));
                } else {
                    newLocalPopArrList.add(ball.copy());
                }
            }
        //Mutation
            for (Ball ball : newLocalPopArrList) {
                if (re.nextDouble() < MUTATION_RATE) {
                    ball.mutate(re.nextDouble(), re);
                }
            }

            // Gather mutated population
            MPI.COMM_WORLD.Gatherv(newLocalPopArrList.toArray(), 0, localNodeCountWorkSize,
                    MPI.OBJECT, globalPopulationArray, 0, nodeCountsSendCounts,
                    nodeCountsDisplacements, MPI.OBJECT, ROOT);


            if (me == ROOT) {
                // Add elite population
                globalPopulationArrayList.addAll(newBestPop);

                // Verify population size
                if (globalPopulationArrayList.size() != POPSIZE) {
                    Logger.log("WARNING: Population size mismatch: " + globalPopulationArrayList.size() + " != " + POPSIZE, LogLevel.Warn);
                }

                // Update GUI
                if (panel != null && GUI_TOGGLE && i % GUI_DRAW_STEPS == 0) {
                    panel.updateVisualization(globalPopulationArrayList, newBestPop, i);
                }

                if (i % 100 == 0) { // Reduced logging frequency
                    Logger.log("Gen " + i + " | Best fitness: " +
                            (globalPopulationArrayList.isEmpty() ? "N/A" : globalPopulationArrayList.get(0).getFitness()), LogLevel.Info);
                }
            }
        }//=-=-=-=-=-=-=-=§: end GenLoop

        //CLose MPI protocol

        MPI.COMM_WORLD.Barrier();
        MPI.Finalize();
    }
/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////xsx
/////////////////////////////////////////////////////////////////////////////
    private static void ConvertArrayToArrayList(ArrayList<Ball> ArrList, Object[] Arr) {
        ArrList.clear();
        for (Object obj : Arr) {
            if (obj != null) {
                ArrList.add((Ball) obj);
            }
        }
    }
}//class
