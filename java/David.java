
import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class David {

  // RuntimeException Wrappers
  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  // Resource Wrappers
  public static BufferedImage loadImage(String path) {
    try {
      return javax.imageio.ImageIO.read(new File(path));
    } catch (IOException e) {
      System.err.println("Problem loading " + path + ". " + e);
      System.exit(1);
      return null;
    }
  }

  public static Font loadFont(String path, double size) {
    try {
      Font font = Font.createFont(java.awt.Font.TRUETYPE_FONT, new File(path));
      font = font.deriveFont((float) size);
      return font;
    } catch (IOException | FontFormatException e) {
      System.err.println("Problem loading " + path + ". " + e);
      System.exit(1);
      return null;
    }
  }

  public static FontMetrics getFontMetrics(Font font) {
    return new Canvas().getFontMetrics(font);
  }

  // Canvas
  private static class DavidCanvas {
    private JPanel panel;
    private BufferedImage backbuffer;
    private BufferedImage frontbuffer;
    private JFrame frame;

    private void update() {
      synchronized (frontbuffer) {
        Graphics2D g = (Graphics2D) frontbuffer.getGraphics();
        highQuality(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, frontbuffer.getWidth(), frontbuffer.getHeight());
        g.drawImage(backbuffer, 0, 0, null);
      }
      panel.repaint();
      Thread.yield();
    }
  }

  private static Map<Integer, Boolean> keys;

  public static boolean isHeld(int key_code) {
    if (keys == null) return false;
    Boolean pressed = keys.get(key_code);
    if (pressed == null) return false;
    return pressed;
  }

  public static void registerKeyboardEventQueue(final List<Object> events) {
    if (keys == null) keys = new TreeMap<>();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
      public boolean dispatchKeyEvent(KeyEvent e) {
        // just keep track of pressed/released state
        if (events == null) {
          if (e.getID() == KeyEvent.KEY_PRESSED) {
            keys.put(e.getKeyCode(), true);
          } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            keys.put(e.getKeyCode(), false);
          }
        }
        // store all events
        else {
          synchronized (events) {
            events.add(e);
            if (e.getID() == KeyEvent.KEY_PRESSED) {
              keys.put(e.getKeyCode(), true);
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
              keys.put(e.getKeyCode(), false);
            }
          }
        }
        return false;
      }
    });
  }

  public static void registerCanvasMouseEventQueue(Object _canvas, final List<Object> events) {
    DavidCanvas canvas = (DavidCanvas) _canvas;
    final Region fit = new Region();
    canvas.panel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        // map x,y
        int W = canvas.panel.getWidth();
        int H = canvas.panel.getHeight();
        // apect ratio blackbars
        Region.fit(fit, canvas.frontbuffer.getWidth(), canvas.frontbuffer.getHeight(), W, H);
        int x = (int) ((e.getX() - fit.getScaledX()) * canvas.frontbuffer.getWidth() / fit.getScaledW());
        int y = (int) ((e.getY() - fit.getScaledY()) * canvas.frontbuffer.getHeight() / fit.getScaledH());
        synchronized (events) {
          events.add(new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(), false, e.getButton()));
        }
      }
    });
  }

  public static Object createCanvas(int w, int h, String title, boolean kill_on_close, boolean smooth) {
    // center a window in the screen
    JFrame frame = new JFrame(title);
    frame.setSize(w, h);
    frame.setLocationRelativeTo(null);

    // create a backbuffer for drawing offline, and a front buffer to use when drawing the JPanel
    final BufferedImage backbuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final BufferedImage frontbuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

    // JPanel for our window
    JPanel panel = new JPanel() {
      private static final long serialVersionUID = 1L;

      public void paint(Graphics g) {
        synchronized (frontbuffer) {
          int W = this.getWidth();
          int H = this.getHeight();
          int w = frontbuffer.getWidth();
          int h = frontbuffer.getHeight();

          // always smooth on minification
          if (smooth || W < w || H < h) {
            highQuality((Graphics2D) g);
          } else {
            lowQuality((Graphics2D) g);
          }

          // respect aspect ratio (i.e. black bars)
          double a = w / (double) h;
          double A = W / (double) H;
          int offsetX = 0;
          int offsetY = 0;
          int aspectW = W;
          int aspectH = H;
          // top/down black bars
          if (a / A > 1) {
            aspectH = W * h / w;
            offsetY = (H - aspectH) / 2;
          }
          // left/right black bars
          else {
            aspectW = H * w / h;
            offsetX = (W - aspectW) / 2;
          }

          g.drawImage(frontbuffer, offsetX, offsetY, aspectW, aspectH, null);
        }
      }
    };

    // store everything in my opaque object
    DavidCanvas canvas = new DavidCanvas();
    canvas.panel = panel;
    canvas.backbuffer = backbuffer;
    canvas.frontbuffer = frontbuffer;
    canvas.frame = frame;

    // draw first frame (white)
    canvas.update();

    // show window
    frame.setContentPane(panel);
    frame.setVisible(true);
    if (kill_on_close) frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    return canvas;
  }

  public static void closeCanvas(Object _canvas) {
    DavidCanvas canvas = (DavidCanvas) _canvas;
    canvas.frame.dispatchEvent(new WindowEvent(canvas.frame, WindowEvent.WINDOW_CLOSING));
  }

  public static void syncCanvas(Object _canvas) {
    // show backbuffer, but don't clear it
    DavidCanvas canvas = (DavidCanvas) _canvas;
    canvas.update();
  }

  private static Color transparent = new Color(0, 0, 0, 0);

  public static void flushCanvas(Object _canvas) {
    // show backbuffer
    DavidCanvas canvas = (DavidCanvas) _canvas;
    canvas.update();
    // clear backbuffer
    int w = canvas.backbuffer.getWidth();
    int h = canvas.backbuffer.getHeight();
    Graphics2D g = (Graphics2D) canvas.backbuffer.getGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
    g.setColor(transparent);
    g.fillRect(0, 0, w, h);
  }

  // @return a Graphics context with all quality rendering setting on 
  public static Graphics2D getGraphics(Object _canvas) {
    Graphics2D g = (Graphics2D) ((DavidCanvas) _canvas).backbuffer.getGraphics();
    highQuality(g);
    return g;
  }

  private static Map<RenderingHints.Key, Object> hints;
  private static Map<RenderingHints.Key, Object> hints_low;
  static {
    hints = new HashMap<RenderingHints.Key, Object>();
    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    hints_low = new HashMap<RenderingHints.Key, Object>();
    hints_low.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    hints_low.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    hints_low.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    hints_low.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    hints_low.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    hints_low.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
  }

  public static void highQuality(Graphics g) {
    ((Graphics2D) g).addRenderingHints(hints);
  }

  public static void lowQuality(Graphics g) {
    ((Graphics2D) g).addRenderingHints(hints_low);
  }

}
