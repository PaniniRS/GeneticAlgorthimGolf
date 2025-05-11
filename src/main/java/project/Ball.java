package project;
import util.Logger;

import java.util.Random;

import static project.Config.*;

public class Ball {
    public double posX, posY, velocity, angle, fitness;

    public Ball(double posX, double posY, double velocity, double angle) {
        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
    }

    public Ball(double posX, double posY, double velocity, double angle, double fitness) {
        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
        this.fitness = fitness;
    }
/////////////////////////////////////
    //RunFitness
    public double evaluateFitness(Random r){
        while(!isStopped()){
                updateBall();
            }
        double distance = Math.abs(posX - HOLEPOS);
        //Ball in hole
        if (distance == 0){
            return 2000;
        }

        //Calculate fitness when ball isn't in hole
        //TODO: Look at this calulation further
//        return 1.0 / (1.0 + distance) + r.nextDouble() * NOISE; //TODO: Make this determinstic somehow for both seq and parallel so it adds more fun to the generations
        return 1.0 / (1.0 + distance);
    }

    //CheckStopped
    public boolean isStopped(){
        return velocity <= 0 && posY <= 0;
    }

    //Used for calculating fitness
    public void updateBall(){
        posX += velocity * Math.cos(Math.toRadians(angle)) * TICK;
        posY += velocity * Math.sin(Math.toRadians(angle)) * TICK - 0.5 * GRAVITY * Math.pow(TICK, 2);
        velocity -= DRAG;
        if (posY < 0) posY = 0;
    }

    //Crossover
    public Ball crossover(Ball b1, Random r){
        double NposX = r.nextDouble() > 0.5 ? this.posX : b1.posX;
        double Nvelocity = r.nextDouble() > 0.5 ? this.velocity : b1.velocity;
        double Nangle = r.nextDouble() > 0.5 ? this.angle : b1.angle;
        return new Ball(NposX,0, Nvelocity, Nangle);
    }

    //Mutation
    public void mutate(double randomNumber, Random r){
        //30% for one gene to mutate
        if (randomNumber < 0.3){
            this.posX = r.nextDouble() * POSX_INIT_BOUND; //seems fishy 1:02am 8Jan
        } else if (randomNumber >= 0.3 &&  randomNumber < 0.6) {
            this.velocity = r.nextDouble() * VELOCITY_BOUND;
        }else{
            this.angle = r.nextDouble() * ANGLE_BOUND;
        }
    }

    //Getters setters
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitness(){
        return fitness;
    }

    public Ball copy(){
        return new Ball(posX, posY, velocity, angle, fitness);
    }

    public double getPosX() {
        return posX;
    }
}
