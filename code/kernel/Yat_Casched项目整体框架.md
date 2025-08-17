# Yat_Casched 内核修改与文件结构说明

## 概述

Yat_Casched是一个基于缓存感知的Linux内核调度器，通过维护多层缓存历史记录来优化任务调度决策，提高系统性能。

## 内核文件修改结构

### 1. 新增核心文件

```
kernel/sched/
├── yat_casched.c                    # 主调度器实现文件 (核心)
├── sched.h                          # 调度器头文件 (修改)
└── core.c                           # 内核调度核心 (修改)

include/linux/sched/
├── yat_casched.h                    # YAT调度器头文件定义 (新增)
└── task.h                           # 任务结构体定义 (修改)

include/uapi/linux/
└── sched.h                          # 用户空间调度策略定义 (修改)
```

### 2. 文件详细说明

#### 2.1 核心实现文件：yat_casched.c

**文件路径**: `kernel/sched/yat_casched.c`

**主要功能**: 
- 实现完整的缓存感知调度算法
- 管理L1/L2/L3三级缓存历史记录
- 提供调度决策引擎和性能监控接口

**关键数据结构**:
```c
// 缓存历史记录结构
struct history_record {
    struct hlist_node hash_node;     // 哈希表节点
    struct list_head time_list_node; // 时间序列链表节点
    int task_id;                     // 任务PID
    u64 timestamp;                   // 记录时间戳
    u64 exec_time;                   // 执行时间
};

// 缓存历史表结构
struct cache_history_table {
    DECLARE_HASHTABLE(records, HISTORY_HASHTABLE_BITS);
    struct list_head time_list;      // 时间顺序链表
    spinlock_t lock;                 // 并发保护锁
};
```

**核心函数列表**:

##### 初始化函数
- `init_yat_casched_rq()` - 初始化YAT调度队列
- `init_history_tables()` - 初始化缓存历史表

##### 调度核心函数
- `select_task_rq_yat_casched()` - CPU选择算法
- `enqueue_task_yat_casched()` - 任务入队操作
- `dequeue_task_yat_casched()` - 任务出队操作
- `pick_next_task_yat_casched()` - 选择下一个执行任务
- `put_prev_task_yat_casched()` - 处理任务切换
- `set_next_task_yat_casched()` - 设置下一个任务

##### 缓存管理函数
- `add_history_record()` - 添加缓存历史记录

- `get_crp_ratio()` - 获取CRP性能比率
- `calculate_and_prune_recency()` - 计算并修剪历史记录

##### 辅助工具函数
- `calculate_cpu_load()` - 计算CPU负载
- `get_wcet()` - 获取最坏情况执行时间
- `sigmoid_transform_wcet()` - WCET的Sigmoid变换

##### 时间片管理
- `task_tick_yat_casched()` - 任务时钟节拍处理
- `wakeup_preempt_yat_casched()` - 抢占检查
- `update_curr_yat_casched()` - 更新当前任务运行时间

##### 调试接口
- `yat_debugfs_init()` - 初始化debugfs接口
- `yat_history_show()` - 显示缓存历史信息
- `yat_accelerator_show()` - 显示加速表信息

#### 2.2 头文件定义：include/linux/sched/yat_casched.h

**主要内容**:
- YAT调度实体结构定义
- 调度策略常量定义
- 函数声明和接口定义

```c
// YAT调度实体结构
struct sched_yat_casched_entity {
    struct rb_node rb_node;          // 红黑树节点
    u64 vruntime;                    // 虚拟运行时间
    u64 wcet;                        // 最坏情况执行时间
    int last_cpu;                    // 上次运行的CPU
    u64 per_cpu_recency[NR_CPUS];    // 每CPU的近因距离
};

// YAT运行队列结构
struct yat_casched_rq {
    struct rb_root_cached tasks;     // 红黑树任务队列
    int nr_running;                  // 运行任务数
    u64 load;                        // 当前负载
    struct task_struct *agent;       // 代理任务
    spinlock_t history_lock;         // 历史记录锁
};
```

#### 2.3 调度器集成：kernel/sched/sched.h

**修改内容**:
- 添加YAT调度类声明
- 集成YAT运行队列到主运行队列结构
- 添加调度器优先级定义

```c
// 调度类声明
extern const struct sched_class yat_casched_sched_class;

// 运行队列结构修改 (在struct rq中添加)
struct yat_casched_rq yat_casched;

// 调度策略定义
#define SCHED_YAT_CASCHED    8
```

#### 2.4 用户空间接口：include/uapi/linux/sched.h

**修改内容**:
- 添加用户空间可见的调度策略常量
- 提供系统调用接口支持

```c
#define SCHED_YAT_CASCHED    8
```

## 关键算法实现

### 1. CPU选择算法 (`select_task_rq_yat_casched`)

**算法逻辑**:
1. **优先选择上次运行的CPU** (缓存热度最高)
2. **寻找空闲CPU** (避免负载冲突)
3. **计算调度收益** (基于CRP模型)
4. **负载均衡** (选择负载最小的CPU)

**伪代码**:
```
if (last_cpu是空闲的):
    return last_cpu
elif (存在其他空闲CPU):
    for each 空闲CPU:
        计算 benefit = (1000 - CRP_ratio) * WCET / 1000
    return 收益最大的CPU
else:
    return 负载最小的CPU
```

### 2. 缓存近因计算 (`calculate_recency`)

**分层策略**:
- **L1缓存**: < 1ms，命中率最高
- **L2缓存**: 1ms ~ 10ms，中等命中率  
- **L3缓存**: 10ms ~ 50ms，较低命中率
- **冷缓存**: > 50ms，需要完全重新加载

### 3. CRP性能建模 (`get_crp_ratio`)

**分段线性函数**:
```c
if (recency < 1ms):     return 600 + (recency * 200) / 1ms    // L1范围
if (recency < 10ms):    return 800 + (recency * 150) / 9ms    // L2范围  
if (recency < 50ms):    return 950 + (recency * 50) / 40ms    // L3范围
else:                   return 1000                           // 冷缓存
```

## 编译与集成

### 1. Makefile修改

**kernel/sched/Makefile**:
```makefile
obj-y += yat_casched.o
```

### 2. 内核配置

需要在内核配置中启用：
- `CONFIG_SCHED_DEBUG=y` (调度调试支持)
- `CONFIG_DEBUG_FS=y` (debugfs支持)
- `CONFIG_FTRACE=y` (可选，ftrace支持)

## 调试与监控接口

### debugfs接口

**挂载路径**: `/sys/kernel/debug/yat_casched/`

**可用文件**:
- `history_table` - 查看缓存历史记录
- `accelerator_table` - 查看调度加速表
- `load_info` - 查看CPU负载信息

**使用示例**:
```bash
# 挂载debugfs
mount -t debugfs none /sys/kernel/debug

# 查看历史表
cat /sys/kernel/debug/yat_casched/history_table

# 查看加速表  
cat /sys/kernel/debug/yat_casched/accelerator_table
```

## 性能特点

### 优势
- **缓存感知**: 基于多层缓存历史优化任务放置
- **智能调度**: CRP模型驱动的调度决策
- **负载均衡**: 动态负载感知和均衡
- **实时监控**: 完善的debugfs调试接口

### 适用场景
- **计算密集型应用**: 受益于缓存局部性优化
- **多任务并发**: 优化任务间的缓存冲突
- **长时间运行**: 历史信息累积提升调度精度

## 测试与验证

### 测试程序
- **TACLe基准测试套件**: 标准化性能测试
- **对比测试**: 与CFS调度器性能对比
- **压力测试**: 多任务并发调度验证

### 性能指标
- **响应时间**: 任务完成时间
- **缓存命中率**: 缓存利用效率  
- **调度开销**: 调度器自身开销
- **负载均衡度**: 各CPU负载分布均匀性

---

**总结**: Yat_Casched调度器通过系统化的缓存感知设计，在保持Linux调度器兼容性的同时，显著提升了缓存敏感应用的性能表现。