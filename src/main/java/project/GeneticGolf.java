package project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static project.Config.*;

public class GeneticGolf {
    static Random r = new Random(SEED);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
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
            ArrayList<Ball> newPop = new ArrayList<Ball>();

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

            // Get the x best chromosomes/balls //TODO best population isnt getting saved!!!!!
            for (int j = 0; j < BEST_POP_TO_GET; j++) {
                if(population.get(j).getFitness() >= 0.95) {
                    System.out.println("!!!! Reached optimal after + " + i + " generations !!!!");
                    System.out.println("Final fitness of "  + j +" th best: " + population.get(j).getFitness());
//                    throw new RuntimeException("FINISHED");
                    break; //TODO: CHECK THIS STOPGAP
                }
                System.out.println("Gen: " + i + " | BestN: " + j + " | Fitness:" + population.get(j).getFitness());
                newPop.add(population.get(j));
            }
            System.out.println("-----------------------------");
            population = newPop;
        }//genLoop

        System.out.println("Time: " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("      " + (System.currentTimeMillis() - startTime)/1000.00 + " s");
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
