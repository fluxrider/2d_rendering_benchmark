// Written by David Lareau
// gcc benchmark.c `pkg-config --cflags --libs sdl2 cairo` -lm

#include <stdio.h>
#include <SDL2/SDL.h>
#include <cairo.h>
#include <time.h>
#include <inttypes.h>
#include <math.h>

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
  int h = (int)(2*r * lid);
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
  int rl = (int)(sqrt(r*r - h2*h2));
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
  if(low > high) { printf("Error in random_inclusive. Low > High. %d %d\n", low, high); return low; }
  return rand() % (high - low + 1) + low;
}

int main(int argc, char * argv[]) {
  printf("Begin\n");
  random_init();

  // because of the way I handle cleanup, all resource must be initialized early.
  SDL_Window * window = NULL;
  SDL_Renderer * renderer = NULL;
  SDL_Texture * texture = NULL;

  // sdl
  if(SDL_Init(SDL_INIT_EVERYTHING)) {
    fprintf(stdout, "SDL_Init: %s\n", SDL_GetError());
    goto end;
  } else {
    fprintf(stdout, "SDL_Init()\n");
  }

  // display
  int W = 800;
  int H = 450;
  int w = W;
  int h = H;
  window = SDL_CreateWindow("Eyes", 10, 10, W, H, SDL_WINDOW_RESIZABLE);
  if(!window) {
    fprintf(stdout, "SDL_CreateWindow: %s\n", SDL_GetError());
    goto end;
  }
  renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
  if(!renderer) {
    fprintf(stdout, "SDL_CreateRenderer: %s\n", SDL_GetError());
    goto end;
  }
  SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "linear");
  SDL_RenderSetLogicalSize(renderer, W, H);

  // render cairo to dynamic texture
  texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, w, h);
  if(!texture) {
    fprintf(stdout, "SDL_CreateTexture: %s\n", SDL_GetError());
    goto end;
  }
  // enable alpha blending when drawing this offscreen texture somewhere
  if(SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_BLEND)) {
    fprintf(stdout, "SDL_SetTextureBlendMode: %s\n", SDL_GetError());
    goto end;
  }

  // start fully transparent
  int pitch = w * 4;
  void * pixels = malloc(pitch * h);
  memset(pixels, 0x00, pitch * h);
  cairo_surface_t * surface = cairo_image_surface_create_for_data (
      pixels,
      CAIRO_FORMAT_ARGB32,
      w,
      h,
      pitch
  );
  cairo_t * g = cairo_create(surface);
  
  // game loop
  int render = 1;
  int frame_count = 0;
  uint64_t t0 = currentTimeMillis();
  while(1) {
    // time flow
    uint64_t t1 = currentTimeMillis();
    double delta_time = (t1 - t0) / 1000.0;
    //if (delta_time < (1.0 / 10)) continue; // commented out, don't cap
    t0 = t1;
    // fps
    frame_count++;
    printf("%d: %f\n", frame_count, 1 / delta_time);

    // events loop
    SDL_Event event;
    while(SDL_PollEvent(&event)) {
      // system events
      if(event.type == SDL_QUIT) {
        goto end;
      }
      // keyboard
      else if(event.type == SDL_KEYDOWN) {
        switch(event.key.keysym.sym) {
          case SDLK_ESCAPE:
            goto end;
        }
      }
    }

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
    // cairo flush
    cairo_surface_flush(surface);
    if(SDL_UpdateTexture(texture, NULL, pixels, pitch)) {
      fprintf(stdout, "SDL_UpdateTexture: %s\n", SDL_GetError());
      goto end;
    }

    // render scene
    if(render) {
      SDL_SetRenderDrawColor(renderer, 100, 100, 100, 255);
      SDL_RenderClear(renderer);

      // image
      SDL_Rect dst = {0, 0, 800, 450};
      SDL_RenderCopy(renderer, texture, NULL, &dst);

      // flush
      SDL_RenderPresent(renderer);
    }
  }

  cairo_destroy(g);
  cairo_surface_destroy(surface);
  free(pixels);

end:
  // display
  if(texture) SDL_DestroyTexture(texture);
  if(renderer) SDL_DestroyRenderer(renderer);
  if(window) SDL_DestroyWindow(window);

  // sdl
  if(SDL_WasInit(SDL_INIT_EVERYTHING)) {
    fprintf(stdout, "SDL_Quit()\n");
    SDL_Quit();
  }

  printf("Over and out\n");
  return 0;
}
