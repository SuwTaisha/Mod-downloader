@echo off
setlocal
set CP=lib\flatlaf-3.7.2.jar;lib\gson-2.14.0.jar;lib\imageio-webp-3.13.1.jar;lib\imageio-core-3.13.1.jar;lib\imageio-metadata-3.13.1.jar;lib\common-lang-3.13.1.jar;lib\common-io-3.13.1.jar;lib\common-image-3.13.1.jar

echo === Cleaning previous build ===
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist

echo === Compiling ===
javac -encoding UTF-8 -cp "%CP%" -d out Main.java ModDownloaderFrame.java I18n.java Fonts.java Icons.java Modrinth.java ImageCache.java InstallManifest.java NativeFolderPicker.java BusyGlassPane.java FadingIcon.java ModHitTransferable.java AppDialog.java Toast.java Logger.java || exit /b 1

echo === Bundling fonts and theme into the classes ===
xcopy /e /i /y resources out\resources >nul
xcopy /e /i /y theme out\theme >nul

echo === Merging dependency jars in (fat jar - no lib folder needed at runtime) ===
pushd out
for %%f in (..\lib\*.jar) do (
  jar xf "%%f"
)
if exist META-INF\MANIFEST.MF del META-INF\MANIFEST.MF
popd

echo === Building modsoft.jar ===
(
  echo Main-Class: Main
) > out\MANIFEST.MF
jar cfm dist\modsoft.jar out\MANIFEST.MF -C out . || exit /b 1

echo.
echo Done. dist\modsoft.jar is a single self-contained file:
echo   java -jar modsoft.jar
echo   (or just double-click it - Java must be installed)
echo.
echo Zip dist\modsoft.jar to upload it as a GitHub release asset.
