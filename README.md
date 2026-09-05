# BerryNgine

BerryNgine is a small, self-contained 2D game engine written in pure Java. It is built around a **software pixel buffer** that you draw into directly, giving you complete, deterministic control over every pixel while still being fast enough for real-time games.

There are no external dependencies: it uses only the standard JDK (`javax.swing` for the window, `javax.sound.sampled` for audio, and `java.awt.image` for image I/O). This makes it easy to drop into any Java project, ship as a single `.jar`, and reason about exactly what the engine is doing under the hood.

---

## Table    of Contents

1. [Philosophy](#philosophy)
2. [What Is Included](#what-is-included)
3. [Quick Start](#quick-start)
4. [Project Setup](#project-setup)
5. [Core Concepts](#core-concepts)
6. [Rendering](#rendering)
7. [Camera](#camera)
8. [Color](#color)
9. [Text & Fonts](#text--fonts)
10. [Shapes & Procedural Graphics](#shapes--procedural-graphics)
11. [Textures, Atlases & Animation](#textures-atlases--animation)
12. [Input](#input)
13. [Audio](#audio)
14. [Post-Processing](#post-processing)
15. [Math, Vectors & Helpers](#math-vectors--helpers)
16. [Asset Loading, Screenshots & Game Metadata](#asset-loading-screenshots--game-metadata)
17. [Cursor](#cursor)
18. [Complete Example Scenes](#complete-example-scenes)
19. [Tips & Best Practices](#tips--best-practices)

---

## Philosophy

BerryNgine is designed for developers who want the feel of a retro, sprite-based game with the convenience of a modern Java toolchain.

- **Software rendering first.** Every frame is drawn into an `int[]` pixel buffer in ARGB format. There is no hidden GPU state, no shader compilation, and no surprises about how a draw call will be interpreted.
- **Pixel-perfect scaling.** The window is decoupled from the internal resolution. You render at a small, fixed resolution (e.g., 320x180) and the engine scales it up with nearest-neighbor interpolation, preserving crisp pixel art.
- **Immediate-mode API.** You draw directly inside `Scene.render(...)`. There are no scene-node hierarchies, no retained UI trees, and no mandatory entity systems. You are free to build those on top if you want them.
- **Fixed timestep for logic, variable timestep for rendering.** Physics and gameplay update at a configurable fixed rate while the renderer runs as fast as the display loop allows.
- **Zero dependencies.** If you have a JDK, you can build and run BerryNgine.

If you have ever wanted to write a game where you can literally `setPixel(x, y, color)`, but still get a resizable window, gamepad-like keyboard input, audio mixing, and a camera system for free, BerryNgine is aimed at you.

---

## What Is Included

| Area | Key Classes |
|------|-------------|
| Window & loop | `GameWindow`, `GameLoop`, `Scene`, `SceneManager` |
| Rendering | `PixelGraphics`, `FramebufferPixelGraphics`, `Camera2D`, `Color`, `PostFX`, `Cursor` |
| Primitives & shapes | `ShapeGenerator`, `TextureAtlas`, `BitmapFont`, `SpriteSheetFont`, `Animation` |
| Input | `Input` |
| Audio | `SoundSystem`, `AudioMixer`, `SoundSynth`, `MusicSynth`, `MidiImporter`, `Sound`, `QOADecoder` |
| Math & utilities | `Mathf`, `Vec2`, `Vec3`, `IVec2`, `IVec3`, `Random`, `Timer`, `Utils`, `GameInformation` |
| Formats | `QOIDecoder`, `MidiImporter`, `PSF1Parser` |

---

## Quick Start

A BerryNgine program is a class that implements `berryngine` and a `main` method that builds and runs a `GameWindow`.

```java
package game;

import berryngine.*;

public class Main {

    public static void main(String[] args) {
        // Optional: set metadata that Utils and save paths use.
        GameInformation.set(
                "MyGame",
                "MyStudio",
                "MIT",
                null,          // data folder (auto)
                null,          // install folder (jar dir)
                "Thanks for playing!"
        );

        GameWindow window = GameWindow
                .builder("My Game", 320, 180)   // internal resolution
                .scale(3)                        // initial window size = 960x540
                .targetFps(60)
                .fixedHz(60)
                .run(new DemoScene());
    }

    static class DemoScene implements Scene {
        @Override
        public void onSceneEnter(GameWindow gw) {
        }

        @Override
        public void onSceneExit(GameWindow gw) {
        }

        @Override
        public void update(GameWindow gw, float dt) {
            if (Input.isKeyDown(java.awt.event.KeyEvent.VK_ESCAPE)) {
                System.exit(0);
            }
        }

        @Override
        public void render(GameWindow gw, FramebufferPixelGraphics pg) {
            pg.clear(Color.DARK_BLUE);
            pg.fillRect(10, 10, 60, 60, Color.RED);
            pg.renderString(SpriteSheetFont.START2P,
                    "HELLO BERRYNGINE",
                    12, 90,
                    Color.WHITE);
        }
    }
}
```

Compile and run. You should see a small blue window with a red rectangle and white pixel-art text.

---

## Project Setup

BerryNgine is a single Java source tree.

- Source root: `src`
- Engine package: `berryngine.*`
- No Maven/Gradle dependencies required.
- Compile everything under `src` with your IDE or with `javac`:

```bash
javac -d out $(find src -name "*.java")
java -cp out game.Main
```

The engine ships with one embedded asset, a 16x16 pixel font atlas at `berryngine`, used by `SpriteSheetFont.START2P`.

---

## Core Concepts

### The Game Window

`GameWindow` extends `java.awt.Canvas` and wraps a `JFrame`. Internally it owns:

- a `BufferedImage` back buffer at the **internal resolution**,
- a `FramebufferPixelGraphics` that draws directly into that buffer,
- a `SceneManager`,
- a `GameLoop`,
- a `SoundSystem` and `AudioMixer`.

Construction is done through a fluent builder:

```java
GameWindow window = GameWindow.builder("Title", 320, 180)
    .scale(3)                         // initial window size
    .resizable(true)                  // window can be resized
    .fullscreen(false)                // or .fullscreen(true)
    .targetFps(60)                    // 0 = uncapped
    .fixedHz(60)
    .setCaptureMouseByDefault(false)
    .build();

window.run(new MyScene());
```

`builder(...).run(scene)` is equivalent to `.build().run(scene)`.

### The Game Loop

`GameLoop` runs on its own thread. Every frame it does the following:

1. Polls input.
2. Runs zero or more `fixedUpdate(fixedDt)` calls at the configured fixed rate.
3. Runs one `update(dt)` call.
4. Runs one `render(pg)` call.
5. Presents the buffer to the window.

Delta time is clamped to a maximum (default 1/15 s) to prevent huge jumps after a debugger pause or OS stutter. Time scale can be changed at runtime:

```java
window.gameLoop.setTimeScale(0.5f);  // half speed
window.gameLoop.setTargetFps(144);
window.gameLoop.setFixedHz(120);
```

You can query timing:

```java
float dt = window.getDeltaTime();
long fps = window.getFps();
```

### Scenes

A `Scene` is the unit of game state. It has lifecycle hooks and three callbacks:

```java
public interface Scene {
    void update(GameWindow gw, float dt);
    default void fixedUpdate(GameWindow gw, float fixedDt) {}
    void render(GameWindow gw, FramebufferPixelGraphics pg);
    void onSceneEnter(GameWindow gw);
    void onSceneExit(GameWindow gw);
    default void onWindowFocusChanged(GameWindow gw, boolean focused) { ... }
}
```

Switch scenes with `window.setScene(new Scene())`. The old scene receives `onSceneExit`, the new one `onSceneEnter`.

The default `onWindowFocusChanged` will capture/release the mouse if `captureMouseByDefault` was enabled in the builder.

---

## Rendering

`PixelGraphics` is both a drawing surface and a sprite/texture buffer. It stores a raw `int[]` in `0xAARRGGBB` format, and the same drawing methods work on any instance, including its subclass `FramebufferPixelGraphics` for the screen buffer.

### Drawing Primitives

```java
pg.setPixel(10, 10, Color.RED);
pg.drawLine(0, 0, 100, 100, Color.WHITE);
pg.drawHorizontalLine(0, 50, 200, Color.GREEN);
pg.drawVLine(100, 0, 179, Color.BLUE);
pg.drawRect(20, 20, 60, 60, Color.YELLOW);
pg.fillRect(20, 20, 60, 60, Color.YELLOW);
pg.fillRectBlended(20, 20, 60, 60, Color.withAlpha(Color.RED, 128));
```

### Images

Images are `PixelGraphics` instances. You can draw them with replacement, blending, tinting, flipping, or scaling:

```java
PixelGraphics sprite = Utils.loadTextureFromResources("/assets/player.png");

pg.drawImage(sprite, 50, 50);                       // overwrite
pg.drawImageBlended(sprite, 50, 50);              // alpha blended
pg.drawImageTinted(sprite, 50, 50, Color.RED);      // multiply tint
pg.drawImageFlipped(sprite, 50, 50, true, false);  // flip horizontally
pg.drawImageScaled(sprite.pixels, sprite.width, sprite.height,
                   50, 50, 128, 128);               // nearest-neighbor scale
```

### Clipping and Global Alpha

```java
pg.setClip(10, 10, 100, 100);
pg.fillRect(0, 0, 300, 180, Color.RED);  // only the clip region is filled
pg.clearClip();

pg.setGlobalAlpha(0.5f);
pg.drawImageBlended(sprite, 0, 0);
pg.setGlobalAlpha(1.0f);
```

### Clearing

```java
pg.clear(Color.BLACK);
```

### Gradients and Effects

```java
// Horizontal gradient from left to right
pg.fillGradientRect(0, 0, 320, 180, Color.MIDNIGHT_BLUE, Color.BLACK, true);

// Vertical gradient using Paint (Java2D gradients are also supported)
java.awt.GradientPaint paint = new java.awt.GradientPaint(
        0, 0, new java.awt.Color(255, 0, 0),
        0, 180, new java.awt.Color(0, 0, 255));
pg.fillRect(0, 0, 320, 180, paint);

pg.applyVignette(Color.BLACK, 1.2f);
```

### Sub-buffers and Scaling

```java
PixelGraphics icon = pg.getSubImage(16, 16, 16, 16);
PixelGraphics bigIcon = icon.scale(4);   // integer nearest-neighbor
```

### Render-to-Texture

`PixelGraphics` has a constructor that allocates its own buffer, so you can render off-screen and then draw it:

```java
PixelGraphics offscreen = new PixelGraphics(64, 64);
offscreen.clear(Color.TRANSPARENT);
offscreen.fillRect(0, 0, 32, 32, Color.RED);
pg.drawImageBlended(offscreen, 100, 50);
```

---

## Camera

`FramebufferPixelGraphics` owns a `Camera2D`. The camera transforms world coordinates to screen coordinates and supports position, zoom, rotation, and screen shake.

```java
Camera2D cam = pg.getCamera();
cam.centerOn(player.x, player.y);
cam.setZoom(2.0f);
cam.setRotationDegrees(15f);
cam.follow(player.x, player.y, dt, 4.0f);
```

### World-Space Drawing

Use the `*World` helpers and the camera will project them for you:

```java
pg.drawImageWorld(playerSprite, player.x, player.y);
pg.fillRectWorld(ground.x, ground.y, ground.w, ground.h, Color.GREEN);
pg.drawLineWorld(0, 0, 100, 100, Color.WHITE);
pg.setPixelWorld(mouseWorld.x, mouseWorld.y, Color.YELLOW);

if (!pg.isVisibleWorld(enemy.x, enemy.y, enemy.w, enemy.h)) {
    // cull off-screen enemies
}
```

### Coordinate Conversion

```java
IVec2 screen = cam.worldToScreen(worldX, worldY);
Vec2 world = cam.screenToWorld(Input.getMouseScaledX(), Input.getMouseScaledY());
Vec2 worldMouse = cam.getWorldMousePosition();

float left   = cam.getLeft();
float right  = cam.getRight();
float top    = cam.getTop();
float bottom = cam.getBottom();
```

### Screen Shake

```java
cam.addTrauma(0.4f);            // impulse
cam.setShakeDecay(3.0f);        // how fast it settles
cam.setShakeMaxOffset(12.0f);   // maximum displacement
```

---

## Color

Colors are packed `int` values in `0xAARRGGBB` format. The `Color` class provides utilities and a large built-in palette.

```java
int red   = Color.fromRGB(255, 0, 0);
int half  = Color.fromRGBA(255, 0, 0, 128);
int hex   = Color.fromHex("#FF5733");
int hsb   = Color.fromHSB(0.5f, 0.8f, 1.0f);

int a = Color.getAlpha(red);
int r = Color.getRed(red);
int g = Color.getGreen(red);
int b = Color.getBlue(red);

int faded = Color.withAlpha(red, 64);
int lit   = Color.multiply(red, 1.5f);
int mix   = Color.lerp(red, blue, 0.5f);
int blend = Color.blend(overlay, background);
```

Named constants include all common colors (`WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `CYAN`, `MAGENTA`, `YELLOW`, `ORANGE`, `PINK`, `PURPLE`, `GRAY`, `DARK_GRAY`, `LIGHT_GRAY`, `SILVER`, `CHARCOAL`, `NAVY`, `OLIVE`, `TEAL`, `INDIGO`, `FOREST_GREEN`, `MIDNIGHT_BLUE`, `STEEL_BLUE`, `SKY_BLUE`, `CHOCOLATE`, `LIME`, `CRIMSON`, `CORAL`, `GOLD`, `BRONZE`, etc.) and `TRANSPARENT`.

---

## Text & Fonts

BerryNgine supports two kinds of fonts:

### Bitmap Fonts (PSF1)

PC Screen Font v1 files can be loaded with `Utils.loadFontFromResources(...)` or `Utils.loadFontFromGameInstall(...)`.

```java
BitmapFont font = Utils.loadFontFromResources("/berryngine/default_assets/fonts/8x16.psf");
pg.renderString(font, "Hello",10,10,Color.WHITE);
pg.renderString(font, "With BG",10,30,Color.WHITE, Color.BLACK);
pg.renderString(font, "Big",10,50,Color.WHITE, Color.BLACK, 2); // scaled 2x
```

### Sprite-Sheet Fonts

A `SpriteSheetFont` maps a string of characters to a grid of glyphs in a `TextureAtlas`.

```java
TextureAtlas atlas = Utils.loadTextureAtlasFromResources("/assets/font.png", 8, 8);
SpriteSheetFont font = new SpriteSheetFont(
        " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", atlas);
pg.renderString(font, "SCORE 001", 10, 10, Color.WHITE);
pg.renderString(font, "SCORE 002", 10, 20, Color.WHITE, 2); // scaled
```

A default font is built in:

```java
pg.renderString(SpriteSheetFont.START2P, "PRESS START", 40, 80, Color.WHITE);
```

---

## Shapes & Procedural Graphics

`ShapeGenerator` returns `PixelGraphics` objects for common shapes, so you can generate sprites at runtime without loading image files.

```java
PixelGraphics circle = ShapeGenerator.circle(16, Color.WHITE);
PixelGraphics ring   = ShapeGenerator.ring(16, 4, Color.YELLOW);
PixelGraphics rect   = ShapeGenerator.rectangle(32, 16, Color.GREEN);
PixelGraphics tri    = ShapeGenerator.filledTriangle(
        0, 0, 32, 0, 16, 32, Color.RED);
PixelGraphics star   = ShapeGenerator.star(32, 32, 5, Color.GOLD);
PixelGraphics heart  = ShapeGenerator.heart(32, 32, Color.RED);
PixelGraphics arrow  = ShapeGenerator.arrow(32, 16, Color.WHITE, 3);
PixelGraphics rndRect= ShapeGenerator.roundedRectangle(64, 32, 8, Color.BLUE);
PixelGraphics grad   = ShapeGenerator.verticalGradient(64, 64, Color.RED, Color.BLUE);
PixelGraphics check  = ShapeGenerator.checkerboard(64, 64, 8, Color.WHITE, Color.BLACK);
PixelGraphics grid   = ShapeGenerator.grid(64, 64, 8, Color.GRAY);
```

These are ordinary `PixelGraphics` instances, so they can be drawn with `pg.drawImage(...)`, stored as sprites, or blitted into atlases.

---

## Textures, Atlases & Animation

### Loading Textures

```java
PixelGraphics tex = Utils.loadTextureFromResources("/assets/tile.png");
PixelGraphics tex2 = Utils.loadTextureFromGameInstall("data/tile.qoi"); // QOI support
```

BerryNgine supports PNG/JPEG (via `ImageIO`) and the QOI image format (via `QOIDecoder`).

### Texture Atlases

```java
TextureAtlas atlas = Utils.loadTextureAtlasFromResources(
        "/assets/tiles.png", 16, 16);

PixelGraphics grass = atlas.getTexture(0);
PixelGraphics water = atlas.getTexture(1, 2);  // column, row

int cols = atlas.getColumns();
int rows = atlas.getRows();
int count = atlas.getTextureCount();
```

### Animation

```java
Animation walk = new Animation(atlas, new int[]{0, 1, 2, 3}, 0.12f, true);

@Override
public void update(GameWindow gw, float dt) {
    walk.update(dt);
}

@Override
public void render(GameWindow gw, FramebufferPixelGraphics pg) {
    pg.drawImageBlended(walk.getCurrentFrame(), playerScreenX, playerScreenY);
}
```

Convenience for contiguous frames:

```java
Animation run = Animation.ofRange(atlas, 4, 7, 0.1f);
run.onDone(() -> System.out.println("loop point")).register();

// Updates all registered animations in one call
Animation.updateRegistered(dt);
```

---

## Input

`Input` is a static, frame-synchronized input state. The engine populates it; you only read from it.

### Keyboard

```java
if (Input.isKey(java.awt.event.KeyEvent.VK_LEFT))  { /* held */ }
if (Input.isKeyDown(java.awt.event.KeyEvent.VK_SPACE)) { /* pressed this frame */ }
if (Input.isKeyUp(java.awt.event.KeyEvent.VK_SPACE))     { /* released this frame */ }

// Convenience axes
float dx = Input.getAxisHorizontal();  // A/D or Left/Right
float dy = Input.getAxisVertical();    // W/S or Up/Down
float custom = Input.getAxis(
        java.awt.event.KeyEvent.VK_Q,
        java.awt.event.KeyEvent.VK_E);
```

### Mouse

```java
int mx = Input.getMouseX();
int my = Input.getMouseY();

// Scaled to internal resolution, useful when the window is resized
int sx = Input.getMouseScaledX();
int sy = Input.getMouseScaledY();

int dx = Input.getMouseDeltaX();
int dy = Input.getMouseDeltaY();
int wheel = Input.getMouseWheel();  // +1 / -1 this frame

if (Input.isMouseButtonDown(1)) { /* left click */ }
if (Input.isMouseButton(3))     { /* right held */ }
```

### Typed Text

```java
if (Input.hasTypedInput()) {
    String typed = Input.getTypedText();
    chatBuffer.append(typed);
}
```

### Mouse Capture

For first-person or look controls:

```java
window.captureMouse();   // hides cursor, warps to center, gives raw deltas
window.releaseMouse();
window.isMouseCaptured();
```

When captured, raw mouse delta is reported via `Input.getMouseDeltaX/Y` instead of cursor position.

---

## Audio

BerryNgine uses a custom software mixer built on `javax.sound.sampled`. Audio assets and generated audio are represented by `Sound`, which contains signed 16-bit PCM samples. `SoundSystem` converts supported sample rates and mono or stereo input to its 44.1 kHz stereo output format.

### Sound System and Mixer

`GameWindow` creates a `SoundSystem` and an `AudioMixer` automatically. Use the mixer in game code so sounds can be assigned to volume groups and controlled together.

```java
Sound jump = Utils.loadSoundFromResources("/assets/jump.qoa");
int handle = window.audioMixer.play(AudioMixer.SFX, jump, 0.8f);

window.audioMixer.setVolume(handle, 0.4f);
window.audioMixer.pause(handle);
window.audioMixer.resume(handle);
window.audioMixer.stop(handle);
```

To play repeating music, pass `true` for the loop argument:

```java
Sound music = Utils.loadSoundFromGameInstall("data/theme.qoa");
int musicHandle = window.audioMixer.play(AudioMixer.MUSIC, music, 0.5f, true);
```

### Mixer Groups

`AudioMixer` defines `MASTER`, `SFX`, `MUSIC`, and `UI` names. `addDefaultGroups()` registers the non-master groups; unknown group names otherwise use full volume until configured.

```java
window.audioMixer.addDefaultGroups();
window.audioMixer.setGroupVolume(AudioMixer.MUSIC, 0.3f);
window.audioMixer.setGroupVolume(AudioMixer.SFX, 1.0f);
window.audioMixer.stopGroup(AudioMixer.MUSIC);
window.audioMixer.pauseAll();
window.audioMixer.resumeAll();
```

### Procedural Sound Effects

`SoundSynth` creates one-shot sound effects at runtime. Presets include `laser`, `coin`, `explosion`, `jump`, `hit`, `powerup`, `blip`, `select`, `error`, `charge`, `death`, and `teleport`.

```java
Sound laser = SoundSynth.laser().build();
Sound coin = SoundSynth.coin().build();
Sound explosion = SoundSynth.explosion().build();

window.audioMixer.play(AudioMixer.SFX, laser, 0.7f);
```

Custom effects can use sine, square, saw, triangle, or noise oscillators; MIDI-note pitch; exponential pitch sweeps; attack, decay, sustain-duration, sustain-level, and release controls; vibrato; tremolo; filters; duty cycle; deterministic noise; and distortion.

```java
Sound effect = SoundSynth.create()
    .waveform(SoundSynth.Waveform.SAW)
    .sweepNotes(60, 72)
    .envelope(0.01f, 0.1f, 0.2f, 0.3f)
    .sustainLevel(0.6f)
    .vibrato(0.05f, 8.0f)
    .tremolo(0.2f, 4.0f)
    .lowpass(2000.0f)
    .distortion(0.2f)
    .gain(0.6f)
    .build();
```

### Procedural Music

`MusicSynth` sequences polyphonic notes in beats and renders the complete arrangement to a stereo `Sound`. It supports multiple tracks, chords, arpeggios, rests, absolute note placement, velocity, transposition, track volume, stereo panning, swing, and ping-pong delay.

The built-in instruments are `lead()`, `bass()`, `pad()`, and `bell()`. A custom instrument can configure waveform, ADSR envelope, gain, square-wave duty cycle, vibrato, and additional harmonics.

```java
MusicSynth composition = MusicSynth.create()
    .tempo(120.0f)
    .gain(0.8f)
    .swing(0.12f)
    .delay(0.75f, 0.3f);

int a3 = MusicSynth.note("A3");
int a4 = MusicSynth.note("A4");

composition.track(MusicSynth.Instrument.pad())
    .volume(0.45f)
    .pan(-0.2f)
    .chord(MusicSynth.chord(a3, 3, 7), 4.0f, 0.7f)
    .chord(MusicSynth.chord(a3 + 5, 4, 7), 4.0f, 0.7f)
    .end();

composition.track(MusicSynth.Instrument.lead().vibrato(0.01f, 5.0f))
    .pan(0.2f)
    .note(a4, 1.0f, 0.9f)
    .note(MusicSynth.note("C5"), 0.5f, 0.75f)
    .note(MusicSynth.note("E5"), 1.5f, 0.85f)
    .rest(1.0f)
    .arpeggio(MusicSynth.chord(a4, 3, 7, 12), 0.25f, 0.4f, 2);

Sound song = composition.build();
window.audioMixer.play(AudioMixer.MUSIC, song, 0.65f);
```

Durations passed to `note`, `chord`, `rest`, and `arpeggio` are measured in beats, not seconds. `noteAt(beat, midiNote, duration, velocity)` places a note without moving the track cursor. Named notes use scientific pitch notation such as `C4`, `F#4`, and `Bb4`.

### MIDI Import

Standard MIDI files can be converted directly into `MusicSynth` arrangements. Loading parses note starts and ends, timing, velocity, MIDI channels, the initial tempo event, and program changes. General MIDI program ranges are mapped to the closest available synthesized instrument.

Load a MIDI file from the classpath:

```java
MusicSynth music = Utils.loadMidiFromResources("/music/theme.mid");
Sound song = music.build();
window.audioMixer.play(AudioMixer.MUSIC, song, 0.7f);
```

Load from an arbitrary `File` or relative to the game installation directory:

```java
MusicSynth fromFile = Utils.loadMidiFromFile(new java.io.File("theme.mid"));
MusicSynth installed = Utils.loadMidiFromGameInstall("data/music/theme.mid");
```

`MidiImporter` can also be used directly with bytes, files, or an existing `javax.sound.midi.Sequence`:

```java
MusicSynth fromBytes = MidiImporter.load(midiBytes);
MusicSynth fromSequence = MidiImporter.convert(sequence);
```

Current MIDI import limitations:

- Only PPQ-timed MIDI sequences are accepted; SMPTE division types are rejected.
- The earliest tempo meta-event sets the composition tempo. Later tempo changes are not currently reproduced.
- MIDI percussion channel 10 is ignored.
- Program changes select approximate oscillator presets rather than General MIDI sample instruments.
- Sustain pedal, pitch bend, channel pressure, and other MIDI controllers are not currently rendered.

`MusicSynth.build()` renders the entire song into memory. Build once and cache the resulting `Sound` instead of rebuilding it every frame.

---

## Post-Processing

`PostFX` provides full-screen image filters that operate directly on the `PixelGraphics` buffer. Call them inside `render` before the frame is presented.

```java
PostFX.grayscale(pg);
PostFX.invert(pg);
PostFX.tint(pg, Color.RED, 0.3f);
PostFX.brightness(pg, 1.5f);
PostFX.scanlines(pg, 4, 0.3f);
PostFX.vignette(pg, 0.6f);
PostFX.pixelate(pg, 4);
PostFX.colorReplace(pg, Color.BLUE, Color.GREEN, 16);
```

Because The engine is software-rendered, you can chain effects freely:

```java
PostFX.tint(pg, Color.ORANGE, 0.15f);
PostFX.scanlines(pg, 2, 0.15f);
PostFX.vignette(pg, 0.5f);
```

---

## Math, Vectors & Helpers

### Vectors

`Vec2` and `Vec3` are mutable float vectors. `IVec2` and `IVec3` are integer variants. They share a consistent fluent API.

```java
Vec2 pos = new Vec2(10, 20);
Vec2 vel = new Vec2(1, 0);
pos.addScaled(vel, dt);
pos.applyDamping(2.0f, dt);

Vec2 dir = pos.directionTo(target);
Vec2 bounce = vel.copy().reflect(normal, 0.8f);

float dist = pos.distanceTo(target);
```

Many `PixelGraphics` methods accept `IVec2` overloads:

```java
pg.setPixel(new IVec2(10, 20), Color.RED);
pg.drawLine(new IVec2(0, 0), new IVec2(100, 100), Color.WHITE);
```

### Mathf

`Mathf` provides fast lookup-table sine/cosine/tangent and common game math.

```java
float s = Mathf.sin(angle);
float c = Mathf.cos(angle);
float a = Mathf.atan2(y, x);
float d = Mathf.distance(x1, y1, x2, y2);
float v = Mathf.clamp(value, 0, 1);
float t = Mathf.lerp(a, b, 0.5f);
float e = Mathf.smoothstep(0.2f, 0.8f, x);
float m = Mathf.map(value, 0, 100, 0, 1);
float r = Mathf.toRadians(90);
```

### Random

```java
Random.setSeed(1234L);
int n  = Random.nextInt(0, 100);
float f = Random.nextFloat(-1f, 1f);
Vec2 point = Random.insideUnitCircle();
Vec2 dir   = Random.onUnitCircle();
String word = Random.pick("fire", "ice", "bolt");

// Or create isolated random state:
Random.State rng = Random.newState(42L);
rng.shuffle(myArray);
```

### Timer

```java
Timer spawnTimer = new Timer(2.0f, true);  // looping

@Override
public void update(GameWindow gw, float dt) {
    spawnTimer.update(dt);
    if (spawnTimer.isReady()) {
        spawnEnemy();
        spawnTimer.reset();
    }
}
```

---

## Asset Loading, Screenshots & Game Metadata

### Asset Loading

`Utils` is the central place for loading berryngine assets.

```java
// From the classpath / resources
PixelGraphics a = Utils.loadTextureFromResources("/assets/a.png");
TextureAtlas atlas = Utils.loadTextureAtlasFromResources("/assets/tiles.png", 16, 16);
BitmapFont font = Utils.loadFontFromResources("/assets/font.psf");
Sound snd = Utils.loadSoundFromResources("/assets/sfx.qoa");
MusicSynth midi = Utils.loadMidiFromResources("/assets/theme.mid");

// From the game install folder next to the jar / project root
PixelGraphics b = Utils.loadTextureFromGameInstall("data/b.png");
Sound music = Utils.loadSoundFromGameInstall("data/music.qoa");
MusicSynth installedMidi = Utils.loadMidiFromGameInstall("data/theme.mid");
MusicSynth fileMidi = Utils.loadMidiFromFile(new java.io.File("theme.mid"));
```

### Screenshots

```java
// Saves to working directory with timestamp
Utils.saveScreenshot(pg);

// Or specify a path
Utils.saveScreenshot(pg, "screenshots/level1.png");
```

### Game Information

`GameInformation` stores process-wide metadata and the two base directories used by the engine. Configure it once near the beginning of `main`, before calling any `Utils.load*FromGameInstall(...)` method.

```java
GameInformation.set(
        "BerryPlatformer",          // game name
        "Acme Games",               // author or studio
        "MIT",                      // license label
        null,                        // data folder: choose the OS default
        null,                        // install folder: choose the code/JAR location
        "Code: Alice\nArt: Bob"     // credits text
);
```

`set(...)` replaces all metadata and folder values, then prints the resulting configuration to standard output. Empty data-folder and install-folder strings behave like `null` and select their defaults. The metadata strings themselves are stored as supplied, so pass meaningful non-null values.

The available values are:

```java
String name = GameInformation.getName();
String author = GameInformation.getAuthor();
String license = GameInformation.getLicense();
String credits = GameInformation.getCredits();
java.io.File dataFolder = GameInformation.getGameDataFolder();
java.io.File installFolder = GameInformation.getGameInstallFolder();

GameInformation.printInformation();
```

Before `set(...)` is called, the defaults are:

- Name: `BerryNgineDemo`
- Author: `Unknown Author`
- License: `All Rights Reserved`
- Credits: `No credits provided.`
- Install folder: the parent directory of the running classes or JAR location
- Data folder: the platform default derived from the default author and game name

When no custom data folder is supplied, `GameInformation` builds the following path:

- Windows: `%APPDATA%\Author\GameName`, falling back to `%USERPROFILE%\AppData\Roaming\Author\GameName` when `APPDATA` is unavailable.
- Other operating systems: `$XDG_DATA_HOME/Author/GameName`, falling back to `~/.local/share/Author/GameName` when `XDG_DATA_HOME` is unavailable.

You can override either folder with a path string:

```java
GameInformation.set(
        "BerryPlatformer",
        "Acme Games",
        "MIT",
        "saves",
        "game-data",
        "Acme Games, 2026"
);
```

Custom relative paths are resolved relative to the process working directory. `GameInformation` stores paths but does not create directories. Create a data directory before writing saves or settings:

```java
java.io.File dataFolder = GameInformation.getGameDataFolder();
if (!dataFolder.exists() && !dataFolder.mkdirs()) {
    throw new IllegalStateException("Could not create data folder: " + dataFolder);
}
```

The install folder is used by `Utils.getFileFromGameInstall(...)`, texture, sound, font, and MIDI game-install loaders. Paths passed to those methods are resolved beneath `GameInformation.getGameInstallFolder()`.

---

## Cursor

BerryNgine can render a software cursor on top of everything. This is useful for custom crosshairs or pointer graphics.

```java
PixelGraphics cursorSprite = ShapeGenerator.cross(11, 11, Color.WHITE, 3);
Cursor cursor = new Cursor(cursorSprite, 5, 5);  // hotspot center
window.setSoftwareCursor(cursor);
```

The cursor tracks the mouse by default. You can also position it manually:

```java
cursor.setTrackMouse(false);
cursor.setPosition(100, 50);
cursor.setVisible(false);
```

If you prefer the native cursor, leave the software cursor unset and use standard Swing cursor visibility.

---

## Complete Example Scenes

### Moving a Player with a Camera

```java
import berryngine.*;

import java.awt.event.KeyEvent;

public class PlatformerScene implements Scene {

    Vec2 player = new Vec2(0, 0);
    Vec2 vel = new Vec2(0, 0);
    PixelGraphics playerGfx = ShapeGenerator.rectangle(16, 16, Color.LIME);

    @Override
    public void onSceneEnter(GameWindow gw) {
    }

    @Override
    public void onSceneExit(GameWindow gw) {
    }

    @Override
    public void update(GameWindow gw, float dt) {
        vel.x = Input.getAxisHorizontal() * 120f;
        vel.y += 500f * dt;  // gravity
        player.addScaled(vel, dt);

        if (player.y > 100) {
            player.y = 100;
            vel.y = 0;
        }
        if (Input.isKeyDown(KeyEvent.VK_SPACE) && player.y >= 100) {
            vel.y = -250f;
        }
    }

    @Override
    public void update(GameWindow gw, float dt) {
        Camera2D cam = gw.getCamera();
        cam.follow(player, dt, 3.0f);
    }

    @Override
    public void render(GameWindow gw, FramebufferPixelGraphics pg) {
        pg.clear(Color.MIDNIGHT_BLUE);
        pg.fillRectWorld(-200, 100, 400, 20, Color.GREEN);
        pg.drawImageWorld(playerGfx, player.x, player.y);
    }
}
```

### UI Overlay + Audio Mixer

```java
public class MenuScene implements Scene {

    Sound blip = SoundSynth.blip().build();

    @Override
    public void onSceneEnter(GameWindow gw) {
        gw.audioMixer.addDefaultGroups();
    }

    @Override
    public void update(GameWindow gw, float dt) {
        if (Input.isMouseButtonDown(1)) {
            gw.audioMixer.play("ui", blip, 0.6f);
            gw.setScene(new GameScene());
        }
    }

    @Override
    public void render(GameWindow gw, FramebufferPixelGraphics pg) {
        pg.clear(Color.BLACK);
        pg.renderString(SpriteSheetFont.START2P,
                "CLICK TO START",
                70, 80,
                Color.WHITE);
    }

    @Override public void onSceneExit(GameWindow gw) {}
}
```

### Procedural Texture as a Sprite

```java
PixelGraphics ball = ShapeGenerator.circle(8, Color.ORANGE);

// Cache it once; draw it every frame
@Override
public void render(GameWindow gw, FramebufferPixelGraphics pg) {
    pg.clear(Color.DARK_GRAY);
    for (int i = 0; i < 10; i++) {
        int x = 20 + i * 28;
        int y = 80 + (int)(Mathf.sin(i + gw.gameLoop.getFps() * 0.05f) * 20);
        pg.drawImage(ball, x, y);
    }
}
```

---

## Tips & Best Practices

- **Cache textures and fonts.** `Utils.loadTexture...` parses image data every time it is called. Load assets once in `onSceneEnter` or a static initializer.
- **Use `update` with `dt` for physics and timers.** Movement and collision should scale with the actual frame delta, not `fixedUpdate`.
- **Use `update` for animation and camera smoothing.** These can run at the render framerate.
- **Use helpers, but avoid allocating them per frame.** `Mathf` gives you fast table-based trig, clamps, and lerps. `Color` packs colors into a single `int` with helpers like `fromRGB`, `lerp`, `multiply`, and `withAlpha`. `Vec2` has in-place methods such as `add`, `sub`, `mul`, `addScaled`, and `lerp` that mutate the same instance, so keep reusable vectors for position and velocity math instead of calling `new Vec2(...)` every frame. Convert to `IVec2` only when you need integer pixel coordinates.
- **Cull with the camera.** `pg.isVisibleWorld(...)` lets you skip drawing off-screen objects.
- **Keep internal resolution low.** 320x180 or 426x240 scaled up 3-4x looks authentically retro and is very cheap to fill.
- **Prefer `drawImage` over `setPixel` loops.** The image methods handle clipping and alpha for you.
- **Audio groups are your friend.** Set up `sfx`, `music`, and `ui` groups early so volume controls are trivial later.
- **Use `PostFX` sparingly.** They touch every pixel; apply only what you need.
- **No dependencies means you control the classpath.** Assets can be loaded from resources (inside the jar) or from a directory next to the jar.

---

## Performance

Because BerryNgine is a software renderer, the fastest way to keep a game smooth is to reduce the number of pixels and objects touched every frame. Keep your internal resolution small (for example 320x180 or 426x240) and let the window scale it up, since filling a 1920x1080 buffer in Java is expensive even for simple scenes. Cache every asset you can: load textures, fonts, and `ShapeGenerator` sprites once in `onSceneEnter` or a static initializer, then draw the cached `PixelGraphics` each frame instead of regenerating them. Reuse off-screen buffers instead of `new PixelGraphics(...)` inside the render loop to avoid GC pressure. Cull objects with the camera (`pg.isVisibleWorld(...)`) so you only draw what is on screen, and batch similar draw calls together to avoid repeated setup. Prefer whole-buffer operations like `drawImage` over per-pixel loops, and use `PostFX` only when necessary because they touch every pixel. Finally, profile before optimizing: measure with and without an effect or a group of objects so you are solving the real bottleneck rather than guessing.

---

BerryNgine is intentionally small. It gives you a window, a loop, a pixel buffer, and a set of helpful utilities, then gets out of your way. Build whatever game architecture you like on top of it, from a single-screen arcade prototype to a multi-scene strategy game.

```java
{{ ... }}
