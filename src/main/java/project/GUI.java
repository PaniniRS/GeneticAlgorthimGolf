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

        // Draw grass
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, getHeight() / 2, getWidth(), getHeight() / 2);

        // Draw hole
        g.setColor(new Color(0, 9, 0));
        int holeX = (int) (HOLEPOS * getWidth() / POSX_INIT_BOUND);
        int holeY = getHeight() / 2 + 10;
        g.fillRect(holeX - 5, holeY - 5, 24, 10);

        // Draw balls
        for (Ball ball : balls) {
            int x = (int) (ball.getPosX() * getWidth() / POSX_INIT_BOUND);
            int y = getHeight() / 2 - 8;
            g.setColor(new Color(131, 125, 4));
            g.fillOval(x, y, 8, 8);
        }

        // Draw elite balls
        for (Ball elite : elites) {
            int x = (int) (elite.getPosX() * getWidth() / POSX_INIT_BOUND);
            int y = getHeight() / 2 - 8;
            g.setColor(new Color(128, 0, 128));
            g.fillOval(x, y, 8, 8);
        }
    }
}