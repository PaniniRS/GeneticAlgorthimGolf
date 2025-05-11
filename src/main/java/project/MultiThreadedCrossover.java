package project;

import util.Helper;
import util.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;

import static project.Config.*;

public class MultiThreadedCrossover implements Callable<List<Ball>> {
    Random r;
    ArrayList<Ball> population;
    ArrayList<Ball> localNewPop;
    int indexStart, indexEnd;
    public MultiThreadedCrossover(int indexStart, int indexEnd, ArrayList<Ball> population) {
        this.population = population;
        this.localNewPop = new ArrayList<Ball>();
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
    }
    @Override
    public ArrayList<Ball> call()  {
        for (int i = indexStart; i < indexEnd; i++) {
            Random r = new Random(SEED + i);
            Ball b = population.get(i);
            if (r.nextDouble() < CROSSOVER_RATE) {
                localNewPop.add(b.crossover(Helper.selectRandom(population, r), r));
            } else {
                localNewPop.add(b);
            }
        }
        return localNewPop;
    }
}
