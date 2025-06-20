package generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entity.Node;
import entity.Node.NodeType;
import entity.RecencyProfileReal;

/**
 * 增强型任务集生成器
 * 
 * 该类专门为Cache-Aware任务调度算法设计，能够生成具有真实特征的任务集。
 * 使用UUnifastDiscard算法确保利用率分布的合理性，并生成缓存敏感的任务属性。
 * 
 * 主要功能：
 * - 基于UUnifastDiscard算法的科学利用率分布
 * - 生成多样化的缓存敏感度特征
 * - 模拟真实的任务执行时间和周期
 * - 支持缓存权重和亲和性配置
 * - 生成适合算法对比的测试任务集
 * 
 * 任务特征建模：
 * - 缓存敏感度分布：高(40%) + 中(40%) + 低(20%)
 * - 周期分布：基于典型实时系统周期
 * - 执行时间：基于利用率和周期的科学计算
 * - 缓存权重：模拟真实的内存访问模式
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class EnhancedTaskGenerator {
    
    private Random rng;
    private int cores;
    
    public EnhancedTaskGenerator(int cores, Random rng) {
        this.cores = cores;
        this.rng = rng;
    }
    
    /**
     * 生成复杂的任务集，使用UUnifastDiscard算法控制利用率分布
     */
    public List<Node> generateComplexTaskSet(int taskCount, double totalUtilization) {
        List<Node> taskSet = new ArrayList<>();
        
        // 使用UUnifastDiscard生成利用率分布
        UUnifastDiscard uunifast = new UUnifastDiscard(
            totalUtilization, taskCount, 1000, cores, false, rng);
        
        ArrayList<Double> utilizations = uunifast.getUtils();
        
        if (utilizations == null || utilizations.size() < taskCount) {
            // 如果UUnifastDiscard失败，使用备用方法
            utilizations = generateFallbackUtilizations(taskCount, totalUtilization);
        }
        
        // 为每个利用率创建任务
        for (int i = 0; i < taskCount; i++) {
            double util = i < utilizations.size() ? utilizations.get(i) : 0.1;
            Node task = createTaskFromUtilization(i, util);
            taskSet.add(task);
        }
        
        return taskSet;
    }
    
    /**
     * 根据利用率创建任务
     */
    private Node createTaskFromUtilization(int taskId, double utilization) {
        RecencyProfileReal crp = new RecencyProfileReal(100);
        
        // 生成周期（影响任务复杂度）
        long period = generatePeriod();
        
        // 根据利用率计算执行时间：WCET = Utilization * Period
        long wcet = (long)(utilization * period);
        wcet = Math.max(wcet, 10); // 最小执行时间
        wcet = Math.min(wcet, period); // 不能超过周期
        
        Node task = new Node(0, NodeType.NORMAL, taskId, taskId / 5, crp, rng);
        task.expectedET = wcet;
        
        // 设置缓存敏感度（这是Cache-Aware算法的关键）
        task.sensitivity = generateCacheSensitivity();
        
        // 设置缓存权重（影响不同缓存级别的性能）
        task.weights = generateCacheWeights();
        
        // 随机设置一些任务为关键任务
        task.isCritical = rng.nextDouble() < 0.3; // 30%的任务是关键任务
        
        return task;
    }
    
    /**
     * 生成周期（使用典型的实时系统周期分布）
     */
    private long generatePeriod() {
        // 使用常见的实时系统周期：10, 20, 50, 100, 200, 500, 1000 ms
        long[] commonPeriods = {10, 20, 50, 100, 200, 500, 1000};
        return commonPeriods[rng.nextInt(commonPeriods.length)];
    }
    
    /**
     * 生成缓存敏感度（这决定了Cache-Aware算法的优势）
     */
    private double generateCacheSensitivity() {
        // 使用三种类型的任务：
        // 1. 高缓存敏感度任务 (0.7-1.0) - 40%
        // 2. 中等缓存敏感度任务 (0.3-0.7) - 40%  
        // 3. 低缓存敏感度任务 (0.0-0.3) - 20%
        
        double rand = rng.nextDouble();
        if (rand < 0.4) {
            // 高缓存敏感度
            return 0.7 + rng.nextDouble() * 0.3;
        } else if (rand < 0.8) {
            // 中等缓存敏感度
            return 0.3 + rng.nextDouble() * 0.4;
        } else {
            // 低缓存敏感度
            return rng.nextDouble() * 0.3;
        }
    }
    
    /**
     * 生成缓存权重分布
     */
    private double[] generateCacheWeights() {
        double[] weights = new double[4]; // L1, L2, L3, Memory
        
        // 根据任务类型生成不同的权重分布
        double taskType = rng.nextDouble();
        
        if (taskType < 0.3) {
            // 计算密集型任务：更依赖L1缓存
            weights[0] = 0.5 + rng.nextDouble() * 0.3; // L1: 0.5-0.8
            weights[1] = 0.2 + rng.nextDouble() * 0.2; // L2: 0.2-0.4
            weights[2] = 0.1 + rng.nextDouble() * 0.1; // L3: 0.1-0.2
            weights[3] = 0.05 + rng.nextDouble() * 0.1; // Memory: 0.05-0.15
        } else if (taskType < 0.6) {
            // 数据密集型任务：更依赖L2/L3缓存
            weights[0] = 0.2 + rng.nextDouble() * 0.2; // L1: 0.2-0.4
            weights[1] = 0.3 + rng.nextDouble() * 0.2; // L2: 0.3-0.5
            weights[2] = 0.2 + rng.nextDouble() * 0.2; // L3: 0.2-0.4
            weights[3] = 0.1 + rng.nextDouble() * 0.2; // Memory: 0.1-0.3
        } else {
            // 内存密集型任务：更多内存访问
            weights[0] = 0.1 + rng.nextDouble() * 0.2; // L1: 0.1-0.3
            weights[1] = 0.2 + rng.nextDouble() * 0.2; // L2: 0.2-0.4
            weights[2] = 0.2 + rng.nextDouble() * 0.2; // L3: 0.2-0.4
            weights[3] = 0.3 + rng.nextDouble() * 0.2; // Memory: 0.3-0.5
        }
        
        // 归一化权重
        double sum = weights[0] + weights[1] + weights[2] + weights[3];
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }
        
        return weights;
    }
    
    /**
     * 备用利用率生成方法
     */
    private ArrayList<Double> generateFallbackUtilizations(int taskCount, double totalUtil) {
        ArrayList<Double> utils = new ArrayList<>();
        double remaining = totalUtil;
        
        for (int i = 0; i < taskCount - 1; i++) {
            double maxUtil = Math.min(remaining, 1.0);
            double util = rng.nextDouble() * maxUtil * 0.8; // 保守分配
            utils.add(util);
            remaining -= util;
        }
        
        // 最后一个任务获得剩余利用率
        utils.add(Math.max(0, remaining));
        
        return utils;
    }
    
    /**
     * 生成缓存密集型任务集，突出缓存敏感特性
     * 专门用于对比Cache-Aware算法的性能优势
     */
    public List<Node> generateCacheIntensiveTasks(int taskCount, double totalUtilization, 
                                                 double highSensitivityRatio) {
        List<Node> taskSet = new ArrayList<>();
        
        // 使用UUnifastDiscard生成利用率分布
        UUnifastDiscard uunifast = new UUnifastDiscard(
            totalUtilization, taskCount, 1000, cores, false, rng);
        
        ArrayList<Double> utilizations = uunifast.getUtils();
        
        if (utilizations == null || utilizations.size() < taskCount) {
            utilizations = generateFallbackUtilizations(taskCount, totalUtilization);
        }
        
        // 计算高敏感度任务数量
        int highSensitivityCount = (int)(taskCount * highSensitivityRatio);
        
        // 为每个利用率创建缓存密集型任务
        for (int i = 0; i < taskCount; i++) {
            double util = i < utilizations.size() ? utilizations.get(i) : 0.1;
            Node task = createCacheIntensiveTask(i, util, i < highSensitivityCount);
            taskSet.add(task);
        }
        
        return taskSet;
    }
      /**
     * 创建缓存密集型任务
     */
    private Node createCacheIntensiveTask(int taskId, double utilization, boolean isHighSensitivity) {
        // 创建RecencyProfile
        RecencyProfileReal profile = createCacheIntensiveProfile(isHighSensitivity);
        
        // 使用Node类的正确构造函数
        Node task = new Node(0, NodeType.NORMAL, taskId, 0, profile, rng);
        
        // 基础执行时间计算
        long baseExecutionTime = (long)(utilization * 1000000); // 基于利用率的执行时间
        task.expectedET = Math.max(baseExecutionTime, 10000); // 最小1万纳秒
        
        // 设置缓存敏感度
        if (isHighSensitivity) {
            // 高敏感度任务：0.7-0.95
            task.sensitivity = 0.7 + rng.nextDouble() * 0.25;
        } else {
            // 低-中敏感度任务：0.1-0.6
            task.sensitivity = 0.1 + rng.nextDouble() * 0.5;
        }
        
        // 设置缓存权重 - 更突出L1缓存的重要性
        if (task.weights == null) {
            task.weights = new double[4];
        }
        
        if (isHighSensitivity) {
            // 高敏感度任务更依赖L1缓存
            task.weights[0] = 0.6 + rng.nextDouble() * 0.3; // L1: 0.6-0.9
            task.weights[1] = 0.2 + rng.nextDouble() * 0.2; // L2: 0.2-0.4
            task.weights[2] = 0.1 + rng.nextDouble() * 0.1; // L3: 0.1-0.2
            task.weights[3] = 0.02 + rng.nextDouble() * 0.03; // Memory: 0.02-0.05
        } else {
            // 低敏感度任务权重分布更均匀
            task.weights[0] = 0.3 + rng.nextDouble() * 0.3; // L1: 0.3-0.6
            task.weights[1] = 0.2 + rng.nextDouble() * 0.2; // L2: 0.2-0.4
            task.weights[2] = 0.15 + rng.nextDouble() * 0.15; // L3: 0.15-0.3
            task.weights[3] = 0.05 + rng.nextDouble() * 0.15; // Memory: 0.05-0.2
        }
        
        // 归一化权重
        double weightSum = task.weights[0] + task.weights[1] + task.weights[2] + task.weights[3];
        for (int i = 0; i < task.weights.length; i++) {
            task.weights[i] /= weightSum;
        }
        
        // 设置处理器亲和性
        if (rng.nextDouble() < 0.6) { // 60%的任务有亲和性偏好
            task.affinity = rng.nextInt(cores);
        } else {
            task.affinity = -1; // 无特定亲和性
        }
        
        return task;
    }
      /**
     * 创建缓存密集型的recency profile
     */
    private RecencyProfileReal createCacheIntensiveProfile(boolean isHighSensitivity) {
        // 根据敏感度设置不同的基础执行时间
        long baseET = isHighSensitivity ? 100 : 150;
        return new RecencyProfileReal(baseET);
    }
    
    /**
     * 生成高局部性的重访问分布（简化版本）
     */
    private double[] generateHighLocalityDistribution() {
        double[] distribution = new double[10];
        // 重点集中在近期访问的数据上
        distribution[0] = 0.4; // 40%访问最近的数据
        distribution[1] = 0.25; // 25%访问次近的数据
        distribution[2] = 0.15; // 15%访问第三近的数据
        
        // 其余数据访问概率递减
        double remaining = 0.2;
        for (int i = 3; i < 10; i++) {
            distribution[i] = remaining / 7;
        }
        
        return distribution;
    }
    
    /**
     * 生成低栈距离分布（更好的局部性）
     */
    private int[] generateLowStackDistance() {
        int[] stackDistance = new int[10];
        // 大部分访问都在较小的栈距离内
        for (int i = 0; i < 10; i++) {
            stackDistance[i] = 1 + (int)(Math.pow(2, i * 0.5)); // 指数增长但较慢
        }
        return stackDistance;
    }
    
    /**
     * 生成标准分布
     */
    private double[] generateStandardDistribution() {
        double[] distribution = new double[10];
        for (int i = 0; i < 10; i++) {
            distribution[i] = 1.0 / 10; // 均匀分布
        }
        return distribution;
    }
    
    /**
     * 生成标准栈距离
     */
    private int[] generateStandardStackDistance() {
        int[] stackDistance = new int[10];
        for (int i = 0; i < 10; i++) {
            stackDistance[i] = (int)Math.pow(2, i); // 标准指数增长
        }
        return stackDistance;
    }
}
