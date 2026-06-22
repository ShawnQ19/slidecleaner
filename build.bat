@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
:: GalleryCleaner 一键编译脚本
:: ============================================

title GalleryCleaner Build

:: 颜色定义
set "COLOR_INFO=[92m"
set "COLOR_WARN=[93m"
set "COLOR_ERROR=[91m"
set "COLOR_SUCCESS=[92m"
set "COLOR_RESET=[0m"

echo.
echo  +==========================================+
echo  ^|     GalleryCleaner 编译脚本              ^|
echo  ^|     Android Debug / Release 构建         ^|
echo  +==========================================+
echo.

:: 检查 Java 环境
echo [INFO] 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请安装 JDK 17 或更高版本
    echo [INFO] 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%g"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)
echo [INFO] Java 版本: %JAVA_VERSION%

:: 检查是否在项目根目录
if not exist "gradlew.bat" (
    echo [ERROR] 未找到 gradlew.bat，请在项目根目录运行此脚本
    pause
    exit /b 1
)

:: 解析参数
set "BUILD_TYPE=debug"
set "SKIP_CLEAN=0"
set "INSTALL_APK=0"

:parse_args
if "%~1"=="" goto :done_parse
if /i "%~1"=="release" set "BUILD_TYPE=release"
if /i "%~1"=="debug" set "BUILD_TYPE=debug"
if /i "%~1"=="--no-clean" set "SKIP_CLEAN=1"
if /i "%~1"=="--install" set "INSTALL_APK=1"
if /i "%~1"=="-i" set "INSTALL_APK=1"
if /i "%~1"=="--help" goto :show_help
if /i "%~1"=="-h" goto :show_help
shift
goto :parse_args
:done_parse

echo [INFO] 构建类型: %BUILD_TYPE%
echo.

:: 清理
echo [INFO] 开始构建...
echo.

if "%SKIP_CLEAN%"=="0" (
    echo [INFO] 清理旧构建...
    call gradlew.bat clean
    if errorlevel 1 (
        echo [ERROR] 清理失败
        pause
        exit /b 1
    )
    echo.
)

:: 构建
echo [INFO] 编译 %BUILD_TYPE% APK...
echo.

if /i "%BUILD_TYPE%"=="release" (
    call gradlew.bat assembleRelease
) else (
    call gradlew.bat assembleDebug
)

if errorlevel 1 (
    echo.
    echo [ERROR] 编译失败！请检查上方错误信息。
    echo.
    pause
    exit /b 1
)

:: 删除旧的 APK 和 build_log
echo [INFO] 清理旧的 APK 和日志...
del /Q GalleryCleaner-*.apk build_log*.txt 2>nul
echo.

:: 查找输出 APK
set "APK_DIR=app\build\outputs\apk\%BUILD_TYPE%"
set "APK_FILE="

for %%f in (%APK_DIR%\*.apk) do (
    set "APK_FILE=%%f"
    set "APK_NAME=%%~nxf"
)

if "!APK_FILE!"=="" (
    echo [WARN] 未找到 APK 文件
    pause
    exit /b 1
)

echo.
echo ==============================================
echo [SUCCESS] 编译成功！
echo.
echo APK 文件: !APK_FILE!
echo 文件名:   !APK_NAME!
echo.

:: 显示文件大小
for %%f in (!APK_FILE!) do (
    set "FILE_SIZE=%%~zf"
    echo 大小:     !FILE_SIZE! 字节
)
echo ==============================================
echo.

:: 获取版本号
for /f "tokens=2 delims==" %%a in ('findstr /r "versionName" app\build.gradle.kts') do (
    set "VERSION_RAW=%%a"
)
set "VERSION=!VERSION_RAW: =!"
set "VERSION=!VERSION:"=!"
set "VERSION=!VERSION:~0,-1!"

:: 生成时间戳
for /f "tokens=2 delims==" %%a in ('wmic os get localdatetime /value 2^>nul') do set "dt=%%a"
set "BUILD_DATE=!dt:~0,8!"

:: 项目根目录 APK
set "ROOT_APK_NAME=GalleryCleaner-%VERSION%-%BUILD_TYPE%-%BUILD_DATE%.apk"
set "ROOT_APK=%CD%\!ROOT_APK_NAME!"

:: 复制 APK 到项目根目录
echo [INFO] 复制 APK 到项目根目录...
copy /Y "!APK_FILE!" "!ROOT_APK!" >nul 2>&1
if errorlevel 1 (
    echo [WARN] 复制失败，APK 在: !APK_FILE!
    set "FINAL_APK=!APK_FILE!"
) else (
    echo [SUCCESS] !ROOT_APK!
    set "FINAL_APK=!ROOT_APK!"
)
echo.

:: 复制 APK 到工作区根目录
set "WORKSPACE_ROOT=%CD%\..\"
set "WORKSPACE_APK=%WORKSPACE_ROOT%!ROOT_APK_NAME!"
echo [INFO] 复制 APK 到工作区根目录...
copy /Y "!APK_FILE!" "!WORKSPACE_APK!" >nul 2>&1
if errorlevel 1 (
    echo [WARN] 复制到工作区失败
) else (
    echo [SUCCESS] !WORKSPACE_APK!
)
echo.

:: 安装 APK
if "%INSTALL_APK%"=="1" (
    echo [INFO] 正在安装到设备...
    adb devices >nul 2>&1
    if errorlevel 1 (
        echo [WARN] 未找到 adb，跳过安装
    ) else (
        adb install -r "!FINAL_APK!"
        if errorlevel 1 (
            echo [WARN] 安装失败，请检查设备连接
        ) else (
            echo [SUCCESS] 安装成功！
        )
    )
    echo.
)


:: === 自动 Git 同步 ===
echo.
echo [INFO] 正在同步到本地 Git 仓库...
git add -A
git commit -m "Auto sync: build %BUILD_TYPE% v%VERSION%" --allow-empty
if errorlevel 1 (
    echo [WARN] Git commit 失败，跳过推送
) else (
    git push origin master >nul 2>&1
    if errorlevel 1 (
        echo [WARN] Git push 失败
    ) else (
        echo [SUCCESS] 已推送到远程仓库
    )
)
echo.

echo [INFO] 构建完成！
echo.
pause
exit /b 0

:show_help
echo 用法: build.bat [debug^|release] [选项]
echo.
echo 参数:
echo   debug       编译 Debug 版本（默认）
echo   release     编译 Release 版本
echo.
echo 选项:
echo   --no-clean  跳过清理步骤，增量编译
echo   --install   编译后自动安装到连接的设备
echo   -h, --help  显示此帮助信息
echo.
echo 示例:
echo   build.bat                  编译 Debug APK
echo   build.bat release          编译 Release APK
echo   build.bat debug --install  编译并安装 Debug APK
echo   build.bat release --no-clean  增量编译 Release
echo.
echo 输出位置:
echo   项目根目录: GalleryCleaner-{version}-{type}-{date}.apk
echo   工作区根目录: 同上
echo.
pause
exit /b 0
