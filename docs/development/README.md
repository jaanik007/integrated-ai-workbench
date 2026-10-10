# Development bootstrap

Status: IMPLEMENTED

Use the Gradle Wrapper with JDK 21:

```powershell
.\gradlew.bat build
.\gradlew.bat runIde
```

The build targets IntelliJ Platform 2026.1.x because that is the current compatible platform line for a JDK 21
toolchain. IntelliJ Platform 2026.2 requires Java 25.
