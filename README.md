# BerryNgine

BerryNgine is a small 2D game engine written in Java. It renders into an ARGB
pixel buffer and scales that buffer to a Swing window. It has no external
dependencies.

The default renderer records drawing commands in source order. At the end of
each frame, it splits the framebuffer into horizontal tiles and renders those
tiles on multiple cores. Every tile processes commands in the order they were
recorded, so overlapping shapes and blended sprites keep their intended order.

## Build and run

Use a JDK with `javac` and `jar` on `PATH`. On Windows:

```bat
build.bat
build.bat test
build.bat benchmark 1000 4
```

`build.bat` writes `build\berryngine-<branch-name>.jar`. `test` launches the
included scene and shows FPS in the window title. `benchmark` runs the same
scene without a window and prints milliseconds per frame and a framebuffer
checksum. Its optional arguments are frame count and thread count.

The original immediate renderer is preserved on the
`immediate-render-baseline` branch. Build that branch to get
`build\berryngine-immediate-render-baseline.jar`. It has the same test scene and
batch interface. The `threaded-render-pipeline` branch tracks development of
the new renderer; `main` uses the threaded renderer by default.

## Start a scene

```java
import berryngine.*;

public final class Demo implements Scene {
    private PixelGraphics player;
    private final Vec2 position = new Vec2(80, 60);

    public static void main(String[] args) {
        GameWindow.builder("Demo", 320, 180)
                .scale(3)
                .renderThreads(4)
                .targetFps(60)
                .run(new Demo());
    }

    public void onSceneEnter(GameWindow gw) {
        player = ShapeGenerator.filledCircle(12, Color.ORANGE);
    }

    public void onSceneExit(GameWindow gw) { }

    public void update(GameWindow gw, float dt) {
        position.x += 20 * dt;
    }

    public void render(GameWindow gw, ThreadedPixelGraphics pg) {
        pg.clear(Color.DARK_BLUE);
        pg.fillGradientRect(0, 0, pg.width, pg.height,
                Color.MIDNIGHT_BLUE, Color.BLACK, false);
        pg.drawImageBlended(player, position);
        pg.renderStringCached(SpriteSheetFont.ABLE4,
                "BERRYNGINE", 8, 8, Color.WHITE);
    }
}
```

`Scene.render` receives a `ThreadedPixelGraphics`. It extends `PixelGraphics`,
so it can be passed to APIs that accept a `PixelGraphics`. The game loop calls
`beginFrame()`, invokes the scene, calls `execute()`, then presents the finished
buffer. Scene code normally only records draw calls. The default thread count
is `Runtime.getRuntime().availableProcessors()`; `.renderThreads(1)` runs the
same command path on the game loop thread.

## Drawing API

The framebuffer API includes the methods below. Each call appears in the
final image in the order it was made.

| Task | Methods |
| --- | --- |
| Pixels and lines | `setPixel`, `blendPixel`, `blendPixelGlobal`, `drawLine`, `drawHorizontalLine`, `drawVLine`, `drawTaperedLine` |
| Rectangles and gradients | `fillRect`, `fillRectBlended`, `drawRect`, `fillGradientRect`, `drawVerticalGradient`, `fillRect(..., Paint)` |
| Images | `drawImage`, `drawImageBlended`, `drawImageTinted`, `drawImageFlipped`, `drawImageScaled`, `drawImageScaledBlended` |
| Text | `renderChar`, `renderString`, `renderStringCached` |
| State and effects | `setClip`, `clearClip`, `setGlobalAlpha`, `applyVignette`, `effect` |
| World coordinates | `drawImageWorld`, `drawImageBlendedWorld`, `fillRectWorld`, `drawRectWorld`, `drawLineWorld`, `setPixelWorld` |

Image methods accept either `PixelGraphics` textures or raw `int[]` pixels with
source dimensions where the original API did. Raw array overloads record the
array reference directly; they do not create a texture wrapper for each draw.
Keep the source pixels unchanged until `execute()` completes.

`setClip` and `setGlobalAlpha` affect later recorded commands. Their values are
captured when each command is recorded. `clear` always covers the whole
framebuffer. `drawImage` replaces nontransparent pixels; blended, tinted,
flipped, and scaled image variants use alpha blending.

The existing `IVec2` overloads are inherited. `Vec2` and `Vec3` overloads are
also available for common pixel, line, rectangle, gradient, image, text, clip,
and world-space calls. Screen-space float coordinates are truncated to integers
when recorded. `Vec3.z` does not set depth: command order controls layering.

### Text

`renderString` draws glyphs without building a string image on every call.
For text that repeats across frames, `renderStringCached` keeps a rendered
pixel array keyed by font instance, text, and color. The cache holds at most
256 strings or one million pixels and removes older entries as needed. Call
`clearStringCache()` if you change a font's glyphs or atlas after caching text.

```java
pg.renderString(BitmapFont.DEFAULT_8X9, "Score: " + score,
        8, 8, Color.WHITE);
pg.renderStringCached(SpriteSheetFont.START2P, "PAUSED",
        120, 80, Color.YELLOW);
```

### Camera and effects

`pg.getCamera()` holds the 2D camera. World-space calls transform coordinates
when recorded, so changing the camera later in the frame does not move already
recorded commands. The software cursor is drawn after the scene.

Some operations need a finished framebuffer. `effect` first executes queued
drawing, runs the callback on the complete buffer, then lets subsequent calls
start a new ordered drawing stage:

```java
pg.drawImageWorld(level, 0, 0);
pg.effect(buffer -> PostFX.grayscale(buffer));
pg.renderStringCached(BitmapFont.DEFAULT_8X9, "PAUSED",
        8, 8, Color.WHITE);
```

`fillRect(..., Paint)`, `applyVignette`, tapered lines, and some scaled or
background-filled text methods also use an ordered full-frame stage. Prefer
ordinary fills, gradients, sprites, and text for high-volume drawing.

## Texture and buffer ownership

`PixelGraphics` remains the class for in-memory textures and offscreen drawing.
Asset loaders, `ShapeGenerator`, fonts, and atlases return these textures. A
`ThreadedPixelGraphics` is tied to the window's framebuffer. Its `setBuffer`
method rejects rebinding because the window presents the original pixel array.

Inherited `pixels`, `width`, and `height` fields are public. Direct reads or
writes to `pixels` bypass the command queue. Call `execute()` before reading
the framebuffer; use drawing methods to change it so ordering stays defined.
`getPixel`, `getSubImage`, `toBufferedImage`, `scale`, and `scaleTo` execute
queued commands before reading. Do not resize or replace the window's pixel
array or change its dimensions.

## Other engine systems

The package also includes `SceneManager`, `GameLoop`, input, camera control,
audio playback and synthesis, animations, shape generation, QOI/QOA decoding,
fonts, color and vector math, and resource loading. All live under
`berryngine.*` and use only the standard JDK.
