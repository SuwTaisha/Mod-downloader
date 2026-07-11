# Modsoft — Modrinth Mod Downloader

A Windows desktop app for browsing, installing, and updating Fabric mods from
[Modrinth](https://modrinth.com), with a custom-themed Swing UI (dark/light,
Vietnamese/English).

## Features

- **Download tab** — search/sort/paginate Fabric mods, filter by Minecraft
  version, pick the exact version to install (or drag a `.jar` in directly),
  resolve required/optional dependencies (honoring the author-pinned version
  when one exists).
- **Update tab** — scan an existing mods folder, check every installed mod
  for updates against a chosen Minecraft version, update selected mods, or
  delete installed mods.
- Detects mods declared **incompatible** with each other and lets you drop
  one side of the conflict before installing.
- Detects **version conflicts** (upgrade/downgrade) against what's already on
  disk, with a choice to delete or archive the replaced file.
- Native Windows folder picker, drag-and-drop, toast notifications, and a
  `logs/modsoft.log` file for troubleshooting failed downloads.

## Requirements

- Windows 10/11
- To run the **portable JAR** release: Java 17 or newer installed
  (`java -version` to check).
- To run the **native .exe** release: nothing — a Java runtime is bundled
  inside it.

## Using the app

1. Download a release from the GitHub Releases page (see below for the two
   variants). Both are single files — no extraction or extra folders needed.
2. Launch it:
   - **Native build**: run `Modsoft-<version>.exe`. This is an installer —
     it installs the app (Start Menu shortcut, desktop shortcut) and needs
     nothing else alongside it. No Java required.
   - **Portable build**: double-click `modsoft.jar`, or run
     `java -jar modsoft.jar`. Needs Java 17+ installed.
3. Choose an install directory first (folder picker, or type/paste a path —
   `%appdata%`-style variables are expanded). Mod selection stays disabled
   until a directory is confirmed.
4. **Download tab**: search or browse mods, double-click (or drag) one into
   the right-hand list, pick the version you want, then click *Install*.
5. **Update tab**: click *Rescan* to read the folder's mods, *Check for
   updates* to compare against Modrinth, tick the ones you want, then
   *Update selected*. Use *Delete selected* (multi-select with Ctrl/Shift)
   to remove mods from the folder.

## Development

### Tech stack

- Plain Java (Swing), no build tool (no Maven/Gradle) — just `javac`/`java`
  and a handful of vendored jars under `lib/`.
- [FlatLaf](https://www.formdev.com/flatlaf/) for theming (custom
  light/dark palette in `theme/`).
- [Gson](https://github.com/google/gson) for JSON.
- [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) for
  decoding WEBP mod thumbnails (the JDK has no built-in WEBP reader, and most
  Modrinth icons are WEBP).
- [Modrinth API v2](https://docs.modrinth.com/api/) for search, versions,
  dependencies, and file-hash lookups.

### Project layout

All classes live in the default package (no `src/` tree):

| File | Purpose |
|---|---|
| `Main.java` | Entry point; sets up FlatLaf, fonts, theme |
| `ModDownloaderFrame.java` | The whole UI (both tabs) and app logic |
| `Modrinth.java` | Modrinth API client + DTOs |
| `I18n.java` | Vietnamese/English string tables |
| `InstallManifest.java` | `modsoft-manifest.json` read/write (tracks installed versions) |
| `NativeFolderPicker.java` | Shells out to a hidden PowerShell/.NET dialog for the real Windows folder picker |
| `AppDialog.java` / `Toast.java` | Custom-styled modal dialogs / floating notifications |
| `BusyGlassPane.java` | Frame-wide "busy" overlay + spinner |
| `Icons.java` / `Fonts.java` | Hand-drawn vector icons, bundled Inter font |
| `Logger.java` | Appends to `logs/modsoft.log` |
| `GenerateIcon.java` | One-off tool: resizes `icon.jpg` into `icon.ico` (multi-size, PNG-in-ICO) for the packaged app's icon |

Resources (`resources/fonts/*.ttf`, `theme/*.properties`) are loaded off the
classpath, so they work both when running from `out/` in development and
once bundled inside the packaged jar.

`run.bat` runs the app straight off `out/` with the vendored jars on the
classpath (`-cp "out;.;lib\...`), which is convenient for development but
**not** how the packaged release works — see below.

### Building and running from source

Requires a JDK 17+ (JDK, not just a JRE) on `PATH`.

```bat
:: compile + run (development loop)
run.bat
```

Dependency jars are already vendored under `lib/` — there's nothing to
fetch. See the top of `run.bat` for the exact classpath if you need it for
an IDE run configuration.

### Building a release

Two packaging scripts are provided; run them from the project root.

**Portable JAR** (needs Java 17+ on the user's machine):

```bat
package.bat
```

Produces a single self-contained `dist/modsoft.jar` — it's a fat jar (our
classes plus every dependency's classes merged in, per `jar xf` in the
script), so `java -jar modsoft.jar` works with no separate `lib/` folder and
no `-cp` needed. That single file is the release asset.

> Earlier builds shipped `modsoft.jar` next to a `lib/` folder with a
> `run.bat` launcher, relying on a `Class-Path` manifest entry. That's what
> caused the "opens and immediately disappears" bug: double-clicking a jar
> (or `java -jar` without an explicit `-cp`) ignores everything except the
> manifest, and the manifest didn't declare `lib/*.jar` — so it threw
> `NoClassDefFoundError` with no console to show it. The fat-jar approach
> above avoids the whole class of bug.

**Native Windows installer** (single `.exe`, bundles its own Java runtime —
nothing required on the user's machine):

```bat
package.bat
package-exe.bat
```

Requires the [WiX Toolset v3](https://wixtoolset.org/) (`candle.exe` /
`light.exe`) — install it once with:

```bat
winget install --id WiXToolset.WiXToolset
```

`package-exe.bat` adds WiX's default install location to `PATH` for its own
run, so nothing else needs configuring. It calls `jpackage --type exe` with
`--icon icon.ico`, producing a single installer at
`release-exe/Modsoft-1.0.0.exe`. That one file is the release asset —
running it installs the app (Start Menu + desktop shortcut, no console
window) with nothing else needed alongside it.

> An earlier iteration used `jpackage --type app-image`, which doesn't need
> WiX but produces a *folder* (`.exe` plus `app/` and `runtime/` subfolders)
> instead of one file — easy to accidentally split up when distributing.
> The installer approach above avoids that.

### Publishing a GitHub release

1. `package.bat && package-exe.bat`
2. Attach `dist/modsoft.jar` directly (already a single file)
3. Attach `release-exe/Modsoft-1.0.0.exe` directly (already a single file)
4. Create a new release on GitHub and attach both.
