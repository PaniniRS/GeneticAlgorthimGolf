package project;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;

import static project.Config.*;

public class MultiThreadedFitness implements Runnable{
    ArrayList<Ball> population;
    int indexStart, indexEnd;
    public MultiThreadedFitness(int indexStart, int indexEnd, ArrayList<Ball> population){
        this.population = population;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
    }
    @Override
    public void run()  {
        for (int i = indexStart; i < indexEnd; i++) {
            //Fitness
            Ball ball = population.get(i);
            ball.setFitness(ball.evaluateFitness());
        }//indexLoop
        try {
            BARRIER.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

    }
}
