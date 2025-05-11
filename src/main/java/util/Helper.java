package util;

import project.Ball;
import project.GUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

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

}
