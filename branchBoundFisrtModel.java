package basicTrain;

import java.util.Arrays;

import edu.princeton.cs.algs4.StdOut;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class branchBoundFisrtModel {

	public static void main(String[] args) {
		// TODO 自动生成的方法存根
		branchBoundFisrtModel b = new branchBoundFisrtModel();
		b.Calculate();
		StdOut.println("ENDENDEND ans is: " + b._signAns + " vals is: " + Arrays.toString(b._signArr));
	}

	public static final int _nVar = 4;

	public IloCplex cplex;
	public IloNumVar[] var;

	// 存储临时最优解
	public int _signAns = Integer.MIN_VALUE;
	public double[] _signArr = new double[_nVar];

	// 主函数！
	public void Calculate() {
		setInitial();
		double[] temp = new double[_nVar];
		int tempAns = solve(temp);
		// 以初始解为起点，开始递归
		dfs(temp, tempAns, 0);
	}

	public void dfs(double[] ansArr, int ans, int level) {
		StdOut.println();
		// 判断目前的分支是返回还是继续分割.
		int isInteger = judgeInteger(ansArr);
		if (ans <= _signAns) {
			StdOut.println("this ans " + ans + "is less than current opt: " + _signAns + ", return!");
			return;
		} else if (ans > _signAns && isInteger == -1) {
			_signAns = ans;
			for (int i = 0; i < _nVar; i++)
				_signArr[i] = ansArr[i];
			StdOut.println("update the current opt, new opt is: " + _signAns + " ,and var is "
					+ Arrays.toString(_signArr) + ", return!");
			return;
		} else if (level >= _nVar) {
			StdOut.println("over the bound! we have only four variables!");
			return;
		}

		// 继续分割，根据贪心思想，分叉前前先计算再选择朝左还是朝右
		StdOut.println("keep searching variable " + isInteger + " because it's double!");
		double[] bagLeft = new double[4];
		IloConstraint tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
		// 计算左边结果,朝左走时先加约束，然后再减去朝左约束，保证接下来计算朝右的计算结果时，没有受到左边约束干扰。
		int leftFirst = solve(bagLeft);
		delConsNullOut(tempConstraint1, isInteger);
		double[] bagRight = new double[4];
		IloConstraint tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
		// 计算右边结果，结束再删掉右边约束
		int rightFirst = solve(bagRight);
		delConsNullOut(tempConstraint2, isInteger);

		// 选择计算结果较大的方向先走
		if (leftFirst >= rightFirst) {
			StdOut.println("due to left " + leftFirst + "is larger than right " + rightFirst + ", turn left!");

			// 一旦朝左走，左边的约束要重新加上，方便进入下一层。当回溯回来时，加上的左边约束要再次删除。
			tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
			dfs(bagLeft, leftFirst, level + 1);
			delConstraint(tempConstraint1, isInteger);

			// 选择朝右走同理。
			tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
			dfs(bagRight, rightFirst, level + 1);
			delConstraint(tempConstraint2, isInteger);
		} else {
			StdOut.println("due to right " + rightFirst + "is larger than left " + leftFirst + ", turn right!");

			// 先朝右走，递归前加上此层约束进入下一层， 回溯回来后，删掉此约束，回到上一层。
			tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
			dfs(bagRight, rightFirst, level + 1);
			delConstraint(tempConstraint2, isInteger);

			// 朝左，同理
			tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
			dfs(bagLeft, leftFirst, level + 1);
			delConstraint(tempConstraint1, isInteger);
		}
	}

	// 建立cplex的初始模型
	public void setInitial() {
		try {
			String[] name = { "x1", "x2", "x3", "x4" };
			int[] objCoe = { 9, 5, 6, 4 };
			int[][] leftCoe = { { 6, 3, 5, 2 }, { 0, 0, 1, 1 }, { -1, 0, 1, 0 }, { 0, -1, 0, 1 } };
			int[] rigCoe = { 10, 1, 0, 0 };

			cplex = new IloCplex();
			cplex.setOut(null);
			var = cplex.numVarArray(_nVar, 0, 1, name);

			cplex.addMaximize(cplex.scalProd(objCoe, var));
			for (int i = 0; i < _nVar; i++)
				cplex.addLe(cplex.scalProd(leftCoe[i], var), rigCoe[i]);
			
			cplex.exportModel("initial model.lp");
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// 判断目前所求的解里， 各变量取值是否都为整数。正数和负数都可以处理
	public int judgeInteger(double[] temp) {
		for (int i = 0; i < temp.length; i++) {
			// -1.999999999 应该是-2， 1+1e-9 应该是1
			if (temp[i] >= 0) {
				if ((temp[i] - (int) temp[i] < 1e-9) || ((int) temp[i] + 1 - temp[i]) < 1e-9)
					continue;
				else
					return i;
			} else {
				if (temp[i] - (int) temp[i] + 1 < 1e-9 || -temp[i] + (int) temp[i] < 1e-9)
					continue;
				return i;
			}
		}
		return -1;
	}

	// 根据所走的方向，向全局变量cplex里面选择要添加的约束，约束要输出，以便于后续的delConstraint。
	public IloConstraint addConstraint(int rootLoc, double root, String direct) {
		try {
			IloConstraint tempConstraint = null;
			if (direct.equals("left")) {
				if (root > 0)
					tempConstraint = cplex.addLe(var[rootLoc], (int) root);
				else
					tempConstraint = cplex.addLe(var[rootLoc], (int) root - 1);
			} else if (direct.equals("right")) {
				if (root > 0)
					tempConstraint = cplex.addGe(var[rootLoc], (int) root + 1);
				else
					tempConstraint = cplex.addGe(var[rootLoc], (int) root);
			}
			cplex.exportModel("add constraint of variable" + rootLoc + ".lp");
			return tempConstraint;
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
			return null;
		}
	}

	// 删除addConstriant里添加的约束，这里要打印输出模型，为了检查回溯回来时约束已经被删除。
	public void delConstraint(IloConstraint tempConstraint, int rootLoc) {
		assert tempConstraint != null;
		try {
			cplex.remove(tempConstraint);
			cplex.exportModel("delete constraint of " + rootLoc + ".lp");
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// 删除约束但不打印lp文件，这是为了在判断分支方向时，要先走下左再走下右，这里的删除约束不用打印模型，重点关注回溯回来后模型
	public void delConsNullOut(IloConstraint tempConstraint, int rootLoc) {
		assert tempConstraint != null;
		try {
			cplex.remove(tempConstraint);
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// 求解模型
	public Integer solve(double[] ans) {
		try {
			if (cplex.solve()) {
				double[] temp = cplex.getValues(var);
				for (int i = 0; i < _nVar; i++) {
					ans[i] = temp[i];
				}
				return (int) cplex.getObjValue();
			}
			return Integer.MIN_VALUE;
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
			return Integer.MIN_VALUE;
		}
	}

}
