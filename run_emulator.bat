@echo off
setlocal enabledelayedexpansion

set "SDK_DIR=C:\LTDD"
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
set "EMULATOR=%SDK_DIR%\emulator\emulator.exe"
set "AVD_NAME=Pixel_7"

if not exist "%ADB%" (
    echo [ERROR] Khong tim thay adb.exe tai: %ADB%
    exit /b 1
)

echo [1/4] Kiem tra trang thai may ao Android...
"%ADB%" get-state >NUL 2>&1
if errorlevel 1 (
    echo [INFO] Chua co thiet bi ket noi. Dang khoi dong may ao %AVD_NAME%...
    start "" "%EMULATOR%" -avd %AVD_NAME% -no-boot-anim
    "%ADB%" wait-for-device
)

:wait_boot
for /f "delims=" %%i in ('"%ADB%" shell getprop sys.boot_completed 2^>NUL') do set "BOOTED=%%i"
if not "%BOOTED%"=="1" (
    timeout /t 2 /nobreak >NUL
    goto wait_boot
)
echo [OK] May ao da san sang!

echo [2/4] Dang build va cai dat APK debug (toi uu cache)...
call "%~dp0gradlew.bat" installDebug --build-cache --configuration-cache
if errorlevel 1 (
    echo [ERROR] Build hoac cai dat that bai!
    exit /b 1
)

echo [3/4] Dang mo ung dung ShadowQuiz...
"%ADB%" shell am start -n com.example.shadowquiz/.LoginActivity
if errorlevel 1 (
    echo [ERROR] Khong the mo ung dung!
    exit /b 1
)

echo [4/4] Hoan tat! Ung dung da chay tren may ao.
endlocal
