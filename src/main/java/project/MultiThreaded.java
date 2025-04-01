package project;

import util.Helper;
import util.LogLevel;
import util.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

import static project.Config.*;

public class MultiThreaded implements Runnable{
    Random r;
    ArrayList<Ball> population;
    int indexStart, indexEnd;
    MultiThreaded(Random r, int indexStart, int indexEnd, ArrayList<Ball> population){
        this.r = r;
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
