package entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 任务节点实体类
 * 
 * 该类表示任务调度系统中的单个任务节点，包含了任务执行所需的所有信息。
 * 特别针对Cache-Aware调度算法进行了扩展，增加了缓存相关的属性和方法。
 * 
 * 主要功能：
 * - 维护任务的基本执行信息（执行时间、依赖关系等）
 * - 记录缓存相关属性（权重、敏感度、亲和性）
 * - 支持DAG（有向无环图）结构的任务依赖关系
 * - 提供实时的缓存命中率统计
 * - 支持任务调度和性能分析
 * 
 * 缓存感知特性：
 * - 四级缓存权重配置（L1、L2、L3、Memory）
 * - 任务缓存敏感度量化
 * - 处理器亲和性支持
 * - 实时缓存命中率跟踪
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class Node implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务节点类型枚举
     * 
     * 定义了DAG中不同类型的任务节点，用于任务调度和依赖关系管理。
     */
    public enum NodeType {
        SOURCE,  // 源节点：DAG的起始节点，无前驱依赖
        SINK,    // 汇节点：DAG的结束节点，无后继依赖
        NORMAL,  // 普通节点：具有前驱和后继依赖的中间节点
        SOLO     // 独立节点：无任何依赖关系的独立任务
    }
    
    // ==================== 基本属性 ====================
    
    /** 任务节点唯一标识符 */
    private int id;
    
    /** 所属DAG的标识符 */
    private int dagID;
    
    /** DAG实例编号 */
    private int dagInstNo;
    
    /** 节点类型 */
    private NodeType type;
    
    /** 任务在DAG中的层级 */
    private int layer;
    
    // ==================== 执行相关属性 ====================
    
    /** 任务的预期执行时间（纳秒） */
    public long expectedET = 100;
    
    /** 任务释放时间（可开始执行的最早时间） */
    public long release = -1;
    
    /** 任务实际开始执行时间 */
    public long start = -1;
    
    /** 任务预期完成时间 */
    public long finishAt = -1;
    
    /** 任务是否已完成执行 */
    public boolean finish = false;
    
    /** 任务是否因资源竞争而延迟 */
    public boolean isDelayed = false;
    
    /** 分配的处理器ID（-1表示未分配） */
    public int partition = -1;
    
    /** 处理器亲和性（建议的处理器ID，-1表示无偏好） */
    public int affinity = -1;
    
    // ==================== DAG结构相关属性 ====================
    
    /** 子任务列表（该任务的后继依赖） */
    private List<Node> children = new ArrayList<>();
    
    /** 父任务列表（该任务的前驱依赖） */
    private List<Node> parents = new ArrayList<>();
    
    // ==================== 缓存相关属性 - Cache-Aware算法核心 ====================
    
    /** 
     * 缓存层级权重数组 [L1, L2, L3, Memory]
     * 表示任务对各级缓存的访问频率和重要性
     */
    public double[] weights = new double[4];
    
    /** 
     * 缓存敏感度 (0.0-1.0)
     * 表示任务性能对缓存命中率的敏感程度
     * 值越高表示任务越依赖缓存性能
     */
    public double sensitivity = 0.0;
    
    /** 缓存性能配置文件 */
    public RecencyProfileReal crp;
    
    /** 是否存在故障（用于容错调度） */
    public boolean hasFaults = false;
    
    /** 是否为关键路径上的任务 */
    public boolean isCritical = false;
    
    // ==================== 实际缓存性能统计 - 用于性能分析 ====================
    
    /** 
     * 实际缓存命中率 (0.0-1.0)
     * 记录任务执行过程中的实际缓存命中情况
     */
    public double actualCacheHitRatio = 0.0;
    
    /** 
     * 各级缓存的实际命中率 [L1, L2, L3]
     * 分别记录L1、L2、L3缓存的命中率
     */
    public double[] actualL1L2L3HitRatio = new double[3];
    
    /**
     * 任务节点构造函数
     * 
     * 创建一个新的任务节点，并初始化其基本属性和缓存相关参数。
     * 
     * @param layer 任务在DAG中的层级
     * @param type 任务节点类型
     * @param id 任务唯一标识符
     * @param dagID 所属DAG标识符
     * @param crp 缓存性能配置文件
     * @param rng 随机数生成器（用于生成随机属性）
     */
    public Node(int layer, NodeType type, int id, int dagID, RecencyProfileReal crp, Random rng) {
        this.id = id;
        this.dagID = dagID;
        this.type = type;
        this.layer = layer;
        this.crp = crp;
        this.weights = new double[4];
        
        // 设置随机化的执行时间（100-150纳秒范围）
        this.expectedET = 100 + (rng != null ? rng.nextInt(50) : 0);
        
        // 设置随机化的缓存敏感度（0.0-1.0范围）
        this.sensitivity = rng != null ? rng.nextDouble() : 0.5;
        
        // 初始化缓存权重 - Cache-Aware算法的关键参数
        // 权重表示任务对各级缓存的访问模式和重要性
        if (rng != null) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = rng.nextDouble(); // 生成0.0-1.0的随机权重
            }
        }
    }
    
    // ==================== Getter和Setter方法 ====================
    
    /** 获取任务ID */
    public int getId() { return id; }
    
    /** 获取DAG ID */
    public int getDagID() { return dagID; }
    
    /** 获取DAG实例编号 */
    public int getDagInstNo() { return dagInstNo; }
    
    /** 设置DAG实例编号 */
    public void setDagInstNo(int instNo) { this.dagInstNo = instNo; }
    
    /** 获取节点类型 */
    public NodeType getType() { return type; }
    
    /** 获取任务层级 */
    public int getLayer() { return layer; }
    
    /** 获取最坏情况执行时间（WCET） */
    public long getWCET() { return expectedET; }
    
    // ==================== DAG结构管理方法 ====================
    
    /** 获取子任务列表 */
    public List<Node> getChildren() { return children; }
    
    /** 获取父任务列表 */
    public List<Node> getParent() { return parents; }
    
    /**
     * 添加子任务
     * 
     * 建立当前任务到子任务的依赖关系，同时在子任务中添加当前任务为父任务。
     * 
     * @param child 要添加的子任务节点
     */
    public void addChildren(Node child) {
        children.add(child);
        child.parents.add(this);
    }
    
    /**
     * 添加父任务
     * 
     * 建立当前任务到父任务的依赖关系，同时在父任务中添加当前任务为子任务。
     * 
     * @param parent 要添加的父任务节点
     */
    public void addParent(Node parent) {
        parents.add(parent);
        parent.children.add(this);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取任务的简短名称
     * 
     * @return 格式为"T{dagID}_{id}"的简短名称
     */
    public String getShortName() {
        return "T" + dagID + "_" + id;
    }
    
    /**
     * 获取任务的执行信息字符串
     * 
     * @return 包含任务名称、时间信息和处理器分配的详细字符串
     */
    public String getExeInfo() {
        return getShortName() + " [" + release + "->" + start + "->" + finishAt + "] P" + partition;
    }
    
    
    /**
     * 重写toString方法
     * 
     * @return 任务的简短名称表示
     */
    @Override
    public String toString() {
        return getShortName();
    }
}