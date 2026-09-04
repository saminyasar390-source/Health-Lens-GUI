# HealthLens — IntelliJ IDEA + Scene Builder Manual

This project is set up as a **Maven** project, which is the easiest way to
get JavaFX working in IntelliJ without fighting module paths by hand.

```
HealthLens/
├── pom.xml
├── README_MANUAL.md
└── src/main/
    ├── java/com/healthlens/
    │   ├── Main.java                  ← app launcher
    │   └── HealthLensController.java  ← all the GUI logic
    └── resources/com/healthlens/
        ├── HealthLens.fxml            ← the GUI layout (edit this in Scene Builder)
        ├── styles.css                 ← colors, fonts, look & feel
        └── background.jpg             ← ADD YOUR OWN IMAGE HERE (see note inside the folder)
```

---

## 1. One-time setup

1. **Install a JDK 17+** (JDK 21 is fine too). IntelliJ can download one for
   you when you create/open the project (File → Project Structure → SDKs →
   "+" → Download JDK).
2. **Install Scene Builder** (free, from Gluon):
   https://gluonhq.com/products/scene-builder/
   Just install it like a normal desktop app — you don't need to configure
   anything inside it yet.
3. You do **not** need to manually download JavaFX jars — Maven does that
   automatically from `pom.xml` the first time you open the project.

---

## 2. Opening the project in IntelliJ

1. `File → Open...` and select the `HealthLens` folder (the one containing
   `pom.xml`).
2. IntelliJ will detect it's a Maven project and show a small popup / bell
   icon ("Load Maven Project") — click it, or click the Maven icon on the
   right sidebar → the refresh (⟳) icon. This downloads JavaFX automatically.
3. Wait for indexing to finish (progress bar at the bottom).

---

## 3. Connecting IntelliJ ↔ Scene Builder

This is the part people usually get confused about — it's actually simple:

1. Go to **File → Settings** (Windows/Linux) or **IntelliJ IDEA → Settings**
   (Mac) → **Languages & Frameworks → JavaFX**.
2. In the "Path to SceneBuilder" field, browse to wherever you installed
   Scene Builder, e.g.:
   - Windows: `C:\Users\<you>\AppData\Local\SceneBuilder\SceneBuilder.exe`
   - Mac: `/Applications/SceneBuilder.app`
   - Linux: wherever you extracted/installed it
3. Click OK.
4. Now, right-click on `HealthLens.fxml` in the project tree (under
   `src/main/resources/com/healthlens/`) → you'll see an option
   **"Open in SceneBuilder"**. That's it — IntelliJ just launches Scene
   Builder pointed at that file.

**How the two tools divide the work:**
- **Scene Builder** = drag-and-drop layout editor. You use it to move things
  around, resize, add new buttons/labels/sliders, and change basic layout
  properties visually.
- **IntelliJ** = where you write the actual logic (`HealthLensController.java`)
  and fine-tune styling (`styles.css`). You also use IntelliJ to run the app.

They both just edit/read the same `HealthLens.fxml` text file — there's no
special "sync" step. Save in one, it's updated when you reopen/refresh in
the other.

---

## 4. The one rule that matters: `fx:id` ↔ `@FXML` fields

Every interactive element in `HealthLens.fxml` has an `fx:id`, e.g.:

```xml
<Slider fx:id="sleepSlider" .../>
```

And in `HealthLensController.java` there's a matching field:

```java
@FXML private Slider sleepSlider;
```

**If you add a new control in Scene Builder:**
1. Select it, and in the right-hand "Properties" panel (or "Code" panel),
   set its `fx:id`.
2. In `HealthLensController.java`, add a matching field:
   `@FXML private Button myNewButton;`
3. If it needs to trigger code (like the "Update Dashboard" button does via
   `onAction="#handleUpdate"`), either set the "On Action" property in Scene
   Builder's Code tab, or type it directly in the FXML.

**If you rename an `fx:id`,** rename the matching Java field too — the names
must match exactly, or the app will crash on launch with a clear error
telling you which field.

---

## 5. Running the app

**From IntelliJ (easiest):**
- Open `Main.java` → click the green ▶ arrow next to `public static void main`.
- If IntelliJ complains about "module javafx.controls not found," just
  re-run — this usually resolves itself once Maven has finished downloading
  dependencies. If it persists, open the Maven tool window (right sidebar) →
  refresh (⟳).

**From a terminal (also works, uses the Maven plugin already configured):**
```bash
mvn clean javafx:run
```

---

## 6. Adding your background image

1. Put your image file into:
   `src/main/resources/com/healthlens/`
2. Name it exactly `background.jpg`
   (a note file in that folder — `PUT_YOUR_BACKGROUND_IMAGE_HERE.txt` —
   explains this too; delete it once you've added your real image).
3. Run the app — the image will automatically stretch to fill the window
   and stay full-size even when you resize the window, because
   `HealthLensController.java` binds the `ImageView`'s width/height to the
   window size:
   ```java
   backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
   backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());
   ```
4. If no image is found, the app doesn't crash — it just shows a plain
   gradient background (defined in `styles.css` under `.root-pane`) so you
   can develop/test without an image ready yet.

There's also a semi-transparent dark "scrim" layer between the image and
the UI card (`.scrim-pane` in `styles.css`) so text stays readable no matter
what photo you use — you can adjust its opacity there.

---

## 7. Where to make common changes

| I want to...                                   | Edit this file |
|--------------------------------------------------|----------------|
| Move/resize/add a button, slider, label, etc.    | `HealthLens.fxml` (via Scene Builder, or by hand) |
| Change colors, fonts, rounded corners, etc.      | `styles.css` |
| Change scoring thresholds (e.g. sleep goal hours) | `HealthLensController.java` (constants at the top) |
| Change what the summary text says                | `HealthLensController.java` → `renderSummary()` |
| Add a new metric (e.g. "steps")                   | Add control in FXML + matching `@FXML` field + include it in `handleUpdate()` |
| Swap the background image                        | Replace `background.jpg` in resources folder |

---

## 8. Asking me for changes later

Because everything is organized by responsibility (layout in the FXML,
logic in the controller, look-and-feel in the CSS), you can just tell me
things like:
- "Make the sleep slider go up to 14 hours instead of 12"
- "Add a fifth metric for steps walked"
- "Change the color scheme to a purple theme"
- "Make the summary box show a warning icon when stress is high"

...and I can point you to (or directly edit) the exact section that needs
to change.
