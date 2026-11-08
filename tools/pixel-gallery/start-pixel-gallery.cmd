@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js was not found. Install Node.js 22.18 or newer and try again.
  pause
  exit /b 1
)
start "Pixel Art Gallery Server" /min cmd /c "node server.js"
timeout /t 2 /nobreak >nul
start "" "http://127.0.0.1:4177"
endlocal
