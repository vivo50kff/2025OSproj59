#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cache-Aware vs WFD Algorithm Performance Comparison Visualization Program v2.0
Redesigned unified visualization analysis tool

Features:
- First chart: Makespan analysis (average comparison, trend analysis, box plot, density plot)
- Second chart: Cache hit rate analysis (average comparison, trend analysis, box plot, density plot)
- Third chart: Execution time comparison and win rate analysis

Author: Cache-Aware Task Scheduling System
Date: June 19, 2025
"""

import os
import sys
import csv
import warnings
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

# Ignore warning messages
warnings.filterwarnings('ignore')

# Try to import optional dependency libraries
HAS_PANDAS = False
HAS_SEABORN = False

try:
    import pandas as pd
    HAS_PANDAS = True
    print("✅ Detected pandas library, enabling advanced data processing features")
except ImportError:
    print("⚠️  Pandas library not detected, will use numpy for data processing")

try:
    import seaborn as sns
    HAS_SEABORN = True
    print("✅ Detected seaborn library, enabling advanced visualization styles")
except ImportError:
    print("⚠️  Seaborn library not detected, using matplotlib default styles")

# Set Chinese font support
try:
    plt.rcParams['font.sans-serif'] = ['SimHei', 'Arial Unicode MS', 'DejaVu Sans']
    plt.rcParams['axes.unicode_minus'] = False
    print("✅ Chinese font settings successful")
except:
    print("⚠️  Chinese font settings failed, will display in English")

# Set chart styles
if HAS_SEABORN:
    sns.set_style("whitegrid")
    sns.set_palette("husl")
else:
    plt.style.use('default')

class AlgorithmVisualizer:
    """Redesigned algorithm performance visualizer"""
    
    def __init__(self, csv_file='algorithm_comparison_results.csv'):
        """Initialize visualizer"""
        self.result_dir = self._find_result_directory()
        self.csv_file = self._find_csv_file(csv_file)
        self.data = None
        self.algorithms = []
        
        print(f"📊 Algorithm Performance Visualizer Initialized")
        print(f"📁 Result Directory: {self.result_dir}")
        print(f"📄 Data File: {self.csv_file}")
    
    def _find_result_directory(self):
        """Find result directory"""
        possible_dirs = [
            'result',
            '../result',
            '../../result',
            os.path.join(os.getcwd(), 'result'),
            os.path.join(os.path.dirname(__file__), '..', 'result')
        ]
        
        for dir_path in possible_dirs:
            abs_path = os.path.abspath(dir_path)
            if os.path.exists(abs_path) and os.path.isdir(abs_path):
                print(f"✅ Found result directory: {abs_path}")
                return abs_path
        
        # If not found, create one
        result_dir = os.path.join(os.getcwd(), 'result')
        os.makedirs(result_dir, exist_ok=True)
        print(f"📁 Created result directory: {result_dir}")
        return result_dir
    
    def _find_csv_file(self, csv_file):
        """Find CSV file"""
        possible_paths = [
            csv_file,
            os.path.join(self.result_dir, csv_file),
            os.path.join(self.result_dir, 'algorithm_comparison_results.csv'),
            'algorithm_comparison_results.csv',
            '../result/algorithm_comparison_results.csv'
        ]
        
        for path in possible_paths:
            abs_path = os.path.abspath(path)
            if os.path.exists(abs_path) and os.path.isfile(abs_path):
                print(f"✅ Found data file: {abs_path}")
                return abs_path
        
        print(f"❌ CSV file not found, will use default path: {csv_file}")
        return csv_file
    
    def load_data(self):
        """Load experiment data"""
        try:
            if HAS_PANDAS:
                self.data = pd.read_csv(self.csv_file)
                self.algorithms = sorted(self.data['Algorithm'].unique())
                print(f"✅ Successfully loaded data using pandas: {len(self.data)} records")
            else:
                # Use native Python to load CSV
                self.data = []
                with open(self.csv_file, 'r', encoding='utf-8') as f:
                    reader = csv.DictReader(f)
                    for row in reader:
                        self.data.append(row)
                
                # Extract algorithm names
                algorithms_set = set()
                for row in self.data:
                    algorithms_set.add(row['Algorithm'])
                self.algorithms = sorted(list(algorithms_set))
                print(f"✅ Successfully loaded data using csv module: {len(self.data)} records")
            
            print(f"📈 Detected algorithms: {', '.join(self.algorithms)}")
            return True
            
        except Exception as e:
            print(f"❌ Data loading failed: {str(e)}")
            return False
    
    def _get_algorithm_data(self, algorithm_name, metric):
        """Get specific algorithm metric data"""
        if HAS_PANDAS:
            algo_data = self.data[self.data['Algorithm'] == algorithm_name]
            return algo_data[metric].astype(float).values
        else:
            values = []
            for row in self.data:
                if row['Algorithm'] == algorithm_name:
                    values.append(float(row[metric]))
            return np.array(values)
    
    def _get_output_path(self, filename):
        """Get output file path"""
        return os.path.join(self.result_dir, filename)
    
    def create_makespan_analysis(self):
        """Create Makespan analysis chart (First chart)"""
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        fig.suptitle('Cache-Aware vs WFD Makespan Comprehensive Analysis', fontsize=16, fontweight='bold')
        
        # Get data
        makespan_data = {}
        for algo in self.algorithms:
            makespan_data[algo] = self._get_algorithm_data(algo, 'Makespan')
        
        # 1. Average comparison (top left)
        ax1 = axes[0, 0]
        means = [np.mean(makespan_data[algo]) for algo in self.algorithms]
        std_devs = [np.std(makespan_data[algo]) for algo in self.algorithms]
        
        colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728'][:len(self.algorithms)]
        bars = ax1.bar(self.algorithms, means, yerr=std_devs, capsize=5, 
                      color=colors, alpha=0.7, edgecolor='black', linewidth=1)
        
        ax1.set_title('Average Makespan Comparison', fontweight='bold')
        ax1.set_ylabel('Makespan (Time Units)')
        ax1.grid(True, alpha=0.3)
        
        # Add value labels
        for i, (bar, mean, std) in enumerate(zip(bars, means, std_devs)):
            height = bar.get_height()
            ax1.text(bar.get_x() + bar.get_width()/2., height + std,
                    f'{mean:.0f}\n±{std:.0f}',
                    ha='center', va='bottom', fontweight='bold')
        
        # Calculate improvement rate
        if len(self.algorithms) >= 2:
            improvement = ((means[1] - means[0]) / means[1]) * 100
            ax1.text(0.5, 0.95, f'Cache-Aware Improvement: {improvement:.1f}%', 
                    transform=ax1.transAxes, ha='center', va='top',
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="yellow", alpha=0.7),
                    fontweight='bold')
        
        # 2. Trend analysis (top right)
        ax2 = axes[0, 1]
        for i, algo in enumerate(self.algorithms):
            test_cases = range(1, len(makespan_data[algo]) + 1)
            ax2.plot(test_cases, makespan_data[algo], 
                    marker='o', label=algo, color=colors[i], 
                    linewidth=2, markersize=4)
        
        ax2.set_title('Makespan Trend Analysis', fontweight='bold')
        ax2.set_xlabel('Test Case')
        ax2.set_ylabel('Makespan (Time Units)')
        ax2.legend()
        ax2.grid(True, alpha=0.3)
        
        # 3. Box plot comparison (bottom left)
        ax3 = axes[1, 0]
        data_for_box = [makespan_data[algo] for algo in self.algorithms]
        box_plot = ax3.boxplot(data_for_box, labels=self.algorithms, 
                              patch_artist=True, notch=True)
        
        # Beautify box plot
        for patch, color in zip(box_plot['boxes'], colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        
        ax3.set_title('Makespan Distribution Comparison (Box Plot)', fontweight='bold')
        ax3.set_ylabel('Makespan (Time Units)')
        ax3.grid(True, alpha=0.3)
        
        # 4. Density plot (bottom right)
        ax4 = axes[1, 1]
        for i, algo in enumerate(self.algorithms):
            data = makespan_data[algo]
            # Calculate density
            hist, bins = np.histogram(data, bins=20, density=True)
            bin_centers = (bins[:-1] + bins[1:]) / 2
            ax4.plot(bin_centers, hist, label=algo, color=colors[i], 
                    linewidth=2, marker='s', markersize=3)
            
            # Fill area
            ax4.fill_between(bin_centers, hist, alpha=0.3, color=colors[i])
        
        ax4.set_title('Makespan Distribution Density', fontweight='bold')
        ax4.set_xlabel('Makespan (Time Units)')
        ax4.set_ylabel('Density')
        ax4.legend()
        ax4.grid(True, alpha=0.3)
        
        plt.tight_layout()
        output_path = self._get_output_path('makespan_analysis.png')
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"✅ Makespan analysis chart saved: {output_path}")
        plt.close()
    
    def create_cache_hit_analysis(self):
        """Create cache hit rate analysis chart (Second chart)"""
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        fig.suptitle('Cache-Aware vs WFD Cache Hit Rate Comprehensive Analysis', fontsize=16, fontweight='bold')
        
        # Get data
        cache_data = {}
        for algo in self.algorithms:
            cache_data[algo] = self._get_algorithm_data(algo, 'CacheHit') * 100  # Convert to percentage
        
        # 1. Average comparison (top left)
        ax1 = axes[0, 0]
        means = [np.mean(cache_data[algo]) for algo in self.algorithms]
        std_devs = [np.std(cache_data[algo]) for algo in self.algorithms]
        
        colors = ['#2E8B57', '#FF6347', '#4169E1', '#FFD700'][:len(self.algorithms)]
        bars = ax1.bar(self.algorithms, means, yerr=std_devs, capsize=5, 
                      color=colors, alpha=0.7, edgecolor='black', linewidth=1)
        
        ax1.set_title('Average Cache Hit Rate Comparison', fontweight='bold')
        ax1.set_ylabel('Cache Hit Rate (%)')
        ax1.set_ylim(0, 100)
        ax1.grid(True, alpha=0.3)
        
        # Add value labels
        for i, (bar, mean, std) in enumerate(zip(bars, means, std_devs)):
            height = bar.get_height()
            ax1.text(bar.get_x() + bar.get_width()/2., height + std,
                    f'{mean:.1f}%\n±{std:.1f}%',
                    ha='center', va='bottom', fontweight='bold')
        
        # Calculate improvement rate
        if len(self.algorithms) >= 2:
            cache_improvement = means[0] - means[1]  # Cache-Aware is usually first
            ax1.text(0.5, 0.95, f'Cache-Aware Improvement: +{cache_improvement:.1f}%', 
                    transform=ax1.transAxes, ha='center', va='top',
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="lightgreen", alpha=0.7),
                    fontweight='bold')
        
        # 2. Trend analysis (top right)
        ax2 = axes[0, 1]
        for i, algo in enumerate(self.algorithms):
            test_cases = range(1, len(cache_data[algo]) + 1)
            ax2.plot(test_cases, cache_data[algo], 
                    marker='o', label=algo, color=colors[i], 
                    linewidth=2, markersize=4)
        
        ax2.set_title('Cache Hit Rate Trend Analysis', fontweight='bold')
        ax2.set_xlabel('Test Case')
        ax2.set_ylabel('Cache Hit Rate (%)')
        ax2.set_ylim(0, 100)
        ax2.legend()
        ax2.grid(True, alpha=0.3)
        
        # 3. Box plot comparison (bottom left)
        ax3 = axes[1, 0]
        data_for_box = [cache_data[algo] for algo in self.algorithms]
        box_plot = ax3.boxplot(data_for_box, labels=self.algorithms, 
                              patch_artist=True, notch=True)
        
        # Beautify box plot
        for patch, color in zip(box_plot['boxes'], colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        
        ax3.set_title('Cache Hit Rate Distribution Comparison (Box Plot)', fontweight='bold')
        ax3.set_ylabel('Cache Hit Rate (%)')
        ax3.set_ylim(0, 100)
        ax3.grid(True, alpha=0.3)
        
        # 4. Density plot (bottom right)
        ax4 = axes[1, 1]
        for i, algo in enumerate(self.algorithms):
            data = cache_data[algo]
            # Calculate density
            hist, bins = np.histogram(data, bins=20, density=True)
            bin_centers = (bins[:-1] + bins[1:]) / 2
            ax4.plot(bin_centers, hist, label=algo, color=colors[i], 
                    linewidth=2, marker='s', markersize=3)
            
            # Fill area
            ax4.fill_between(bin_centers, hist, alpha=0.3, color=colors[i])
        
        ax4.set_title('Cache Hit Rate Distribution Density', fontweight='bold')
        ax4.set_xlabel('Cache Hit Rate (%)')
        ax4.set_ylabel('Density')
        ax4.legend()
        ax4.grid(True, alpha=0.3)
        
        plt.tight_layout()
        output_path = self._get_output_path('cache_hit_analysis.png')
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"✅ Cache hit rate analysis chart saved: {output_path}")
        plt.close()
    
    def create_execution_time_and_win_rate_analysis(self):
        """Create execution time comparison and win rate analysis chart (Third chart)"""
        fig, axes = plt.subplots(1, 2, figsize=(16, 6))
        fig.suptitle('Task Execution Time Comparison and Algorithm Win Rate Analysis', fontsize=16, fontweight='bold')
        
        # 1. Total task execution time comparison (left chart)
        ax1 = axes[0]
        
        exec_time_data = {}
        for algo in self.algorithms:
            exec_time_data[algo] = self._get_algorithm_data(algo, 'TotalExecutionTime')
        
        means = [np.mean(exec_time_data[algo]) for algo in self.algorithms]
        std_devs = [np.std(exec_time_data[algo]) for algo in self.algorithms]
        
        colors = ['#9370DB', '#FF8C00', '#20B2AA', '#DC143C'][:len(self.algorithms)]
        bars = ax1.bar(self.algorithms, means, yerr=std_devs, capsize=5, 
                      color=colors, alpha=0.7, edgecolor='black', linewidth=1)
        
        ax1.set_title('Total Task Execution Time Comparison', fontweight='bold')
        ax1.set_ylabel('Total Execution Time (Time Units)')
        ax1.grid(True, alpha=0.3)
        
        # Add value labels
        for i, (bar, mean, std) in enumerate(zip(bars, means, std_devs)):
            height = bar.get_height()
            ax1.text(bar.get_x() + bar.get_width()/2., height + std,
                    f'{mean:.0f}\n±{std:.0f}',
                    ha='center', va='bottom', fontweight='bold')
        
        # Calculate improvement rate
        if len(self.algorithms) >= 2:
            time_improvement = ((means[1] - means[0]) / means[1]) * 100
            ax1.text(0.5, 0.95, f'Execution Time Improvement: {time_improvement:.1f}%', 
                    transform=ax1.transAxes, ha='center', va='top',
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="lightblue", alpha=0.7),
                    fontweight='bold')
        
        # 2. Algorithm win rate analysis (right chart)
        ax2 = axes[1]
        
        if len(self.algorithms) >= 2:
            # Calculate win rates for each metric
            metrics = ['Makespan', 'CacheHit', 'TotalExecutionTime']
            metric_labels = ['Makespan Win Rate', 'Cache Hit Rate Win Rate', 'Execution Time Win Rate']
            
            win_rates = []
            for metric in metrics:
                algo1_data = self._get_algorithm_data(self.algorithms[0], metric)
                algo2_data = self._get_algorithm_data(self.algorithms[1], metric)
                
                wins = 0
                total = min(len(algo1_data), len(algo2_data))
                
                for i in range(total):
                    if metric == 'CacheHit':
                        # Higher cache hit rate is better
                        if algo1_data[i] > algo2_data[i]:
                            wins += 1
                    else:
                        # Lower makespan and execution time is better
                        if algo1_data[i] < algo2_data[i]:
                            wins += 1
                
                win_rate = (wins / total) * 100
                win_rates.append(win_rate)
            
            # Draw win rate bar chart
            y_pos = np.arange(len(metric_labels))
            bars = ax2.barh(y_pos, win_rates, color=['#FF6B6B', '#4ECDC4', '#45B7D1'], 
                           alpha=0.8, edgecolor='black', linewidth=1)
            
            ax2.set_yticks(y_pos)
            ax2.set_yticklabels(metric_labels)
            ax2.set_xlabel('Win Rate (%)')
            ax2.set_title(f'{self.algorithms[0]} vs {self.algorithms[1]} Win Rate Comparison', fontweight='bold')
            ax2.set_xlim(0, 100)
            ax2.grid(True, alpha=0.3, axis='x')
            
            # Add value labels
            for i, (bar, rate) in enumerate(zip(bars, win_rates)):
                width = bar.get_width()
                ax2.text(width + 1, bar.get_y() + bar.get_height()/2,
                        f'{rate:.1f}%',
                        ha='left', va='center', fontweight='bold')
            
            # Add 50% reference line
            ax2.axvline(x=50, color='red', linestyle='--', alpha=0.7, linewidth=2)
            ax2.text(50, len(metric_labels)-0.5, 'Balance Line', ha='center', va='bottom',
                    bbox=dict(boxstyle="round,pad=0.2", facecolor="white", alpha=0.8))
            
            # Calculate overall win rate
            overall_win_rate = np.mean(win_rates)
            ax2.text(0.02, 0.98, f'Overall Win Rate: {overall_win_rate:.1f}%', 
                    transform=ax2.transAxes, ha='left', va='top',
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="yellow", alpha=0.7),
                    fontweight='bold')
        
        plt.tight_layout()
        output_path = self._get_output_path('execution_time_and_win_rate_analysis.png')
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"✅ Execution time and win rate analysis chart saved: {output_path}")
        plt.close()
    
    def generate_comprehensive_report(self):
        """Generate comprehensive performance analysis report"""
        print("\n" + "="*80)
        print("📊 Cache-Aware vs WFD Algorithm Comprehensive Performance Analysis Report")
        print("="*80)
        
        # Basic statistics
        total_cases = len(self.data) if HAS_PANDAS else len(self.data)
        cases_per_algo = total_cases // len(self.algorithms)
        
        print(f"📈 Experiment Overview:")
        print(f"   - Total Test Cases: {total_cases}")
        print(f"   - Number of Algorithms: {len(self.algorithms)}")
        print(f"   - Tests per Algorithm: {cases_per_algo} cases")
        print(f"   - Detected Algorithms: {', '.join(self.algorithms)}")
        
        # Detailed performance comparison
        metrics = ['Makespan', 'CacheHit', 'TotalExecutionTime']
        metric_names = ['Makespan', 'Cache Hit Rate', 'Total Execution Time']
        
        print(f"\n📊 Detailed Performance Metrics Comparison:")
        for metric, name in zip(metrics, metric_names):
            print(f"\n🔍 {name}:")
            for algo in self.algorithms:
                data = self._get_algorithm_data(algo, metric)
                if metric == 'CacheHit':
                    data = data * 100  # Convert to percentage
                
                mean_val = np.mean(data)
                std_val = np.std(data)
                min_val = np.min(data)
                max_val = np.max(data)
                
                unit = '%' if metric == 'CacheHit' else 'Time Units'
                print(f"   {algo:15s}: Avg {mean_val:8.1f}{unit} | "
                      f"Std {std_val:6.1f} | Range [{min_val:.1f}, {max_val:.1f}]")
        
        # Improvement rate calculation
        if len(self.algorithms) >= 2:
            print(f"\n🚀 Cache-Aware Algorithm Improvement Effects:")
            
            # Makespan improvement
            makespan_ca = np.mean(self._get_algorithm_data(self.algorithms[0], 'Makespan'))
            makespan_wfd = np.mean(self._get_algorithm_data(self.algorithms[1], 'Makespan'))
            makespan_improvement = ((makespan_wfd - makespan_ca) / makespan_wfd) * 100
            
            # Cache hit rate improvement
            cache_ca = np.mean(self._get_algorithm_data(self.algorithms[0], 'CacheHit')) * 100
            cache_wfd = np.mean(self._get_algorithm_data(self.algorithms[1], 'CacheHit')) * 100
            cache_improvement = cache_ca - cache_wfd
            
            # Execution time improvement
            exec_ca = np.mean(self._get_algorithm_data(self.algorithms[0], 'TotalExecutionTime'))
            exec_wfd = np.mean(self._get_algorithm_data(self.algorithms[1], 'TotalExecutionTime'))
            exec_improvement = ((exec_wfd - exec_ca) / exec_wfd) * 100
            
            print(f"   📉 Makespan Reduction: {makespan_improvement:+.1f}%")
            print(f"   📈 Cache Hit Rate Improvement: {cache_improvement:+.1f}%")
            print(f"   ⚡ Execution Time Reduction: {exec_improvement:+.1f}%")
        
        print("\n" + "="*80)
        print("✅ Comprehensive Analysis Report Generated")
        print("="*80)
    
    def run_full_analysis(self):
        """Run complete visualization analysis process"""
        print("\n🚀 Starting complete algorithm performance visualization analysis...")
        
        # Load data
        if not self.load_data():
            print("❌ Data loading failed, cannot continue analysis")
            return False
        
        # Generate all charts
        print("\n📊 Generating Makespan analysis chart...")
        self.create_makespan_analysis()
        
        print("\n📊 Generating cache hit rate analysis chart...")
        self.create_cache_hit_analysis()
        
        print("\n📊 Generating execution time and win rate analysis chart...")
        self.create_execution_time_and_win_rate_analysis()
        
        # Generate comprehensive report
        print("\n📋 Generating comprehensive performance analysis report...")
        self.generate_comprehensive_report()
        
        print("\n✅ All analysis completed! All charts saved to result directory")
        return True

def main():
    """Main function"""
    print("🚀 Cache-Aware vs WFD Algorithm Performance Visualization Analysis Program v2.0")
    print("📅 Experiment Date: June 19, 2025")
    print("🔄 Features: Redesigned unified visualization analysis tool")
    print("📊 Functions: Makespan analysis, Cache hit rate analysis, Execution time and win rate analysis")
    print()
    
    # Check command line arguments
    csv_file = 'algorithm_comparison_results.csv'
    if len(sys.argv) > 1:
        csv_file = sys.argv[1]
        print(f"📄 Using specified data file: {csv_file}")
    
    # Create visualizer and run complete analysis
    visualizer = AlgorithmVisualizer(csv_file)
    success = visualizer.run_full_analysis()
    
    if success:
        print("\n🎉 Algorithm performance visualization analysis completed successfully!")
        print("📁 Please check the generated charts in the result directory:")
        print("   - makespan_analysis.png: Detailed Makespan analysis")
        print("   - cache_hit_analysis.png: Detailed cache hit rate analysis")
        print("   - execution_time_and_win_rate_analysis.png: Execution time and win rate analysis")
    else:
        print("\n❌ Errors occurred during analysis, please check data file and configuration")
        return 1
    
    return 0

if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
