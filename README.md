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
   variants) and extract the zip.
2. Launch it:
   - Native build: run `Modsoft.exe`.
   - Portable build: run `run.bat` (or `java -cp "modsoft.jar;lib\*" Main`).
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

Resources (`resources/fonts/*.ttf`, `theme/*.properties`) are loaded off the
classpath, so they work both when running from `out/` in development and
once bundled inside the packaged jar.

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

Produces `dist/modsoft.jar`, `dist/lib/*.jar`, and `dist/run.bat`. Zip the
`dist` folder — that's the release asset.

**Native Windows app** (bundles its own Java runtime, nothing required on
the user's machine):

```bat
package.bat
package-exe.bat
```

Uses `jpackage` (bundled with the JDK since Java 14) to produce an
app-image at `release-exe/Modsoft/Modsoft.exe`. Zip the `release-exe/Modsoft`
folder — that's the release asset.

By default `package-exe.bat` keeps a console window behind the app (useful
while testing — you'll see any startup errors). Drop the `--win-console`
flag inside the script for a clean release build with no console window.

> `jpackage --type msi`/`--type exe` (a proper installer) additionally
> requires the [WiX Toolset](https://wixtoolset.org/) to be installed. The
> app-image build above doesn't need it — it's just a folder you unzip and
> run, which is the simpler option for a GitHub release.

### Publishing a GitHub release

1. `package.bat && package-exe.bat`
2. Zip `dist/` → e.g. `modsoft-portable-v1.0.0.zip`
3. Zip `release-exe/Modsoft/` → e.g. `modsoft-windows-v1.0.0.zip`
4. Create a new release on GitHub and attach both zips.
