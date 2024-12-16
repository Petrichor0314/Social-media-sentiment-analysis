@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVAFX_HOME=C:\JavaFX\javafx-sdk-21.0.5

java --module-path "%JAVAFX_HOME%\lib" ^
--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.swing ^
--add-exports java.base/sun.nio.ch=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED ^
--add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED ^
-cp "target\classes;target\dependency\*" ^
com.sentimentanalysis.application.SentimentAnalysisApplication
