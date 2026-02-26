# Physics Game

A small experimental physics sandbox built with libGDX. This project uses only libGDX's graphics and collision features (no built-in physics engine like Box2D) to let users draw objects into the world and see simple physics applied: gravity, collisions, and restitution. It's meant as a learning / demo project for experimenting with collision response and simple object behaviors.

Summary

- Project structure:
  - `core` — shared application logic and game code.
  - `lwjgl3` — desktop launcher using LWJGL3 (starts the application on desktop).
- Purpose: allow a user to draw an object at a specified location; a randomly selected shape is created and physics (gravity, simple collision resolution) is applied so it interacts with the world (including a slanted floor example at the bottom of the screen).

How the game works (brief)

- On launch the application opens a window and starts the render loop.
- The world contains a static slanted floor near the bottom of the screen used to demonstrate collisions and resting contact.
- When the player clicks/taps a supported input location (mouse / touch), the game creates a new dynamic object at that location. The object type or appearance is chosen at random from the available set (for example: circle, rectangle, or an image sprite).
- Each dynamic object is simulated by simple physics code: position, velocity, gravity, and collision detection/response against static and other dynamic objects. Collisions try to prevent overlaps and apply simple impulse-based responses so objects bounce, slide, and rest naturally.

Play controls

- Left click (or primary touch) — spawn a random object at the pointer location.
- Optional keys / mouse controls the project may support (implementation dependent):
  - R — reset/clear dynamic objects.
  - Space — toggle pause simulation.

See the source for exact controls if you want to change or extend them.

Run & build

This project uses Gradle with the included wrapper. From the repository root you can:

- Run from Gradle (desktop):

  On Windows (PowerShell):

  ./gradlew.bat lwjgl3:run

  or if using the wrapper script:

  ./gradlew lwjgl3:run

- Build a runnable jar (desktop):

  ./gradlew.bat lwjgl3:jar

The built jar will be in `lwjgl3/build/libs/`.

IDE setup (IntelliJ IDEA recommended)

1. Open IntelliJ IDEA -> "Open or Import" -> select the project root folder (the Gradle project). Allow IntelliJ to import the Gradle project.
2. Ensure a suitable JDK is configured for the project (Java 8+ is typical; match the project's Gradle settings). Set the Project SDK in File → Project Structure if needed.
3. If IntelliJ asks to auto-import Gradle changes, accept it so dependencies and modules are configured.
4. Run configuration:
   - Create or use the existing `lwjgl3` run configuration that launches `io.github.physics_game.lwjgl3.Lwjgl3Launcher` (or use the Gradle task `lwjgl3:run`).
5. Run/debug directly from the IDE for iterative development.

Project layout (quick dev guide)

- `core/src/main/java` — main game logic, rendering, input handling, physics and collision code.
- `lwjgl3/src/main/java` — desktop launcher classes. Keep platform-specific code (window size, icon loading) here.
- `assets/` and `lwjgl3/src/main/resources` — images and other assets used by the game (sprites, icons).

How to contribute

Thanks for your interest! A few suggestions to get started:

- Workflow:
  1. Fork the repository.
  2. Create a feature branch: `git checkout -b feat/your-feature`.
  3. Implement your feature or fix.
  4. Add or update tests if applicable.
  5. Open a pull request back to the main repository with a clear description.

- Development notes:
  - Keep platform-independent game logic in `core` so it remains reusable.
  - If adding assets, put them in `lwjgl3/src/main/resources` (or `assets/` for shared assets) and reference them from `core` through the asset paths used in the desktop launcher.
  - Follow existing code style: Java, small classes, descriptive names.

- IDE tips:
  - Use IntelliJ's Gradle integration for running and debugging.
  - Make sure to run `./gradlew build` before opening a PR to confirm the project compiles.

Extending the game (ideas)

- Add configurable physics parameters: gravity strength, restitution, friction.
- Implement different object types (polygons, compound shapes) and a small editor to configure them.
- Add serialization to save and load scenes.
- Integrate a proper physics engine (Box2D) as an advanced mode.

Troubleshooting

- Gradle import issues: try refreshing the Gradle project in the IDE and verify the JDK version.
- Missing assets: ensure `lwjgl3/src/main/resources` and `assets/` contents are available to the run configuration's classpath.

License & Contact

- This project is experimental and released without a strict license in the repository. If you want a specific license added, open an issue or PR.
- For questions, open an issue in the repository describing the problem or idea.


(Short development note) If you're looking for the exact input-to-object mapping or want to tweak the slanted floor placement, check `core/src/main/java/io/github/physics_game` and `lwjgl3/src/main/java/io/github/physics_game/lwjgl3` for the launcher and main application class implementations.
