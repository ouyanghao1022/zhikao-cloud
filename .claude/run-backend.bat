@echo off
rem Backend launcher for zhikao-cloud — forces JDK 21 (system JAVA_HOME is JDK 8)
set "JAVA_HOME=D:\AppData\Java\JDK\JDK21"
D:\AppData\Java\Maven\apache-maven-3.8.9\bin\mvn.cmd spring-boot:run -f D:\Claude-Code-workplace\zhikao-cloud\backend\pom.xml
