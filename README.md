# BerryNgine

BerryNgine is a small, dependency-free 2D game engine in Java. This branch uses
an ordered, tile-parallel software render pipeline. A scene records drawing
commands, and the engine renders screen tiles on multiple cores before showing
the frame. Commands within each tile run in their recorded order, so overlapping
shapes and transparent sprites have deterministic ordering.

## Build and compare

On Windows, run `build.bat` to create `build\berryngine-<branch>.jar`. Run
`build.bat test` to build and launch the included basic scene. The same scene
and batch interface are present on `main`, which uses the original immediate
renderer. Both scenes draw 400 rectangles, 400 blended sprites, and a line per
frame, with FPS shown in the title. Run with comparable window size and Java
settings when comparing branches.

For a display-independent comparison on this branch:

```bat
build.bat benchmark 300 4
```

The benchmark runs the test scene without a window and reports milliseconds
per frame and an output checksum. Pass `1` as the second argument for a serial
pipeline run. Use the same command on `main` for the original renderer.

## Scene API

```java
import berryngine.*;

public class Demo implements Scene {
    private PixelGraphics sprite;

    public static void main(String[] args) {
        GameWindow.builder("Demo", 640, 360)
                .scale(2)
                .renderThreads(4) // total, including the game loop thread
                .targetFps(60)
                .run(new Demo());
    }

    public void onSceneEnter(GameWindow gw) {
        sprite = ShapeGenerator.filledCircle(12, 0x88ff8800);
    }
    public void onSceneExit(GameWindow gw) { }
    public void update(GameWindow gw, float dt) { }
    public void render(GameWindow gw, ThreadedPixelGraphics pg) {
        pg.clear(Color.DARK_BLUE);
        pg.fillRect(20, 20, 80, 40, Color.BLUE);
        pg.drawImageBlended(sprite, 40, 30);
    }
}
```

The game loop calls `beginFrame()`, invokes `Scene.render`, calls `execute()`,
and only then presents the framebuffer. Scenes do not normally call those
methods themselves. The default render thread count is the number of available
processors. Set `.renderThreads(1)` for a serial comparison.

## Rendering rules

- `ThreadedPixelGraphics` is the frame-facing API. Its calls enqueue drawing
  commands. The source order of calls is the visible draw order.
- `PixelGraphics` remains an in-memory texture and asset buffer. Generate
  shapes, load textures, or draw into offscreen textures with it before using
  them in the render pipeline.
- Keep a texture's pixel array unchanged from the time it is queued until the
  frame has executed. Command coordinates, clip, and global alpha are captured
  as commands are recorded.
- `setClip`, `clearClip`, and `setGlobalAlpha` affect subsequent commands.
  `clear` always covers the whole framebuffer.
- `effect(pg -> PostFX.grayscale(pg))` executes preceding drawing first, applies
  the effect to the complete buffer, then lets later commands form the next
  ordered drawing stage. Existing `PostFX` functions can be used this way.
- The camera on `pg.getCamera()` transforms world-space drawing calls when
  those calls are recorded. The software cursor is rendered after scene calls.
- The render pipeline is owned by the game loop. Do not record commands into
  it from other threads or hold it past `render`.
- For repeated labels, use `renderStringCached(font, text, x, y, color)` with
  either `BitmapFont` or `SpriteSheetFont`. It reuses generated string images
  across frames and keeps at most 256 entries or one million cached pixels.
  Font identity, text, and color form the cache key. Call `clearStringCache()`
  after changing glyphs or an atlas used by a cached font.

The supported frame commands include pixels, blended pixels, filled and outline
rectangles, lines, images, blended images, scaled images, text, and common
world-space variants. Use `PixelGraphics` for texture preparation. Assets,
input, audio, animation, and utility classes remain available under
`berryngine.*`.

## Runtime requirements

A JDK with `javac` and `jar` on `PATH` is needed to build. The runtime uses
standard Java Swing, AWT, and Java Sound, with no external libraries.
