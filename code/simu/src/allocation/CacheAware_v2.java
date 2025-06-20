package allocation;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import entity.Node;
import parameters.SystemParameters;

/**
 * Cache-Aware任务分配算法
 * 
 * 综合考虑缓存收益、负载均衡、缓存亲和性的多维度评分算法。
 * 权重分配：缓存收益40%、负载均衡30%、缓存亲和性20%、缓存质量10%
 * 
 *  Cache-Aware Task Scheduling System
 */
public class CacheAware_v2 extends AllocationMethods {      /** 处理器负载跟踪 */
    private static Map<Integer, Double> processorLoads = new HashMap<>();
    
    /** 处理器缓存状态跟踪 */
    private static Map<Integer, CacheState> cacheStates = new HashMap<>();
    
    /** 处理器任务计数 */
    private static Map<Integer, Integer> taskCount = new HashMap<>();
    
    /** 缓存命中率统计 */
    private static Map<Integer, CacheHitStats> cacheHitStats = new HashMap<>();
      /**
     * 处理器缓存状态
     */
    private static class CacheState {
        double l1Utilization = 0.0;     // L1缓存利用率
        double l2Utilization = 0.0;     // L2缓存利用率
        double l3Utilization = 0.0;     // L3缓存利用率
        int recentTaskCount = 0;         // 最近任务数量
        double avgTaskSensitivity = 0.0; // 平均缓存敏感度
    }
      /**
     * 缓存命中率统计
     */
    private static class CacheHitStats {
        double totalL1Accesses = 0.0;   // L1总访问次数
        double totalL2Accesses = 0.0;   // L2总访问次数
        double totalL3Accesses = 0.0;   // L3总访问次数
        double totalL1Hits = 0.0;       // L1总命中次数
        double totalL2Hits = 0.0;       // L2总命中次数
        double totalL3Hits = 0.0;       // L3总命中次数
        int totalTasks = 0;             // 处理的任务总数
        
    }
        /**
     * Cache-Aware任务分配核心算法
     * 
     * 多维度评分策略：缓存收益、负载均衡、缓存亲和性、缓存质量
     */
    public int allocate(List<Node> readyNodes, List<Integer> availableProc) {
        // 参数有效性检查
        if (readyNodes.isEmpty() || availableProc.isEmpty()) {
            return -1;
        }
        
        // 获取要分配的任务（每次处理一个任务）
        Node nodeToAllocate = readyNodes.get(0);
        
        int bestProcessor = -1;              // 最优处理器ID
        double bestScore = Double.MIN_VALUE; // 最高适合度分数
        
        // 为每个可用处理器计算Cache-Aware适合度分数
        for (int procId : availableProc) {
            double score = calculateCacheAwareScore(nodeToAllocate, procId);
            
            if (score > bestScore) {
                bestScore = score;
                bestProcessor = procId;
            }
        }
        
        // 更新选中处理器的状态和统计信息
        if (bestProcessor != -1) {
            updateProcessorState(bestProcessor, nodeToAllocate);
            recordTaskCacheHitRatio(bestProcessor, nodeToAllocate);
        }
        
        // 返回选中的处理器ID，失败时返回第一个可用处理器
        return bestProcessor != -1 ? bestProcessor : availableProc.get(0);
    }    /**
     * 计算Cache-Aware综合适合度分数
     * 多维度加权评分：缓存收益40%、负载均衡30%、缓存亲和性20%、缓存质量10%、缓存干扰惩罚5%
     */
    private double calculateCacheAwareScore(Node node, int processorId) {
        // 初始化处理器状态（如果是首次访问该处理器）
        if (!cacheStates.containsKey(processorId)) {
            cacheStates.put(processorId, new CacheState());
            processorLoads.put(processorId, 0.0);
            taskCount.put(processorId, 0);
        }
        
        CacheState cacheState = cacheStates.get(processorId);
        double currentLoad = processorLoads.get(processorId);
        
        double score = 0.0;
        
        // 1. 缓存收益分数 (权重40%)
        // 评估任务在该处理器上可能获得的缓存性能提升
        double cacheScore = calculateCacheBenefitScore(node, processorId, cacheState) * 0.4;
        
        // 2. 负载均衡分数 (权重30%)
        // 倾向于选择当前负载较轻的处理器，维持系统负载均衡
        double loadScore = calculateLoadBalanceScore(currentLoad, node.expectedET) * 0.3;
        
        // 3. 缓存亲和性分数 (权重20%)
        // 考虑任务与处理器之间的缓存亲和关系，提升缓存局部性
        double affinityScore = calculateCacheAffinityScore(node, processorId, cacheState) * 0.2;
        
        // 4. 缓存质量分数 (权重10%)
        // 评估目标处理器当前的缓存状态质量，偏好缓存状态良好的处理器
        double cacheQualityScore = calculateCacheQualityScore(node, cacheState) * 0.1;
        
        // 5. 缓存干扰惩罚分数 (权重5%)
        // 减少因任务分配不当导致的缓存干扰，保护已有的缓存效果
        double interferenceScore = calculateCacheInterferenceScore(node, cacheState) * 0.05;
        
        // 计算最终的综合适合度分数
        score = cacheScore + loadScore + affinityScore + cacheQualityScore - interferenceScore;
        
        return score;
    }    /**
     * 计算缓存收益分数
     */
    private double calculateCacheBenefitScore(Node node, int processorId, CacheState cacheState) {
        if (node.weights == null || node.weights.length < 4) {
            return 0.5; // 默认中等分数，参考v1
        }
        
        // 基础缓存收益
        double l1Benefit = node.weights[0] * node.sensitivity * 0.6; // 降低基础收益
        double l2Benefit = node.weights[1] * node.sensitivity * 0.4; // L2效果调整
        double l3Benefit = node.weights[2] * node.sensitivity * 0.3; // L3效果调整
        
        // 缓存状态衰减
        double l1UtilizationPenalty = cacheState.l1Utilization * 0.7; // 线性衰减，更温和
        double l2UtilizationPenalty = cacheState.l2Utilization * 0.5; // 减少惩罚
        double l3UtilizationPenalty = cacheState.l3Utilization * 0.3; // 减少惩罚
        
        // 缓存收益
        double adjustedL1Benefit = l1Benefit * (1.0 - l1UtilizationPenalty);
        double adjustedL2Benefit = l2Benefit * (1.0 - l2UtilizationPenalty);
        double adjustedL3Benefit = l3Benefit * (1.0 - l3UtilizationPenalty);
        
        // 敏感度
        double sensitivityBonus = 0.0;
        if (node.sensitivity > 0.8) {
            sensitivityBonus = 0.15; // 降低到15%
        } else if (node.sensitivity > 0.6) {
            sensitivityBonus = 0.08; // 降低到8%
        }
        
        return (adjustedL1Benefit + adjustedL2Benefit + adjustedL3Benefit) * (1.0 + sensitivityBonus);
    }    /**
     * 计算缓存亲和性分数
     */
    private double calculateCacheAffinityScore(Node node, int processorId, CacheState cacheState) {
        double score = 0.0;
          // L2缓存共享亲和性 
        if (SystemParameters.Level2CoreNum > 0) {
            // 如果当前L2组有相似的任务，给予亲和性加分
            if (cacheState.avgTaskSensitivity > 0.0) {
                double sensitivitySimilarity = 1.0 - Math.abs(node.sensitivity - cacheState.avgTaskSensitivity);
                score += sensitivitySimilarity * 0.3; // 降低权重，更保守
            } else {
                score += 0.5; // 空缓存组给予中等分数
            }
        }
        
        // 处理器亲和性
        if (node.affinity != -1) {
            int distance = Math.abs(node.affinity - processorId);
            score += Math.max(0, 1.0 - distance * 0.1); // 使用v1的衰减率
        } else {
            score += 0.5; // 无亲和性偏好时给予中等分数
        }
        
        return Math.min(score, 1.0); // 限制最大值为1.0
    }
      /**
     * 计算缓存质量分数
     */
    private double calculateCacheQualityScore(Node node, CacheState cacheState) {
        double score = 0.0;
        
        // 缓存利用率质量 - 偏好中等利用率的缓存（避免过度拥挤和完全空闲）
        double optimalL1Utilization = 0.6; // 最优L1利用率
        double optimalL2Utilization = 0.5; // 最优L2利用率
        double optimalL3Utilization = 0.4; // 最优L3利用率
        
        double l1Quality = 1.0 - Math.abs(cacheState.l1Utilization - optimalL1Utilization);
        double l2Quality = 1.0 - Math.abs(cacheState.l2Utilization - optimalL2Utilization);
        double l3Quality = 1.0 - Math.abs(cacheState.l3Utilization - optimalL3Utilization);
        
        score += (l1Quality * 0.5 + l2Quality * 0.3 + l3Quality * 0.2);
        
        // 任务密度质量 - 避免过度集中
        if (cacheState.recentTaskCount < 3) {
            score += 0.3; // 任务较少时加分
        } else if (cacheState.recentTaskCount > 8) {
            score -= 0.2; // 任务过多时减分
        }
        
        return Math.max(0.0, Math.min(score, 1.0));
    }    /**
     * 计算缓存干扰惩罚分数
     */
    private double calculateCacheInterferenceScore(Node node, CacheState cacheState) {
        // 任务密度惩罚 - 降低惩罚强度
        double taskCountPenalty = Math.min(cacheState.recentTaskCount * 0.05, 0.3); // 降低惩罚
        
        // 敏感度差异惩罚
        double sensitivityDifference = Math.abs(node.sensitivity - cacheState.avgTaskSensitivity);
        double sensitivityPenalty = sensitivityDifference * 0.15; // 降低惩罚强度
        
        return taskCountPenalty + sensitivityPenalty;
    }
      /**
     * 计算负载均衡分数
     */
    private double calculateLoadBalanceScore(double currentLoad, long taskET) {
        // 简单的负载均衡评分：负载越低分数越高
        double loadRatio = currentLoad / (currentLoad + taskET);
        return 1.0 - loadRatio;
    }    /**
     * 更新处理器状态
     */
    private void updateProcessorState(int processorId, Node task) {
        CacheState cacheState = cacheStates.get(processorId);
        double currentLoad = processorLoads.get(processorId);
        int currentTaskCount = taskCount.get(processorId);
        
        // 更新负载
        processorLoads.put(processorId, currentLoad + task.expectedET);
        
        // 更新任务计数
        taskCount.put(processorId, currentTaskCount + 1);
        
        // 缓存利用率更新
        if (task.weights != null && task.weights.length >= 4) {
            double utilizationIncrement = task.sensitivity * 0.05; // 减少增量，更保守
            
            // 利用率增长
            cacheState.l1Utilization = Math.min(0.9, cacheState.l1Utilization + 
                                               utilizationIncrement * task.weights[0] * 0.8);
            cacheState.l2Utilization = Math.min(0.8, cacheState.l2Utilization + 
                                               utilizationIncrement * task.weights[1] * 0.6);
            cacheState.l3Utilization = Math.min(0.7, cacheState.l3Utilization + 
                                               utilizationIncrement * task.weights[2] * 0.4);
            
            // 添加缓存衰减机制 - 模拟缓存替换
            if (cacheState.recentTaskCount > 5) {
                double decayFactor = 0.95; // 轻微衰减
                cacheState.l1Utilization *= decayFactor;
                cacheState.l2Utilization *= decayFactor;
                cacheState.l3Utilization *= decayFactor;
            }
        }
        
        // 更新平均任务敏感度
        cacheState.avgTaskSensitivity = (cacheState.avgTaskSensitivity * cacheState.recentTaskCount + 
                                        task.sensitivity) / (cacheState.recentTaskCount + 1);
        
        // 更新任务计数
        cacheState.recentTaskCount++;
        
        // 更新缓存命中率统计
        updateCacheHitStats(task, processorId);
    }
      /**
     * 更新缓存命中率统计
     */
    private void updateCacheHitStats(Node task, int processorId) {
        if (!cacheHitStats.containsKey(processorId)) {
            cacheHitStats.put(processorId, new CacheHitStats());
        }
        
        CacheHitStats stats = cacheHitStats.get(processorId);
        
        // 更新总访问次数
        stats.totalL1Accesses += task.weights[0];
        stats.totalL2Accesses += task.weights[1];
        stats.totalL3Accesses += task.weights[2];
        
        // 更新命中次数（基于当前处理器状态的估计）
        double l1EstimatedHits = task.weights[0] * (1.0 - cacheStates.get(processorId).l1Utilization * 0.8);
        double l2EstimatedHits = task.weights[1] * (1.0 - cacheStates.get(processorId).l2Utilization * 0.6);
        double l3EstimatedHits = task.weights[2] * (1.0 - cacheStates.get(processorId).l3Utilization * 0.4);
        
        stats.totalL1Hits += l1EstimatedHits;
        stats.totalL2Hits += l2EstimatedHits;
        stats.totalL3Hits += l3EstimatedHits;
        
        stats.totalTasks++;
    }
      /**
     * 记录任务的实际缓存命中率
     */
    private void recordTaskCacheHitRatio(int processorId, Node task) {
        if (!cacheHitStats.containsKey(processorId)) {
            cacheHitStats.put(processorId, new CacheHitStats());
        }
        
        CacheHitStats stats = cacheHitStats.get(processorId);
        CacheState cacheState = cacheStates.get(processorId);
        
        if (task.weights == null || task.weights.length < 4) {
            return;
        }
        
        // 计算实际的缓存访问和命中
        double l1Access = task.weights[0] * task.sensitivity;
        double l2Access = task.weights[1] * task.sensitivity;
        double l3Access = task.weights[2] * task.sensitivity;
        
        // 基于当前缓存状态计算命中率
        double l1HitRatio = Math.max(0, task.weights[0] * (1.0 - cacheState.l1Utilization * 0.8));
        double l2HitRatio = Math.max(0, task.weights[1] * (1.0 - cacheState.l2Utilization * 0.6));
        double l3HitRatio = Math.max(0, task.weights[2] * (1.0 - cacheState.l3Utilization * 0.4));
        
        // 记录访问次数和命中次数
        stats.totalL1Accesses += l1Access;
        stats.totalL2Accesses += l2Access;
        stats.totalL3Accesses += l3Access;
        
        stats.totalL1Hits += l1Access * l1HitRatio;
        stats.totalL2Hits += l2Access * l2HitRatio;
        stats.totalL3Hits += l3Access * l3HitRatio;
          stats.totalTasks++;
        
        // 将命中率信息记录到任务中（用于后续分析）
        double totalAccesses = stats.totalL1Accesses + stats.totalL2Accesses + stats.totalL3Accesses;
        double totalHits = stats.totalL1Hits + stats.totalL2Hits + stats.totalL3Hits;
        task.actualCacheHitRatio = totalAccesses > 0 ? totalHits / totalAccesses : 0.0;
        
        // 记录各级缓存的命中率
        task.actualL1L2L3HitRatio[0] = l1HitRatio;
        task.actualL1L2L3HitRatio[1] = l2HitRatio;
        task.actualL1L2L3HitRatio[2] = l3HitRatio;
    }
      /**
     * 计算缓存感知的执行时间
     */
    public static long calculateExecutionTime(Node task, int processor) {
        if (!cacheStates.containsKey(processor)) {
            return task.expectedET; // 没有缓存信息，返回原始时间
        }
        
        CacheState cacheState = cacheStates.get(processor);
        
        if (task.weights == null || task.weights.length < 4) {
            return task.expectedET;
        }
        
        // 更保守的缓存收益计算
        double l1HitRatio = Math.max(0, task.weights[0] * (1.0 - cacheState.l1Utilization * 0.8));
        double l2HitRatio = Math.max(0, task.weights[1] * (1.0 - cacheState.l2Utilization * 0.6));
        double l3HitRatio = Math.max(0, task.weights[2] * (1.0 - cacheState.l3Utilization * 0.4));
        
        // 更保守的缓存命中时间节省
        double l1Savings = l1HitRatio * task.sensitivity * 0.25; // 降低L1节省到25%
        double l2Savings = l2HitRatio * task.sensitivity * 0.15; // 降低L2节省到15%
        double l3Savings = l3HitRatio * task.sensitivity * 0.08; // 降低L3节省到8%
        
        double totalSavings = l1Savings + l2Savings + l3Savings;
        
        // 更保守的额外收益
        if (task.sensitivity > 0.8) {
            totalSavings *= 1.15; // 降低到15%额外收益
        } else if (task.sensitivity > 0.6) {
            totalSavings *= 1.08; // 降低到8%额外收益
        }
        
        // 限制最大收益为50%，更保守
        totalSavings = Math.min(totalSavings, 0.5);
        
        long adjustedET = (long)(task.expectedET * (1.0 - totalSavings));
        return Math.max(adjustedET, task.expectedET / 2); // 最多减少50%执行时间
    }
      /**
     * 重置所有状态
     */
    public static void resetState() {
        processorLoads.clear();
        cacheStates.clear();
        taskCount.clear();
        cacheHitStats.clear();
    }
      /**
     * 获取处理器缓存状态报告
     */
    public static Map<Integer, String> getCacheStateReport() {
        Map<Integer, String> report = new HashMap<>();
        
        for (Map.Entry<Integer, CacheState> entry : cacheStates.entrySet()) {
            int procId = entry.getKey();
            CacheState state = entry.getValue();
            
            String stateStr = String.format(
                "Proc%d: L1=%.2f L2=%.2f L3=%.2f Tasks=%d AvgSens=%.3f Load=%.0f",
                procId, state.l1Utilization, state.l2Utilization, state.l3Utilization,
                state.recentTaskCount, state.avgTaskSensitivity, 
                processorLoads.getOrDefault(procId, 0.0)
            );
            
            report.put(procId, stateStr);
        }
        
        return report;
    }
      /**
     * 计算整体缓存命中率
     */
    public static double calculateOverallCacheHitRatio() {
        double totalAccesses = 0.0;
        double totalHits = 0.0;
        
        for (CacheHitStats stats : cacheHitStats.values()) {
            totalAccesses += stats.totalL1Accesses + stats.totalL2Accesses + stats.totalL3Accesses;
            totalHits += stats.totalL1Hits + stats.totalL2Hits + stats.totalL3Hits;
        }
        
        return totalAccesses > 0 ? totalHits / totalAccesses : 0.0;
    }
      /**
     * 计算基于任务列表的缓存命中率
     */
    public static double calculateCacheHitRatioFromTasks(List<Node> tasks) {
        double totalWeightedHitRatio = 0.0;
        double totalWeight = 0.0;
        
        for (Node task : tasks) {
            if (task.weights != null && task.weights.length >= 3) {
                // 使用任务的缓存权重作为权重
                double weight = task.weights[0] + task.weights[1] + task.weights[2];
                totalWeightedHitRatio += task.actualCacheHitRatio * weight;
                totalWeight += weight;
            }
        }
        
        return totalWeight > 0 ? totalWeightedHitRatio / totalWeight : 0.0;
    }
      /**
     * 获取详细的缓存命中率报告
     */
    public static Map<String, Double> getDetailedCacheHitReport() {
        Map<String, Double> report = new HashMap<>();
        
        double totalL1Accesses = 0.0, totalL1Hits = 0.0;
        double totalL2Accesses = 0.0, totalL2Hits = 0.0;
        double totalL3Accesses = 0.0, totalL3Hits = 0.0;
        
        for (CacheHitStats stats : cacheHitStats.values()) {
            totalL1Accesses += stats.totalL1Accesses;
            totalL1Hits += stats.totalL1Hits;
            totalL2Accesses += stats.totalL2Accesses;
            totalL2Hits += stats.totalL2Hits;
            totalL3Accesses += stats.totalL3Accesses;
            totalL3Hits += stats.totalL3Hits;
        }
        
        report.put("L1_Hit_Ratio", totalL1Accesses > 0 ? totalL1Hits / totalL1Accesses : 0.0);
        report.put("L2_Hit_Ratio", totalL2Accesses > 0 ? totalL2Hits / totalL2Accesses : 0.0);
        report.put("L3_Hit_Ratio", totalL3Accesses > 0 ? totalL3Hits / totalL3Accesses : 0.0);
        report.put("Overall_Hit_Ratio", calculateOverallCacheHitRatio());
        
        return report;
    }
}
