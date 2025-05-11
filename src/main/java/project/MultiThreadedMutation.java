package project;

import util.Logger;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

import static project.Config.BARRIER;
import static project.Config.MUTATION_RATE;

public class MultiThreadedMutation implements Runnable{
    Random r;
    ArrayList<Ball> population;
    int indexStart, indexEnd;
    MultiThreadedMutation(Random r, int indexStart, int indexEnd, ArrayList<Ball> population){
        this.r = r;
        this.population = population;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
    }
    @Override
    public void run()  {
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
