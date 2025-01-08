package project;

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
        double distance = Math.abs(posX - GeneticGolf.HOLEPOS);
        //Ball in hole
        if (distance == 0){
            return Double.MAX_VALUE;
        }
        //Calculate fitness when ball isn't in hole
        return 1.0 / (1.0 + distance);
    }

    //CheckStopped
    public boolean isStopped(){
        return velocity <= 0 && posY <= 0;
    }

    //Used for calculating fitness
    public void updateBall(){
        posX += velocity * Math.cos(Math.toRadians(angle)) * GeneticGolf.TICK;
        posY += velocity * Math.sin(Math.toRadians(angle)) * GeneticGolf.TICK - 0.5 * GeneticGolf.GRAVITY * Math.pow(GeneticGolf.TICK, 2);
        velocity -= GeneticGolf.DRAG;
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
            this.posX = Math.random() * GeneticGolf.POSX_INIT_BOUND; //seems fishy 1:02am 8Jan
        } else if (randomNumber >= 0.3 &&  randomNumber < 0.6) {
            this.velocity = Math.random() * GeneticGolf.VELOCITY_BOUND;
        }else{
            this.angle = Math.random() * GeneticGolf.ANGLE_BOUND;
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
