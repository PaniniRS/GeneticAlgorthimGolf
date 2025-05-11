package project;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

import static project.Config.*;

public class MultiThreadedMutation implements Runnable{
    ArrayList<Ball> population;
    int indexStart, indexEnd, generation;
    public MultiThreadedMutation(int indexStart, int indexEnd, ArrayList<Ball> population, int generationCount){
        this.population = population;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.generation = generationCount;
    }
    @Override
    public void run()  {
        Random r = new Random(SEED + generation);
        for (int i = indexStart; i < indexEnd; i++) {
            Ball b = population.get(i);
            double tempDouble = r.nextDouble();
            if (tempDouble < MUTATION_RATE) {
                b.mutate(tempDouble * 10, r);
            }
        }
        try {
            BARRIER.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

    }
}
