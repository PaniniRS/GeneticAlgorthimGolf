package project;
import static project.Config.*;

public class Ball {
    private double posX, posY, velocity, angle, fitness;

    public Ball(double posX, double posY, double velocity, double angle) {
        this.posX = posX;
        this.posY = posY;
        this.velocity = velocity;
        this.angle = angle;
    }

    //RunFitness
    public double evaluateFitness(){
        double fitness;
        while(!isStopped()){
                updateBall();
            }
        double distance = Math.abs(posX - HOLEPOS);
        //Ball in hole
        if (distance == 0){
            return Double.MAX_VALUE;
        }

        //Ball in starting area, ex 180 angle
//        if (posX <= GeneticGolf.POSX_INIT_BOUND){
//            return -10;
//        }
        //Calculate fitness when ball isn't in hole
        //TODO: Look at this calulation further
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
    public void crossover(Ball b1){
        this.posX = Math.random() > 0.5 ? this.posX : b1.posX;
        this.velocity = Math.random() > 0.5 ? this.velocity : b1.velocity;
        this.angle = Math.random() > 0.5 ? this.angle : b1.angle;
    }

    //Mutation
    public void mutate(double randomNumber){
        //RANDOM RESET Mutation, check this in the morning
        //30% for one gene to mutate
        if (randomNumber < 0.3){
            this.posX = Math.random() * POSX_INIT_BOUND; //seems fishy 1:02am 8Jan
        } else if (randomNumber >= 0.3 &&  randomNumber < 0.6) {
            this.velocity = Math.random() * VELOCITY_BOUND;
        }else{
            this.angle = Math.random() * ANGLE_BOUND;
        }
    }

    //Getters setters
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitness(){
        return fitness;
    }
}
