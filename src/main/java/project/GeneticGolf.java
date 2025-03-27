package project;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

import static project.Config.*;

import util.Helper;
import util.LogLevel;
import util.Logger;

import javax.swing.*;

public class GeneticGolf {
    public static void main(String[] args) throws BrokenBarrierException, InterruptedException, TimeoutException {
        RunSingleThreaded();
        Logger.log("--------------------------------");
        RunMultiThreaded();
    }


    private static void RunSingleThreaded(){
        Random r = useGlobalRandom();
        long startTime = System.currentTimeMillis();

        new SingleThreaded(r).run();

        Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Info);
    }

    private static void RunMultiThreaded() throws BrokenBarrierException, InterruptedException, TimeoutException {
        Random r = useGlobalRandom();
        long startTime = System.currentTimeMillis();

        ArrayList<Ball> population = Helper.generatePopulation(r);
        int indexCut = (int) Math.floor(population.size() / THREADS); //TODO

        ExecutorService THREADPOOL = Executors.newFixedThreadPool(THREADS);

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> Helper.createAndShowGUI(panel));
        }

        for (int i = 0; i < GENERATIONS; i++) {
            Logger.log("GEN[" + i + "]" + "started...", LogLevel.Status);
            //Fitness Multithreaded
            for (int j = 0; j < THREADS; j++) {
                int indexStart = j*indexCut;
                int indexEnd = j*indexCut+indexCut;
                Logger.log("GEN["+i+"] "+"Submitting array at " + indexStart + " to " + indexEnd, LogLevel.Status);
                THREADPOOL.submit(new MultiThreaded(r, indexStart, indexEnd, population));
                Logger.log("GEN["+i+"] "+"Submitted array at " + indexStart + " to " + indexEnd, LogLevel.Status);
            }
            Logger.log("GEN["+i+"] "+"Waiting for barrier...", LogLevel.Status);
            BARRIER.await(5, TimeUnit.MINUTES);
            Logger.log("GEN["+i+"] "+"Barrier passed", LogLevel.Status);

            //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            ArrayList<Ball> newPop = new ArrayList<>();
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);
            Logger.log("GEN["+i+"] "+"Selection end", LogLevel.Status);
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
            Logger.log("GEN["+i+"] "+"Elite balls found", LogLevel.Status);

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

            //Crossover
            for (Ball ball : population) {
                if (r.nextDouble() < CROSSOVER_RATE) {
                    newPop.add(Helper.selectRandom(population, r).crossover(Helper.selectRandom(population, r)));
                }
            }

            Logger.log("GEN["+i+"] "+"Crossover end", LogLevel.Status);
            //Mutation
            for (Ball ball : newPop) {
                double tempDouble = r.nextDouble();
                if (tempDouble < MUTATION_RATE) {
                    ball.mutate(tempDouble * 10);
                }
            }
            Logger.log("GEN["+i+"] "+"Mutation end", LogLevel.Status);
            //Adding ELITE chromosome to population
            newPop.addAll(newBestPop);
            population = newPop;

            Logger.log("GEN["+i+"] "+"Elite Added", LogLevel.Status);
            if (GUI_TOGGLE && i % 1000 == 0 && panel != null) {
                Logger.log("GEN["+i+"] "+"Refreshing GUI", LogLevel.Status);
                panel.updateVisualization(population, newBestPop, i);
            }
        }
    }
}//class
