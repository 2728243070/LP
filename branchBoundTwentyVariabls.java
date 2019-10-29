package basicTrain;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.StdRandom;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

public class branchBoundTwentyVariabls {

	public static void main(String[] args) throws FileNotFoundException {
		branchBoundTwentyVariabls b = new branchBoundTwentyVariabls();
		b.Calculate();
		System.err.println("ENDENDEND, we get the opt: " + b._signAns + "  , x :" + b.printAnsArr(b._signArr));
		System.err.flush();
	}

	public static final int _nProduct = 4;
	public static final int _nProcedure = 5;

	public static int _dfsLevel = 0;

	public IloCplex cplex;
	public IloNumVar[][] x;

	// store the result of the last calculation.if preAns == grandAns == Ans, we think there is a cycle!
	public double preAns = -1;
	public double grandAns = -1;

	// Store temporary opt
	public double _signAns = Double.MAX_VALUE;
	public double[][] _signArr = new double[_nProcedure][_nProduct];

	// main function!
	public void Calculate() throws FileNotFoundException {
		setInitial();
		double[][] temp = new double[_nProcedure][_nProduct];
		double tempAns = solve(temp);
		preAns = 0;
		// recurse with original solution
		StdOut.println("If we cannot solve a LP, we define the value of obj as Double.MAX_VALUE.");
		StdOut.println("First, we solve the initial LP.");
		dfs(temp, tempAns, 0);
	}

	// judge whether two double are equal or not!
	public boolean equals(double a, double b) {
		return Math.abs(a - b) < 1e-6;
	}

	public boolean smallerAndEqualTo(double a, double b) {
		return a < b || equals(a, b);
	}

	public void dfs(double[][] ansArr, double ans, int level) {
		// these "if" aim to decide whether keep branching or not
		// !!! this "if" aims to avoid final local cycle.
		if (equals(ans, preAns) && equals(ans, grandAns)) {
			StdOut.println("ans:" + ans + ", preAns is: " + preAns + ", stuck in local cycle, return!");
			return;
		}
		grandAns = preAns;
		preAns = ans;
		int[] isDouble = judgeInteger(ansArr);
		StdOut.println();
		StdOut.println("new ans: " + ans + " ,new value of variables: " + printAnsArr(ansArr));
		if (smallerAndEqualTo(_signAns, ans)) {
			StdOut.println("Now we're in recursion, level " + level);
			StdOut.println("current ans " + ans + " is >= OPT: " + _signAns + ", cut branching here!");
			return;
		} else if (ans < _signAns && isDouble[0] == -1) {
			StdOut.println("Now we're in recursion, level " + level);
			_signAns = ans;
			for (int i = 0; i < _nProcedure; i++)
				_signArr[i] = Arrays.copyOf(ansArr[i], ansArr[i].length);
			StdOut.println("########UPDATE OPT, new OPT is: " + _signAns + " ,and var is " + printAnsArr(_signArr)
					+ ", stop here!");
			System.err.println("########UPDATE OPT, new OPT is: " + _signAns + " ,and var is " + printAnsArr(_signArr)
					+ ", stop here!");
			System.err.flush();
			return;
		} else {
			if (level != 0)
				StdOut.println("Now we're in recursion, level " + level);
			int left, right;
			if (ansArr[isDouble[0]][isDouble[1]] > 0) {
				left = (int) ansArr[isDouble[0]][isDouble[1]];
				right = left + 1;
			} else {
				left = (int) ansArr[isDouble[0]][isDouble[1]] - 1;
				right = left + 1;
			}

			// keep branching. Depending on greedy algorithm, we calculate the results of
			// turning left road and turning right road before branching.
			StdOut.println("Can't cut the branch, the first non-integer var is x_" + Arrays.toString(isDouble)
					+ " , next, branch at x" + Arrays.toString(isDouble) + " whose value is: "
					+ ansArr[isDouble[0]][isDouble[1]]);
			double[][] bagLeft = new double[_nProcedure][_nProduct];
			IloConstraint tempConstraint1 = addConstNullOut(isDouble, left, "left");
			// when calculating left result, we must delete the constraints which are added
			// just now, aiming to avoid influence next steps.
			double leftFirst = solve(bagLeft);
			delConstraint(tempConstraint1);
			double[][] bagRight = new double[_nProcedure][_nProduct];
			IloConstraint tempConstraint2 = addConstNullOut(isDouble, right, "right");
			// Calculating the right root is the same as the left.
			double rightFirst = solve(bagRight);
			delConstraint(tempConstraint2);

			StdOut.println("if we turn left, we get the obj:" + leftFirst + " , if we turn right, we get the obj:"
					+ rightFirst);
			// Depending on the above results, we choose the way with less OBJ due to we
			// hope to find the smallest cost.

			if (leftFirst <= rightFirst) {
				StdOut.println("because left " + leftFirst + " is <= right " + rightFirst + ", turn left at first!");
				// if we decide to turn left, we should add the constraint which must be deleted
				// after returning from recursion.
				tempConstraint1 = addConstraint(isDouble, left, "left", level);
				StdOut.println("add x" + Arrays.toString(isDouble) + " < " + left + " ,we will recalculate the lp");
				dfs(bagLeft, leftFirst, level + 1);
				delConstraint(tempConstraint1);

				// the same as turning left.
				StdOut.println("Now we go back to level " + level);
				tempConstraint2 = addConstraint(isDouble, right, "right", level);
				StdOut.println("add x" + Arrays.toString(isDouble) + " > " + right + " ,we will recalculate the lp");
				dfs(bagRight, rightFirst, level + 1);
				delConstraint(tempConstraint2);
			} else {
				StdOut.println("because right " + rightFirst + " is < left " + leftFirst + " , turn right at first!");

				// add constraints and delete it after returning from recursion.
				tempConstraint2 = addConstraint(isDouble, right, "right", level);
				StdOut.println("add x" + Arrays.toString(isDouble) + " > " + right + " ,we will recalculate the lp");
				dfs(bagRight, rightFirst, level + 1);
				delConstraint(tempConstraint2);

				StdOut.println("Now we go back to level " + level);
				tempConstraint1 = addConstraint(isDouble, left, "left", level);
				StdOut.println("add x" + Arrays.toString(isDouble) + " < " + left + " ,we will recalculate the lp");
				dfs(bagLeft, leftFirst, level + 1);
				delConstraint(tempConstraint1);
			}
		}
	}

	public String printAnsArr(double[][] ans) {
		String s = "";
		for (int i = 0; i < ans.length; i++)
			s = s + Arrays.toString(ans[i]);
		return s;
	}

	// construct the initial model
	public void setInitial() throws FileNotFoundException {
		double[][] cost = { { 6, 10, 8, 10 }, { 6.2, 10.7, 8.5, 10.7 }, { 7.2, 12, 9.6, 12 }, { 3, 5, 4, 5 },
				{ 3.1, 5.4, 4.3, 5.4 } };
		double[][] time = { { 0.04, 0.17, 0.06, 0.12 }, { 0.05, 0.14, 0, 0.14 }, { 0.06, 0.13, 0.05, 0.10 },
				{ 0.05, 0.21, 0.02, 0.1 }, { 0.03, 0.15, 0.04, 0.15 } };
		String[][] name = { { "x11", "x12", "x13", "x14" }, { "x21", "x22", "x23", "x24" },
				{ "x31", "x32", "x33", "x34" }, { "x41", "x42", "x43", "x44" }, { "x51", "x52", "x53", "x54" } };
		double[] sheetMetal = { 1.2, 1.6, 2.1, 2.4 };
		int[] production = { 1800, 1400, 1600, 1800 };
		int totalSteel = 10000;
		int overTime = 100;
		int[] totalTime = { 500, 400, 600, 550, 500 };

		try {
			cplex = new IloCplex();
			cplex.setOut(new PrintStream(new FileOutputStream("CPLEXDEBUG.txt")));

			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("DEBUG.txt"))));
			System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream("ERRDEBUG.txt"))));

			x = new IloNumVar[_nProcedure][_nProduct];

			for (int i = 0; i < _nProcedure; i++)
				for (int j = 0; j < _nProduct; j++)
					x[i][j] = cplex.numVar(0, Double.MAX_VALUE, name[i][j]);
			/*
			 * x[0][i] Number of the ith tool regularly manufactured in normal working hours
			 * in the first stage x[1][i] Number of the ith tool manufactured in overtime in
			 * the first stage x[2][i] Number of the ith tool manufactured by the
			 * contractors in the first stage x[3][i] Number of the ith tool regularly
			 * manufactured in normal working hours in the second stage x[4][i] Number of
			 * the ith tool regularly manufactured in overtime in the second stage
			 */

			IloNumExpr totalCost = cplex.numExpr();

			totalCost = cplex.sum(cplex.scalProd(cost[0], x[0]), cplex.scalProd(cost[1], x[1]),
					cplex.scalProd(cost[2], x[2]), cplex.scalProd(cost[3], x[3]), cplex.scalProd(cost[4], x[4]));

			cplex.addMinimize(totalCost);

			IloNumExpr totalSteelNum = cplex.numExpr();
			for (int i = 0; i < _nProduct; i++) {
				totalSteelNum = cplex.sum(totalSteelNum, cplex.prod(sheetMetal[i], cplex.sum(x[0][i], x[1][i])));
			}
			cplex.addLe(totalSteelNum, totalSteel);

			// x11 + x21 + x31 = x41 + x51 production of products should meet the
			// requirements!
			for (int i = 0; i < _nProduct; i++) {
				cplex.addGe(cplex.sum(x[0][i], x[1][i], x[2][i]), production[i]);
				cplex.addEq(cplex.sum(x[0][i], x[1][i], x[2][i]), cplex.sum(x[3][i], x[4][i]));
			}

			// constraints of time! regular time and overtime
			for (int i = 0; i < 2; i++) {
				cplex.addLe(cplex.scalProd(x[0], time[i]), totalTime[i]);
				cplex.addLe(cplex.sum(cplex.scalProd(x[1], time[i]), cplex.scalProd(x[0], time[i])),
						overTime + totalTime[i]);
				// cplex.addLe(cplex.scalProd(over1, time[i]), overTime);
			}
			for (int i = 2; i < _nProcedure; i++) {
				cplex.addLe(cplex.scalProd(x[3], time[i]), totalTime[i]);
				cplex.addLe(cplex.sum(cplex.scalProd(x[3], time[i]), cplex.scalProd(x[4], time[i])),
						overTime + totalTime[i]);
				// cplex.addLe(cplex.scalProd(over2, time[i]), overTime);
			}

			cplex.exportModel("InitialTool.lp");

		} catch (IloException e) {
			StdOut.println("IloExeption is" + e);
		}
	}

	// judge the value of variables is whether integer or not. Return the location
	// of first non-integer variable according the natural order.
	public int[] judgeInteger(double[][] temp) {
		double contrast = 1;
		int[] loc = { -1, -1 };
		for (int i = 0; i < temp.length; i++) {
			for (int j = 0; j < temp[0].length; j++) {
				// -1.999999999 should be -2£¬ 1+1e-9 should be 1
				if (temp[i][j] >= 0) {
					if (equals(temp[i][j], (int) temp[i][j])) {
						temp[i][j] = (int) temp[i][j];
						continue;
					} else if (equals((int) temp[i][j] + 1, temp[i][j])) {
						temp[i][j] = (int) temp[i][j] + 1;
						continue;
					} else {		
						loc[0] = i;
						loc[1] = j;
						break;
					}
					// choose the branching variable randomly.
					/*ArrayList<int[]> a = new ArrayList<>();
					 * int[] tempLoc = { i, j }; a.add(tempLoc);
					 * 
					 * if (a.size() != 0) return a.get(StdRandom.uniform(a.size())); 
					 * else return new int[] { -1, -1 };
					 */
					// choose the branching variable which has the longest distance with nearest integer!
					/*
					 * if (Math.abs(temp[i][j] - (int) temp[i][j] - 0.5) < contrast ||
					 * equals(Math.abs(temp[i][j] - (int) temp[i][j] - 0.5), contrast)) { contrast =
					 * Math.abs(temp[i][j] - (int) temp[i][j] - 0.5); loc[0] = i; loc[1] = j; }
					 * 
					 * return loc;
					 */
				} else {
					if (equals(temp[i][j], (int) temp[i][j] + 1)) {
						temp[i][j] = (int) temp[i][j] - 1;
						continue;
					} else if (equals((int) temp[i][j], temp[i][j])) {
						temp[i][j] = (int) temp[i][j];
						continue;
					} else {
						loc[0] = i;
						loc[1] = j;
						break;
					}
				}
			}
		}
		return loc;
	}

	// add constraints to "cplex" and export the model.
	public IloConstraint addConstraint(int[] rootLoc, int bound, String direct, int level) {
		try {
			IloConstraint tempConstraint = null;
			String pre = "";
			if (direct.equals("left")) {
				tempConstraint = cplex.addLe(x[rootLoc[0]][rootLoc[1]], bound);
				pre = "step " + _dfsLevel + ", dfslevel " + level + ", x" + Arrays.toString(rootLoc) + " less than "
						+ bound + " .lp";
			} else if (direct.equals("right")) {
				tempConstraint = cplex.addGe(x[rootLoc[0]][rootLoc[1]], bound);
				pre = "step " + _dfsLevel + ", dfslevel " + level + ", x" + Arrays.toString(rootLoc) + " large than "
						+ bound + " .lp";
			}
			_dfsLevel++;
			// cplex.exportModel(pre);
			return tempConstraint;
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
			return null;
		}
	}

	// add constraints but don't export the model
	public IloConstraint addConstNullOut(int[] rootLoc, int bound, String direct) {
		try {
			IloConstraint tempConstraint = null;
			if (direct.equals("left")) {
				tempConstraint = cplex.addLe(x[rootLoc[0]][rootLoc[1]], bound);
			} else if (direct.equals("right")) {
				tempConstraint = cplex.addGe(x[rootLoc[0]][rootLoc[1]], bound);
			}
			return tempConstraint;
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
			return null;
		}
	}

	public void delConstraint(IloConstraint tempConstraint) {
		assert tempConstraint != null;
		try {
			cplex.remove(tempConstraint);
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// solve the model!
	public Double solve(double[][] ans) {
		try {
			if (cplex.solve()) {
				for (int i = 0; i < _nProcedure; i++) {
					ans[i] = Arrays.copyOf(cplex.getValues(x[i]), x[i].length);
				}
				return cplex.getObjValue();
			}
			return Double.MAX_VALUE;
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
			return Double.MAX_VALUE;
		}
	}

}
