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

    public static void Crossover(ArrayList<Ball> population, ArrayList<Ball> newPop, Random r) {
        for (Ball ball : population) {
            if (r.nextDouble() < CROSSOVER_RATE) {
                newPop.add(ball.crossover(Helper.selectRandom(population, r)));
            }else{
                newPop.add(ball);
            }
            if (newPop.size()==POPSIZE-4) break;
        }
    }

}
