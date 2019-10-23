package basicTrain;

import java.util.Arrays;

import edu.princeton.cs.algs4.StdOut;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class Fund {

	public static void main(String[] args) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
		
			String[] name = {"x11", "x12", "x13", "x14", "x21", "x31", "x32",
					 "x41", "x43", "x51", "x52", "x6",};
			IloNumVar[] x = cplex.numVarArray(12, 0, Double.MAX_VALUE, name);
			
			cplex.addMinimize(cplex.sum(x[0], x[1], x[2], x[3]));
			
			double[] r = {0.012, 0.035, 0.058, 0.11};

			cplex.addEq(cplex.prod(1+r[0], x[0]), x[4]);
			cplex.addGe(cplex.sum(cplex.prod(1+r[1], x[1]), cplex.prod(1+r[0], x[4]), cplex.negative(x[5]), cplex.negative(x[6])), 50000);     
			cplex.addEq(cplex.sum(cplex.prod(1+r[2], x[2]), cplex.prod(1+r[0], x[5])), cplex.sum(x[7],x[8]));
			cplex.addGe(cplex.sum(cplex.prod(1+r[0], x[7]), cplex.prod(1+r[1], x[6]), cplex.negative(x[9]), cplex.negative(x[10])), 50000);
			cplex.addEq(cplex.prod(1+r[0], x[9]), x[11]);
			cplex.addGe(cplex.sum(cplex.prod(1+r[0], x[11]), cplex.prod(1+r[2], x[8]),
					cplex.prod(1+r[3], x[3]), cplex.prod(1+r[1], x[10])), 200000);
			cplex.exportModel("Fund.lp");
			
			if(cplex.solve()) {
				StdOut.println("value is:" + cplex.getObjValue());
				double[] ans = cplex.getValues(x);
				StdOut.println("value of variables are: " + Arrays.toString(ans));
			}
			cplex.end();
		}catch(IloException e) {
			StdOut.println("IloExeption is" + e);
		}
	}

}
