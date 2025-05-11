package project;

import util.LogLevel;
import util.Logger;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;

public class Config {
    public static final int THREADS = Runtime.getRuntime().availableProcessors();
    public static CyclicBarrier BARRIER = new CyclicBarrier(THREADS + 1,  null);
//Logger.log("All threads passed BARRIER", LogLevel.Status)
    public static final boolean LOGGING = false;

    public static final int GENERATIONS = 500_000;
    public static final double HOLEPOS = 500000.0;
    public static final int POPSIZE = 1000;
    public static final int BEST_POP_TO_GET = 4;

    public static final double MUTATION_RATE = 0.2;
    public static final double CROSSOVER_RATE = 0.5;
    public static final double NOISE = 0.0006;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.006;

    public static final int GUIWidth = 600;
    public static final int GUIHeight = 400;

    public static final int SEED = 1;

    public static final int ANGLE_BOUND = 180;
    public static final int VELOCITY_BOUND = 2500;
    public static final int POSX_INIT_BOUND = 100;

    private static int OPTIMAL_REACHED = 0;
    public static boolean GUI_TOGGLE = false;

    public static void optimalToggle(){
        OPTIMAL_REACHED = Math.abs(OPTIMAL_REACHED - 1);
    }

    public static int getOptimalReached(){
        return OPTIMAL_REACHED;
    }
}
