package project;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

import static project.Config.*;

//////////////////////////////////
//////////////////////////////////
//////////////////////////////////

public class GeneticGolf {
    static Random r = GLOBAL_RANDOM;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ArrayList<Ball> population = generatePopulation();

        GUI panel = GUI_TOGGLE ? new GUI() : null;
        if (panel != null) {
            SwingUtilities.invokeLater(() -> createAndShowGUI(panel));
        }

        for (int i = 0; i < GENERATIONS; i++) {
        //Fitness
            for (Ball ball : population) {
                ball.setFitness(ball.evaluateFitness());
            }
        //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            ArrayList<Ball> newPop = new ArrayList<>();
            ArrayList<Ball> newBestPop = new ArrayList<>(BEST_POP_TO_GET);

            // Get the x best chromosomes/balls
            for (int j = 0; j < Math.min(BEST_POP_TO_GET, population.size()); j++) {
                Ball tempBall = population.get(j);
                if(tempBall.getFitness() >= 0.95) {
                    System.out.println("!!!! Reached optimal after " + i + " generations !!!!");
                    System.out.println("Final fitness of "  + j +" th best: " + tempBall.getFitness());
                    Config.optimalToggle();
                    break;
                }
                System.out.println("Gen: " + i + " | BestN: " + j + " | Fitness:" + tempBall.getFitness());
                newBestPop.add(tempBall.copy());
            }
            System.out.println("-----------------------------");

            if(getOptimalReached() == 1) {
                assert panel != null;
                panel.updateVisualization(population, newBestPop, i);
                break;}

        //Crossover
            for (Ball ball : population) {
                if (r.nextDouble() < CROSSOVER_RATE) {
                    newPop.add(selectRandom(population).crossover(selectRandom(population)));
                }
            }
        //Mutation
            for (Ball ball : newPop) {
                double tempDouble = r.nextDouble();
                if (tempDouble < MUTATION_RATE) {
                    ball.mutate(tempDouble * 10);
                }
            }

            //Adding ELITE chromosome to population
            newPop.addAll(newBestPop);
            population = newPop;

            if (GUI_TOGGLE && i % 100 == 0 && panel != null) {
                panel.updateVisualization(population, newBestPop, i);
            }
        }//genLoop

        System.out.println("Time: " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("      " + (System.currentTimeMillis() - startTime)/1000.00 + " s");
    }//main

////////////////////////////////////
////////////////////////////////////
////////////////////////////////////

    private static ArrayList<Ball> generatePopulation(){
        ArrayList<Ball> pop = new ArrayList<>();
        for (int i = 0; i < POPSIZE; i++) {
            double angle = r.nextDouble(ANGLE_BOUND);
            double velocity = r.nextDouble(VELOCITY_BOUND);
            double posX = r.nextDouble(POSX_INIT_BOUND);
            pop.add(new Ball(posX, 0, velocity, angle));
        }
        return pop;
    }//GeneratePopulationMethod

    private static Ball selectRandom(ArrayList<Ball> population) {
        return population.get(r.nextInt(population.size())); //nextInt is upper exclusive
    }//selectRandom

    private static void createAndShowGUI(GUI panel) {
        JFrame frame = new JFrame("Genetic Golf Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(800, 400);
        frame.setVisible(true);
    }

}//class
