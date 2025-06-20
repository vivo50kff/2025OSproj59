package allocation;

import java.util.List;
import entity.Node;

/**
 * Worst Fit Decreasing (WFD) 任务分配算法
 * 
 * 经典负载均衡算法，采用最差适配策略，将任务分配给当前负载最轻的处理器。
 * 不考虑缓存局部性，适合作为对比基准。
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class WFD extends AllocationMethods {
    
    /** 处理器负载跟踪数组 */
    private static int[] processorLoads = new int[8];
    
    /**
     * WFD任务分配核心算法
     * 
     * 选择当前负载最轻的处理器执行新任务
     */
    @Override
    public int allocate(List<Node> readyNodes, List<Integer> availableProc) {
        // 参数有效性检查
        if (readyNodes.isEmpty() || availableProc.isEmpty()) {
            return -1;
        }
        
        // 获取要分配的任务（WFD每次处理一个任务）
        Node nodeToAllocate = readyNodes.get(0);
        long taskLoad = nodeToAllocate.expectedET; // 任务的预期执行时间作为负载
        
        int bestProcessor = -1;      // 最优处理器ID
        int minLoad = Integer.MAX_VALUE; // 当前最小负载
        
        // 遍历所有可用处理器，寻找负载最小的处理器
        for (int procId : availableProc) {
            if (procId < processorLoads.length && processorLoads[procId] < minLoad) {
                minLoad = processorLoads[procId];
                bestProcessor = procId;
            }
        }
        
        // 更新选中处理器的负载统计
        if (bestProcessor != -1 && bestProcessor < processorLoads.length) {
            processorLoads[bestProcessor] += taskLoad;
        }
        
        // 返回选中的处理器ID，如果没有找到合适的处理器则返回第一个可用处理器
        return bestProcessor != -1 ? bestProcessor : availableProc.get(0);
    }
    
    /**
     * 重置所有处理器的负载统计
     */
    public static void resetLoads() {
        for (int i = 0; i < processorLoads.length; i++) {
            processorLoads[i] = 0;
        }
    }
    
    /**
     * 获取当前所有处理器的负载情况
     */
    public static int[] getProcessorLoads() {
        return processorLoads.clone();
    }
}
