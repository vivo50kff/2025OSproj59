package visualizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import analyzer.PerformanceAnalyzer.ExperimentResult;

/**
 * 实验结果可视化器
 * 
 * 该类负责将算法性能对比的实验结果以直观的方式展示出来。
 * 提供ASCII图表生成、CSV数据导出和性能总结分析功能。
 * 
 * 主要功能：
 * - 生成ASCII条形图进行性能对比
 * - 导出CSV格式的实验数据
 * - 计算和显示性能改进摘要
 * - 支持多算法对比分析
 * - 生成趋势分析图表
 * 
 * 支持的可视化类型：
 * - Makespan对比图表
 * - 缓存命中率对比图表
 * - 负载均衡度对比图表
 * - 性能改进总结
 * 
 * @author Cache-Aware Task Scheduling System
 */
public class ResultVisualizer {
    
    /** 数字格式化器，用于结果显示 */
    private DecimalFormat df = new DecimalFormat("#.##");      /**
     * 生成ASCII性能对比图表
     * 
     * 该方法创建直观的文本图表来比较不同算法的性能表现。
     * 包括makespan、缓存命中率、负载均衡度、执行时间等关键指标的可视化对比。
     * 
     * @param algorithmResults 包含各算法实验结果的映射表
     */
    public void generatePerformanceChart(Map<String, List<ExperimentResult>> algorithmResults) {
        System.out.println("\n=== 性能对比图表 ===");
        
        String[] algorithms = algorithmResults.keySet().toArray(new String[0]);
        if (algorithms.length < 2) return;
        
        // 计算各算法的平均性能指标
        double[] avgMakespan = new double[algorithms.length];
        double[] avgEnergy = new double[algorithms.length];
        double[] avgCacheHit = new double[algorithms.length];
        double[] avgLoadBalance = new double[algorithms.length];
        double[] avgResponseTime = new double[algorithms.length];
        double[] avgTotalExecTime = new double[algorithms.length];
        double[] avgTaskExecTime = new double[algorithms.length];
        
        for (int i = 0; i < algorithms.length; i++) {
            List<ExperimentResult> results = algorithmResults.get(algorithms[i]);
            avgMakespan[i] = results.stream().mapToDouble(r -> r.makespan).average().orElse(0);
            avgEnergy[i] = results.stream().mapToDouble(r -> r.energyConsumption).average().orElse(0);
            avgCacheHit[i] = results.stream().mapToDouble(r -> r.cacheHitRatio).average().orElse(0);
            avgLoadBalance[i] = results.stream().mapToDouble(r -> r.loadBalance).average().orElse(0);
            avgResponseTime[i] = results.stream().mapToDouble(r -> r.averageResponseTime).average().orElse(0);
            avgTotalExecTime[i] = results.stream().mapToDouble(r -> r.totalTaskExecutionTime).average().orElse(0);
            avgTaskExecTime[i] = results.stream().mapToDouble(r -> r.averageTaskExecutionTime).average().orElse(0);
        }
        
        // 生成完成时间对比图
        System.out.println("\n【Makespan对比】(时间单位)");
        double maxMakespan = Math.max(avgMakespan[0], avgMakespan[1]);
        for (int i = 0; i < algorithms.length; i++) {
            String bar = generateBar(avgMakespan[i], maxMakespan, 40);
            System.out.printf("%-15s: %s %.2f\n", algorithms[i], bar, avgMakespan[i]);
        }
        
        // 生成完整任务执行时间对比图
        System.out.println("\n【完整任务执行时间对比】(时间单位)");
        double maxTotalExec = Math.max(avgTotalExecTime[0], avgTotalExecTime[1]);
        for (int i = 0; i < algorithms.length; i++) {
            String bar = generateBar(avgTotalExecTime[i], maxTotalExec, 40);
            System.out.printf("%-15s: %s %.2f\n", algorithms[i], bar, avgTotalExecTime[i]);
        }
        
        // 显示执行时间改进
        if (avgTotalExecTime[0] != 0 && avgTotalExecTime[1] != 0) {
            double improvement = ((avgTotalExecTime[0] - avgTotalExecTime[1]) / avgTotalExecTime[0]) * 100;
            System.out.printf("📈 执行时间改进: %.2f%%\n", improvement);
        }
        
        // 生成平均任务执行时间对比图
        System.out.println("\n【平均任务执行时间对比】(时间单位)");
        double maxTaskExec = Math.max(avgTaskExecTime[0], avgTaskExecTime[1]);
        for (int i = 0; i < algorithms.length; i++) {
            String bar = generateBar(avgTaskExecTime[i], maxTaskExec, 40);
            System.out.printf("%-15s: %s %.2f\n", algorithms[i], bar, avgTaskExecTime[i]);
        }
        
        // 生成缓存命中率对比图
        System.out.println("\n【缓存命中率对比】(百分比)");
        double maxCacheHit = Math.max(avgCacheHit[0], avgCacheHit[1]);
        for (int i = 0; i < algorithms.length; i++) {
            String bar = generateBar(avgCacheHit[i], maxCacheHit, 40);
            System.out.printf("%-15s: %s %.1f%%\n", algorithms[i], bar, avgCacheHit[i] * 100);
        }
        
        // 生成负载均衡对比图
        System.out.println("\n【负载均衡度对比】(0-1,越高越好)");
        double maxLoadBalance = Math.max(avgLoadBalance[0], avgLoadBalance[1]);
        for (int i = 0; i < algorithms.length; i++) {
            String bar = generateBar(avgLoadBalance[i], maxLoadBalance, 40);
            System.out.printf("%-15s: %s %.3f\n", algorithms[i], bar, avgLoadBalance[i]);
        }
        
        generateImprovementSummary(algorithms, avgMakespan, avgCacheHit, avgLoadBalance, 
                                 avgResponseTime, avgTotalExecTime, avgTaskExecTime);
    }
    
    private String generateBar(double value, double max, int maxLength) {
        if (max == 0) return "";
        int barLength = (int) ((value / max) * maxLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) bar.append("█");
        for (int i = barLength; i < maxLength; i++) bar.append("░");
        return bar.toString();
    }    private void generateImprovementSummary(String[] algorithms, double[] makespan, 
            double[] cacheHit, double[] loadBalance, double[] responseTime,
            double[] totalExecTime, double[] avgExecTime) {
        System.out.println("\n=== 性能改进总结 ===");
        
        if (algorithms.length >= 2) {
            String algo1 = algorithms[0];
            String algo2 = algorithms[1];
            
            System.out.println("📊 " + algo1 + " vs " + algo2 + " 综合对比:");
            
            // 执行时间改进
            if (totalExecTime[0] != 0) {
                double improvement = ((totalExecTime[0] - totalExecTime[1]) / totalExecTime[0]) * 100;
                System.out.printf("  ⏱️  完整任务执行时间: %s%.2f%%\n", 
                    improvement > 0 ? "改进 " : "提升", Math.abs(improvement));
            }
            
            if (avgExecTime[0] != 0) {
                double improvement = ((avgExecTime[0] - avgExecTime[1]) / avgExecTime[0]) * 100;
                System.out.printf("  ⏰ 平均任务执行时间: %s%.2f%%\n", 
                    improvement > 0 ? "改进 " : "提升", Math.abs(improvement));
            }
            
            // Makespan改进
            if (makespan[0] != 0) {
                double improvement = ((makespan[0] - makespan[1]) / makespan[0]) * 100;
                System.out.printf("  📈 Makespan: %s%.2f%%\n", 
                    improvement > 0 ? "改进 " : "增加 ", Math.abs(improvement));
            }
            
            // 缓存命中率改进
            double cacheImprovement = (cacheHit[1] - cacheHit[0]) * 100;
            System.out.printf("  🗄️  缓存命中率: %s%.2f个百分点\n", 
                cacheImprovement > 0 ? "提升 " : "降低 ", Math.abs(cacheImprovement));
        }
    }
    
    public void generateTrendChart(Map<String, List<ExperimentResult>> algorithmResults) {
        System.out.println("\n=== 性能趋势分析 ===");
    }    public void exportToCSV(Map<String, List<ExperimentResult>> algorithmResults, String filename) {
        try {
            // 创建目录（如果不存在）
            File file = new File(filename);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 写入CSV文件，包含执行时间数据
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write("Algorithm,TestCase,Makespan,CacheHit,TotalExecutionTime,AvgExecutionTime,ResponseTime,CpuUtilization,LoadBalance\n");
                for (String algorithm : algorithmResults.keySet()) {
                    for (ExperimentResult result : algorithmResults.get(algorithm)) {
                        writer.write(String.format("%s,%d,%.2f,%.3f,%.2f,%.2f,%.2f,%.3f,%.3f\n",
                            algorithm, 
                            result.testCaseId, 
                            result.makespan, 
                            result.cacheHitRatio,
                            result.totalTaskExecutionTime,
                            result.averageTaskExecutionTime,
                            result.averageResponseTime,
                            result.cpuUtilization,
                            result.loadBalance));
                    }
                }
                System.out.println("✅ CSV数据已导出到: " + filename);
            }
        } catch (IOException e) {
            System.err.println("❌ CSV导出失败: " + e.getMessage());
        }
    }
      public void generateComparisonMatrix(Map<String, List<ExperimentResult>> algorithmResults) {
        // 移除对比矩阵输出，由主程序控制输出格式
    }
}
