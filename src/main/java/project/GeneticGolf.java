package project;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

import static project.Config.*;

import util.GeneticFunction;
import util.Helper;
import util.LogLevel;
import util.Logger;

import javax.swing.*;

public class GeneticGolf {
    public static void main(String[] args) throws Exception {
        Logger.log("Running main");
        RunSingleThreaded();
        Logger.log("--------------------------------");
        RunMultiThreaded();
    }


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
            multiNonAltering(indexCut, THREADPOOL, population, GeneticFunction.Fitness);
            BARRIER.await(1, TimeUnit.MINUTES);

            //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            ArrayList<Ball> newPop = new ArrayList<>(POPSIZE-BEST_POP_TO_GET);
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);

            // Get the x best chromosomes/balls
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

                Logger.log("Threadpool shutdown success", LogLevel.Status);

                Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
                return;
            }

            //Crossover -> TODO: multithread w/ return

//-----------------------
//            indexCut = (int) Math.floor((POPSIZE-BEST_POP_TO_GET) / THREADS);
//            ArrayList<MultiThreadedCrossover> tasks = new ArrayList<>(THREADS-1);
//
//            for (int j = 0; j < THREADS; j++){
//                int indexStart = j*indexCut;
//                int indexEnd = (j == THREADS-1) ? POPSIZE-BEST_POP_TO_GET : j*indexCut+indexCut;
//
//                tasks.add(new MultiThreadedCrossover(new Random(SEED + j), indexStart, indexEnd, population));
//            }
//
//            //Running all Threads and waiting to finish
//            List<Future<List<Ball>>> futures = THREADPOOL.invokeAll(tasks);
//            //Joining the results into the newPop array
//            for (Future<List<Ball>> future : futures) {
//                newPop.addAll(future.get());
//            }
//-----------------------
            Helper.Crossover(population, newPop, r);


            //Mutation -> TODO: multithread on newpop
            for (Ball ball : newPop) {
                double tempDouble = r.nextDouble();
                if (tempDouble < MUTATION_RATE) {
                    ball.mutate(tempDouble * 10, r);
                }
            }

            //Mutation multithread
// multiNonAltering(indexCut, THREADPOOL, newPop, GeneticFunction.Mutation);
//            BARRIER.await(10, TimeUnit.MINUTES);

            //Adding ELITE chromosome to population
            population = newPop;
            population.addAll(newBestPop);
            if (population.size() != POPSIZE){
                throw new Exception("POPSIZE ISNT THE SAME" + population.size() + "!=" + POPSIZE);
            }

            if (GUI_TOGGLE && i % 1000 == 0 && panel != null) {
//                Logger.log("GEN["+i+"] "+"Refreshing GUI", LogLevel.Status);
                panel.updateVisualization(population, newBestPop, i);
            }
        }
    }

    private static void multiNonAltering(int indexCut, ExecutorService THREADPOOL, ArrayList<Ball> population, GeneticFunction funcType) {
        for (int j = 0; j < THREADS; j++) {
            int indexStart = j* indexCut;
            int indexEnd = (indexCut*j+indexCut != population.size() && j == THREADS-1) ? population.size() : j * indexCut + indexCut;
            switch (funcType){
                case Selection -> {
                }
                case Crossover -> {
                }
                case Mutation -> {
                    THREADPOOL.submit(new MultiThreadedMutation(new Random(SEED + j), indexStart, indexEnd, population));
                }
                case Fitness -> {
                    THREADPOOL.submit(new MultiThreadedFitness(new Random(SEED + j), indexStart, indexEnd, population));
                }
            }

        }
    }
}//class
