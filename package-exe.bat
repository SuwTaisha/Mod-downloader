@echo off
setlocal
set PATH=%PATH%;C:\Program Files (x86)\WiX Toolset v3.14\bin

if not exist dist\modsoft.jar (
  echo dist\modsoft.jar not found - run package.bat first.
  exit /b 1
)

where candle.exe >nul 2>nul
if errorlevel 1 (
  echo candle.exe not found - the WiX Toolset v3 is required to build a single-file installer .exe.
  echo Install it with: winget install --id WiXToolset.WiXToolset
  exit /b 1
)

echo === Staging the jar for jpackage ===
if exist jpackage-input rmdir /s /q jpackage-input
if exist release-exe rmdir /s /q release-exe
mkdir jpackage-input
copy /y dist\modsoft.jar jpackage-input\ >nul

echo === Running jpackage (bundles its own Java runtime into a single installer .exe) ===
jpackage ^
  --type exe ^
  --input jpackage-input ^
  --dest release-exe ^
  --name Modsoft ^
  --main-jar modsoft.jar ^
  --main-class Main ^
  --app-version 1.0.0 ^
  --vendor "Modsoft" ^
  --icon icon.ico ^
  --win-shortcut ^
  --win-menu || exit /b 1

echo.
echo Done. A single installer file is in the "release-exe" folder (Modsoft-1.0.0.exe).
echo That ONE file is the release asset - running it installs the app (with a Start
echo Menu entry and desktop shortcut) and needs nothing else alongside it. The app
echo itself has no console window; only this build script does.
