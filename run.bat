@echo off
set CP=lib\flatlaf-3.7.2.jar;lib\gson-2.14.0.jar;lib\imageio-webp-3.13.1.jar;lib\imageio-core-3.13.1.jar;lib\imageio-metadata-3.13.1.jar;lib\common-lang-3.13.1.jar;lib\common-io-3.13.1.jar;lib\common-image-3.13.1.jar
javac -encoding UTF-8 -cp "%CP%" -d out Main.java ModDownloaderFrame.java I18n.java Fonts.java Icons.java Modrinth.java ImageCache.java InstallManifest.java NativeFolderPicker.java BusyGlassPane.java FadingIcon.java ModHitTransferable.java AppDialog.java Toast.java Logger.java || exit /b 1
java -cp "out;.;%CP%" Main
