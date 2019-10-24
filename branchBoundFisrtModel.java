package basicTrain;

import java.util.Arrays;

import edu.princeton.cs.algs4.StdOut;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class branchBoundFisrtModel {

	public static void main(String[] args) {
		// TODO �Զ����ɵķ������
		branchBoundFisrtModel b = new branchBoundFisrtModel();
		b.Calculate();
		StdOut.println("ENDENDEND ans is: " + b._signAns + " vals is: " + Arrays.toString(b._signArr));
	}

	public static final int _nVar = 4;

	public IloCplex cplex;
	public IloNumVar[] var;

	// �洢��ʱ���Ž�
	public int _signAns = Integer.MIN_VALUE;
	public double[] _signArr = new double[_nVar];

	// ��������
	public void Calculate() {
		setInitial();
		double[] temp = new double[_nVar];
		int tempAns = solve(temp);
		// �Գ�ʼ��Ϊ��㣬��ʼ�ݹ�
		dfs(temp, tempAns, 0);
	}

	public void dfs(double[] ansArr, int ans, int level) {
		StdOut.println();
		// �ж�Ŀǰ�ķ�֧�Ƿ��ػ��Ǽ����ָ�.
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

		// �����ָ����̰��˼�룬�ֲ�ǰǰ�ȼ�����ѡ�����ǳ���
		StdOut.println("keep searching variable " + isInteger + " because it's double!");
		double[] bagLeft = new double[4];
		IloConstraint tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
		// ������߽��,������ʱ�ȼ�Լ����Ȼ���ټ�ȥ����Լ������֤���������㳯�ҵļ�����ʱ��û���ܵ����Լ�����š�
		int leftFirst = solve(bagLeft);
		delConsNullOut(tempConstraint1, isInteger);
		double[] bagRight = new double[4];
		IloConstraint tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
		// �����ұ߽����������ɾ���ұ�Լ��
		int rightFirst = solve(bagRight);
		delConsNullOut(tempConstraint2, isInteger);

		// ѡ��������ϴ�ķ�������
		if (leftFirst >= rightFirst) {
			StdOut.println("due to left " + leftFirst + "is larger than right " + rightFirst + ", turn left!");

			// һ�������ߣ���ߵ�Լ��Ҫ���¼��ϣ����������һ�㡣�����ݻ���ʱ�����ϵ����Լ��Ҫ�ٴ�ɾ����
			tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
			dfs(bagLeft, leftFirst, level + 1);
			delConstraint(tempConstraint1, isInteger);

			// ѡ������ͬ��
			tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
			dfs(bagRight, rightFirst, level + 1);
			delConstraint(tempConstraint2, isInteger);
		} else {
			StdOut.println("due to right " + rightFirst + "is larger than left " + leftFirst + ", turn right!");

			// �ȳ����ߣ��ݹ�ǰ���ϴ˲�Լ��������һ�㣬 ���ݻ�����ɾ����Լ�����ص���һ�㡣
			tempConstraint2 = addConstraint(isInteger, ansArr[isInteger], "right");
			dfs(bagRight, rightFirst, level + 1);
			delConstraint(tempConstraint2, isInteger);

			// ����ͬ��
			tempConstraint1 = addConstraint(isInteger, ansArr[isInteger], "left");
			dfs(bagLeft, leftFirst, level + 1);
			delConstraint(tempConstraint1, isInteger);
		}
	}

	// ����cplex�ĳ�ʼģ��
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

	// �ж�Ŀǰ����Ľ�� ������ȡֵ�Ƿ�Ϊ�����������͸��������Դ���
	public int judgeInteger(double[] temp) {
		for (int i = 0; i < temp.length; i++) {
			// -1.999999999 Ӧ����-2�� 1+1e-9 Ӧ����1
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

	// �������ߵķ�����ȫ�ֱ���cplex����ѡ��Ҫ��ӵ�Լ����Լ��Ҫ������Ա��ں�����delConstraint��
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

	// ɾ��addConstriant����ӵ�Լ��������Ҫ��ӡ���ģ�ͣ�Ϊ�˼����ݻ���ʱԼ���Ѿ���ɾ����
	public void delConstraint(IloConstraint tempConstraint, int rootLoc) {
		assert tempConstraint != null;
		try {
			cplex.remove(tempConstraint);
			cplex.exportModel("delete constraint of " + rootLoc + ".lp");
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// ɾ��Լ��������ӡlp�ļ�������Ϊ�����жϷ�֧����ʱ��Ҫ���������������ң������ɾ��Լ�����ô�ӡģ�ͣ��ص��ע���ݻ�����ģ��
	public void delConsNullOut(IloConstraint tempConstraint, int rootLoc) {
		assert tempConstraint != null;
		try {
			cplex.remove(tempConstraint);
		} catch (IloException e) {
			StdOut.println("Exception e: " + e);
		}
	}

	// ���ģ��
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
