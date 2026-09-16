@REM Maven Wrapper script for Windows - downloads Maven if not already present
@echo off
setlocal enabledelayedexpansion

set "WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties"

if not exist "%WRAPPER_PROPERTIES%" (
    echo Cannot find %WRAPPER_PROPERTIES%
    exit /b 1
)

@REM Extract distributionUrl from the properties file
set "DISTRIBUTION_URL="
for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
    if "%%A"=="distributionUrl" set "DISTRIBUTION_URL=%%B"
)

if "%DISTRIBUTION_URL%"=="" (
    echo distributionUrl not found in %WRAPPER_PROPERTIES%
    exit /b 1
)

if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "DISTS_DIR=%MAVEN_USER_HOME%\wrapper\dists"

@REM Detect an already-extracted Maven distribution
set "MVN_EXEC="
for /f "delims=" %%M in ('dir /b /s "%DISTS_DIR%\mvn.cmd" 2^>nul') do (
    if not defined MVN_EXEC set "MVN_EXEC=%%M"
)

if not defined MVN_EXEC (
    if not exist "%DISTS_DIR%" mkdir "%DISTS_DIR%"
    echo Downloading Maven from %DISTRIBUTION_URL% ...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DISTS_DIR%\maven.zip'"
    if errorlevel 1 (
        echo Failed to download Maven.
        exit /b 1
    )
    powershell -NoProfile -Command "Expand-Archive -Path '%DISTS_DIR%\maven.zip' -DestinationPath '%DISTS_DIR%' -Force"
    if errorlevel 1 (
        echo Failed to extract Maven.
        exit /b 1
    )
    del "%DISTS_DIR%\maven.zip"
    for /f "delims=" %%M in ('dir /b /s "%DISTS_DIR%\mvn.cmd" 2^>nul') do (
        if not defined MVN_EXEC set "MVN_EXEC=%%M"
    )
)

if not defined MVN_EXEC (
    @REM Fallback: use system Maven if wrapper failed
    set "MVN_EXEC=mvn"
)

call "%MVN_EXEC%" %*
exit /b %ERRORLEVEL%
