package project;

public class GeneticReturn {
    Ball bestFitness;
    int endGeneration;
    long time;
    
    GeneticReturn(Ball bestFitness, int endGeneration, long time) {
        this.bestFitness = bestFitness;
        this.endGeneration = endGeneration;
        this.time = time;
    }
}
