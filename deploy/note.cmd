@echo off
setlocal EnableExtensions
title CipherWS Note - Environment and Start

rem ---------------------------------------------------------------------------
rem Project paths
rem ---------------------------------------------------------------------------
set "DEPLOY=%~dp0"
pushd "%DEPLOY%.." >nul
set "ROOT=%CD%"
popd >nul
set "SERVER=%ROOT%\server"
set "FRONTEND=%ROOT%\frontend\note"
set "TOOLS=%DEPLOY%.tools"
set "LOGDIR=%DEPLOY%.logs"
set "ENV_FILE=%DEPLOY%note.env.local"
set "JAR=%SERVER%\target\cipherws-note-0.0.1-SNAPSHOT.jar"

set "JAVA_HOME_ORIG=%JAVA_HOME%"
set "FORCE_BUILD=0"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1

rem ---------------------------------------------------------------------------
rem Dispatch
rem ---------------------------------------------------------------------------
set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=start"
if /I "%ACTION%"=="check"  goto :check
if /I "%ACTION%"=="setup"  goto :setup
if /I "%ACTION%"=="start"  goto :start
if /I "%ACTION%"=="stop"   goto :stop
if /I "%ACTION%"=="status" goto :status
if /I "%ACTION%"=="build"  goto :build
if /I "%ACTION%"=="help"   goto :help
if /I "%ACTION%"=="-h"     goto :help
if /I "%ACTION%"=="--help" goto :help
goto :help

rem ===========================================================================
rem check
rem ===========================================================================
:check
echo ============================================================
echo   CipherWS Note - Environment Check
echo ============================================================
call :find_java
call :find_node
call :find_mysql

echo.
if defined JAVA_HOME (
    echo [OK]   JDK 17      : %JAVA_HOME%
) else (
    echo [MISS] JDK 17      : not found
)
if defined NODE_EXE (
    echo [OK]   Node.js     : %NODE_EXE%
) else (
    echo [MISS] Node.js 18+ : not found
)
if defined NPM_CMD (
    echo [OK]   npm         : %NPM_CMD%
) else (
    echo [MISS] npm         : not found
)
if defined MYSQL_EXE (
    echo [OK]   MySQL 8     : %MYSQL_EXE%
) else (
    echo [MISS] MySQL 8     : not found
)

echo.
echo Port status:
call :show_port 3306
call :show_port 8084
call :show_port 8002

echo.
echo Hints:
echo   * MySQL 8 must be installed and running; this script does not install MySQL.
echo   * First start asks for the MySQL password and writes deploy\note.env.local.
echo   * The backend is a single Spring Boot app on port 8084.
echo.
exit /b 0

rem ===========================================================================
rem setup
rem ===========================================================================
:setup
call :find_java
call :ensure_java
if not defined JAVA_HOME (
    echo [ERROR] No usable JDK 17.
    exit /b 1
)

call :find_node
call :ensure_node
if not defined NODE_EXE (
    echo [ERROR] No usable Node.js 18+.
    exit /b 1
)

call :find_mysql
call :ensure_mysql
if not defined MYSQL_EXE (
    echo [ERROR] No usable MySQL 8 client.
    exit /b 1
)

call :ensure_env
if errorlevel 1 exit /b 1

call :ensure_db
if errorlevel 1 exit /b 1

call :build_backend
if errorlevel 1 exit /b 1

echo.
echo [OK] Environment is ready.
exit /b 0

rem ===========================================================================
rem start
rem ===========================================================================
:start
call :setup
if errorlevel 1 (
    echo.
    echo [ERROR] Setup failed; services were not started.
    exit /b 1
)

call :load_env
call :find_java
set "PATH=%JAVA_HOME%\bin;%PATH%"
call :find_node

echo.
echo ============================================================
echo   Starting the note backend and frontend
echo ============================================================

call :start_backend
if errorlevel 1 goto :start_failed
call :start_frontend
if errorlevel 1 goto :start_failed

echo.
echo ============================================================
echo   Started
echo ============================================================
echo   Frontend:  http://127.0.0.1:8002
echo   Backend:   http://127.0.0.1:8084
echo   Health:    http://127.0.0.1:8084/actuator/health
echo.
echo   Stop:      deploy\note.cmd stop
echo   Status:    deploy\note.cmd status
echo   Logs:      deploy\.logs
echo ============================================================
start "" "http://127.0.0.1:8002"
echo.
echo Services are running in the background.
echo Press any key to close this window; services keep running.
if /I "%~2"=="--no-pause" exit /b 0
pause >nul
exit /b 0

:start_failed
echo.
echo [ERROR] Service start failed. Check logs under deploy\.logs
echo         Started services can be stopped with deploy\note.cmd stop.
pause
exit /b 1

rem ===========================================================================
rem build
rem ===========================================================================
:build
call :find_java
if not defined JAVA_HOME (
    echo [ERROR] No usable JDK 17.
    exit /b 1
)
set "FORCE_BUILD=1"
call :build_backend
if errorlevel 1 exit /b 1
exit /b 0

rem ===========================================================================
rem stop
rem ===========================================================================
:stop
echo ============================================================
echo   Stopping note services
echo ============================================================
for %%S in (backend frontend) do call :kill_pidfile %%S
for %%P in (8084 8002) do call :kill_port %%P
echo [OK] Stop completed.
exit /b 0

:kill_pidfile
set "PIDFILE=%LOGDIR%\%~1.pid"
if not exist "%PIDFILE%" exit /b 0
set "SVC_PID="
set /p SVC_PID=<"%PIDFILE%"
if defined SVC_PID (
    echo   Stopping %~1 PID %SVC_PID%
    taskkill /PID %SVC_PID% /T /F >nul 2>&1
)
del "%PIDFILE%" >nul 2>&1
exit /b 0

:kill_port
set "P=%~1"
for /f "tokens=5" %%I in ('netstat -ano ^| findstr /R /C:":%P% .*LISTENING"') do (
    echo   Killing PID %%I on port %P%
    taskkill /PID %%I /T /F >nul 2>&1
)
exit /b 0

rem ===========================================================================
rem status
rem ===========================================================================
:status
echo ============================================================
echo   note runtime status
echo ============================================================
call :show_port 8084
call :show_port 8002
echo.
exit /b 0

rem ===========================================================================
rem help
rem ===========================================================================
:help
echo CipherWS Note environment and start script
echo.
echo Usage:
echo   deploy\note.cmd            check, install missing tools, start backend + frontend
echo   deploy\note.cmd check      check environment only
echo   deploy\note.cmd setup      check/install, init database, build backend
echo   deploy\note.cmd start      same as default
echo   deploy\note.cmd build      force rebuild backend
echo   deploy\note.cmd stop       stop backend and frontend
echo   deploy\note.cmd status     show port status
echo.
echo Notes:
echo   * MySQL 8 must be installed and running; this script does not install MySQL.
echo   * JDK 17 / Node.js are downloaded only after confirmation.
echo   * First start asks for the MySQL password and writes deploy\note.env.local.
exit /b 0

rem ===========================================================================
rem find / install helpers
rem ===========================================================================
:find_java
set "JAVA_HOME="
set "JAVA_EXE="
if defined JAVA_HOME_ORIG call :check_java_home "%JAVA_HOME_ORIG%"
if not defined JAVA_HOME if exist "%TOOLS%\jdk-17\bin\java.exe" set "JAVA_HOME=%TOOLS%\jdk-17"
if not defined JAVA_HOME for /d %%D in ("%ProgramFiles%\Java\jdk-17*" "%ProgramFiles%\Eclipse Adoptium\jdk-17*" "%USERPROFILE%\.jdks\*17*" "D:\java\JDK\jdk-17*" "D:\java\JDK\*17*") do call :check_java_home "%%~fD"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
exit /b 0

:check_java_home
if defined JAVA_HOME exit /b 0
if not exist "%~1\bin\java.exe" exit /b 0
"%~1\bin\java.exe" -version 2>&1 | findstr /i /c:"17." >nul
if not errorlevel 1 set "JAVA_HOME=%~1"
exit /b 0

:ensure_java
if defined JAVA_HOME exit /b 0
echo.
echo [MISS] JDK 17 was not found.
choice /c YN /n /m "Download JDK 17 (~182 MB) to deploy\.tools? [Y/N] "
if errorlevel 2 exit /b 1
if not exist "%TOOLS%" mkdir "%TOOLS%"
if not exist "%TOOLS%\fetch-jdk17.ps1" (
    echo [ERROR] Missing %TOOLS%\fetch-jdk17.ps1
    exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%TOOLS%\fetch-jdk17.ps1"
if errorlevel 1 (
    echo [ERROR] JDK 17 download/extract failed.
    exit /b 1
)
call :find_java
if defined JAVA_HOME exit /b 0
exit /b 1

:find_node
set "NODE_EXE="
set "NPM_CMD="
set "NODE_DIR="
for /f "delims=" %%N in ('where node 2^>nul') do if not defined NODE_EXE set "NODE_EXE=%%~fN"
if not defined NODE_EXE if exist "%ProgramFiles%\nodejs\node.exe" set "NODE_EXE=%ProgramFiles%\nodejs\node.exe"
if not defined NODE_EXE if exist "D:\tool\node\nodejs\node.exe" set "NODE_EXE=D:\tool\node\nodejs\node.exe"
if not defined NODE_EXE if defined NVM_HOME if exist "%NVM_HOME%\node.exe" set "NODE_EXE=%NVM_HOME%\node.exe"
if not defined NODE_EXE if exist "%TOOLS%\node\node.exe" set "NODE_EXE=%TOOLS%\node\node.exe"
if defined NODE_EXE (
    "%NODE_EXE%" -e "process.exit(parseInt(process.versions.node,10)>=18?0:1)" >nul 2>&1
    if errorlevel 1 set "NODE_EXE="
)
if defined NODE_EXE for %%P in ("%NODE_EXE%") do set "NODE_DIR=%%~dpP"
if defined NODE_DIR set "PATH=%NODE_DIR%;%PATH%"
if defined NODE_DIR if exist "%NODE_DIR%npm.cmd" set "NPM_CMD=%NODE_DIR%npm.cmd"
if not defined NPM_CMD for /f "delims=" %%N in ('where npm.cmd 2^>nul') do if not defined NPM_CMD set "NPM_CMD=%%~fN"
exit /b 0

:ensure_node
if defined NODE_EXE if defined NPM_CMD exit /b 0
echo.
echo [MISS] Node.js 18+ / npm was not found.
choice /c YN /n /m "Download Node.js 22 (~30 MB) to deploy\.tools? [Y/N] "
if errorlevel 2 exit /b 1
if not exist "%TOOLS%" mkdir "%TOOLS%"
set "NODE_ZIP=%TOOLS%\node-v22.19.0-win-x64.zip"
if not exist "%NODE_ZIP%" (
    echo Downloading Node.js ...
    curl.exe -L --retry 3 --max-time 900 -o "%NODE_ZIP%" "https://nodejs.org/dist/v22.19.0/node-v22.19.0-win-x64.zip"
    if errorlevel 1 (
        echo [ERROR] Node.js download failed.
        exit /b 1
    )
)
if exist "%TOOLS%\node" rmdir /s /q "%TOOLS%\node" >nul 2>&1
if exist "%TOOLS%\node-v22.19.0-win-x64" rmdir /s /q "%TOOLS%\node-v22.19.0-win-x64" >nul 2>&1
tar -xf "%NODE_ZIP%" -C "%TOOLS%"
if errorlevel 1 (
    echo [ERROR] Node.js extract failed.
    exit /b 1
)
if not exist "%TOOLS%\node-v22.19.0-win-x64\node.exe" (
    echo [ERROR] node.exe was not found after extract.
    exit /b 1
)
move "%TOOLS%\node-v22.19.0-win-x64" "%TOOLS%\node" >nul
call :find_node
if defined NODE_EXE exit /b 0
exit /b 1

:find_mysql
set "MYSQL_EXE="
set "MYSQLADMIN_EXE="
set "MYSQL_SERVICE="
for /f "delims=" %%M in ('where mysql.exe 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%~fM"
if not defined MYSQL_EXE if exist "%ProgramFiles%\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=%ProgramFiles%\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE if exist "D:\mysql\bin\mysql.exe" set "MYSQL_EXE=D:\mysql\bin\mysql.exe"
if defined MYSQL_EXE for %%P in ("%MYSQL_EXE%") do if exist "%%~dpPmysqladmin.exe" set "MYSQLADMIN_EXE=%%~dpPmysqladmin.exe"
sc query MySQL80 >nul 2>&1
if not errorlevel 1 set "MYSQL_SERVICE=MySQL80"
exit /b 0

:ensure_mysql
if not defined MYSQL_EXE (
    echo.
    echo [MISS] MySQL 8 client was not found.
    echo        Install MySQL 8: https://dev.mysql.com/downloads/installer/
    echo        Make sure the MySQL service is running, then rerun this script.
    exit /b 1
)
if defined MYSQL_SERVICE (
    echo [INFO] Ensuring MySQL service %MYSQL_SERVICE% is running ...
    net start "%MYSQL_SERVICE%" >nul 2>&1
)
call :port_open 3306
if not errorlevel 1 exit /b 0
echo.
echo [ERROR] MySQL is installed but port 3306 is not listening.
if defined MYSQL_SERVICE echo         Try: net start %MYSQL_SERVICE%
echo         Start the MySQL service manually and rerun.
exit /b 1

rem ===========================================================================
rem env
rem ===========================================================================
:ensure_env
if not exist "%ENV_FILE%" call :create_env
if errorlevel 1 exit /b 1
call :load_env
if not defined DB_USERNAME set "DB_USERNAME=root"
if not defined DB_PASSWORD call :ask_db_password
if errorlevel 1 exit /b 1
if not defined NOTE_JWT_SECRET call :ask_jwt
if not defined NOTE_JWT_SECRET (
    echo [ERROR] NOTE_JWT_SECRET must not be empty.
    exit /b 1
)
powershell -NoProfile -Command "exit ([int]($env:NOTE_JWT_SECRET.Length -lt 32))" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] NOTE_JWT_SECRET must be at least 32 bytes.
    echo         Edit %ENV_FILE%
    exit /b 1
)
exit /b 0

:create_env
echo.
echo First run: creating %ENV_FILE%
echo.
set "DB_USERNAME=root"
set /p "DB_USERNAME=MySQL user [root]: "
if "%DB_USERNAME%"=="" set "DB_USERNAME=root"
set "DB_PASSWORD="
set /p "DB_PASSWORD=MySQL password: "
if "%DB_PASSWORD%"=="" (
    echo [ERROR] MySQL password must not be empty.
    exit /b 1
)
set "JWT="
set /p "JWT=JWT secret (blank = generate, at least 32 bytes): "
if "%JWT%"=="" call :gen_secret
set "ENV_DB_PASS=%DB_PASSWORD%"
set "ENV_JWT=%JWT%"
powershell -NoProfile -Command "$p='%ENV_FILE%'; $lines=@('DB_URL=jdbc:mysql://127.0.0.1:3306/CipherWS?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false','DB_USERNAME=%DB_USERNAME%',('DB_PASSWORD='+$env:ENV_DB_PASS),('NOTE_JWT_SECRET='+$env:ENV_JWT),'NOTE_JWT_TTL=2592000','NOTE_UPLOAD_DIR=%SERVER%\uploads'); $enc=New-Object System.Text.UTF8Encoding -ArgumentList $false; [IO.File]::WriteAllLines($p,$lines,$enc)" >nul
if errorlevel 1 (
    set "ENV_DB_PASS="
    set "ENV_JWT="
    echo [ERROR] Failed to write %ENV_FILE%
    exit /b 1
)
set "ENV_DB_PASS="
set "ENV_JWT="
echo [OK] Created %ENV_FILE%
exit /b 0

:ask_db_password
echo.
set "DB_PASSWORD="
set /p "DB_PASSWORD=MySQL password: "
if "%DB_PASSWORD%"=="" exit /b 1
set "ENV_DB_PASS=%DB_PASSWORD%"
powershell -NoProfile -Command "Add-Content -LiteralPath '%ENV_FILE%' -Value ('DB_PASSWORD='+$env:ENV_DB_PASS) -Encoding UTF8" >nul
set "ENV_DB_PASS="
exit /b 0

:ask_jwt
echo.
set "JWT="
set /p "JWT=JWT secret (blank = generate): "
if "%JWT%"=="" call :gen_secret
set "ENV_JWT=%JWT%"
powershell -NoProfile -Command "Add-Content -LiteralPath '%ENV_FILE%' -Value ('NOTE_JWT_SECRET='+$env:ENV_JWT) -Encoding UTF8" >nul
set "ENV_JWT="
set "NOTE_JWT_SECRET=%JWT%"
exit /b 0

:gen_secret
set "JWT="
for /f "usebackq delims=" %%S in (`powershell -NoProfile -Command "[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')"`) do set "JWT=%%S"
if not defined JWT set "JWT=%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%"
exit /b 0

:load_env
if not exist "%ENV_FILE%" exit /b 1
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do call :set_env "%%A" "%%B"
exit /b 0

:set_env
set "K=%~1"
if "%K%"=="" exit /b 0
if "%K:~0,1%"=="#" exit /b 0
set "%K%=%~2"
exit /b 0

rem ===========================================================================
rem database
rem ===========================================================================
:ensure_db
if not defined MYSQL_EXE (
    echo [ERROR] No MySQL client; cannot initialize the database.
    exit /b 1
)
if not defined DB_PASSWORD (
    echo [ERROR] DB_PASSWORD is empty.
    exit /b 1
)

set "MYSQL_PWD=%DB_PASSWORD%"
"%MYSQL_EXE%" -h127.0.0.1 -P3306 -u%DB_USERNAME% -N -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    set "MYSQL_PWD="
    echo [ERROR] Cannot connect to MySQL. Check:
    echo         - MySQL80 service is running
    echo         - DB_USERNAME / DB_PASSWORD in %ENV_FILE%
    exit /b 1
)

"%MYSQL_EXE%" -h127.0.0.1 -P3306 -u%DB_USERNAME% -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='CipherWS'" > "%TEMP%\note_db_count.txt" 2>nul
set "DB_TABLES="
set /p DB_TABLES=<"%TEMP%\note_db_count.txt"
del "%TEMP%\note_db_count.txt" >nul 2>&1

if "%DB_TABLES%"=="0" (
    echo.
    echo The CipherWS database is empty or missing.
    choice /c YN /n /m "Import server\sql\note-schema.sql? [Y/N] "
    if errorlevel 2 (
        set "MYSQL_PWD="
        echo [ERROR] Database was not initialized.
        exit /b 1
    )
    echo Importing schema ...
    "%MYSQL_EXE%" -h127.0.0.1 -P3306 -u%DB_USERNAME% --default-character-set=utf8mb4 < "%SERVER%\sql\note-schema.sql"
    if errorlevel 1 (
        set "MYSQL_PWD="
        echo [ERROR] Schema import failed.
        exit /b 1
    )
    echo [OK] Database initialized.
) else (
    echo [OK] CipherWS has %DB_TABLES% tables; skip init.
)

set "MYSQL_PWD="
exit /b 0

rem ===========================================================================
rem build
rem ===========================================================================
:build_backend
if not defined JAVA_HOME (
    echo [ERROR] No JDK 17; cannot build.
    exit /b 1
)
if "%FORCE_BUILD%"=="0" if exist "%JAR%" (
    echo [OK] %JAR% exists; skip build. Run deploy\note.cmd build to force.
    exit /b 0
)

echo.
echo [BUILD] Packaging backend ...
set "PATH=%JAVA_HOME%\bin;%PATH%"
pushd "%SERVER%"
call mvnw.cmd -DskipTests package
set "BUILD_RC=%ERRORLEVEL%"
popd
if not "%BUILD_RC%"=="0" (
    echo [ERROR] Maven build failed.
    exit /b 1
)
if not exist "%JAR%" (
    echo [ERROR] Build finished but %JAR% is missing.
    exit /b 1
)
echo [OK] Backend build completed.
exit /b 0

rem ===========================================================================
rem start backend / frontend
rem ===========================================================================
:start_backend
call :port_open 8084
if not errorlevel 1 (
    echo [SKIP] Backend already listening on 8084.
    exit /b 0
)
if not exist "%JAR%" (
    echo [ERROR] Missing %JAR%
    exit /b 1
)
if not defined JAVA_EXE set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
echo [START] Backend ...
del "%LOGDIR%\backend.out.log" "%LOGDIR%\backend.err.log" >nul 2>&1
powershell -NoProfile -Command "$p=Start-Process -FilePath '%JAVA_EXE%' -ArgumentList @('-jar','%JAR%') -WorkingDirectory '%ROOT%' -RedirectStandardOutput '%LOGDIR%\backend.out.log' -RedirectStandardError '%LOGDIR%\backend.err.log' -WindowStyle Hidden -PassThru; $p.Id | Set-Content -LiteralPath '%LOGDIR%\backend.pid'"
if not exist "%LOGDIR%\backend.pid" (
    echo [ERROR] Failed to launch backend.
    exit /b 1
)
call :wait_port 8084 90
if errorlevel 1 (
    echo [ERROR] Backend did not listen on 8084; see deploy\.logs
    if exist "%LOGDIR%\backend.err.log" powershell -NoProfile -Command "Get-Content -LiteralPath '%LOGDIR%\backend.err.log' -Tail 15"
    exit /b 1
)
echo [OK] Backend started.
exit /b 0

:start_frontend
call :port_open 8002
if not errorlevel 1 (
    echo [SKIP] Frontend already listening on 8002.
    exit /b 0
)
if not defined NPM_CMD (
    echo [ERROR] npm not found.
    exit /b 1
)
if exist "%FRONTEND%\node_modules" goto :frontend_deps_ok
echo [INSTALL] npm install ...
pushd "%FRONTEND%"
call "%NPM_CMD%" install
set "NPM_RC=%ERRORLEVEL%"
popd
if not "%NPM_RC%"=="0" (
    echo [ERROR] npm install failed.
    exit /b 1
)
:frontend_deps_ok
echo [START] Frontend Vite ...
del "%LOGDIR%\frontend.out.log" "%LOGDIR%\frontend.err.log" >nul 2>&1
powershell -NoProfile -Command "$p=Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c','npm run dev') -WorkingDirectory '%FRONTEND%' -RedirectStandardOutput '%LOGDIR%\frontend.out.log' -RedirectStandardError '%LOGDIR%\frontend.err.log' -WindowStyle Hidden -PassThru; $p.Id | Set-Content -LiteralPath '%LOGDIR%\frontend.pid'"
if not exist "%LOGDIR%\frontend.pid" (
    echo [ERROR] Failed to launch frontend.
    exit /b 1
)
call :wait_port 8002 60
if errorlevel 1 (
    echo [ERROR] Frontend did not listen on 8002; see deploy\.logs
    if exist "%LOGDIR%\frontend.err.log" powershell -NoProfile -Command "Get-Content -LiteralPath '%LOGDIR%\frontend.err.log' -Tail 15"
    exit /b 1
)
echo [OK] Frontend started.
exit /b 0

rem ===========================================================================
rem ports
rem ===========================================================================
:port_open
set "P=%~1"
netstat -ano | findstr /R /C:":%P% .*LISTENING" >nul 2>&1
exit /b %errorlevel%

:show_port
call :port_open %~1
if errorlevel 1 (
    echo   %~1 : DOWN
) else (
    echo   %~1 : LISTENING
)
exit /b 0

:wait_port
set "WPORT=%~1"
set "WTRIES=0"
:wait_port_loop
call :port_open %WPORT%
if not errorlevel 1 exit /b 0
set /a WTRIES+=1
if %WTRIES% GEQ %2 exit /b 1
ping -n 3 127.0.0.1 >nul
goto :wait_port_loop
