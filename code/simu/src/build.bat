@echo off
REM Cache-Aware vs WFD 算法对比实验 Windows批处理构建脚本
REM 这是Makefile的备用方案，适合没有Make工具的Windows环境

echo ==========================================
echo Cache-Aware vs WFD 算法对比实验构建系统
echo ==========================================
echo.

if "%1"=="" (
    echo 使用方法:
    echo   build.bat compile    - 编译所有模块
    echo   build.bat lib-test   - 运行库依赖测试
    echo   build.bat run        - 运行完整流程（编译+实验+可视化）
    echo   build.bat picture    - 仅生成Python可视化结果图
    echo   build.bat clean      - 清理编译文件
    echo   build.bat all        - 完整构建和测试流程
    echo.
    echo 推荐使用: build.bat run （一键完成所有任务）
    echo ==========================================
    goto :end
)

REM 设置项目配置
set JAVA_SRC=.
set CLASS_DIR=classes
set LIB_DIR=lib
set MAIN_CLASS=AlgorithmComparisonExperiment
set LIB_TEST_CLASS=LibraryVerification
set CLASSPATH=%LIB_DIR%/*;%CLASS_DIR%;%JAVA_SRC%

if "%1"=="compile" goto :compile
if "%1"=="lib-test" goto :lib_test
if "%1"=="run" goto :run
if "%1"=="picture" goto :picture
if "%1"=="clean" goto :clean
if "%1"=="all" goto :all

echo 错误：未知命令 "%1"
goto :end

:compile
echo ==========================================
echo 开始编译所有Java模块...
echo ==========================================

REM 创建classes目录
if not exist "%CLASS_DIR%" mkdir "%CLASS_DIR%"

echo 1. 编译parameters模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" parameters/*.java
if errorlevel 1 goto :error
echo ✓ parameters模块编译完成

echo 2. 编译entity模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" entity/*.java
if errorlevel 1 goto :error
echo ✓ entity模块编译完成

echo 3. 编译allocation模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" allocation/*.java
if errorlevel 1 goto :error
echo ✓ allocation模块编译完成

echo 4. 编译generator模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" generator/*.java
if errorlevel 1 goto :error
echo ✓ generator模块编译完成

echo 5. 编译analyzer模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" analyzer/*.java
if errorlevel 1 goto :error
echo ✓ analyzer模块编译完成

echo 6. 编译visualizer模块...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" visualizer/*.java
if errorlevel 1 goto :error
echo ✓ visualizer模块编译完成

echo 7. 编译主程序和测试程序...
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" %LIB_TEST_CLASS%.java
if errorlevel 1 goto :error
javac -cp "%CLASSPATH%" -d "%CLASS_DIR%" %MAIN_CLASS%.java
if errorlevel 1 goto :error
echo ✓ 主程序和测试程序编译完成

echo ==========================================
echo 🎉 所有模块编译成功！
echo ==========================================
goto :end

:lib_test
call :compile
if errorlevel 1 goto :error

echo ==========================================
echo 运行库依赖测试...
echo ==========================================
java -cp "%CLASSPATH%" %LIB_TEST_CLASS%
if errorlevel 1 goto :error
echo ==========================================
echo ✓ 库依赖测试完成
echo ==========================================
goto :end

:run
call :compile
if errorlevel 1 goto :error

echo ==========================================
echo 运行主实验程序...
echo ==========================================
echo ⏱️  预计运行时间: 2-5分钟（取决于系统性能）
echo ==========================================
java -cp "%CLASSPATH%" %MAIN_CLASS%
if errorlevel 1 goto :error
echo ==========================================
echo 🎉 主实验程序运行完成！
echo ==========================================

REM 自动运行可视化程序
echo.
echo ==========================================
echo 自动生成可视化图表...
echo ==========================================
call :picture_internal
if errorlevel 1 (
    echo ⚠️  可视化生成失败，但实验数据已保存
) else (
    echo ✅ 完整流程执行成功！实验数据和图表都已生成
)
echo ==========================================
goto :end

:picture
echo ==========================================
echo 生成Python可视化结果图 v2.0...
echo ==========================================
call :picture_internal
goto :end

:picture_internal
REM 检查实验结果文件是否存在（优先检查result目录）
set CSV_FILE=""
if exist "result\algorithm_comparison_results.csv" (
    echo ✓ 找到实验结果文件: result\algorithm_comparison_results.csv
    set CSV_FILE=result\algorithm_comparison_results.csv
) else if exist "visualizer\result\algorithm_comparison_results.csv" (
    echo ✓ 找到实验结果文件: visualizer\result\algorithm_comparison_results.csv
    set CSV_FILE=visualizer\result\algorithm_comparison_results.csv
) else if exist "result\experiment_results.csv" (
    echo ✓ 找到实验结果文件: result\experiment_results.csv
    set CSV_FILE=result\experiment_results.csv
) else if exist "visualizer\result\experiment_results.csv" (
    echo ✓ 找到实验结果文件: visualizer\result\experiment_results.csv
    set CSV_FILE=visualizer\result\experiment_results.csv
) else if exist "algorithm_comparison_results.csv" (
    echo ✓ 找到实验结果文件: algorithm_comparison_results.csv
    set CSV_FILE=algorithm_comparison_results.csv
) else if exist "experiment_results.csv" (
    echo ✓ 找到实验结果文件: experiment_results.csv
    set CSV_FILE=experiment_results.csv
) else (
    echo ❌ 未找到实验结果文件
    echo    查找路径: result\algorithm_comparison_results.csv
    echo              visualizer\result\algorithm_comparison_results.csv
    echo              result\experiment_results.csv
    echo              algorithm_comparison_results.csv
    echo              experiment_results.csv
    exit /b 1
)

echo 📊 检查Python环境和依赖...
cd visualizer
pip install -q -r requirements.txt >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Python依赖安装失败，将尝试基础可视化
)

echo 🎨 使用全新设计的可视化程序生成图表...
python visualize.py >nul 2>&1
if errorlevel 1 (
    echo ⚠️  新版可视化生成失败，尝试备用版本
    python visualize_results.py >nul 2>&1
    if errorlevel 1 (
        echo ❌ 所有可视化方案均失败
        cd ..
        exit /b 1
    )
)

cd ..
echo ✅ 可视化图表生成完成！
echo 生成的文件位置: result\
if exist "result\makespan_analysis.png" echo   ✓ makespan_analysis.png - Makespan Analysis
if exist "result\cache_hit_analysis.png" echo   ✓ cache_hit_analysis.png - Cache Hit Rate Analysis
if exist "result\execution_time_and_win_rate_analysis.png" echo   ✓ execution_time_and_win_rate_analysis.png - Execution Time and Win Rate Analysis
if exist "result\unified_algorithm_comparison.png" echo   ✓ unified_algorithm_comparison.png (backup)
if exist "result\unified_win_rate_comparison.png" echo   ✓ unified_win_rate_comparison.png (backup)
exit /b 0

:clean
echo ==========================================
echo 清理编译文件...
echo ==========================================
if exist "%CLASS_DIR%" rmdir /s /q "%CLASS_DIR%"
if exist "experiment_results.csv" del "experiment_results.csv"
if exist "*.png" del "*.png"
echo ✓ 清理完成
goto :end

:all
call :clean
call :compile
if errorlevel 1 goto :error
call :lib_test
if errorlevel 1 goto :error

echo ==========================================
echo 🎉 完整构建和测试流程完成！
echo ==========================================
echo 现在可以运行以下命令：
echo   build.bat run     - 运行主实验
echo   build.bat picture - 生成可视化图表
echo ==========================================
goto :end

:error
echo ==========================================
echo ❌ 构建过程中发生错误！
echo ==========================================
exit /b 1

:end
