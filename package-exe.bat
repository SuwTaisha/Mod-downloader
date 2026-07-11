@echo off
setlocal

if not exist dist\modsoft.jar (
  echo dist\modsoft.jar not found - run package.bat first.
  exit /b 1
)

echo === Staging jars for jpackage ===
if exist jpackage-input rmdir /s /q jpackage-input
if exist release-exe rmdir /s /q release-exe
mkdir jpackage-input
copy /y dist\modsoft.jar jpackage-input\ >nul
copy /y dist\lib\*.jar jpackage-input\ >nul

echo === Running jpackage (this bundles its own Java runtime, so end users don't need Java installed) ===
jpackage ^
  --type app-image ^
  --input jpackage-input ^
  --dest release-exe ^
  --name Modsoft ^
  --main-jar modsoft.jar ^
  --main-class Main ^
  --app-version 1.0.0 ^
  --win-console || exit /b 1

echo.
echo Done. Native app is in: release-exe\Modsoft\Modsoft.exe
echo Zip the "release-exe\Modsoft" folder to upload it as a GitHub release asset.
echo (Remove --win-console above and re-run if you don't want a console window behind the app.)
