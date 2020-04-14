
import java.awt.*;
import java.awt.event.*;

public class Benchmark {

  public static void draw_eye(Graphics g, int x, int y, int r, double lid) {
    // eye ball
    g.setColor(Color.GRAY);
    g.fillOval(x - r, y - r, r * 2, r * 2);
    // eye pupil
    int rp1 = r / 5;
    int rp2 = r / 7;
    //g.setColor(new Color(0x654321));
    g.setColor(Color.RED);
    g.fillOval(x - rp1, y - rp1, rp1 * 2, rp1 * 2);
    g.setColor(Color.BLACK);
    g.fillOval(x - rp2, y - rp2, rp2 * 2, rp2 * 2);
    // eye lid
    int h = (int)(2*r * lid);
    Graphics2D clip = (Graphics2D)g.create();
    clip.clip(new java.awt.geom.Rectangle2D.Double(x - r, y - r, r * 2, h));
    clip.setColor(Color.PINK);
    clip.fillOval(x - r, y - r, r * 2, r * 2);
    clip.dispose();
    // eye outline
    g.setColor(Color.BLACK);
    g.drawOval(x - r, y - r, r * 2, r * 2);
    // eye lid outline
    int h2 = Math.abs(r-h);
    int rl = (int)(Math.sqrt(r*r - h2*h2));
    g.drawLine(x - rl, y - r + h, x + rl, y - r + h);
    // eye lashes?
  }

  public static void main(String[] args) {
    // create window
    int W = 800;
    int H = 450;
    Object canvas = David.createCanvas(W, H, "Eyes", true, true);
    Graphics g = David.getGraphics(canvas);

    // game loop
    long t0 = System.currentTimeMillis();
    boolean running = true;
    int frame_count = 0;
    while (running) {
      // time flow
      long t1 = System.currentTimeMillis();
      double delta_time = (t1 - t0) / 1000.0;
      //if (delta_time < (1.0 / 10)) continue; // commented out, don't cap
      t0 = t1;
      // fps
      frame_count++;
      System.out.println(frame_count + ": " + 1 / delta_time);

      // eye
      double lid = t1 % 1000 / 1000.0;
      if(lid < .5) {
        lid *= 2;
      } else {
        lid = 1 - (lid - .5) * 2;
      }
      draw_eye(g, W / 2, H / 2, Math.min(H, W) / 5, lid);
 
      // flush our drawing to the screen
      David.flushCanvas(canvas);
    }

    // if the loop ended, close the canvas
    David.closeCanvas(canvas);
  }

}
