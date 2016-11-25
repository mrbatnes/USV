/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usv;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.FunctionsUtils;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.LPOptimizationRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author lars-harald
 */
public class ThrustAllocUSV {

    /*
    alpha = arctan(uy/ux)
    Tmin = 0
    Tmax = limited form thrust region
    config matrix B:  1   0     for each thruster
                      0   1
                     -Ly Lx

    tau = [fx fy mz]
    u = [ux uy]

    B*u = tau

    W = w 0 0  power weight matrix
        0 w 0
        0 0 wn

    n = number of thrusters

    position vector B1 to Bn element in R3x2
    B = [B1,...Bn]
    u = (u1,...un)'

    W element in R2n*2n
    B element in R3*2n
    u element in R2n
    s element in R


    linear approximation of thruster region:

    circle with radius Tmax = R > 0 approximated by a N sided rectangular polygon
    (N >= 3) the polygon divides the circular region into N circular sectors, each
    having a central angle of phi = 2pi/N.
    the inner radius of the polygon is:
    r = R*cos(phi/2) = R*cos(pi/N)
    maximum approx error epsilon = R(1-cos(pi/N))
    epsilon defined as 1% (Tmax/100) gives N:
        N = pi/arccos(1-(epsilon/R)) with R > 0

    this gives the linear inequalities
    [cos(phiK) sin(phiK)] [ ux      <= r
                            uy ]

    for every
        phiK = (2K + 1)pi/N with K = 0,....N-1

    in the inequality matrix A:

    |phi0,1 pih0,2|                 |b0|
    |.            |                 |. |
    |.            | x   |ux|    <=  |. |
    |.            |     |uy|        |. |
    |phiN,1 phiN,2|                 |bN|

    = Au <= b

     */
    private static double PI = Math.PI;
    private double thrusterMaxForce = 50; // Newton
    private double epsilon = thrusterMaxForce / 100d; // 1% error
    private double[][] B;
    private double[][] W;
    private double[][] Q;
    private double[] s;
    private double[] u;
    private double[] u_max;
    private BlockRealMatrix A;
    //private BlockRealMatrix B;
    private ArrayRealVector tau;
    private ArrayRealVector b;
    private PDQuadraticMultivariateRealFunction objectiveFunction;
    private ConvexMultivariateRealFunction[] inequalities;
    private JOptimizer opt;

    // illigal thruster angles contercloskwise start-end pairs
    private double[] constrainVectorM1 = new double[0];//{rad(-20d), rad(-10d), rad(10d), rad(20d)};
    private double[] constrainVectorM2 = new double[0];//{rad(85d), rad(95d)};
    private double[] constrainVectorM3 = new double[0];//{rad(-95), rad(-85)};

    public ThrustAllocUSV() {
        //    x1   y1    x2     y2    x3    y3  slack   q weight
        setup(0d, 1.5d, -.5d, -.5d, .5d, -.5d, 0.001d, 100d);
    }

    private void setup(double Lx1, double Ly1, double Lx2, double Ly2, double Lx3, double Ly3, double slack, double q) {

        // position matrix
        B = new double[][]{
            {1d, 0d, 1d, 0d, 1d, 0d},
            {0d, -1d, 0d, -1d, 0d, -1d},
            {-Lx1, -Ly1, -Lx2, -Ly2, -Lx3, -Ly3}};

        // power weight matrix
        W = new double[][]{
            {1d, 0d, 0d, 0d, 0d, 0d},
            {0d, 1d, 0d, 0d, 0d, 0d},
            {0d, 0d, 1d, 0d, 0d, 0d},
            {0d, 0d, 0d, 1d, 0d, 0d},
            {0d, 0d, 0d, 0d, 1d, 0d},
            {0d, 0d, 0d, 0d, 0d, 1d}
        };

        // slack variable cost matrix (Q >> W > 0)
        Q = new double[][]{
            {q, 0d, 0d, 0d, 0d, 0d},
            {0d, q, 0d, 0d, 0d, 0d},
            {0d, 0d, q, 0d, 0d, 0d},
            {0d, 0d, 0d, q, 0d, 0d},
            {0d, 0d, 0d, 0d, q, 0d},
            {0d, 0d, 0d, 0d, 0d, q}
        };

        // slack vector
        s = new double[Q.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = slack;
        }

        // force vector
        tau = new ArrayRealVector(new double[3]);

        // u variable vector
        u = new double[W.length];

        // create polygon of thruster forces (linear approx)
        Double[][] m1 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM1);
        Double[][] m2 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM2);
        Double[][] m3 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM3);

        List<Double[][]> constrainList = new ArrayList<>();
        constrainList.add(m1);
        constrainList.add(m2);
        constrainList.add(m3);

        // get A matrix
        A = getMatrix_A(constrainList);
        b = getMatrix_B(constrainList);


        // create objective function
        //                                                        S    beta*u_max
        objectiveFunction = new PDQuadraticMultivariateRealFunction(W, null, 0);

        // set equality B*u = tau
        inequalities = new ConvexMultivariateRealFunction[A.getRowDimension()];
        opt = new JOptimizer();
    }

    public double[] calculateOutput(double[] tau) throws Exception {

        for (int i = 0; i < A.getRowDimension(); i++) {
            // hver ulikhet settes opp
            //System.out.println(Arrays.toString(A.getRow(i))+ "   b = " + b.getEntry(i));

            inequalities[i] = new LinearMultivariateRealFunction(A.getRow(i), -b.getEntry(i));
        }

        // optimaliserings problemet
        OptimizationRequest or = new OptimizationRequest();

        // set objektiv funksjon
        or.setF0(objectiveFunction);

        // set ulikhet
        or.setFi(inequalities);

        // set likhet
        or.setA(B);
        // posisjons matrise skal være lik TAU
        or.setB(tau);

        // set tolleranse på resultat. lavere tall = større nøyaktighet
        or.setTolerance(1.E-1);

        // optimalisering
        opt.setOptimizationRequest(or);
        int returnCode = opt.optimize();

        if (returnCode == OptimizationResponse.FAILED) {
            System.out.println("FAILED");
        }

        return opt.getOptimizationResponse().getSolution();

    }

    private Double[][] getConstrainMatrixForAzimuthThruster(double R, double epsilon, double[] constrain) {
        // constains is valid for thruster
        int constrainPairs = 0;
        if (constrain.length % 2 == 0) {
            constrainPairs = constrain.length / 2;
        }

        // N number of polygons required for acciving the epsilon error percent
        Double N = round(PI / acos(1 - (epsilon / R)));
        double r = R * cos(PI / N); // inner radius of thruster region linearized

        Double[][] m = new Double[N.intValue() + constrain.length][3];
        int k;
        for (k = 0;
                k < N;
                k++) {
            double phiK = (2 * k + 1) * PI / N;

            m[k] = new Double[]{cos(phiK), sin(phiK), r};
        }

        // add constrain angles if present
        if (constrainPairs > 0) {
            for (int i = 0; k < m.length && i < constrainPairs; i++) {
                m[k++] = new Double[]{sin(constrain[2 * i]), -cos(constrain[2 * i]), 0d};
                m[k++] = new Double[]{-sin(constrain[2 * i + 1]), cos(constrain[2 * i + 1]), 0d};

            }
        }
        return m;
    }

    private BlockRealMatrix getMatrix_A(List<Double[][]> constrains) {
        int rows = 0;
        int columns = 2;

        for (Double[][] d : constrains) {
            rows = rows + d.length;
        }

        BlockRealMatrix A = new BlockRealMatrix(new double[rows][columns * constrains.size()]);
        int index = 0;
        for (int i = 1;
                i < constrains.size();
                i++) {
            Double[][] m = (constrains.get(i));
            double[][] mm = new double[m.length][columns];

            for (int j = 0; j < mm.length; j++) {
                mm[j] = new double[]{m[j][0], m[j][1]};
            }

            for (int k = 0;
                    k < mm.length;
                    k++) {
                ArrayRealVector arv = new ArrayRealVector(constrains.size() * 2);
                arv.setSubVector((i - 1) * 2, mm[k]);
                A.setRowVector(index, arv);

                //System.out.println("row nr: " + index + " " +Arrays.toString(A.getRow(index)));
                index++;

            }
        }
        return A;
    }

    private ArrayRealVector getMatrix_B(List<Double[][]> constrains) {
        
        int rows = 0;

        for (Double[][] d : constrains) {
            rows = rows + d.length;
        }
        
        ArrayRealVector b = new ArrayRealVector(new double[rows]);
        int i = 0;
        for(Double[][] d : constrains){
            for(Double[] dd : d){
                if(!(dd.length != 3 && i < rows)){
                b.setEntry(i, dd[2]);
                i++;
                } else {
                    throw new IndexOutOfBoundsException("index not valid in getMatrix_B");
                }
            }
        }
        return b;
    }

    private static double[][] zeros(int rows, int columns) {
        return new double[rows][columns];
    }

    private static double[][] ones(int rows, int columns) {
        double[][] result = new double[rows][columns];

        for (double[] result1 : result) {
            for (double v : result1) {
                v = 1d;
            }
        }
        return result;
    }

    private static double[][] minusOnes(int rows, int columns) {
        double[][] result = new double[rows][columns];

        for (double[] result1 : result) {
            for (int j = 0; j < result1.length; j++) {
                result1[j] = -1d;
            }
        }
        return result;
    }

    private static double sin(double v) {
        return Math.sin(v);
    }

    private static double cos(double v) {
        return Math.cos(v);
    }

    private static double acos(double v) {
        return Math.acos(v);
    }

    private static double round(double v) {
        return Math.round(v);
    }

    private static double rad(double v) {
        return (PI / 180d) * v;
    }

    private static double deg(double v) {
        return (180d / PI) * v;
    }
}
