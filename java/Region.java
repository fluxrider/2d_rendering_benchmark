
import java.awt.Graphics;

public class Region {

  private double x;
  private double y;
  public double z;
  private double w;
  private double h;
  public double scale = 1;
  public int mode = DEFAULT_MODE;

  public static final int LEFT = 0b00000001;
  public static final int RIGHT = 0b00000010;
  public static final int TOP = 0b00000100;
  public static final int BOTTOM = 0b00001000;
  public static final int FIT = 0b00010000;
  public static final int DEBUG = 0b00100000;
  public static final int DEFAULT_MODE = FIT;// | DEBUG;

  public static void init(Region buffer, double x, double y, double w, double h) {
    buffer.x = x;
    buffer.y = y;
    buffer.w = w;
    buffer.h = h;
    buffer.scale = 1;
    buffer.mode = DEFAULT_MODE;
  }

  public static void init(Region buffer, double x, double y, double w, double h, int mode) {
    buffer.x = x;
    buffer.y = y;
    buffer.w = w;
    buffer.h = h;
    buffer.scale = 1;
    buffer.mode = mode;
  }

  public String toString() {
    return String.format("Region %.2f x %.2f at (%.2f,%.2f)", w, h, x, y);
  }

  public static void renderRegion(Graphics g, Region region) {
    g.setColor(java.awt.Color.RED);
    int x = (int) region.getScaledX();
    int y = (int) region.getScaledY();
    int w = (int) region.getScaledW();
    int h = (int) region.getScaledH();
    g.drawRect(x, y, w, h);
    g.drawLine(x, y, x + w, y + h);
  }

  public void dup(Region buffer) {
    buffer.x = x;
    buffer.y = y;
    buffer.z = z;
    buffer.w = w;
    buffer.h = h;
    buffer.scale = scale;
    buffer.mode = mode;
  }

  // Methods
  public double getScaledX() {
    if ((mode & LEFT) != 0) return x;
    if ((mode & RIGHT) != 0) return x + (w - getScaledW());
    return x + w / 2 - getScaledW() / 2;
  }

  public double getScaledY() {
    if ((mode & TOP) != 0) return y;
    if ((mode & BOTTOM) != 0) return y + (h - getScaledH());
    return y + h / 2 - getScaledH() / 2;
  }

  public double getScaledW() {
    return w * scale;
  }

  public double getScaledH() {
    return h * scale;
  }

  public boolean hit(double x, double y) {
    //double x1, double y1, double w1, double h1, double x2, double y2, double w2, double h2
    return collides_2D(x, y, 0, 0, getScaledX(), getScaledY(), getScaledW(), getScaledH());
  }

  public static void fit(Region buffer, double w, double h, Region frame) {
    fit(buffer, w, h, frame.getScaledW(), frame.getScaledH(), frame.getScaledX(), frame.getScaledY(), FIT);
  }

  public static void fit(Region buffer, double w, double h, Region frame, int mode) {
    fit(buffer, w, h, frame.getScaledW(), frame.getScaledH(), frame.getScaledX(), frame.getScaledY(), mode);
  }

  public static void fit(Region buffer, double w, double h, double W, double H) {
    fit(buffer, w, h, W, H, 0, 0, FIT);
  }

  public static void fit(Region buffer, double w, double h, double W, double H, double X, double Y, int mode) {
    double offsetX = 0;
    double offsetY = 0;
    double aspectW = w;
    double aspectH = h;

    // scale to fit
    if ((mode & FIT) != 0) {
      double a = W / H;
      double A = w / h;
      if (a / A > 1) {
        aspectW = w * H / h;
        aspectH = H;
      } else {
        aspectW = W;
        aspectH = h * W / w;
      }
    }

    // alignment
    if ((mode & BOTTOM) != 0) {
      offsetY = H - aspectH;
    } else if ((mode & TOP) != 0) {
      offsetY = 0;
    } else {
      offsetY = (H - aspectH) / 2;
    }

    if ((mode & LEFT) != 0) {
      offsetX = 0;
    } else if ((mode & RIGHT) != 0) {
      offsetX = W - aspectW;
    } else {
      offsetX = (W - aspectW) / 2;
    }

    buffer.x = offsetX + X;
    buffer.y = offsetY + Y;
    buffer.w = aspectW;
    buffer.h = aspectH;
  }

  // Methods (neo)
  public static Region create(double x, double y, double w, double h) {
    Region neo = new Region();
    init(neo, x, y, w, h);
    return neo;
  }

  public static Region create(double x, double y, double w, double h, int mode) {
    Region neo = new Region();
    init(neo, x, y, w, h, mode);
    return neo;
  }

  public Region dup() {
    Region neo = new Region();
    dup(neo);
    return neo;
  }

  public static Region fit(double w, double h, Region frame) {
    return fit(w, h, frame.getScaledW(), frame.getScaledH(), frame.getScaledX(), frame.getScaledY(), FIT);
  }

  public static Region fit(double w, double h, Region frame, int mode) {
    return fit(w, h, frame.getScaledW(), frame.getScaledH(), frame.getScaledX(), frame.getScaledY(), mode);
  }

  public static Region fit(double w, double h, double W, double H) {
    return fit(w, h, W, H, 0, 0, FIT);
  }

  public static Region fit(double w, double h, double W, double H, double X, double Y, int mode) {
    Region neo = new Region();
    fit(neo, w, h, W, H, X, Y, mode);
    return neo;
  }

  // collision test
  public static boolean collides_1D(double x1, double x2, double y1, double y2) {
    double x_min = Math.min(x1, x2);
    double x_max = Math.max(x1, x2);
    double y_min = Math.min(y1, y2);
    double y_max = Math.max(y1, y2);
    return x_max >= y_min && y_max >= x_min;
  }

  public static boolean collides_2D(double x1, double y1, double w1, double h1, double x2, double y2, double w2, double h2) {
    return collides_1D(x1, x1 + w1, x2, x2 + w2) && collides_1D(y1, y1 + h1, y2, y2 + h2);
  }

}
