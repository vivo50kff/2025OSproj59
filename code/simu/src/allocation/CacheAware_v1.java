package allocation;

import java.util.List;
import entity.Node;

/**
 * Cache-Aware v1算法实现
 * 核心思想：考虑缓存亲和性和资源变化性来优化任务分配
 */
public class CacheAware_v1 extends AllocationMethods {
    
    @Override
    public int allocate(List<Node> readyNodes, List<Integer> availableProc) {
        if (readyNodes.isEmpty() || availableProc.isEmpty()) {
            return -1;
        }
        
        // Cache-Aware Resource Variable算法核心逻辑
        Node nodeToAllocate = readyNodes.get(0); // 假设已经按优先级排序
        
        int bestProcessor = -1;
        double bestScore = Double.MIN_VALUE;
        
        // 为每个可用处理器计算适合度分数
        for (int procId : availableProc) {
            double score = calculateCacheAwareScore(nodeToAllocate, procId);
            
            if (score > bestScore) {
                bestScore = score;
                bestProcessor = procId;
            }
        }
        
        return bestProcessor != -1 ? bestProcessor : availableProc.get(0);
    }
    
    /**
     * 计算Cache-Aware适合度分数
     * 考虑因素：
     * 1. 缓存敏感度
     * 2. 处理器亲和性
     * 3. 负载均衡
     * 4. 缓存层次结构
     */
    private double calculateCacheAwareScore(Node node, int processorId) {
        double score = 0.0;
        
        // 1. 缓存敏感度权重 (越敏感的任务越需要考虑缓存)
        double cacheSensitivityWeight = node.sensitivity * 0.4;
        
        // 2. 处理器亲和性 (同一DAG的任务倾向于分配到相近的处理器)
        double affinityWeight = calculateAffinityScore(node, processorId) * 0.3;
        
        // 3. 负载均衡权重 (避免处理器过载)
        double loadBalanceWeight = calculateLoadBalanceScore(processorId) * 0.2;
        
        // 4. 缓存层次优势 (L2缓存共享考虑)
        double cacheHierarchyWeight = calculateCacheHierarchyScore(node, processorId) * 0.1;
        
        score = cacheSensitivityWeight + affinityWeight + loadBalanceWeight + cacheHierarchyWeight;
        
        return score;
    }
    
    /**
     * 计算处理器亲和性分数
     * 同一DAG的任务分配到相近处理器可能有缓存共享优势
     */
    private double calculateAffinityScore(Node node, int processorId) {
        // 如果节点有亲和性偏好
        if (node.affinity != -1) {
            int distance = Math.abs(node.affinity - processorId);
            return Math.max(0, 1.0 - distance * 0.1); // 距离越近分数越高
        }
        
        // 默认返回中等分数
        return 0.5;
    }
    
    /**
     * 计算负载均衡分数
     * 简化实现：假设处理器负载相对均衡
     */
    private double calculateLoadBalanceScore(int processorId) {
        // 简化实现：返回基于处理器ID的分布分数
        return 1.0 - (processorId % 4) * 0.1; // 轻微偏好低ID处理器
    }
    
    /**
     * 计算缓存层次结构分数
     * 考虑L2缓存共享等因素
     */
    private double calculateCacheHierarchyScore(Node node, int processorId) {
        // L2缓存通常每2个核心共享
        int l2ClusterId = processorId / 2;
        
        // 如果节点有缓存权重配置
        if (node.weights != null && node.weights.length >= 4) {
            // L1权重 + L2权重(共享优势)
            return node.weights[0] * 0.6 + node.weights[1] * 0.4;
        }
        
        return 0.5; // 默认中等分数
    }
}