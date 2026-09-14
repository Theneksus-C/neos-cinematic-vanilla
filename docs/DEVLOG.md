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
