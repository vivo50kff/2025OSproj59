[![Linux](https://img.shields.io/badge/Linux-Kernel%206.8+-orange.svg)](https://www.kernel.org/)
[![Build](https://img.shields.io/badge/Build-Success-green.svg)](#)
[![License](https://img.shields.io/badge/License-GPLv2-blue.svg)](#)
[![Platform](https://img.shields.io/badge/Platform-Ubuntu%20%7C%20QEMU%20%7C%20x86_64-lightgrey.svg)](#)

# 2025OSproj59
项目描述

本项目已同步至 GitHub 和 GitLab：

[![GitHub](https://img.shields.io/badge/GitHub-2025OSproj59-181717?logo=github)](https://github.com/vivo50kff/2025OSproj59)
[![GitLab](https://img.shields.io/badge/GitLab-2025OSproj59-FCA121?logo=gitlab)](https://gitlab.com/vivo50kff/2025OSproj59)

CFS 越来越臃肿, 对终端以及小型系统不太友好

其主要存在的问题:

CFS 体现完全公平, 缺乏针对关键线程的优先供给;
CFS 本身并不严格保证时延, 因此对时延敏感型的进程不友好;
由于 CFS 本身的越来越硬肿, 在开启组调度之后, 负载计算以及更新本身引入的开销越来越大.
该项目调度器目标:

适用于终端、车载以及云等场景;
支持对指定数量关键进程的优先调度, 这些线程可以抢占其他非关键进程;
在 2 的基础上, 进一步达成极低的调度延迟和唤醒延迟, 不同进程可以指定延时的 QOS, 调度器将尽可能的满足调度的 QOS 需求.
