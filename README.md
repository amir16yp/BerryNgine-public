# BerryNgine

BerryNgine is a small, dependency-free **2D game engine written in pure Java**. It draws into an ARGB integer pixel buffer, scales that buffer into a Swing window with nearest-neighbor upscaling, and ships everything you need for a retro-style game in a single package: scenes, input, camera, fonts, procedural shapes, post-processing, audio playback, and sound/music synthesis.

There is no OpenGL, no native library, no Maven/Gradle plugin required for the engine itself — only a JDK with `javac` and `jar`.

---

## Philosophy

BerryNgine is built around a few deliberate choices.

### Pixels are the truth

The engine does not hide the framebuffer behind a high-level scene graph or a GPU abstraction. A frame is an `int[]` of ARGB pixels (`0xAARRGGBB`). Textures, fonts, shapes, screenshots, and the window backbuffer are all the same kind of object: `PixelGraphics` (or a subclass that records into one).

If you can write a pixel, you can build a game. That keeps the mental model small and makes every drawing operation inspectable.

### Software rendering, on purpose

BerryNgine targets **software rasterization**: Bresenham lines, nearest-neighbor scaling, src-over alpha blending, and integer coordinates. That is not a compromise left over from missing GPU support — it is the aesthetic and technical target.

Software rendering gives you:

- Predictable pixel art (no GPU filtering surprises)
- Identical behavior across machines that can run a JVM
- Easy offscreen buffers, atlases, and procedural textures
- A codebase you can read end-to-end without graphics-driver lore

Presentation is intentionally dumb: scale the internal resolution to the window with nearest-neighbor and show it. Crisp pixels stay crisp.

### Order is layering

There is no z-buffer and no depth sort. **Draw order is layering.** Later commands sit on top of earlier ones. `Vec3` overloads exist for convenience when you already carry a z component, but `z` does not affect depth — only call order does.

This matches how classic 2D engines and immediate-mode UI think, and it keeps overlapping sprites and blended shapes honest.

### Record, then execute

The default renderer (`ThreadedPixelGraphics`) does not paint the moment you call `fillRect`. It **records** a command, then at the end of the frame **executes** the whole list across horizontal tiles on multiple cores. Every tile replays commands in the same recorded order, so parallel rendering does not scramble painter’s algorithm.

That split — *game thread records, workers rasterize* — is the central performance idea. Scene code stays simple; the engine parallelizes the pixel work.

### Zero dependencies, one package

Everything lives under `berryngine.*` and uses the standard JDK (`java.awt`, `javax.swing`, `javax.sound.sampled`, `javax.imageio`). Asset formats preferred by the engine (QOI images, QOA audio, PSF1 fonts, MIDI import) are decoded in-tree. You can drop the JAR next to your game and ship.

### Batteries for small games

BerryNgine is not trying to be a full editor ecosystem. It is trying to be enough to finish a small or mid-sized 2D game without glue code:

- Procedural shapes and UI chrome (`ShapeGenerator`)
- Built-in bitmap / spritesheet fonts
- Camera with follow, zoom, rotation, and trauma shake
- PostFX for CRT, bloom, palette quantization, etc.
- SFX and multi-track music synthesizers
- Resource loaders for classpath, install folder, and zip packs

### Own your resolution

Games choose an **internal resolution** (for example `320×180` or `640×360`). The window is that resolution times an integer scale. Mouse coordinates are remapped back into internal space. Think in game pixels; let the window be a magnifying glass.

---

## Architecture overview

```
┌─────────────────────────────────────────────────────────────┐
│  GameWindow (Swing Canvas + BufferStrategy)                 │
│    owns int[] backbuffer → ThreadedPixelGraphics            │
│    SceneManager · GameLoop · SoundSystem · AudioMixer       │
└───────────────────────────┬─────────────────────────────────┘
                            │
              GameLoop thread ("GameLoop")
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   Input.poll()      Scene.update/fixed     audioMixer.cleanup()
                            │
                            ▼
              graphics.beginFrame()
              Scene.render(pg)      ← records draw commands
              graphics.renderCursor()
              graphics.execute()    ← tile-parallel rasterize
              window.present()      ← nearest-neighbor blit
```

### Frame path

Each tick of `GameLoop`:

1. Clamp delta time (default max ≈ 1/15 s — avoids spiral-of-death after a pause)
2. If the mouse is captured, read raw delta from screen center and warp the cursor back
3. `Input.poll()` — promote raw key/mouse state into held / down / up edges
4. Fixed updates while the accumulator ≥ fixed step (default 60 Hz)
5. Variable `update(dt * timeScale)`
6. Audio mixer cleanup of finished voices
7. `ThreadedPixelGraphics.update(dt)` (camera shake, software cursor)
8. `beginFrame()` → scene `render` → software cursor → `execute()` → `present()`
9. Optional frame-rate sleep when `targetFps > 0`

On shutdown the loop closes the mixer, sound system, and render workers.

### Ownership rules

| Object | Role |
| --- | --- |
| `GameWindow` | Owns the presented `BufferedImage` / `int[]` pixels |
| `ThreadedPixelGraphics` | Records commands into that buffer; cannot rebind `setBuffer` |
| `PixelGraphics` | In-memory textures, atlases, offscreen drawing, shape output |
| `Scene` | Your game logic and draw recording |

Direct writes to `pg.pixels` bypass the command queue. Prefer drawing methods so order stays defined. Methods that *read* the framebuffer (`getPixel`, `getSubImage`, `toBufferedImage`, `scale`, `scaleTo`) call `execute()` first.

**Do not** resize or replace the window’s pixel array.

---

## Build and run

Requirements: a JDK with `javac` and `jar` on `PATH`.

### Windows

```bat
build.bat
build.bat test
build.bat benchmark 1000 4
```

### Linux / macOS

```bash
./build.sh
./build.sh test
./build.sh benchmark 1000 4
```

`build.bat` / `build.sh` compile every `src/**/*.java`, copy `default_assets` onto the classpath, and write:

```text
build/berryngine-<branch-name>.jar
```

- `test` — launches `TestScene` (FPS in the title bar)
- `benchmark [frames] [threads]` — headless render of the same workload; prints ms/frame and a framebuffer checksum

### Linux / macOS (manual)

```bash
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
javac -d build/classes @build/sources.txt
cp -r src/berryngine/default_assets build/classes/berryngine/
jar --create --file build/berryngine.jar --main-class berryngine.TestScene -C build/classes .
java -jar build/berryngine.jar
java -jar build/berryngine.jar --benchmark 1000 4
```

### Branches and renderer history

- **`main`** — threaded (ordered, tile-parallel) renderer by default
- **`immediate-render-baseline`** — original immediate `PixelGraphics` path for comparison
- **`threaded-render-pipeline`** — development branch for the new renderer

Both comparison paths share the same `TestScene` workload so FPS and checksums are meaningful across builds.

---

## Quick start

```java
import berryngine.*;

public final class Demo implements Scene {
    private PixelGraphics player;
    private final Vec2 position = new Vec2(80, 60);

    public static void main(String[] args) {
        GameInformation.set(
                "Demo", "Your Name", "MIT",
                null, null, "Made with BerryNgine");

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
        position.x += Input.getAxisHorizontal() * 80 * dt;
        position.y += Input.getAxisVertical() * 80 * dt;
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

`Scene.render` receives a `ThreadedPixelGraphics`. It extends `PixelGraphics`, so APIs that accept textures or offscreen buffers still type-check. Default render thread count is `Runtime.getRuntime().availableProcessors()`. Use `.renderThreads(1)` to keep the same command path entirely on the game-loop thread (useful for debugging).

---

## Window and game loop

### `GameWindow.builder`

| Option | Default | Meaning |
| --- | --- | --- |
| `scale(n)` | `1` | Window size = internal resolution × n |
| `resizable(bool)` | `true` | User can resize the Swing frame |
| `fullscreen(bool)` | `false` | Borderless fullscreen on the current display |
| `targetFps(n)` | `0` | Cap frames; `0` = uncapped |
| `fixedHz(n)` | `60` | Fixed-timestep rate for `fixedUpdate` |
| `renderThreads(n)` | CPU count | Total render threads including the game loop thread |
| `setCaptureMouseByDefault(bool)` | `false` | Capture on focus (good for look/aim) |
| `icon(PixelGraphics)` | none | Window / taskbar icon |
| `build()` / `run(Scene)` | — | Construct, or construct + start |

Useful runtime APIs on the window:

- `getGraphicsAPI()` → `ThreadedPixelGraphics`
- `getCamera()` / `getSoftwareCursor()` / `setSoftwareCursor(Cursor)`
- `captureMouse()` / `releaseMouse()` / `isMouseCaptured()`
- `setBorderlessFullscreen(boolean)`
- `present()` — double-buffered blit of the internal backbuffer

### `GameLoop`

Runs on a dedicated `"GameLoop"` thread.

| Control | Notes |
| --- | --- |
| `getDeltaTime()` / `getRawDeltaTime()` | Scaled vs unscaled |
| `setTimeScale(float)` | Slow-mo / pause helpers |
| `setTargetFps` / `setFixedHz` | Frame cap and fixed step |
| `setMaxDeltaTime` | Spiral-of-death clamp |
| `getFps()` | Measured over the last second |

### Scenes

```java
public interface Scene {
    void update(GameWindow gw, float dt);
    default void fixedUpdate(GameWindow gw, float fixedDt) { }
    void render(GameWindow gw, ThreadedPixelGraphics pg);
    void onSceneEnter(GameWindow gameWindow);
    void onSceneExit(GameWindow gameWindow);
    default void onWindowFocusChanged(GameWindow gameWindow, boolean focused) { ... }
}
```

`SceneManager.setScene` exits the old scene, enters the new one, and requests window focus. Use scenes for menus, levels, and mode switches without tearing down the window.

---

## Rendering

### Color format

All colors are packed ARGB integers: `0xAARRGGBB`.

```java
int c = Color.rgba(255, 128, 0, 200);
int tinted = Color.withAlpha(Color.ORANGE, 180);
int mixed = Color.lerp(Color.RED, Color.BLUE, 0.5f);
```

`Color` also provides a large named palette, HSB conversion, multiply/blend helpers, and hex parsing (`#RRGGBB` or `RRGGBBAA`).

### Drawing API (`ThreadedPixelGraphics` / `PixelGraphics`)

Each call appears in the final image **in the order it was made**.

| Task | Methods |
| --- | --- |
| Pixels | `setPixel`, `blendPixel`, `blendPixelGlobal`, `getPixel` |
| Lines | `drawLine`, `drawHorizontalLine`, `drawVLine`, `drawTaperedLine` |
| Rectangles / gradients | `fillRect`, `fillRectBlended`, `drawRect`, `fillGradientRect`, `drawVerticalGradient`, `fillRect(..., Paint)` |
| Images | `drawImage`, `drawImageBlended`, `drawImageTinted`, `drawImageFlipped`, `drawImageScaled`, `drawImageScaledBlended` |
| Text | `renderChar`, `renderString`, `renderStringCached` |
| State / FX | `setClip`, `clearClip`, `setGlobalAlpha`, `applyVignette`, `effect` |
| World space | `drawImageWorld`, `drawImageBlendedWorld`, `fillRectWorld`, `drawRectWorld`, `drawLineWorld`, `setPixelWorld`, `isVisibleWorld` |

Image methods accept either a `PixelGraphics` texture or a raw `int[]` plus source width/height. Raw-array overloads store the array **reference** — do not mutate those pixels until `execute()` returns.

Notes:

- `drawImage` replaces non-transparent pixels; blended / tinted / flipped / scaled variants use alpha blending
- `setClip` and `setGlobalAlpha` are captured when each command is recorded
- `clear` always fills the whole framebuffer
- Float/`Vec2`/`Vec3` screen coordinates are truncated to integers when recorded
- Integer scaling uses nearest-neighbor (pixel-art friendly)

### Text

Two font kinds ship with the engine:

| Font | Type | Source |
| --- | --- | --- |
| `BitmapFont.DEFAULT_8X9` | PSF1 glyph bitmaps (CP437) | `default_assets/fonts/default8x9.psf` |
| `SpriteSheetFont.START2P` | 8×8 atlas | `default_assets/fonts/start2p_8x8.qoi` |
| `SpriteSheetFont.ABLE4` | 6×6 atlas | `default_assets/fonts/able4_6x6.qoi` |

`renderString` draws glyphs without allocating a full string bitmap every call. For HUD text that repeats across frames, prefer `renderStringCached` — it keeps up to **256** strings or **1,000,000** cached pixels (LRU). Call `clearStringCache()` if you mutate a font’s glyphs or atlas after caching.

```java
pg.renderString(BitmapFont.DEFAULT_8X9, "Score: " + score, 8, 8, Color.WHITE);
pg.renderStringCached(SpriteSheetFont.START2P, "PAUSED", 120, 80, Color.YELLOW);
```

Load custom fonts with `Utils.loadFontFromResources` / `PSF1Parser`, or build a `SpriteSheetFont` from a `TextureAtlas`.

### Camera (`Camera2D`)

Accessed via `pg.getCamera()` (also `gw.getCamera()`).

- `position`, `zoom`, `rotation`
- `centerOn` / `follow` (smoothed)
- `worldToScreen` / `screenToWorld` / `getWorldMousePosition`
- `isVisible` + world bounds helpers
- Trauma-based screen shake: `addTrauma`, `setShakeDecay`, `setShakeMaxOffset`

World-space draw helpers transform coordinates **when the command is recorded**. Changing the camera later in the same frame does not move already-recorded draws.

### Effects and post-processing

Some operations need a finished buffer. `effect` flushes the queue, runs a callback on the complete framebuffer, then lets later calls start a new ordered stage:

```java
pg.drawImageWorld(level, 0, 0);
PostFX.scanlines(pg, 2, 0.35f);
// or: pg.effect(buffer -> PostFX.scanlines(buffer, 2, 0.35f));
pg.renderStringCached(BitmapFont.DEFAULT_8X9, "PAUSED", 8, 8, Color.WHITE);
```

`PostFX` mutates pixels in place. Passed a `ThreadedPixelGraphics`, it flushes queued draws first. Highlights:

- Color: grayscale, invert, tint, brightness, contrast, saturation, levels, posterize, palette quantize (+ Bayer dither), color replace
- CRT / retro: scanlines, noise, chromatic aberration, screen curve, color fringing, horizontal distortion
- Lighting: vignette, fade, flash, fog, night vision, heat vision
- Filters: pixelate, box blur, gaussian blur (stacked boxes), bloom, outline

`fillRect(..., Paint)`, `applyVignette`, tapered lines, and some scaled/background text paths also use a full-frame stage. Prefer ordinary fills, gradients, sprites, and text for high-volume drawing.

### Software cursor

`Cursor` draws a hotspot-aware sprite after the scene. It can track `Input` mouse position in internal resolution space. Set it with `gw.setSoftwareCursor(...)` or `pg.setCursor(...)`.

---

## Input

`Input` is a static polled API. Call sites read state **after** the loop’s `poll()` (you do not call `poll` yourself from a scene).

| Kind | API |
| --- | --- |
| Keys | `isKey` / `isKeyDown` / `isKeyUp` (AWT key codes) |
| Mouse buttons | `isMouseButton` / `Down` / `Up` (buttons 1–7) |
| Position | `getMouseX/Y`, `getMouseScaledX/Y` (window → internal) |
| Delta | `getMouseDeltaX/Y` (raw when captured) |
| Wheel | `getMouseWheel` |
| Text entry | `getTypedText()`, `hasTypedInput()` |
| Axes | `getAxisHorizontal`, `getAxisVertical`, `getAxis(neg, pos)` |

Horizontal/vertical axes default to WASD + arrow keys.

---

## Textures, atlases, animation, shapes

### Loading

```java
PixelGraphics tex = Utils.loadTextureFromResources("/game/hero.qoi");
TextureAtlas atlas = Utils.loadTextureAtlasFromResources("/game/tiles.qoi", 16, 16);
Sound sfx = Utils.loadSoundFromResources("/game/jump.qoa");
MusicSynth song = Utils.loadMidiFromResources("/game/theme.mid");
```

Loaders exist for:

- Classpath resources
- Game install folder (`GameInformation.getGameInstallFolder()`)
- Zip archives of either of the above

Preferred image format is **QOI** (decoded by `QOIDecoder`); other formats go through `ImageIO`. Preferred audio format is **QOA** (`QOADecoder`).

`Utils.saveScreenshot` and `textureFromBufferedImage` cover capture / conversion edges.

### `TextureAtlas`

Slices a sheet into cached `PixelGraphics` cells. Supports `getTexture(index)` / `(x, y)`, `scale`, `addTexture`, and `fromTextures`.

### `Animation`

Frame sequences over an atlas:

```java
Animation walk = Animation.ofRange(atlas, 0, 7, 0.08f, true);
walk.register(); // optional WeakReference registry
// later: Animation.updateRegistered(dt);
PixelGraphics frame = walk.getCurrentFrame();
```

Supports loop flags, progress, `onDone`, and finish detection.

### `ShapeGenerator`

A large procedural texture factory — circles, rings, polygons, stars, hearts, UI chrome (pills, speech/thought bubbles, buttons), decorative props (sun, moon, cloud, gear, flower), and HUD pieces (crosshair, health bar, radar, compass, progress bars). Everything returns a ready-to-draw `PixelGraphics`.

```java
PixelGraphics coin = ShapeGenerator.coin(16, Color.GOLD, Color.ORANGE);
PixelGraphics bar = ShapeGenerator.healthBar(64, 8, 0.75f, Color.RED, Color.DARK_GRAY);
```

---

## Audio

### Playback

- `Sound` — raw PCM container (channels, sample rate, samples)
- `SoundSystem` — 44.1 kHz stereo mix, soft failure if no line is available, weak clip cache so finished sounds do not pin memory forever
- `AudioMixer` — named groups (`MASTER`, `SFX`, `MUSIC`, `UI`) with per-group and per-voice volume, pause/resume/stop

```java
Sound jump = SoundSynth.jump().build();
int handle = gw.audioMixer.play(AudioMixer.SFX, jump, 0.8f);
```

### `SoundSynth` — procedural SFX

Fluent builder over waveforms (`SINE`, `SQUARE`, `SAW`, `TRIANGLE`, `NOISE`) with ADSR, duty cycle, vibrato, tremolo, frequency sweeps (including MIDI notes), low/high-pass, and soft-clip distortion.

Presets: `laser`, `coin`, `explosion`, `jump`, `hit`, `powerup`, `blip`, `select`, `error`, `charge`, `death`, `teleport`.

### `MusicSynth` — multi-track music

Compose tracks with instruments (`lead`, `bass`, `pad`, `bell`, or custom waveforms), tempo, swing, delay, pan, transpose, notes, chords, and arpeggios. `build()` produces a peak-normalized stereo `Sound`.

`MidiImporter` can load PPQ MIDI into a `MusicSynth` (channel 9 / drums skipped; GM programs mapped to instrument presets).

---

## Math and utilities

| Class | Role |
| --- | --- |
| `Vec2` / `Vec3` | Mutable float vectors; motion helpers on `Vec2` (damping, force, reflect, lerp, fromAngle, …) |
| `IVec2` / `IVec3` | Mutable integer vectors |
| `Mathf` | Fast LUT trig (4096), clamp/lerp/smoothstep/map, `fastInvSqrt`, angle helpers |
| `Random` | Static facade + nestable `State` (ranges, chance, pick, shuffle, colors, unit circle) |
| `Timer` | Duration timer with optional loop and `onDone` |
| `GameInformation` | Name, author, license, credits, install folder, OS data folder |

Configure game identity early:

```java
GameInformation.set("MyGame", "Studio", "MIT", null, null, "Thanks players");
// data folder defaults to:
//   Windows: %APPDATA%/Studio/MyGame
//   Linux:   $XDG_DATA_HOME/Studio/MyGame or ~/.local/share/Studio/MyGame
```

---

## Bundled assets

```text
src/berryngine/default_assets/fonts/
  default8x9.psf      → BitmapFont.DEFAULT_8X9
  start2p_8x8.qoi     → SpriteSheetFont.START2P
  able4_6x6.qoi       → SpriteSheetFont.ABLE4
```

These are copied into the JAR by the build. Classpath paths used by the engine:

- `/berryngine/default_assets/fonts/default8x9.psf`
- `/berryngine/default_assets/fonts/start2p_8x8.qoi`
- `/berryngine/default_assets/fonts/able4_6x6.qoi`

---

## Performance tips

1. **Record, don’t thrash the buffer.** Let `execute()` do the pixel work; avoid reading `pixels` mid-frame.
2. **Reuse textures.** Prefer `ShapeGenerator` / atlas cells created once in `onSceneEnter`, not every frame.
3. **Cache HUD strings.** Use `renderStringCached` for labels that change infrequently.
4. **Keep heavy FX rare.** `effect` and full-frame PostFX flush the pipeline — great for pause menus and transitions, costly every frame.
5. **Tune threads.** Default is one worker pool sized to your CPU. Benchmark with `build.bat benchmark` / `--benchmark` before locking a thread count.
6. **Pick a modest internal resolution.** `320×180` × 3 or `640×360` × 2 is usually smoother than a huge native buffer.

---

## Package map

| Area | Types |
| --- | --- |
| App shell | `GameWindow`, `GameLoop`, `Scene`, `SceneManager`, `GameInformation`, `TestScene` |
| Graphics | `PixelGraphics`, `ThreadedPixelGraphics`, `Camera2D`, `Cursor`, `PostFX`, `TextureAtlas`, `Animation`, `ShapeGenerator` |
| Fonts | `BitmapFont`, `SpriteSheetFont`, `PSF1Parser` |
| Input | `Input` |
| Audio | `Sound`, `SoundSystem`, `AudioMixer`, `SoundSynth`, `MusicSynth`, `MidiImporter`, `QOADecoder` |
| Assets | `Utils`, `QOIDecoder` |
| Math / color | `Color`, `Vec2`, `Vec3`, `IVec2`, `IVec3`, `Mathf`, `Random`, `Timer` |

All of the above are pure Java under `berryngine.*`.

---

## License and credits

Set your own game metadata with `GameInformation.set(...)`. The engine JAR’s demo entry point is `berryngine.TestScene`.

BerryNgine’s stance is simple: **small engine, visible pixels, no mystery layers.** If something looks wrong, the buffer is right there — open it, read a pixel, and keep going.
