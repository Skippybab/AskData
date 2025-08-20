@echo off
echo 启动数据问答系统...
echo.

echo 1. 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请先安装JDK 17+
    pause
    exit /b 1
)

echo.
echo 2. 编译项目...
call mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo 错误: 编译失败
    pause
    exit /b 1
)

echo.
echo 3. 启动应用...
echo 应用将在 http://localhost:8080 启动
echo 前端将在 http://localhost:3000 启动
echo.
echo 按 Ctrl+C 停止应用
echo.

call mvn spring-boot:run

pause
