package project;

import util.Logger;

import java.util.concurrent.CyclicBarrier;

public class Config {
    public static final int THREADS = Runtime.getRuntime().availableProcessors();
    public static CyclicBarrier BARRIER = new CyclicBarrier(THREADS + 1,  null);

    public static int GENERATIONS = 500_000;
    public static double HOLEPOS = 500000.0;
    public static int POPSIZE = 1000;
    public static int BEST_POP_TO_GET = 4;

    public static double MUTATION_RATE = 0.2;
    public static double CROSSOVER_RATE = 0.5;
    public static double NOISE = 0.0006;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.006;
    public static final double GRAVITY_DOWN_VECTOR = 0.5 * GRAVITY * (TICK*TICK);

    public static final int GUIWidth = 600;
    public static final int GUIHeight = 400;

    public static final int SEED = 1;

    public static final int ANGLE_BOUND = 180;
    public static final int VELOCITY_BOUND = 2500;
    public static final int POSX_INIT_BOUND = 100;

    private static int OPTIMAL_REACHED = 0;
    public static boolean GUI_TOGGLE = false;
    public static final int GUI_DRAW_STEPS = 125;

    public static void optimalToggle(){
        OPTIMAL_REACHED = Math.abs(OPTIMAL_REACHED - 1);
    }

    public static int getOptimalReached(){
        return OPTIMAL_REACHED;
    }

    public static void changeSettings (int generations, double holePos, int popsize, int bestPopSize, double mutationRate, double crossoverRate) {
        GENERATIONS = generations;
        HOLEPOS = holePos;
        POPSIZE = popsize;
        BEST_POP_TO_GET = bestPopSize;
        MUTATION_RATE = mutationRate;
        CROSSOVER_RATE = crossoverRate;
    }
}
