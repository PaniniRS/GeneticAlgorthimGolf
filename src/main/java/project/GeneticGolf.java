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
        MPI.Init(args);
        if (MPI.COMM_WORLD.Rank() == 0) {
        Logger.log("Running main");
        Logger.log("----------------ST-----------------", LogLevel.Debug);
        RunSingleThreaded();
        Logger.log("----------------MT-----------------",LogLevel.Debug);
        RunMultiThreaded();
        }
        Logger.log("----------------DT-----------------",LogLevel.Debug);
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
                    //part below can be optimized I think, since code repeats with below check
                    newBestPop.add(tempBall.copy()); //only needed if visualization is on if not we can skip it
                    optimalToggle();
                    break;
                }
                newBestPop.add(tempBall.copy());
            }

            if(getOptimalReached() == 1) {
                Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                Logger.log("\tBest fitness: " + newBestPop.get(0).getFitness(), LogLevel.Status);
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
        while (getOptimalReached() == 1){optimalToggle();}
        long startTime = System.currentTimeMillis();
        final int ROOT = 0;
        int me = MPI.COMM_WORLD.Rank();
        int nodeCounts = MPI.COMM_WORLD.Size();
        Random r = new Random(SEED);

        Object[] globalPopulationArray = new Object[POPSIZE];
        ArrayList<Ball> globalPopulationArrayList = null;

        int localNodeCountWorkSize = POPSIZE/nodeCounts + (me < (POPSIZE%nodeCounts) ? 1 : 0);
        Object[] localPopArray = new Object[localNodeCountWorkSize];
        ArrayList<Ball> localPopulationArrayList = new ArrayList<>(localNodeCountWorkSize);

        //Root initializes global population
        if (me == ROOT) {
            globalPopulationArrayList = Helper.generatePopulation(r); // Same seed as sequential
            globalPopulationArray = globalPopulationArrayList.toArray();
        }//!!! Root code block !!!


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
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE, globalPopulationArray, localPopArray, ROOT);

            // Convert to ArrayList
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            //Run Fitness (All nodes)
            for (Ball ball : localPopulationArrayList) {
                ball.setFitness(ball.evaluateFitness());
            }

            //Getting back the calculated fitness results
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, POPSIZE, localPopulationArrayList.toArray(), globalPopulationArray, ROOT);



            //Exists everywhere for elite selection
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);

            if (me == ROOT) {
            //Converting the full population array so we can extract elites
                ConvertArrayToArrayList(globalPopulationArrayList, globalPopulationArray);
                globalPopulationArrayList.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
        //Extract elite pop **ROOT ONLY**
                for (int j = 0; j < Math.min(BEST_POP_TO_GET, globalPopulationArrayList.size()); j++) {
                    Ball tempBall = globalPopulationArrayList.get(j);

                    //If optimal ball is found
                    if (tempBall.getFitness() >= 0.95) {
                        //only needed if visualization is on if not we can skip it
                        newBestPop.add(tempBall.copy());
                        optimalToggle();
                        break;
                    }
                    //If it's not optimal just add the ball to the array
                    newBestPop.add(tempBall.copy());
                }

                //Check local optimality
                if(getOptimalReached() == 1) {
                    Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                    Logger.log("\tBest fitness: " + newBestPop.get(0).getFitness(), LogLevel.Status);
                    if(GUI_TOGGLE&& panel != null) {

                        //Visualizing the whole population on complete
                        panel.updateVisualization(globalPopulationArrayList, newBestPop, i);
                    }
                        Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
                    MPI.COMM_WORLD.Barrier();
                    MPI.Finalize();
                    return;}
                // Update globalPopulationArrayList to be the working population only
                globalPopulationArray = globalPopulationArrayList.toArray();
            }

            MPI.COMM_WORLD.Bcast(globalPopulationArray,0,POPSIZE, MPI.OBJECT, ROOT);

            //Scatter for distributed crossover
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE-BEST_POP_TO_GET, globalPopulationArray, localPopArray, ROOT);

            //Update local population after scatter
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            //////////////////////////////////////////////////
            //////////////////////////////////////////////////
            //Crossover
            ArrayList<Ball> modifiedLocalPopArrList = new ArrayList<>();

            for (int iC = 0; iC < localPopulationArrayList.size(); iC++) {
                Ball ball = localPopulationArrayList.get(iC);
                int SEED_CROSSOVER = SEED + (iC + localNodeCountWorkSize * me); //mimic multithreaded seeding

                Random rC = new Random(SEED_CROSSOVER);
                if (rC.nextDouble() < CROSSOVER_RATE && localPopulationArrayList.size() > 1) {
                    Ball partner = Helper.selectRandom(localPopulationArrayList, rC);
                    modifiedLocalPopArrList.add(ball.crossover(partner, rC));
                } else {
                    modifiedLocalPopArrList.add(ball.copy());
                }
            }

        //Mutations
                Helper.mutate(modifiedLocalPopArrList, i);

// Gather modified population (only ROOT needs the destination array)
            Helper.MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, POPSIZE-BEST_POP_TO_GET, modifiedLocalPopArrList.toArray(), globalPopulationArray, ROOT);


            if (me == ROOT) {
                //Convert the array into an array list for manipulation
                ConvertArrayToArrayList(globalPopulationArrayList, globalPopulationArray);

                // Add elite population
                for (int j = 0; j < BEST_POP_TO_GET; j++) {
                globalPopulationArrayList.remove(globalPopulationArrayList.size() - 1);
                }
                globalPopulationArrayList.addAll(newBestPop);


                //Size verification
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
