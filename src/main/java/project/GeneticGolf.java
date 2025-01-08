package project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticGolf {
    public static final int GENERATIONS = 1000;
    public static final double HOLEPOS = 10000.0;
    public static final int POPSIZE = 500;
    private static final int BEST_POP_TO_GET = 2;
    public static final double MUTATION_RATE = 0.1;
    public static final double CROSSOVER_RATE = 0.6;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.0006;

    private static final int SEED = 1;

    public static final int ANGLE_BOUND = 180;
    public static final int VELOCITY_BOUND = 300;
    public static final int POSX_INIT_BOUND = 1000;

    static Random r = new Random(SEED);

    public static void main(String[] args) {
        ArrayList<Ball> population = generatePopulation();
        ArrayList<Ball> bestPop = new ArrayList<>();

        for (int i = 0; i < GENERATIONS; i++) {
        //Fitness
            for (Ball ball : population) {
                ball.setFitness(ball.evaluateFitness());
            }
        //Selection
            //Sort the array based on fitness
            population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));

            // Get the x best chromosomes/balls
            for (int j = 0; j < BEST_POP_TO_GET; j++) {
                bestPop.add(population.get(j));
                System.out.println("Gen: " + i + " | BestN: " + j + " | Fitness:" + population.get(j).getFitness());
            }
            System.out.println("-----------------------------");

            ArrayList<Ball> newPop = generatePopulation();

        //Crossover
            for (Ball ball : population) {
                if (r.nextDouble() < CROSSOVER_RATE) {
                    ball.crossover(selectRandom(newPop));
                }
            }
        //Mutation
            for (Ball ball : newPop) {
                double tempDouble = r.nextDouble();
                if (tempDouble < MUTATION_RATE) {
                    ball.mutate(tempDouble * 10);
                }
            }

            population = newPop;
        }//genLoop
    }//main

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
    }
}//class
