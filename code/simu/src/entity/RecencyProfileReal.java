package entity;

import java.util.List;
import org.apache.commons.math3.util.Pair;

/**
 * 缓存性能配置文件
 * 
 * 该类模拟真实的缓存访问行为和性能特征，为Cache-Aware算法提供
 * 执行时间计算的基础数据。通过分析缓存历史和访问模式，
 * 计算考虑缓存效果的任务执行时间。
 * 
 * 主要功能：
 * - 模拟多级缓存的访问延迟
 * - 基于访问历史计算缓存命中率
 * - 提供执行时间的动态调整
 * - 支持故障容错的时间计算
 * 
 * 缓存层次模型：
 * - L1缓存：最快，命中可减少20%执行时间
 * - L2缓存：较快，命中可减少10%执行时间
 * - L3缓存：一般，命中可减少5%执行时间
 * - 主内存：最慢，无性能提升
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class RecencyProfileReal {
    
    // 基础执行时间
    private long baseExecutionTime = 100;
    
    public RecencyProfileReal() {
        // 默认构造函数
    }
    
    public RecencyProfileReal(long baseET) {
        this.baseExecutionTime = baseET;
    }
    
    /**
     * 计算考虑缓存效果的执行时间
     * @param level 缓存级别
     * @param history1 L1缓存历史
     * @param history2 L2缓存历史  
     * @param history3 L3缓存历史
     * @param node 当前节点
     * @param processor 处理器ID
     * @param cacheAware 是否考虑缓存
     * @param param1 参数1
     * @param param2 参数2
     * @param hasFaults 是否有故障
     * @return Pair<Pair<执行时间, 缓存效果>, 缓存级别>
     */
    public Pair<Pair<Long, Double>, Integer> computeET(int level, 
            List<List<Node>> history1, List<List<Node>> history2, List<Node> history3,
            Node node, int processor, boolean cacheAware, 
            double param1, double param2, boolean hasFaults) {
        
        long executionTime = baseExecutionTime;
        int cacheLevel = 1; // 1=L1命中, 2=L2命中, 3=L3命中, 4=内存访问
        
        if (cacheAware) {
            // 简化的缓存模拟
            if (hasRecentAccess(history1.get(processor), node)) {
                executionTime = (long)(baseExecutionTime * 0.8); // L1命中，快20%
                cacheLevel = 1;
            } else if (hasRecentAccess(history2.get(processor / 2), node)) {
                executionTime = (long)(baseExecutionTime * 0.9); // L2命中，快10%
                cacheLevel = 2;
            } else if (hasRecentAccess(history3, node)) {
                executionTime = (long)(baseExecutionTime * 0.95); // L3命中，快5%
                cacheLevel = 3;
            } else {
                executionTime = (long)(baseExecutionTime * 1.2); // 内存访问，慢20%
                cacheLevel = 4;
            }
        }
        
        // 考虑故障影响
        if (hasFaults) {
            executionTime = (long)(executionTime * 1.5);
        }
        
        Double cacheEffect = 1.0;
        return new Pair<>(new Pair<>(executionTime, cacheEffect), cacheLevel);
    }
    
    private boolean hasRecentAccess(List<Node> history, Node node) {
        // 简化的缓存命中判断：检查最近几个访问
        int recentWindow = 5;
        int count = 0;
        for (int i = history.size() - 1; i >= 0 && count < recentWindow; i--, count++) {
            Node historyNode = history.get(i);
            if (historyNode.getDagID() == node.getDagID()) {
                return true; // 同一个DAG的节点可能共享数据
            }
        }
        return false;
    }
}
