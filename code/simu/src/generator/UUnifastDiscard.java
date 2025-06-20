package generator;

import java.util.ArrayList;
import java.util.Random;

/* A. Burns and R. I. Davis. Improved priority assignment for global Fixed priority pre-emptive 
 * scheduling in multiprocessor real-time systems. Real-Time Systems, 47(1):1{40, 2010.
*/

public class UUnifastDiscard {

	private double uUtil;
	private int uNum;
	private ArrayList<Double> uUs;
	private boolean shallDiscard;
	private int discardNum;
	private Random r;
	private int cores;

	private boolean takeAllUtil;

	public UUnifastDiscard(double util, int num, int discard, int cores, boolean takeAllUtil, Random ran) {
		this.uUtil = util;
		this.uNum = num;
		this.uUs = new ArrayList<Double>();
		this.shallDiscard = false;
		this.discardNum = discard;
		this.cores = cores;
		this.r = ran;
		this.takeAllUtil = takeAllUtil;
	}

	public void setUtil(double x) {
		this.uUtil = x;
	}

	public void setNum(int x) {
		this.uNum = x;
	}

	public double getUtil() {
		return this.uUtil;
	}

	public int getNum() {
		return this.uNum;
	}

	public ArrayList<Double> getUtils() {
		if (uUnifastDiscard()) {
			if (!takeAllUtil) {
				if (uUs.size() != uNum + 1) {
					System.err.println("UUnifastDiscard.getUtils(): the number of utilisations generated is wrong!");
					System.exit(-1);
				}

				uUs.remove(uUs.size() - 1);
			}
			return uUs;
		} else
			return null;
	}

	private boolean uUnifast() {
		uUs.clear();
		double sumU = this.uUtil;
		double nextSum = 0;
		double temp = 0;
		this.shallDiscard = false;
		for (int i = takeAllUtil ? 1 : 0; i < this.uNum; i++) {

			nextSum = sumU * Math.pow(r.nextDouble(), (1.0 / (this.uNum - i)));
			temp = sumU - nextSum;
			if (temp > cores) {
				this.shallDiscard = true;
				break;
			}
			this.uUs.add(temp);
			sumU = nextSum;
		}
		if (!shallDiscard) {
			if (sumU <= cores)
				uUs.add(sumU);
			else
				shallDiscard = true;
		}
		return this.shallDiscard;

	}

	private boolean uUnifastDiscard() {
		boolean isComplete = false;
		for (int i = 0; i < this.discardNum; i++) {
			if (!this.uUnifast()) {
				isComplete = true;
				break;
			} else {
				this.uUs.clear();
			}
		}
		return isComplete;
	}
	
	public static void main(String args[]) {
		
		UUnifastDiscard uu = new UUnifastDiscard(8.0, 1, 1000, 8, false, new Random(1000));
		
		uu.getUtils();
		
		for(int i=0; i<1000; i++) {
			
			System.out.println(uu.getUtils().get(0));
		}
	}

}