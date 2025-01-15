package project;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import static project.Config.*;

public class GUI extends JPanel {
    private ArrayList<Ball> balls = new ArrayList<>();
    private ArrayList<Ball> elites = new ArrayList<>();
    private Label generationLabel = new Label();

    public GUI(){
        this.add(generationLabel);
    }

    public void updateVisualization(ArrayList<Ball> balls, ArrayList<Ball> elites, int generation) {
        this.balls = new ArrayList<>(balls);
        this.elites = new ArrayList<>(elites);
        this.generationLabel = new Label("Gen: " + generation);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int ballDiameter = 16;

        //Draw background
        g.setColor(new Color(124, 233, 252, 120));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw the generation label
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 255, 255));
        g.drawString(generationLabel.getText(), getWidth() / 2 - 60, getHeight() / 10);

        // Draw grass
        g.setColor(new Color(40, 170, 30));
        g.fillRect(0, getHeight() - getHeight()/3, getWidth(), getHeight() / 3);

        // Draw hole
        g.setColor(new Color(20, 50, 15));
        int holeX = (int) (HOLEPOS % GUIWidth); // Scale correctly
        int holeY = getHeight() - getHeight()/3;
        g.fillRect(holeX, holeY, 24, 12);

        //Draw hole flag
        g.setColor(Color.red);
        g.fillRect(holeX + ballDiameter, holeY - 25, 20, 10);

        g.setColor(Color.black);
        g.fillRect(holeX + ballDiameter, holeY - 25, 4, 25);

        // Draw balls
        for (Ball ball : balls) {
            int x = (int) (Math.round(ball.getPosX()) % GUIWidth ); // Scale correctly
            int y = getHeight() - getHeight()/3 - ballDiameter;
            g.setColor(new Color(255, 255, 255));
            g.fillOval(x, y, ballDiameter, ballDiameter);
        }

        // Draw elite balls
        for (Ball elite : elites) {
            int x = (int) (elite.getPosX() % GUIWidth); // Scale correctly
            int y = getHeight() - getHeight()/3 - ballDiameter;
            g.setColor(new Color(5, 100, 190));
            g.fillOval(x, y, ballDiameter, ballDiameter);
        }
    }
}