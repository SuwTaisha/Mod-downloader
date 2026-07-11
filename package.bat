@echo off
setlocal
set CP=lib\flatlaf-3.7.2.jar;lib\gson-2.14.0.jar;lib\imageio-webp-3.13.1.jar;lib\imageio-core-3.13.1.jar;lib\imageio-metadata-3.13.1.jar;lib\common-lang-3.13.1.jar;lib\common-io-3.13.1.jar;lib\common-image-3.13.1.jar

echo === Cleaning previous build ===
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist
mkdir dist\lib

echo === Compiling ===
javac -encoding UTF-8 -cp "%CP%" -d out Main.java ModDownloaderFrame.java I18n.java Fonts.java Icons.java Modrinth.java ImageCache.java InstallManifest.java NativeFolderPicker.java BusyGlassPane.java FadingIcon.java ModHitTransferable.java AppDialog.java Toast.java Logger.java || exit /b 1

echo === Bundling fonts and theme into the classes ===
xcopy /e /i /y resources out\resources >nul
xcopy /e /i /y theme out\theme >nul

echo === Building modsoft.jar ===
(
  echo Main-Class: Main
) > out\MANIFEST.MF
jar cfm dist\modsoft.jar out\MANIFEST.MF -C out . || exit /b 1

echo === Copying dependency jars ===
copy /y lib\*.jar dist\lib\ >nul

echo === Writing launcher ===
(
  echo @echo off
  echo java -cp "modsoft.jar;lib\*" Main
) > dist\run.bat

echo.
echo Done. Portable build is in the "dist" folder:
echo   dist\modsoft.jar
echo   dist\lib\*.jar
echo   dist\run.bat  ^(double-click to launch^)
echo.
echo Zip the "dist" folder to upload it as a GitHub release asset.
