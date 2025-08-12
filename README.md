[![Linux](https://img.shields.io/badge/Linux-Kernel%206.8-orange.svg)](https://www.kernel.org/)
[![Build](https://img.shields.io/badge/Build-Success-green.svg)](#)
[![License](https://img.shields.io/badge/License-GPLv2-blue.svg)](#)
[![Platform](https://img.shields.io/badge/Platform-Ubuntu%20%7C%20QEMU%20%7C%20x86_64-lightgrey.svg)](#)

# 2025OSproj59

## 队伍信息

| 字段   | 内容                 |
| ------ | -------------------- |
| 队伍ID | T202510558995172     |
| 队伍名 | 从容应队             |
| 项目   | proj59               |
| 成员   | 林炜东、马福泉、刘昊 |

本项目已同步至 GitHub 和 GitLab：

[![GitHub](https://img.shields.io/badge/GitHub-2025OSproj59-181717?logo=github)](https://github.com/vivo50kff/2025OSproj59)
[![GitLab](https://img.shields.io/badge/GitLab-2025OSproj59-FCA121?logo=gitlab)](https://gitlab.eduxiji.net/T202510558995172/project2721707-287881)

## 项目介绍

**Yat_Casched** (Yet Another Task Cache-Aware Scheduler) 是一个专注于缓存感知的高性能任务调度器项目。项目包含完整的内核级调度器实现和仿真验证系统，旨在解决现代多核处理器环境下传统调度器对缓存局部性考虑不足的问题。

### 背景与问题

传统的 Linux CFS 调度器存在以下问题：

- **公平性过度强调**：CFS 体现完全公平，缺乏针对关键线程的优先供给
- **时延保证不足**：CFS 本身并不严格保证时延，对时延敏感型进程不友好
- **负载过重**：在开启组调度后，负载计算及更新引入的开销越来越大
- **缓存局部性忽视**：频繁的任务迁移导致缓存失效，性能下降

### 项目目标

本项目调度器致力于实现：

1. **多场景适用**：适用于终端、车载以及云计算等场景
2. **优先级调度**：支持对指定数量关键进程的优先调度，可抢占非关键进程
3. **低延迟保证**：达成极低的调度延迟和唤醒延迟
4. **QoS 支持**：不同进程可指定延迟 QoS，调度器尽力满足需求
5. **缓存感知**：基于Cache Recency Profile理论，优化缓存局部性

## 项目架构

本项目分为两个核心模块：

### 内核模块 (Kernel Module)

- **内核调度器实现**：基于 Linux 6.8+ 内核的缓存感知调度器
- **调度策略 ID**：`SCHED_YAT_CASCHED = 8`
- **核心特性**：
  - 五策略智能调度算法
  - 三层缓存历史表建模
  - CRP缓存重用距离计算
  - 完整的调度类实现

### 仿真系统 (Simulation System)

- **高性能调度模拟器**：基于 Java 的可视化仿真平台
- **多算法支持**：WFD、Cache-Aware 算法对比
- **核心功能**：
  - 多级缓存建模（L1/L2/L3）
  - 实时性能分析
  - 可视化结果展示
  - 算法性能对比

## 快速开始

### 内核模块测试（需要完整内核，仓库地址见kernel文件夹里）

```bash
# 进入内核模块测试目录，下面路径需要修改，需要放到linux内核根目录下
cd boot_test_scripts

# 启动 QEMU 测试环境（x86_64）
./start_yat_simple_test.sh

# 或者使用ARM64环境
./start_yat_simple_test_with_arm64.sh

# 在 QEMU 环境中测试调度器
./yat_simple_test
```

### 进阶测试（优先级和性能测试）

```bash
# 进入高级测试目录，同上，该文件夹要放到完整内核根目录下
cd yat_test

# 启动优先级测试环境
./start_priority_test.sh

# 启用ftrace追踪的测试环境
./start_yat_simple_test_ftrace.sh
```

### 仿真系统运行

```bash
# 进入仿真目录
cd code/simu/src

# 编译项目
make

# 运行算法对比实验
java AlgorithmComparisonExperiment

# 运行库验证
java LibraryVerification
```

## 核心技术特性

### 缓存感知调度

- **五策略智能调度**：基于系统状态的差异化CPU选择算法
- **三层缓存建模**：精确的L1/L2/L3缓存历史表架构
- **CRP算法**：Cache Recency Profile理论的缓存收益计算

### 高性能优化

- **微秒级调度延迟**：内存池+红黑树优化
- **负载均衡**：缓存亲和性与负载平衡的智能权衡
- **可扩展设计**：模块化架构，易于扩展新算法

### 全面验证

- **真实内核环境**：基于 Linux 6.8+ 的完整实现
- **仿真对比验证**：多算法性能对比分析
- **可视化展示**：性能指标图表化展示

## 实验结果

根据仿真实验结果，Yat_Casched 调度器相比传统算法具有显著优势：

- **算法胜率**：在100个测试案例中胜率达98.7%
- **缓存命中率提升**：平均提升 63.3%
- **任务完成时间减少**：减少 11.9%
- **系统性能提升**：特定任务集性能改善9.77%

详细实验数据请参考：[仿真实验结果](code/simu/src/result/)

## 文档索引

### 📚 详细文档

- **[Kernel 模块完整文档](code/kernel/README.md)** - 内核调度器实现详解
- **[仿真系统使用指南](code/simu/README.md)** - 仿真平台操作手册
- **[基础测试脚本说明](code/kernel/boot_test_scripts/README.md)** - QEMU 基础测试环境
- **[高级测试说明](code/kernel/yat_test/)** - 优先级和性能测试
- **[TacleBench测试](code/kernel/tacle-bench/README.md)** - 标准基准测试

### 🔧 开发资源

- **源码文件**：
  - [yat_casched.c](code/kernel/yat_casched.c) - 核心调度器实现
  - [yat_casched.h](code/kernel/yat_casched.h) - 调度器头文件
  - [AlgorithmComparisonExperiment.java](code/simu/src/AlgorithmComparisonExperiment.java) - 算法对比实验

### 📊 测试与验证

- **[基础测试程序](code/kernel/boot_test_scripts/)** - 基础功能测试
- **[高级测试程序](code/kernel/yat_test/)** - 优先级和性能测试
- **[TacleBench测试](code/kernel/tacle-bench/)** - 标准基准测试集
- **[实验结果](code/simu/src/result/)** - 性能测试数据和图表

## 技术栈

### 内核模块

- **语言**：C
- **平台**：Linux 6.8+
- **架构**：x86_64, ARM64
- **编译工具**：GCC, Make, Kbuild
- **虚拟化**：QEMU (x86_64/ARM64)
- **文件系统**：Initramfs, BusyBox
- **调试工具**：debugfs, ftrace, perf
- **性能分析**：TacleBench基准测试集

### 仿真系统

- **语言**：Java 17+
- **依赖**：Jackson, Commons-Math3, JGraphT
- **构建**：Make/Gradle
- **可视化**：自研图表库
