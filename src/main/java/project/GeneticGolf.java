package project;
import java.util.Random;
import static project.Config.*;
import project.SingleThreaded;
import util.LogLevel;
import util.Logger;

public class GeneticGolf {
    static Random r = GLOBAL_RANDOM;

    public static void main(String[] args) {
        RunSingleThreaded(r);
    }


    private static void RunSingleThreaded(Random rand){
        long startTime = System.currentTimeMillis();
        new SingleThreaded().Run(rand);
        Logger.log("Time: " + (System.currentTimeMillis() - startTime) + " ms" + "\t"+ (System.currentTimeMillis() - startTime)/1000.00 + " s", LogLevel.Status);
    }
}//class
