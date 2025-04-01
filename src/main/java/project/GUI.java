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
        final int panelHeight = getHeight();
        final int panelWidth = getWidth();
        double scale = (double)panelWidth / HOLEPOS;
        ///
        final int ballDiameter = 16;
        final int holeWidth = 24;
        final int flagPoleWidth = 4;
        ///
        final int grassHeight = panelHeight/3;
        final int generationLabelHeight = panelHeight / 10;
        final int groundLevelY = panelHeight - grassHeight;

        //Draw background
        g.setColor(new Color(124, 233, 252, 120));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw the generation label
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 255, 255));
        g.drawString(generationLabel.getText(), panelWidth / 2 - 60, generationLabelHeight);

        // Draw grass
        g.setColor(new Color(40, 170, 30));
        g.fillRect(0, groundLevelY, panelWidth, grassHeight);

        // Draw hole
        g.setColor(new Color(20, 50, 15));
        int holeX = panelWidth - holeWidth - flagPoleWidth;
        g.fillRect(holeX, groundLevelY, holeWidth, 12);

        //Draw hole flag
        g.setColor(Color.red);
        g.fillRect(holeX + ballDiameter, groundLevelY - 25, flagPoleWidth*5, 10);

        g.setColor(Color.black);
        g.fillRect(holeX + ballDiameter, groundLevelY - 25, flagPoleWidth, 25);


//        TODO: for some reason the balls get reset at the last generation
        // Draw balls
        for (Ball ball : balls) {
            int x = (int) (ball.getPosX() * scale ); // Scale correctly
            int y = getHeight() - getHeight()/3 - ballDiameter;
            g.setColor(new Color(255, 255, 255));
            g.fillOval(x, y, ballDiameter, ballDiameter);
        }

        // Draw elite balls
        for (Ball elite : elites) {
            int x = (int) (elite.getPosX() * scale); // Scale correctly
            int y = getHeight() - getHeight()/3 - ballDiameter;
            g.setColor(new Color(5, 100, 190));
            g.fillOval(x, y, ballDiameter, ballDiameter);
        }
    }
}