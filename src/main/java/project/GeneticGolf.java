package project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GeneticGolf {
    public static final int GENERATIONS = 10000;
    public static final double HOLEPOS = 1000.0;
    public static final int POPSIZE = 5000;
    private static final int BEST_POP_TO_GET = 6;
    public static final double MUTATION_RATE = 0.1;
    public static final double CROSSOVER_RATE = 0.6;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.001;

    private static final int SEED = 1;

    public static final int ANGLE_BOUND = 180;
    public static final int VELOCITY_BOUND = 2000;
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
            population.sort(Comparator.comparingDouble(Ball::getFitness)); //TODO LOOK AT THIS
            ArrayList<Ball> newPop = generatePopulation();

            // Get the x best chromosomes/balls //TODO best population isnt getting saved!!!!!
            for (int j = 0; j < BEST_POP_TO_GET; j++) {
                if(population.get(j).getFitness() >= 1.0) {
                    System.out.println("!!!! Reached optimal after + " + i + " generations !!!!");
                    System.out.println("Final fitness of "  + j +" th best: " + population.get(j).getFitness());
                    break;
                }
                System.out.println("Gen: " + i + " | BestN: " + j + " | Fitness:" + population.get(j).getFitness());
                newPop.add(population.get(j));
            }
            System.out.println("-----------------------------");


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
