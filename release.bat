@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

title GalleryCleaner Release

echo.
echo  +==========================================+
echo  ^|   GalleryCleaner 一键发布脚本            ^|
echo  ^|   版本更新 + 编译 + Git 同步              ^|
echo  +==========================================+
echo.

:: 检查环境
if not exist "gradlew.bat" (
    echo [ERROR] 请在项目根目录运行此脚本
    pause & exit /b 1
)
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java
    pause & exit /b 1
)

:: 读取当前版本号
for /f "tokens=2 delims==" %%a in ('findstr /r "versionCode" app\build.gradle.kts') do (
    set "VC_RAW=%%a"
)
set "CURRENT_VC=!VC_RAW: =!"

for /f "tokens=2 delims==" %%a in ('findstr /r "versionName" app\build.gradle.kts') do (
    set "VN_RAW=%%a"
)
set "CURRENT_VN=!VN_RAW: =!"
set "CURRENT_VN=!CURRENT_VN:"=!"

echo [INFO] 当前版本: !CURRENT_VN! (!CURRENT_VC!)
echo.

:: 解析版本号 1.7.1 -> 1.7.2
for /f "tokens=1,2,3 delims=." %%a in ("!CURRENT_VN!") do (
    set "V_MAJOR=%%a"
    set "V_MINOR=%%b"
    set "V_PATCH=%%c"
)
set /a "NEW_PATCH=!V_PATCH!+1"
set "NEW_VN=!V_MAJOR!.!V_MINOR!.!NEW_PATCH!"

:: versionCode: 10701 -> 10702
set /a "NEW_VC=!CURRENT_VC!+1"

echo [INFO] 新版本: !NEW_VN! (!NEW_VC!)
echo.

:: 更新 build.gradle.kts
echo [INFO] 更新版本号...
powershell -Command "(Get-Content 'app\build.gradle.kts') -replace 'versionCode = !CURRENT_VC!', 'versionCode = !NEW_VC!' -replace 'versionName = \"!CURRENT_VN!\"', 'versionName = \"!NEW_VN!\"' | Set-Content 'app\build.gradle.kts'"
if errorlevel 1 (
    echo [ERROR] 版本号更新失败
    pause & exit /b 1
)

:: 编译
echo [INFO] 编译 debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo [ERROR] 编译失败！
    pause & exit /b 1
)

:: 复制 APK 到项目根目录
set "APK_SRC=app\build\outputs\apk\debug\app-debug.apk"
set "APK_DST=GalleryCleaner-!NEW_VN!-debug.apk"

if exist "!APK_DST!" del /Q "!APK_DST!"
copy /Y "!APK_SRC!" "!APK_DST!" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 复制 APK 失败
    pause & exit /b 1
)

for %%f in ("!APK_DST!") do set "APK_SIZE=%%~zf"
echo [SUCCESS] APK 已生成: !APK_DST! (!APK_SIZE! 字节)
echo.

:: 删除旧版本 APK
for %%f in (GalleryCleaner-*-debug.apk) do (
    if not "%%f"=="!APK_DST!" (
        echo [INFO] 删除旧版本: %%f
        del /Q "%%f"
    )
)

:: Git 提交并推送
echo [INFO] 同步到 Git 仓库...
git add app\build.gradle.kts "!APK_DST!"
git commit -m "v!NEW_VN!: version bump + build"
if errorlevel 1 (
    echo [WARN] Git commit 失败
) else (
    git push origin master
    if errorlevel 1 (
        echo [WARN] Git push 失败
    ) else (
        echo [SUCCESS] 已推送到 E:\git\slidecleaner.git
    )
)

echo.
echo ==============================================
echo  发布完成！
echo  版本: v!NEW_VN!
echo  APK:  !APK_DST!
echo ==============================================
echo.
pause
