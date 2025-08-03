package project;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.*;

import static project.Config.*;
import static util.Helper.*;

import mpi.MPIException;
import util.*;

import mpi.MPI;

import javax.swing.*;

public class GeneticGolf {

    private enum Target {
        Single, Multi, Distributed
    }

    public static String[] argsP;
    public static void main(String[] args) throws Exception {
        argsP = args;
        // Safely check args[0] before accessing
        if (argsP.length > 0) {
            //Logger.log("Main arguments: " + argsP[0], LogLevel.Warn);
        } else {
            //Logger.log("Main arguments: (none provided to application)", LogLevel.Warn);
        }
//        MPI.Init(argsP);

        List<ConfigSettings> testConfigs = new ArrayList<>();
        int[] generations = {10000, 20000};
        double[] holePositions = {300_000.0, 500_000.0, 800_000.0};
        int[] popSizes = {1000, 2000};
        int[] bestSizes = {4, 8};
        double[] mutationRates = {0.1, 0.3, 0.6};
        double[] crossoverRates = {0.2, 0.6};

        //Filling testing array
        for (int g : generations)
            for (double h : holePositions)
                for (int p : popSizes)
                    for (int b : bestSizes)
                        for (double m : mutationRates)
                            for (double c : crossoverRates)
                                testConfigs.add(new ConfigSettings(g, h, p, b, m, c));

//        MPI.COMM_WORLD.Barrier();
        testSingle(testConfigs);
//        testMulti(testConfigs);
//        Logger.log("Running testDist");
//        testDistributed(testConfigs);
//        MPI.Finalize();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static GeneticReturn RunSingleThreaded(){
        Random r = new Random(SEED);
        GeneticReturn runReturn = new SingleThreaded(r).run();
        optimalToggle();
        return runReturn;
    }

    private static GeneticReturn RunMultiThreaded() throws Exception {
        while (getOptimalReached() == 1){optimalToggle(); }
        Random r = new Random(SEED);
        long startTime = System.currentTimeMillis();
        ArrayList<Ball> population = generatePopulation(r);
        int indexCut = (int) Math.floor((double) population.size() / THREADS); //TODO: Check if it rounds the cuts
        ExecutorService THREADPOOL = Executors.newFixedThreadPool(THREADS);

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> createAndShowGUI(panel));
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
                if(GUI_TOGGLE) {
                    assert panel != null;
                    panel.updateVisualization(population, newBestPop, i);
                }
                THREADPOOL.shutdownNow();

                return new GeneticReturn(newBestPop.get(0), i,System.currentTimeMillis() - startTime);
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

        return new GeneticReturn(null, GENERATIONS,System.currentTimeMillis() - startTime);
    }

    private static GeneticReturn RunDistributed() throws Exception {
        MPI.COMM_WORLD.Barrier();
        while (MPI.COMM_WORLD.Rank() == 0 && getOptimalReached() == 1){optimalToggle();}
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
            globalPopulationArrayList = generatePopulation(r);
            globalPopulationArray = globalPopulationArrayList.toArray();
        }

        //GUI setup (Only Root)
        GUI panel = (GUI_TOGGLE && me==ROOT) ? new GUI() : null;
        if (me == ROOT && panel != null) {
            SwingUtilities.invokeLater(() -> createAndShowGUI(panel));
        }

        // ADD: Array to broadcast optimal status
        int[] optimalStatus = new int[1]; // 0 = continue, 1 = optimal found

        ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);
        GeneticReturn result = null;

        for (int i = 0; i < GENERATIONS; i++){
            MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE, globalPopulationArray, localPopArray, ROOT);

            // Convert to ArrayList
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            //Run Fitness (All nodes)
            for (Ball ball : localPopulationArrayList) {
                ball.setFitness(ball.evaluateFitness());
            }

            //Getting back the calculated fitness results
            MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, POPSIZE, localPopulationArrayList.toArray(), globalPopulationArray, ROOT);

            if (me == ROOT) {
                //Converting the full population array so we can extract elites
                ConvertArrayToArrayList(globalPopulationArrayList, globalPopulationArray);
                globalPopulationArrayList.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));

                // Clear previous best population
                newBestPop.clear();

                //Extract elite pop **ROOT ONLY**
                for (int j = 0; j < Math.min(BEST_POP_TO_GET, globalPopulationArrayList.size()); j++) {
                    Ball tempBall = globalPopulationArrayList.get(j);

                    //If optimal ball is found
                    if (tempBall.getFitness() >= 0.95) {
                        newBestPop.add(tempBall.copy());
                        optimalToggle();
                        optimalStatus[0] = 1; // Signal optimal found
                        break;
                    }
                    newBestPop.add(tempBall.copy());
                }

                //Check local optimality
                if(getOptimalReached() == 1) {
                    if(GUI_TOGGLE&& panel != null) {
                        panel.updateVisualization(globalPopulationArrayList, newBestPop, i);
                    }
                    // Prepare result but don't return yet
                    result = new GeneticReturn(newBestPop.get(0), i, System.currentTimeMillis() - startTime);
                }

                // Update globalPopulationArrayList to be the working population only
                globalPopulationArray = globalPopulationArrayList.toArray();
            }

            // BROADCAST optimal status to all processes
            MPI.COMM_WORLD.Bcast(optimalStatus, 0, 1, MPI.INT, ROOT);

            // ALL processes check if optimal was found
            if (optimalStatus[0] == 1) {
                break; // All processes exit the loop together
            }

            MPI.COMM_WORLD.Bcast(globalPopulationArray,0,POPSIZE, MPI.OBJECT, ROOT);

            //Scatter for distributed crossover
            MPI_POPULATION_GENERIC(MPIOPERATION.SCATTER, POPSIZE-BEST_POP_TO_GET, globalPopulationArray, localPopArray, ROOT);

            //Update local population after scatter
            ConvertArrayToArrayList(localPopulationArrayList, localPopArray);

            //Crossover
            ArrayList<Ball> modifiedLocalPopArrList = new ArrayList<>();

            for (int iC = 0; iC < localPopulationArrayList.size(); iC++) {
                Ball ball = localPopulationArrayList.get(iC);
                int SEED_CROSSOVER = SEED + (iC + localNodeCountWorkSize * me);

                Random rC = new Random(SEED_CROSSOVER);
                if (rC.nextDouble() < CROSSOVER_RATE && localPopulationArrayList.size() > 1) {
                    Ball partner = selectRandom(localPopulationArrayList, rC);
                    modifiedLocalPopArrList.add(ball.crossover(partner, rC));
                } else {
                    modifiedLocalPopArrList.add(ball.copy());
                }
            }

            //Mutations
            mutate(modifiedLocalPopArrList, i);

            // Gather modified population
            MPI_POPULATION_GENERIC(MPIOPERATION.GATHER, POPSIZE-BEST_POP_TO_GET, modifiedLocalPopArrList.toArray(), globalPopulationArray, ROOT);

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
            }
        }

        // All processes reach this point together
        MPI.COMM_WORLD.Barrier();

        // Return appropriate result
        if (me == ROOT && result != null) {
            return result;
        } else {
            return new GeneticReturn(null, GENERATIONS, System.currentTimeMillis() - startTime);
        }
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

    public static GeneticReturn benchmark(Target target, int timesToRun) throws Exception {
        long totalTime = 0;
        GeneticReturn runReturn;
        int generation = 101010101;
        Ball bestFitness = null;
//        if (MPI.COMM_WORLD.Rank() == 0){
        System.out.print("Running " + target + " " + timesToRun + "x:\t");
//        }

        for (int i = 0; i < timesToRun; i++) {

            switch (target) {
                case Single -> {
                    runReturn = RunSingleThreaded();
                    totalTime += runReturn.time;
                    generation = runReturn.endGeneration;
                    bestFitness = runReturn.bestFitness;
                }
                case Multi -> {
                    runReturn = RunMultiThreaded();
                    totalTime += runReturn.time;
                    generation = runReturn.endGeneration;
                    bestFitness = runReturn.bestFitness;
                }
                case Distributed -> {
                    runReturn = RunDistributed();

//                    if (MPI.COMM_WORLD.Rank() == 0) {
                        totalTime += runReturn.time;
                        generation = runReturn.endGeneration;
                        bestFitness = runReturn.bestFitness;
//                    }
                }
            }
//            if (MPI.COMM_WORLD.Rank() == 0) {
                if (i % 5 == 0) System.out.print(" ");
                System.out.print("|");
//            }

            // Add barrier to synchronize all processes between runs
//            MPI.COMM_WORLD.Barrier();
        }

        long avg = 0;
//        if (MPI.COMM_WORLD.Rank() == 0) {
            avg = totalTime / timesToRun;
            System.out.println(" (" + avg + "ms)");
//        }

        return new GeneticReturn(bestFitness, generation, avg);
    }

    public static void testMulti(List<ConfigSettings> testConfigs) throws Exception {
//        if (MPI.COMM_WORLD.Rank() == 0) {
            int id = 0;
            File f;
            long multiTime = 0;
            BufferedWriter bw = null;
            // Config parameter sets

            // Prepare output
            f = new File("resultsGolfMulti.csv");
            bw = new BufferedWriter(new FileWriter(f));
            bw.write("id, generations, popSize, bestPopSize, holePos, mutationRate, crossoverRate, runType, endFitness, endGeneration, time(ms), time(s)\n");

            for (ConfigSettings config : testConfigs) {
                config.apply();
                GeneticReturn multiReturn = benchmark(Target.Multi, 3);
                multiTime = multiReturn.time;
                Double multiFitness = multiReturn.bestFitness != null ? multiReturn.bestFitness.getFitness() : null;
                bw.write(String.format("%d,%d,%d,%d,%.2f,%.2f,%.2f,Multi,%f,%d,%d ms, %d s\n", id, config.generations, config.popSize, config.bestPopSize, config.holePos, config.mutationRate, config.crossoverRate, multiFitness, multiReturn.endGeneration, multiTime, multiTime / 1000));
                bw.flush();
                id++;
            }//loopcfg
                bw.close();
//        }//root
    }
    public static void testSingle(List<ConfigSettings> testConfigs) throws Exception {
//        if (MPI.COMM_WORLD.Rank() == 0) {
            int id = 0;
            File f;
            long singleTime = 0;
            BufferedWriter bw = null;
            // Config parameter sets

            // Prepare output
            f = new File("resultsGolfSingle.csv");
            bw = new BufferedWriter(new FileWriter(f));
            bw.write("id, generations, popSize, bestPopSize, holePos, mutationRate, crossoverRate, runType, time(ms), time(s)\n");

            for (ConfigSettings config : testConfigs) {
                config.apply();
                GeneticReturn singleReturn = benchmark(Target.Single, 3);
                singleTime = singleReturn.time;
                Double singleFitness = singleReturn.bestFitness != null ? singleReturn.bestFitness.getFitness() : null;
                bw.write(String.format("%d,%d,%d,%d,%.2f,%.2f,%.2f,Multi,%f,%d,%d ms, %d s\n", id, config.generations, config.popSize, config.bestPopSize, config.holePos, config.mutationRate, config.crossoverRate, singleFitness, singleReturn.endGeneration, singleTime, singleTime / 1000));

                bw.flush();
                id++;
            }//loopcfg
//        }//root
                bw.close();
    }
    public static void testDistributed(List<ConfigSettings> testConfigs) throws Exception {
        int id = 0;
        File f = null;
        long distributedTime = 0;
        BufferedWriter bw = null;

        // Only root process handles file operations
        if (MPI.COMM_WORLD.Rank() == 0) {
            f = new File("resultsGolfDist.csv");
            bw = new BufferedWriter(new FileWriter(f));
            bw.write("id, generations, popSize, bestPopSize, holePos, mutationRate, crossoverRate, runType, time(ms), time(s)\n");
        }

        for (ConfigSettings config : testConfigs) {
            config.apply();

            // Ensure all processes have applied config before proceeding
            MPI.COMM_WORLD.Barrier();

            GeneticReturn distributedReturn = benchmark(Target.Distributed, 3);

            if (MPI.COMM_WORLD.Rank() == 0) {
                distributedTime = distributedReturn.time;
                Double distributedFitness = distributedReturn.bestFitness != null ? distributedReturn.bestFitness.getFitness() : null;
                bw.write(String.format("%d,%d,%d,%d,%.2f,%.2f,%.2f,Distributed,%f,%d,%d ms, %d s\n",
                        id, config.generations, config.popSize, config.bestPopSize, config.holePos,
                        config.mutationRate, config.crossoverRate, distributedFitness,
                        distributedReturn.endGeneration, distributedTime, distributedTime / 1000));
                bw.flush();
            }
            if (MPI.COMM_WORLD.Rank() == 0) Logger.log("Progress: "+(id+1)+"/"+testConfigs.size());
            id++;
            MPI.COMM_WORLD.Barrier();
        }

        if (MPI.COMM_WORLD.Rank() == 0 && bw != null) {
            bw.close();
            Logger.log("Root: Closed output file");
        }

        // Final barrier before finishing
        MPI.COMM_WORLD.Barrier();
    }


}//class
