package project;

import java.util.Random;

public class Config {
    public static final int GENERATIONS = 500_000;
    public static final double HOLEPOS = 10000.0;
    public static final int POPSIZE = 1000;
    public static final int BEST_POP_TO_GET = 4;

    public static final double MUTATION_RATE = 0.2;
    public static final double CROSSOVER_RATE = 0.5;
    public static final double NOISE = 0.0006;

    public static final double GRAVITY = 9.8;
    public static final double DRAG = 0.5;
    public static final double TICK = 0.006;

    public static final int SEED = 1;

    public static final int ANGLE_BOUND = 180;
    public static final int VELOCITY_BOUND = 5000;
    public static final int POSX_INIT_BOUND = 1000;

    private static int OPTIMAL_REACHED = 0;

    public static final Random GLOBAL_RANDOM = new Random(SEED);

    public static void optimalToggle(){
        OPTIMAL_REACHED = Math.abs(OPTIMAL_REACHED - 1);
    }
    public static int getOptimalReached(){
        return OPTIMAL_REACHED;
    }
}
