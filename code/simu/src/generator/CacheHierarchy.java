package generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CacheHierarchy implements Serializable {
	
	private static final long serialVersionUID = -6902741116934537381L;

	public final int coreNum;
	public final int cacheLevel;
	public final int level2ClusterNum;
	public final int level2ClusterSize;

	public final List<int[]> level1;
	public final List<int[]> level2;
	public final List<int[]> level3;

	public CacheHierarchy(int coreNum, int cacheLevel, List<int[]> level2) {

		if (coreNum < 1) {
			System.err.println("CoreNum = " + coreNum + ", which is less than 1.");
		}

		if (cacheLevel < 2) {
			System.err.println(
					"Cache Hierarchy setting error! Cache levels  = " + cacheLevel + ", which are less than 2");
		}

		this.coreNum = coreNum;
		this.cacheLevel = cacheLevel;
		this.level2ClusterSize = -1;

		this.level2 = level2;
		level1 = new ArrayList<int[]>(); // new int[coreNum][1];
		level3 = new ArrayList<int[]>(); // new int[1][coreNum];

		int[] perLevel3 = new int[coreNum];
		for (int i = 0; i < coreNum; i++) {
			int[] perLevel1 = new int[1];
			perLevel1[0] = i;
			level1.add(perLevel1);

			perLevel3[i] = i;
		}
		level3.add(perLevel3);

		this.level2ClusterNum = level2.size();

	}

	public CacheHierarchy(int coreNum, int cacheLevel, int level2ClusterSize) {

		if (coreNum < 1) {
			System.err.println("CoreNum = " + coreNum + ", which is less than 1.");
		}

		if (coreNum < level2ClusterSize) {
			System.err.println("CoreNum = " + coreNum + ", which is less the level 2 cluster size, which is "
					+ level2ClusterSize + ".");
		}

		if (cacheLevel < 2) {
			System.err.println(
					"Cache Hierarchy setting error! Cache levels  = " + cacheLevel + ", which are less than 2");
		}

		if (coreNum % level2ClusterSize != 0) {
			System.err.println("Cache Hierarchy setting error! level 2 cache cannot be evenly shared! coreNum = "
					+ coreNum + ", cachelevel");
		}

		this.coreNum = coreNum;
		this.cacheLevel = cacheLevel;
		this.level2ClusterSize = level2ClusterSize;

		level2 = new ArrayList<int[]>();
		level1 = new ArrayList<int[]>(); // new int[coreNum][1];
		level3 = new ArrayList<int[]>(); // new int[1][coreNum];

		for (int i = 0; i < coreNum / level2ClusterSize; i++) {
			int[] perLevel2 = new int[level2ClusterSize];
			level2.add(perLevel2);
		}

		int[] perLevel3 = new int[coreNum];
		for (int i = 0; i < coreNum; i++) {
			int[] perLevel1 = new int[1];
			perLevel1[0] = i;
			level1.add(perLevel1);

			int clusterID = i / level2ClusterSize;
			level2.get(clusterID)[i % level2ClusterSize] = i;

			perLevel3[i] = i;
		}
		level3.add(perLevel3);

		this.level2ClusterNum = coreNum / level2ClusterSize;

	}

	private String getOneCacheLevel(int level) {
		String out = "";

		switch (level) {
		case 1:
			out += "level 1: \n";

			out += Arrays.deepToString(level1.toArray()) + "\n";

			break;
		case 2:
			out += "level 2: \n";

			out += Arrays.deepToString(level2.toArray()) + "\n";
			break;
		case 3:
			out += "level 3: \n";

			out += Arrays.deepToString(level3.toArray()) + "\n";
			break;

		default:
			break;
		}

		return out;
	}

	public int getLevel2ClusterID(int core) {
	
		for (int i = 0; i < level2.size(); i++) {
			int[] cores = level2.get(i);
			
			for(int k=0; k<cores.length;k++) {
				if(cores[k] == core)
					return i;
			}
		}

		
		return -1;

	}

	@Override
	public String toString() {

		String out = "CoreNum = " + coreNum + ", cacheLevel = " + cacheLevel + ", level2ClusterSize = "
				+ level2ClusterSize + "\n";

		for (int i = 0; i < cacheLevel; i++) {
			out += getOneCacheLevel(i + 1);
		}

		return out;
	}

	public static void main(String args[]) {
		CacheHierarchy ch = new CacheHierarchy(8, 3, 4);
		System.out.println(ch.toString());

		System.out.println("**************************************************************\n");

		List<int[]> level2 = new ArrayList<int[]>();

		int[] level2_1 = { 0 };
		int[] level2_2 = { 1, 2 };

		level2.add(level2_1);
		level2.add(level2_2);

		CacheHierarchy ch1 = new CacheHierarchy(3, 3, level2);
		System.out.println(ch1.toString());
	}

}
