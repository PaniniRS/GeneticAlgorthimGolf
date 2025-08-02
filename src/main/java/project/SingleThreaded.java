package project;
import util.LogLevel;
import util.Logger;
import javax.swing.*;
import util.Helper;
import java.util.ArrayList;
import java.util.Random;

import static project.Config.*;

public class SingleThreaded {
    Random r;
    SingleThreaded(Random r){
        this.r = r;
    }
    public GeneticReturn run()  {
        long startTime = System.currentTimeMillis();
        ArrayList<Ball> population = Helper.generatePopulation(r);

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> Helper.createAndShowGUI(panel));
        }

        for (int i = 0; i < GENERATIONS; i++) {
            //Fitness
            for (Ball ball : population) {
                ball.setFitness(ball.evaluateFitness());
            }
            //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            ArrayList<Ball> newPop = new ArrayList<>(POPSIZE-BEST_POP_TO_GET);
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);

            // Get the x best chromosomes/balls
            for (int j = 0; j < Math.min(BEST_POP_TO_GET, population.size()); j++) {
                Ball tempBall = population.get(j);
                if(tempBall.getFitness() >= 0.95) {
                    //part below can be optimized I think, since code repeats with below check
                    newBestPop.add(tempBall.copy());
                    Config.optimalToggle();
                    break;
                }
                newBestPop.add(tempBall.copy());
            }

            if(getOptimalReached() == 1) {
                //Logger.log("GEN["+i+"] "+"Optimal reached", LogLevel.Status);
                //Logger.log("\tBest fitness: " + newBestPop.get(0).getFitness(), LogLevel.Status);
                if(GUI_TOGGLE) {
                    assert panel != null;
                    panel.updateVisualization(population, newBestPop, i);
                }
                return new GeneticReturn(newBestPop.get(0), i,System.currentTimeMillis() - startTime);
            }

            //Crossover
            Helper.crossover(population, newPop);
            //Mutation
            Helper.mutate(newPop, i);
            //Adding ELITE chromosome to population
            newPop.addAll(newBestPop);
            population = newPop;

            if (GUI_TOGGLE && i % GUI_DRAW_STEPS == 0 && panel != null) {
                panel.updateVisualization(population, newBestPop, i);
            }
        }//genLoop
        return new GeneticReturn(null, GENERATIONS,System.currentTimeMillis() - startTime);
    }
}
