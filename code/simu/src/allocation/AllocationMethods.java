package allocation;

import java.util.List;
import entity.Node;

/**
 * 抽象任务分配算法基类
 * 
 * 定义了所有任务分配算法必须实现的基本接口。
 * 该类作为策略模式的抽象策略，为不同的任务分配算法提供统一的接口。
 * 
 * 主要功能：
 * - 提供统一的任务分配接口
 * - 支持多种分配策略（WFD、Cache-Aware等）
 * - 便于算法性能对比和扩展
 * 
 * @author Cache-Aware Task Scheduling System
 */
public abstract class AllocationMethods {
    
    /**
     * 任务分配核心方法
     * 
     * 该方法是所有分配算法的核心接口，负责将就绪任务分配到合适的处理器上。
     * 
     * @param readyNodes 已就绪的任务列表，这些任务满足依赖关系，可以被立即执行
     * @param availableProc 当前可用的处理器ID列表
     * @return 分配的处理器ID，如果分配失败返回-1
     */
    public abstract int allocate(List<Node> readyNodes, List<Integer> availableProc);
}
