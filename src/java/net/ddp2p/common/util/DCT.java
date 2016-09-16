package net.ddp2p.common.util;

public class DCT {
	/**
	 * Super slow implementation of DCT-II (JPEG DCT)
	 * @param x
	 * @return
	 */
	static double[][] toDCT(double[][] x) {
		int N = x.length; // N rows
		int M = x[0].length; // M columns
		double[][] X = new double[N][M];
		double[][] tmp = new double[N][M];
		
		// first by rows
		for (int i = 0; i < N; i ++) { // over all rows
			// DCT for row i
			for (int k = 0; k < M; k ++) { // over all columns
				// DCT at position k (for row i)
				X[i][k] = 0;
				// on row i go over the M columns
				for (int n = 0; n < M; n ++) {
					X[i][k] += x[i][n]*Math.cos(Math.PI/M*(n+0.5)*k);
				}
				X[i][k] *= Math.sqrt(2.0/M);
			}
			X[i][0] *= 1/Math.sqrt(2);
		}
		//System.out.println("\nAfter rows");
		//dumpMatrix(X);
		x = X;
		X = tmp;
		// then by cols
		for (int j = 0; j < M; j ++) { // over all cols
			for (int k = 0; k < N; k ++) { // over all rows
				X[k][j] = 0;
				// on one row go over rows
				for (int n = 0; n < N; n ++) {
					X[k][j] += x[n][j]*Math.cos(Math.PI/N*(n+0.5)*k);
				}
				X[k][j] *= Math.sqrt(2.0/N);
			}
			X[0][j] *= 1/Math.sqrt(2);
		}		
		return X;
	}
	/**
	 * Super slow implementation of IDCT DCT-III
	 * @param x
	 * @return
	 */
	static double[][] fromDCT(double[][] x) {
		int N = x.length; // N rows
		int M = x[0].length; // M columns
		double[][] X = new double[N][M];
		double[][] tmp = new double[N][M];
		
		// first by rows
		for (int i = 0; i < N; i ++) { // over all rows
			x[i][0] *= Math.sqrt(2);
			// DCT for row i
			for (int k = 0; k < M; k ++) { // over all columns
				// DCT at position k (for row i)
				X[i][k] = 0.5*x[i][0];
				// on row i go over the M columns
				for (int n = 1; n < M; n ++) {
					X[i][k] += x[i][n]*Math.cos(Math.PI/M*n*(k+0.5));
				}
				X[i][k] *= Math.sqrt(2.0/M);
			}
			//System.out.println("\nAfter row "+i);
			//dumpMatrix(X);
		}
		//System.out.println("\nAfter rows");
		//dumpMatrix(X);
		x = X;
		X = tmp;
		// then by cols
		for (int j = 0; j < M; j ++) { // over all cols
			x[0][j] *= Math.sqrt(2);
			for (int k = 0; k < N; k ++) { // over all rows
				X[k][j] = 0.5*x[0][j];
				// on one row go over rows
				for (int n = 1; n < N; n ++) {
					X[k][j] += x[n][j]*Math.cos(Math.PI/N*n*(k+0.5));
				}
				X[k][j] *= Math.sqrt(2.0/N);
			}
		}		
		return X;
	}
	public static void dumpMatrix(double[][] x) {
		int N = x.length; // N rows
		int M = x[0].length; // M columns
		for (int i = 0; i < N; i ++) { // over all rows
			for (int j = 0; j < M; j ++) { // over all columns
				//System.out.print(x[i][j] + " ");
				System.out.printf("%.1f ", x[i][j]);
			}
			System.out.println("\n ");
		}
	}
	/**
	 * Fill all elements to val
	 * @param x
	 * @param val
	 */
	public static void fillMatrix(double[][] x, double val) {
		int N = x.length; // N rows
		int M = x[0].length; // M columns
		for (int i = 0; i < N; i ++) { // over all rows
			for (int j = 0; j < M; j ++) { // over all columns
				x[i][j] = val;
			}
			System.out.println("\n ");
		}
	}
	/**
	 * Without setting the first coef by sqrt(2)
	 * @param x
	 */
	private static void normalizeToDCT(double[][]x) {
		int N = x.length; // N rows
		int M = x[0].length; // M columns
		for (int i = 0; i < N; i ++) { // over all rows
			for (int j = 0; j < M; j ++) { // over all columns
				x[i][j] *= Math.sqrt(2.0/N);
				//x[i][0] *= 1/Math.sqrt(2);
			}
			System.out.println("\n ");
		}
	}
	public static void main(String args[]) {
		int N = Integer.parseInt(args[0]);
		int val = Integer.parseInt(args[1]);
		double x[][] = new double[N][N];
		fillMatrix(x, val);
		System.out.println("In: ");
		dumpMatrix(x);
		double[][] X = toDCT(x);
		System.out.println("Out: ");
		dumpMatrix(X);
		double[][] R = fromDCT(X);
		System.out.println("R: ");
		dumpMatrix(R);
	}
}
