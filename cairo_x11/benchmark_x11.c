// gcc benchmark_x11.c -Wall $(pkg-config --libs --cflags cairo x11) -lm
#include <stdio.h>
#include <stdlib.h>
#include <cairo-xlib.h>
#include <X11/Xlib.h>
#include <time.h>
#include <inttypes.h>
#include <math.h>
#include <stdbool.h>

#define XK_MISCELLANY
#include <X11/keysymdef.h>

void draw_eye(cairo_t * g, int x, int y, int r, double lid) {
  // eye ball
  cairo_set_source_rgb(g, .5, .5, .5);
  cairo_arc(g, x, y, r, 0, M_PI * 2);
  cairo_fill(g);
  // eye pupil
  int rp1 = r / 5;
  int rp2 = r / 7;
  cairo_set_source_rgb(g, .5, 0, 0);
  cairo_arc(g, x, y, rp1, 0, M_PI * 2);
  cairo_fill(g);
  cairo_set_source_rgb(g, 0, 0, 0);
  cairo_arc(g, x, y, rp2, 0, M_PI * 2);
  cairo_fill(g);
  // eye lid
  int h = 2*r * lid;
  cairo_save(g);
  cairo_rectangle(g, x - r, y - r, r * 2, h);
  cairo_clip(g);
  cairo_set_source_rgb(g, 1, .89, .82);
  cairo_arc(g, x, y, r, 0, M_PI * 2);
  cairo_fill(g);
  cairo_restore(g);
  // eye outline
  cairo_set_source_rgb(g, 0, 0, 0);
  cairo_arc(g, x, y, r, 0, M_PI * 2);
  // eye lid outline
  int h2 = fabs(r-h);
  int rl = sqrt(r*r - h2*h2);
  cairo_move_to(g, x - rl, y - r + h);
  cairo_line_to(g, x + rl, y - r + h);
  cairo_close_path(g);
  // eye lashes?
  // commit outlines
  cairo_stroke(g);
}

uint64_t currentTimeMillis() {
  struct timespec tp;
  if(clock_gettime(CLOCK_MONOTONIC, &tp) == -1) { perror("read"); exit(1); }
  return tp.tv_sec * 1000 + tp.tv_nsec / 1000000;
}

void random_init() {
  srand(time(NULL));
}

int random_inclusive(int low, int high) {
  if(low == high) return low;
  if(low > high) { printf("Error in random_inclusive. Low > High. %d %d\n", low, high); exit(1); }
  return rand() % (high - low + 1) + low;
}

int handler(Display * d, XErrorEvent * e) {
  printf("X11 ERROR CAUGHT: %d\n", e->error_code);
  exit(1);
  return 0;
}

int main(int argc, char** argv) {
  random_init();
  int W = 800;
  int H = 450;

  // https://stackoverflow.com/questions/33385243/cairo-c-program-wont-draw-to-x11-window
  XSetErrorHandler(handler);
  Display * display = XOpenDisplay(NULL); if(display == NULL) { printf("XOpenDisplay\n"); exit(1); }
  int screen = DefaultScreen(display);
  Drawable window = XCreateSimpleWindow(display, DefaultRootWindow(display), 0, 0, W, H, 0, 0, 0);
  XSelectInput(display, window, ButtonPressMask | KeyPressMask | KeyReleaseMask);
  XMapWindow(display, window);
  cairo_surface_t* surface = cairo_xlib_surface_create(display, window, DefaultVisual(display, screen), W, H);
  cairo_t* g = cairo_create(surface);

  int frame_count = 0;
  uint64_t t0 = currentTimeMillis();
  bool running = true;
  while(running) {
    // events
    while(XPending(display)) {
      XEvent event;
      XNextEvent(display, &event);
      switch(event.type) {
        // keyboard
        case KeyRelease: {
          KeySym key = XLookupKeysym(&event.xkey, 0);
          switch(key) {
            case XK_Escape: running = false; break;
          }
          break;
        }
        // mouse
        case ButtonPress:
          printf("You pressed a button at (%d,%d)\n", event.xbutton.x, event.xbutton.y);
          break;
      }
    }

    // time flow
    uint64_t t1 = currentTimeMillis();
    double delta_time = (t1 - t0) / 1000.0;
    if (delta_time < (1.0 / 60)) continue; // commented out, don't cap
    t0 = t1;
    // fps
    frame_count++;
    printf("%d: %f\n", frame_count, 1 / delta_time);

    // cairo clear
    cairo_set_source_rgb(g, 1, 1, 1);
    cairo_paint(g);
    
    // eyes (crazy random bg)
    for(int i = 0; i < 1000; i++) {
      int r = random_inclusive(0, fmin(H, W) / 5);
      int x = random_inclusive(0, W + 2*r) - r;
      int y = random_inclusive(0, H + 2*r) - r;
      double lid = random_inclusive(0, 1000) / 1000.0;
      draw_eye(g, x, y, r, lid);
    }

    // eye (animated, centered)
    double lid = t1 % 1000 / 1000.0;
    if(lid < .5) {
      lid *= 2;
    } else {
      lid = 1 - (lid - .5) * 2;
    }
    draw_eye(g, W / 2, H / 2, fmin(H, W) / 5, lid);

    // flush
    cairo_surface_flush(surface);
    XFlush(display);
  }

  // cleanup
  cairo_surface_destroy(surface);
  XDestroyWindow(display, window);
  XCloseDisplay(display);
  return 0;
}
