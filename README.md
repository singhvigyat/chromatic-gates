# Chromatic Gates

A small **computer graphics lab** demo: a timing game built with **Java**, **LWJGL 3**, **GLFW**, and **OpenGL 3.3 Core** (programmable shaders, 2D quads). Move to match the colored gap in falling “gates” and switch your color with number keys.

---

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **JDK 17+** | This project targets Java 17 (`maven.compiler.source` / `target` in `pom.xml`). |
| **Apache Maven 3.6+** | Used to download dependencies and run the game. |
| **GPU with OpenGL 3.3** | The window requests an OpenGL **3.3 Core** profile. Update graphics drivers if the window fails to create. |
| **Windows (x64)** | `pom.xml` currently bundles **LWJGL natives for Windows** only. See [Other operating systems](#other-operating-systems) for Linux/macOS. |

---

## Installation from scratch

### 1. Install Java 17

1. Download a **JDK 17** distribution (e.g. [Eclipse Temurin](https://adoptium.net/), Amazon Corretto, or Oracle JDK).
2. Run the installer and ensure **“Add to PATH”** (or equivalent) is enabled, or note the install path.
3. Open a **new** terminal and verify:

   ```bash
   java -version
   ```

   You should see a version line mentioning **17** (or newer).

4. Set `JAVA_HOME` if your tools do not find Java (Windows example — adjust the path):

   ```powershell
   [System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot", "User")
   ```

   Restart the terminal after changing environment variables.

### 2. Install Maven

1. Download Maven from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) (binary zip).
2. Extract to a folder, e.g. `C:\Program Files\Apache\maven`.
3. Add Maven’s `bin` directory to your **PATH**.
4. Verify:

   ```bash
   mvn -version
   ```

### 3. Get the project

Clone the repository (or unzip an archive) and go into the project folder:

```bash
git clone https://github.com/singhvigyat/chromatic-gates.git
cd chromatic-gates
```

*(If you already have the folder from your course, `cd` into that directory instead.)*

### 4. Build and run

From the project root (where `pom.xml` is):

```bash
mvn -q clean package
mvn -q exec:java
```

Or in one step (compile + run via the exec plugin):

```bash
mvn -q compile exec:java
```

The configured main class is `com.cglab.chromaticgates.Main`.

### 5. Optional: run the packaged JAR

After `mvn package`, Maven produces `target/chromatic-gates-1.0-SNAPSHOT.jar`. Dependencies are not shaded into that JAR by default, so **prefer `mvn exec:java`** for running unless you add a fat-jar plugin.

---

## Controls

| Action | Keys |
|--------|------|
| Move left / right | **A** / **D** or **←** / **→** |
| Select player color | **1**/**2**/**3** (top row or keypad) · **Q** red · **W** green · **E** blue |
| Restart run | **R** |

Close the window to quit.

### How to play

1. **Click the game window** so keyboard input goes to the game (not the terminal or browser).
2. Each falling row is dark **walls** with a **colored gap**. The gap color is the one you must match.
3. Use **1/2/3** (or keypad, or **Q/W/E**) so your **ship color** matches the **gap color**. The top HUD shows three chips; the one with the **gold border** is your current color.
4. Move with **A/D** (or arrows) so you pass through the **gap**, not the walls.
5. Passing through the gap with the **wrong** ship color costs a life, same as hitting a wall. Match first, then slide through.
6. Press **R** to restart after game over or anytime you want a fresh run.

---

## Project layout

```
src/main/java/com/cglab/chromaticgates/
  Main.java          # GLFW window + OpenGL context + main loop
  Game.java          # Rules and state
  GameConfig.java    # Window size, speeds, tuning
  Input.java         # Keyboard
  Renderer2D.java    # 2D drawing
  ShaderProgram.java # GLSL compile/link
  Gate.java, Player.java
pom.xml              # Java 17, LWJGL 3.3.4, exec plugin
```

---

## Troubleshooting

| Problem | What to try |
|---------|----------------|
| `JAVA_HOME` / “invalid target release” | Install JDK **17+** and point `JAVA_HOME` at it; use `java -version` and `mvn -version` to confirm. |
| Window fails or OpenGL errors | Update **GPU drivers**. Confirm OpenGL **3.3** support. |
| `UnsatisfiedLinkError` / missing natives | You are likely not on **Windows x64**, or natives do not match the dependency set. See below. |
| Maven download failures | Check firewall/proxy; retry `mvn -U compile`. |

### Other operating systems

This repository’s `pom.xml` lists LWJGL artifacts with **`natives-windows`**. On **Linux** or **macOS**, add the matching native classifiers for your platform (e.g. `natives-linux` / `natives-macos` / `natives-macos-arm64`) alongside the existing dependencies, or use **Maven profiles** to pick one OS per build. See [LWJGL customize](https://www.lwjgl.org/customize).

---

## Development tips

- **IDE**: Import as a **Maven** project (IntelliJ IDEA, Eclipse, VS Code with Java extension pack).
- **Format / encoding**: Source encoding is UTF-8 (`project.build.sourceEncoding`).

---

