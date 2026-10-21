@echo off
title Padam
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Padam needs Node.js. Opening the download page...
  echo Install the LTS version, then double-click "Start Padam" again.
  start "" https://nodejs.org/en/download
  pause
  exit /b 1
)
node launcher.js %*
if errorlevel 1 pause
