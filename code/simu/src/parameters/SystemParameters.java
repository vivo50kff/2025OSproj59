package parameters;

/**
 * 系统参数配置类
 * 
 * 该类定义了整个Cache-Aware任务调度系统的全局参数配置。
 * 包含处理器架构、缓存层次结构、任务生成和算法选择等关键参数。
 * 
 * 主要配置项：
 * - 处理器核心数量和架构参数
 * - 多级缓存权重分配和层次结构
 * - DAG任务生成参数
 * - 算法类型和实验配置
 * - 性能评估相关参数
 * 
 * 设计理念：
 * - 集中化配置管理，便于参数调优
 * - 支持多种硬件架构和算法组合
 * - 提供灵活的实验配置选项
 * - 易于扩展新的参数类型
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class SystemParameters {
    
    // ==================== 基本系统参数 ====================
    
    /** 处理器核心数量 */
    public static int coreNum = 4;
    
    /** 系统中的任务数量（Number of Tasks） */
    public static int NoS = 100;
    
    /** 每个任务的默认利用率 */
    public static double utilPerTask = 0.8;
    
    // ==================== 缓存相关参数 ====================
    
    /** 
     * 缓存层级权重分配 [L1, L2, L3, Memory]
     * 表示各级缓存在性能评估中的重要性权重
     * 默认均等分配，可根据实际硬件架构调整
     */
    public static double[] cc_weights = {0.25, 0.25, 0.25, 0.25};
    
    /** 缓存层级数量（不包括主内存） */
    public static int cacheLevel = 3;
    
    /** L2缓存共享的核心数量（每N个核心共享一个L2缓存） */
    public static int Level2CoreNum = 2;
    
    // ==================== DAG任务生成参数 ====================
    
    /** DAG中最大节点数量 */
    public static int maxNodes = 10;
    
    /** DAG中最小节点数量 */
    public static int minNodes = 5;
    
    // ==================== 枚举类型定义 ====================
    
    /**
     * 仿真类型枚举
     * 定义系统支持的不同仿真精度级别
     */
    public static enum SimuType {
        CLOCK_LEVEL  // 时钟级别精度仿真
    }
    
    /**
     * 硬件类型枚举
     * 定义系统支持的硬件配置类型
     */
    public static enum Hardware {
        PROC,           // 仅处理器（无缓存感知）
        PROC_CACHE      // 处理器+缓存（缓存感知）
    }
    
    /**
     * 分配算法枚举
     * 定义系统支持的任务分配算法类型
     */
    public static enum Allocation {
        WORST_FIT,      // WFD（Worst Fit Decreasing）算法
        CACHE_AWARE     // Cache-Aware算法
    }
    
    /**
     * 实验名称枚举
     * 定义不同类型的性能评估实验
     */
    public static enum ExpName {
        WFD_TEST,           // WFD算法单独测试
        CACHE_AWARE_TEST,   // Cache-Aware算法单独测试
        COMPARISON_TEST     // 算法对比测试
    }
    
    /**
     * 时间相关类型枚举
     * 定义时间戳和时间窗口处理方式
     */
    public static enum RecencyType {
        TIME_DEFAULT    // 默认时间处理方式
    }
}
