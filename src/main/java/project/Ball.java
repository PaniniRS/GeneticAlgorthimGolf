package project;

public class Ball {
    private double posX, posY, velocity, angle;

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

    public void updateBall(){
        posX += velocity * Math.cos(Math.toRadians(angle)) * GeneticGolf.TICK;
        posY += velocity * Math.sin(Math.toRadians(angle)) * GeneticGolf.TICK - 0.5 * GeneticGolf.GRAVITY * Math.pow(GeneticGolf.TICK, 2);
        velocity -= GeneticGolf.DRAG;
        if (posY < 0) posY = 0;
    }
}
