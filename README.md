# Physics Sketch Game

Physics Sketch Game is a libGDX-based prototype inspired by games like Brain It On.
The long-term goal is to let players draw in a constrained area, spawn randomized objects, and watch them interact using custom physics and collision logic.

Current prototype status:
- Desktop app built with libGDX + LWJGL3.
- A slanted floor test setup at the bottom of the screen.
- A custom concave-polygon contact detector (ear clipping + SAT) called every frame from the render loop.

## Project Structure

- `core/` - shared game logic, rendering, and collision/math code.
- `lwjgl3/` - desktop launcher and platform-specific window configuration.
- `assets/` - shared textures/assets.

## How the Game Works (Brief)

The intended gameplay loop is:
1. Player points/clicks (or touches) in a valid draw zone.
2. The game spawns a random object at that location.
3. Physics updates move objects over time.
4. Collisions are detected and resolved so objects can slide, stack, and bounce.

In the current codebase, the focus is collision math experimentation:
- A concave polygon body is moved downward each frame.
- It is checked against a slanted floor polygon.
- On contact, the game logs penetration depth and contact normal continuously.

## Collision System Notes

The custom detector in `core/src/main/java/io/github/physics_game/collision/` is independent of Box2D contact callbacks:
- Concave polygons are decomposed into triangles with ear clipping.
- Triangle pairs are broad-phase filtered with AABB overlap.
- Narrow-phase SAT checks produce overlap depth and normal.
- The final contact normal follows the convention `A -> B` for future impulse-based simulation.

## How To Play

Right now this is a prototype collision demo:
- Run the desktop launcher.
- Watch the moving test shape descend toward the slanted floor.
- Open logs/console to see continuous contact output once overlap begins.

As user drawing/spawning controls are expanded, this section should be updated with exact controls.

## Run and Build

From the project root (Windows PowerShell):

```powershell
.\gradlew.bat lwjgl3:run
.\gradlew.bat lwjgl3:jar
```

Output jar path:
- `lwjgl3/build/libs/`

## IntelliJ IDEA Setup (Contributors)

1. Open IntelliJ IDEA and choose **Open** on the repository root.
2. Import as a Gradle project when prompted.
3. Ensure a valid JDK is set in **File -> Project Structure**.
4. Let IntelliJ finish Gradle sync.
5. Run `lwjgl3:run` or launch `io.github.physics_game.lwjgl3.Lwjgl3Launcher`.

## Contributing

1. Fork the repository.
2. Create a branch, for example:
   - `feat/custom-contact`
   - `fix/collision-normal`
3. Implement your change in `core` when platform-independent.
4. Verify it builds and runs.
5. Open a pull request with:
   - What changed
   - Why it changed
   - How it was tested

Recommended contribution areas:
- Draw-zone input and freehand shape capture.
- Randomized object generation rules.
- Collision response (impulses/friction/resting contact).
- Level goals and win conditions.
