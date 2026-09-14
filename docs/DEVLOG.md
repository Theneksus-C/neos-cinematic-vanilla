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
