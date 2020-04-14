// Written by David Lareau
// Compile: gcc benchmark.c `pkg-config --cflags --libs sdl2 cairo`

#include <stdio.h>
#include <SDL2/SDL.h>
#include <cairo.h>
#include <time.h>
#include <inttypes.h>

uint64_t currentTimeMillis() {
  struct timespec tp;
  if(clock_gettime(CLOCK_MONOTONIC, &tp) == -1) { perror("read"); exit(1); }
  return tp.tv_sec * 1000 + tp.tv_nsec / 1000000;
}

int main(int argc, char * argv[]) {
  printf("Begin\n");

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
  {
    int w = 800;
    int h = 450;
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
    cairo_move_to(g, 128.0, 25.6);
    cairo_line_to(g, 230.4, 230.4);
    cairo_rel_line_to(g, -102.4, 0.0);
    cairo_curve_to(g, 51.2, 230.4, 51.2, 128.0, 128.0, 128.0);
    cairo_close_path(g);
    cairo_move_to(g, 64.0, 25.6);
    cairo_rel_line_to(g, 51.2, 51.2);
    cairo_rel_line_to(g, -51.2, 51.2);
    cairo_rel_line_to(g, -51.2, -51.2);
    cairo_close_path(g);
    cairo_set_line_width(g, 10.0);
    cairo_set_source_rgb(g, 0, 0, 1);
    cairo_fill_preserve(g);
    cairo_set_source_rgb(g, 0, 0, 0);
    cairo_stroke(g);
    cairo_destroy(g);
    cairo_surface_flush(surface);
    cairo_surface_destroy(surface);

    if(SDL_UpdateTexture(texture, NULL, pixels, pitch)) {
      fprintf(stdout, "SDL_UpdateTexture: %s\n", SDL_GetError());
      goto end;
    }
    free(pixels);

  }

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
