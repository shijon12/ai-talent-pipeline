@echo off
setlocal

set "MAVEN_VERSION=3.9.6"
set "MAVEN_WRAPPER_HOME=%USERPROFILE%\.m2\wrapper"
set "MAVEN_ZIP=%MAVEN_WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_HOME=%MAVEN_WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto java_ready

set "JAVA_HOME="
for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%~fJ"

:java_ready
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java 17 was not found. Set JAVA_HOME to a valid JDK installation. 1>&2
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%MAVEN_WRAPPER_HOME%" mkdir "%MAVEN_WRAPPER_HOME%"

if not exist "%MAVEN_ZIP%" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
        "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri $env:MAVEN_URL -OutFile $env:MAVEN_ZIP -UseBasicParsing"
    if errorlevel 1 exit /b 1
)

if not exist "%MAVEN_CMD%" (
    echo Extracting Apache Maven %MAVEN_VERSION%...
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
        "$ErrorActionPreference = 'Stop'; Expand-Archive -LiteralPath $env:MAVEN_ZIP -DestinationPath $env:MAVEN_WRAPPER_HOME -Force"
    if errorlevel 1 exit /b 1
)

call "%MAVEN_CMD%" %*
set "MAVEN_EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %MAVEN_EXIT_CODE%
