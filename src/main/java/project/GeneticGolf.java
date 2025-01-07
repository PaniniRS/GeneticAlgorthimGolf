package project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticGolf {
    public static final int GENERATIONS = 1000;
    public static final double HOLEPOS = 10000.0;
    public static final int POPSIZE = 500;
    private static final int BEST_POP_TO_GET = 10;
    public static final double MUTATIONRATE = 0.1;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.0006;

    private static final int SEED = 1;

    private static final int ANGLE_BOUND = 180;
    private static final int VELOCITY_BOUND = 500;
    private static final int POSX_INIT_BOUND = 1000;

    static Random r = new Random(SEED);

    public static void main(String[] args) {
        ArrayList<Ball> population = generatePopulation();
        ArrayList<Ball> bestPop = new ArrayList<>();

        for (int i = 0; i < GENERATIONS; i++) {
        //Fitness
            for (Ball ball : population) {

            }
        //Selection

        //Crossover

        //Mutation


        }//genLoop
    }//main

    private static ArrayList<Ball> generatePopulation(){
        ArrayList<Ball> pop = new ArrayList<>();
        for (int i = 0; i < POPSIZE; i++) {
            double angle = r.nextDouble() * ANGLE_BOUND;
            double velocity = r.nextDouble() * VELOCITY_BOUND;
            double posX = r.nextDouble() * POSX_INIT_BOUND;
            pop.add(new Ball(posX, 0, velocity, angle));
        }
        return pop;
    }//GeneratePopulationMethod
}//class
