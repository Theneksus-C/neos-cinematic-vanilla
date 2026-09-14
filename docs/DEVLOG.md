# Development Log

A record of errors, obstacles, and non-obvious decisions encountered while
building this mod. Entries are newest last.

Format: symptom, cause, resolution. The goal is that a problem solved once
never has to be diagnosed twice.

---

## 2026-09-14: Ecosystem research before any code

Most Minecraft modding material available online is outdated as of 2026 for
three reasons, all verified against official sources before starting:

1. Version numbering changed from the `1.21.x` scheme to year-based drops.
   Current stable is 26.2, released 2026-06-16.
2. Minecraft 26.1 was the first unobfuscated release. Fabric consequently
   deprecated Yarn mappings. Build scripts must not contain a `mappings` line,
   and `modImplementation` and `remapJar` are replaced by the standard
   `implementation` and `jar`.
3. Minecraft 26.x requires Java 25. Java 17 and Java 21 will not run it.

Any tutorial predating 2026 will be wrong on all three points.

---

## 2026-09-14: `java -version` reported command not found after a successful install

**Symptom:** `winget install EclipseAdoptium.Temurin.25.JDK` reported success,
but `java -version` returned "The term 'java' is not recognized".

**Cause:** the Temurin MSI exposes PATH registration and JAVA_HOME registration
as separate optional features. Installed silently through winget, it set
`JAVA_HOME` at machine scope but did not append the JDK `bin` directory to
`PATH`. The JDK was fully installed and functional the entire time, just not
resolvable by bare command name.

**Resolution:** appended `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin`
to the user-scope `PATH`. Machine-scope `JAVA_HOME` was already correct and was
left untouched.

**Note:** a newly spawned process inherits its parent's environment block, so
an already-open terminal will not observe the change. A terminal launched fresh
after the edit is required.

**Generalisation:** "command not found" on Windows usually indicates a PATH
problem rather than a missing installation. Verify presence on disk before
reinstalling anything.

---

## 2026-09-14: Project folder could not be renamed while a handle was open

**Symptom:** renaming the project directory failed three different ways.
Bash reported "Device or resource busy", PowerShell reported "The process
cannot access the file because it is being used by another process", and after
moving every shell out of the directory it still reported "Access to the path
is denied".

**Cause:** Windows refuses to rename a directory while any process holds an
open handle to it. The handle here was not a shell working directory, since
moving both shells to the parent did not release it. Probing showed that files
inside the directory could be created and deleted freely, which narrowed it to
a handle on the directory itself rather than a permissions problem. A file
watcher belonging to the editor was the likely holder.

**Resolution:** copied the directory to the new name, verified the copy had
complete Git history and a clean working tree, then deleted the original.
Deletion succeeded even though renaming did not, which confirms the lock was
specific to the rename operation on the directory entry.

**Generalisation:** on Windows, a directory rename is far more fragile than
creating, writing, or deleting files inside that directory. When a rename is
refused and no shell is sitting in the folder, copy and delete instead of
hunting for the process holding the handle. Always verify `git log` in the copy
before removing the original.

---

## 2026-09-14: Rewritten commit history did not clear the GitHub contributor list

**Symptom:** after stripping a `Co-Authored-By` trailer from every commit with
`git filter-branch` and force pushing, the repository still displayed a second
contributor on its GitHub page.

**Cause:** two separate caches. GitHub recomputes the contributor list
asynchronously rather than on push, so the badge continued to serve stale data
while the underlying commits were already clean. Querying the contributors API
directly returned an empty list, confirming a rebuild was in progress rather
than a failed rewrite.

Separately, `git filter-branch` preserves the pre-rewrite state under
`.git/refs/original/`. Those refs keep the old commits reachable, so
`git log --all` still showed the removed trailer locally even though both
branches pointed at rewritten commits.

**Resolution:** verified the rewrite through the GitHub commits API rather than
the rendered page, then deleted `.git/refs/original` to drop the local backup
refs. No new repository was needed.

**Generalisation:** verify a push through the API, not the web UI, when a page
element looks stale. After any `filter-branch`, delete the backup refs or the
old history stays reachable locally and looks like the rewrite failed.

---

## 2026-09-14: Camera.getPosition() does not exist in Mojang mappings

**Symptom:** `cannot find symbol: method getPosition(), location: variable
camera of type Camera` when compiling the first fog mixin.

**Cause:** Mojang's official names frequently omit the `get` prefix that Yarn
mappings used. The accessor is `Camera.position()`, not `getPosition()`. Yarn
called it `getPos()`. Since Yarn was deprecated at 26.1, every pre-2026
tutorial and every mod still targeting 1.21 or earlier uses names that no
longer resolve.

**Resolution:** used `camera.position()`.

**Generalisation:** do not guess Minecraft method names from memory or from
older mods. Run `javap -classpath <minecraft-merged.jar> <fully.qualified.Class>`
against the Loom cache at `~/.gradle/caches/fabric-loom/26.2/` to read the real
signatures. Full decompiled sources are available through `gradlew genSources`.

---

## 2026-09-14: Visible brightness step at the altitude ceiling

**Symptom:** a sudden change in brightness when crossing roughly y=135 to y=139
on the surface. Fog appeared to switch between two states rather than fade.

**Cause:** the fog band was interpolated between an assumed clear band and a
dense band. The assumed clear band used a near edge of one render distance,
but vanilla's actual Overworld default is a near edge of 0 with a far edge of
1024. Those are not the same thing. A 0 to 1024 band still produces roughly a
quarter of full fog at 256 blocks, whereas a band starting at 256 produces
almost none. Below the altitude ceiling the mod therefore replaced vanilla's
faint gradient with something clearer than vanilla, and above the ceiling it
did nothing, so crossing the boundary flipped between the two.

A second defect shared the same root. The assumed clear band scaled with render
distance while vanilla's 1024 is absolute, so the two would also diverge at any
render distance other than 16 chunks.

**Resolution:** interpolate away from the values vanilla actually produced
instead of from an assumed baseline:

```java
float start = Mth.lerp(density, fog.environmentalStart, denseStart);
float end   = Mth.lerp(density, fog.environmentalEnd,   denseEnd);
```

At zero density this is identical to vanilla by construction, at any render
distance, and it preserves vanilla's rain offset underneath rather than
competing with it.

**Generalisation:** when a mod blends with vanilla behaviour, one end of the
blend must be vanilla's live output, not a constant chosen to approximate it.
Any approximation becomes a discontinuity at the point where the effect fades
out, and that point is exactly where it is most visible.

---

## 2026-09-14: Screen state moved off Minecraft in 26.2

**Symptom:** `cannot find symbol: variable screen, location: variable client of
type Minecraft` when checking whether a screen was already open.

**Cause:** 26.2 moved screen handling out of `Minecraft` and onto the `Gui`
class. `Minecraft.screen` no longer exists in any form, public or private.
Reading vanilla's own `setScreenAndShow` showed it delegating to
`this.gui.setScreen(screen)`.

**Resolution:** `Minecraft.getInstance().gui.screen()` returns the current
screen, and `setScreenAndShow` remains on `Minecraft`.

**Generalisation:** when a field vanishes between versions, find a vanilla
method that must still use it and read what that method delegates to. That is
faster than guessing at renamed accessors, and it confirms the replacement is
the one vanilla itself uses.

---

## 2026-09-15: Settings screen darkened with every preset press

**Symptom:** pressing any preset button made the screen background darker.
Repeated presses darkened it further, while the option values themselves were
correct.

**Cause:** `Screen.rebuildWidgets` is unsafe on an `OptionsSubScreen`. It
clears the screen's own widget list and calls `init()` again, but
`OptionsSubScreen` holds its layout in a field initialised once:

```java
public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
```

and its `init()` adds to that layout rather than replacing its contents:

```java
protected void init() {
    this.addTitle();
    this.addContents();   // layout.addToContents(new OptionsList(...))
    this.addFooter();     // layout.addToFooter(...)
    this.layout.visitWidgets(this::addRenderableWidget);
}
```

So each rebuild appended a second title, a second options list, and a second
pair of footer buttons. The duplicated list backgrounds are semi transparent
and rendered over each other, darkening the screen by one more layer per press.
`HeaderAndFooterLayout` has no method to clear itself.

**Resolution:** replace the screen instead of rebuilding it, which constructs a
fresh layout:

```java
this.minecraft.setScreenAndShow(new ConfigScreen(this.lastScreen));
```

**Generalisation:** `rebuildWidgets` is only safe when a screen's `init` builds
everything from scratch. Where a screen accumulates into a field that outlives
`init`, rebuilding duplicates rather than refreshes. Check what a base class
keeps across `init` before calling it twice. The visible symptom was opacity,
which points nowhere near the actual cause, so the lead was the fact that
repeated presses compounded it.

---

## 2026-09-15: Settings reverted when the screen closed

**Symptom:** a value changed in the settings screen took effect immediately, so
leaves genuinely stopped at a frequency of 0, but pressing Done reverted it.
The config file on disk held the values from the last preset press rather than
the edit.

**Cause:** two faults compounding.

`load()` replaced the whole config object:

```java
instance = parsed;
```

which swaps the fog, particle, and leaf section objects for new ones. The
screen's widgets had captured references to the previous objects in their
setter lambdas, so after any reload they were writing into objects no longer
attached to anything. The change appeared to work because the widget's own
displayed value updated, and the effect appeared to work for as long as the
old object was still the live one.

Reloads happen on a one second timer driven from the fog controller, which
keeps running while the screen is open because the world renders behind it. A
reload during editing therefore silently detached every widget, and the
subsequent save on close wrote the reloaded values.

**Resolution:** setters now resolve the settings object at call time rather
than capturing it:

```java
v -> CinematicConfig.fog().intensity = v
```

so object replacement cannot orphan them. Separately, reloading is suspended
while the settings screen is open, since a person editing in game is a more
authoritative source than the file.

**Generalisation:** any object handed to a UI widget becomes a reference that
outlives the call. Config systems that reload by replacing their root will
break every such reference, and the breakage is silent because writes still
succeed, just into an object nothing reads. Either mutate in place on reload,
or resolve through an accessor at the point of use.

---

## 2026-09-15: Wind sound never played, and wind strength oscillated

Two faults found together while investigating silence.

### The sound was rejected at submission

**Symptom:** no wind audio at all, and no warning in the log. The sound event
resolved, since a missing one logs "Unable to play unknown soundEvent".

**Cause:** `SoundEngine.play` refuses any instance whose volume is zero at the
moment it is submitted:

```java
if (volume == 0.0F) {
    if (!instance.canStartSilent() && soundSource != SoundSource.MUSIC) {
        LOGGER.debug("Skipped playing sound {}, volume was zero.");
        return SoundEngine.PlayResult.NOT_STARTED;
    }
}
```

The instance deliberately starts silent so that entering a world does not begin
with a blast, which meant it was never started at all. The rejection is logged
at debug level, so nothing appeared in the log.

**Resolution:** override `canStartSilent()` to return true. Vanilla uses the
same override for its own fading loops, including the bee and minecart sounds.

### Strength ran away to the clamp and back to zero

**Symptom:** debug logging reported `strength=1.000` in clear weather, where
roughly 0.29 was expected.

**Cause:** one field served as both the eased accumulator and the scaled
output:

```java
strength += (strengthTarget - strength) * STRENGTH_EASE;
strength = clamp(strength * gust * exposure * config.strength, 0, 1);
```

Storing the scaled result back into the accumulator applies the gust factor
again on every subsequent tick, compounding it. Simulating three thousand ticks
showed the value swinging between 0.0095 and a clamped 1.0 rather than settling.

**Resolution:** keep the eased base in its own field and derive the output from
it, so nothing scaled is ever fed back:

```java
baseStrength += (strengthTarget - baseStrength) * STRENGTH_EASE;
strength = clamp(baseStrength * gust * exposure * config.strength, 0, 1);
```

The corrected loop settles between 0.19 and 0.40 in calm clear weather.

**Generalisation:** never store a scaled value back into the variable being
eased. A smoothing filter has to keep its own state, and anything applied on top
belongs in a separate output. Simulating the loop in isolation found this in
seconds, where reading the code had not.
