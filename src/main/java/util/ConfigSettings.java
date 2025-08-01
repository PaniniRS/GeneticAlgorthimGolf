package util;

import project.Config;

public class ConfigSettings {
    public int generations;
    public double holePos;
    public int popSize;
    public int bestPopSize;
    public double mutationRate;
    public double crossoverRate;

    public ConfigSettings(int generations, double holePos, int popSize, int bestPopSize, double mutationRate, double crossoverRate) {
        this.generations = generations;
        this.holePos = holePos;
        this.popSize = popSize;
        this.bestPopSize = bestPopSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
    }

    public void apply() {
        Config.changeSettings(generations, holePos, popSize, bestPopSize, mutationRate, crossoverRate);
    }

    @Override
    public String toString() {
        return String.format("Gen=%d, HolePos=%.2f, Pop=%d, BestPop=%d, Mut=%.3f, Cross=%.3f",
                generations, holePos, popSize, bestPopSize, mutationRate, crossoverRate);
    }
}

