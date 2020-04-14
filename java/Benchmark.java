
import java.awt.*;
import java.awt.event.*;

public class Benchmark {

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
      int x = W / 2;
      int y = H / 2;
      int r = Math.min(H, W) / 5;
      // eye ball
      g.setColor(Color.GRAY);
      g.fillOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));
      // eye pupil
      int rp1 = r / 5;
      int rp2 = r / 7;
      //g.setColor(new Color(0x654321));
      g.setColor(Color.RED);
      g.fillOval((int) (x - rp1), (int) (y - rp1), (int) (rp1 * 2), (int) (rp1 * 2));
      g.setColor(Color.BLACK);
      g.fillOval((int) (x - rp2), (int) (y - rp2), (int) (rp2 * 2), (int) (rp2 * 2));
      // eye lid
      double eye_opening = t1 % 1000 / 1000.0;
      if(eye_opening < .5) {
        eye_opening *= 2;
      } else {
        eye_opening = 1 - (eye_opening - .5) * 2;
      }
      int h = (int)(2*r * eye_opening);
      Graphics2D clip = (Graphics2D)g.create();
      clip.clip(new java.awt.geom.Rectangle2D.Double(x - r, y - r, r * 2, h));
      clip.setColor(Color.PINK);
      clip.fillOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));
      clip.dispose();
      // eye outline
      g.setColor(Color.BLACK);
      g.drawOval((int) (x - r), (int) (y - r), (int) (r * 2), (int) (r * 2));
      // eye lid outline
      int h2 = Math.abs(r-h);
      int rl = (int)(Math.sqrt(r*r - h2*h2));
      g.drawLine(x - rl, y - r + h, x + rl, y - r + h);
      // eye lashes?

      // flush our drawing to the screen
      David.flushCanvas(canvas);
    }

    // if the loop ended, close the canvas
    David.closeCanvas(canvas);
  }

}
