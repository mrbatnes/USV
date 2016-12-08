/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

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
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

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
    private BlockRealMatrix P; // W and Q matrix together
    private double[][] B;
    private double[][] W;
    private double[][] Q;
    private double[] s;
    private double[] u;
    private double[] u_max;
    private double[] beta;

    private double Lx1;
    private double Ly1;
    private double Lx2;
    private double Ly2;
    private double Lx3;
    private double Ly3;

    private BlockRealMatrix A;
    private ArrayRealVector b;
    private PDQuadraticMultivariateRealFunction objectiveFunction;
    private ConvexMultivariateRealFunction[] inequalities;
    private JOptimizer opt;

    // illigal thruster angles contercloskwise start-end pairs
    private double[] constrainVectorM1 = new double[0];//{/*rad(-85d), rad(-95d), */rad(85d), rad(95d)};
    private double[] constrainVectorM2 = new double[0];//{rad(85d), rad(95d)};
    private double[] constrainVectorM3 = new double[0];//{rad(-95), rad(-85)};

    public ThrustAllocUSV() {
        //    x1   y1    x2     y2    x3    y3  slack   q weight
        setup(1.4, .34,  1.04,  .4,   1.04, .4, new double[]{100d, 100d, 100d});
    }

    private void setup(double Lx1, double Ly1, double Lx2, double Ly2, double Lx3, double Ly3, double[] q) {

        this.Lx1 = Lx1;
        this.Ly1 = Ly1;
        this.Lx2 = Lx2;
        this.Ly2 = Ly2;
        this.Lx3 = Lx3;
        this.Ly3 = Ly3;

        // initial position matrix
        B = getConfigurationMatrix_B(this.Lx1, this.Ly1, this.Lx2, this.Ly2, this.Lx3, this.Ly3, false);

        // power weight matrix (identity matrix because identical motors)
        W = MatrixUtils.createRealIdentityMatrix(6).getData();

        // slack variable cost matrix (Q >> W > 0)
        Q = MatrixUtils.createRealDiagonalMatrix(q).getData();

        P = new BlockRealMatrix(new double[9][9]);
        P.setSubMatrix(W, 0, 0);
        P.setSubMatrix(Q, 6, 6);

        // create polygon of thruster forces (linear approx)
        double[][] m1 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM1);
        double[][] m2 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM2);
        double[][] m3 = getConstrainMatrixForAzimuthThruster(thrusterMaxForce, epsilon, constrainVectorM3);

        List<double[][]> constrainList = new ArrayList<>();
        constrainList.add(m1);
        constrainList.add(m2);
        constrainList.add(m3);

        // get A matrix
        // unequalities A*u <= b
        A = getMatrix_A(constrainList);

        b = getConstrainVector_b(constrainList);

        beta = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1};

        // create objective function
        //                                                             beta*u_max
        objectiveFunction = new PDQuadraticMultivariateRealFunction(P.getData(), null, 0);

        // set equality B*u = tau
        inequalities = new ConvexMultivariateRealFunction[A.getRowDimension()];
        opt = new JOptimizer();
    }

    public double[] calculateOutput(double[] tau, boolean travelMode) {

        //  set the configuration matrix from travelmode or not
        B = getConfigurationMatrix_B(Lx1, Ly1, Lx2, Ly2, Lx3, Ly3, travelMode);

        for (int i = 0; i < A.getRowDimension(); i++) {
            // hver ulikhet settes opp
            inequalities[i] = new LinearMultivariateRealFunction(A.getRow(i), -b.getEntry(i));
        }

        // optimaliserings problemet
        OptimizationRequest or = new OptimizationRequest();

        // set objektiv funksjon
        or.setF0(objectiveFunction);

        // set ulikhet
        or.setFi(inequalities);

        // set likhet B*u = tau
        or.setA(B);
        // posisjons matrise skal være lik TAU
        or.setB(tau);

        // set tolleranse på resultat. lavere tall = større nøyaktighet
        or.setTolerance(1.E-1);

        // optimalisering
        opt.setOptimizationRequest(or);
        int returnCode = 0;

        try {
            returnCode = opt.optimize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (returnCode == OptimizationResponse.FAILED) {
            System.out.println("FAILED");
            return null;
        } else {
            return this.opt.getOptimizationResponse().getSolution();
        }

    }

    private double[][] getConstrainMatrixForAzimuthThruster(double R, double epsilon, double[] constrain) {
        // constains is valid for thruster
        int constrainPairs = 0;
        int constIndex = 0;
        if (constrain.length % 2 == 0) {
            constrainPairs = constrain.length / 2;
        }

        // N number of polygons required for acciving the epsilon error percent
        double N = round(PI / acos(1 - (epsilon / R)));
        double r = R * cos(PI / N); // inner radius of thruster region linearized

        double[][] m = new double[(int) N + constrain.length][3];
        int k;
        for (k = 0;
                k < N;
                k++) {
            double phiK = (2 * k + 1) * PI / N;

            m[k] = new double[]{cos(phiK), sin(phiK), r};

        }
        /*
        // add constrain angles if present
        if (constrainPairs > 0) {
            for (int i = 0; k < m.length && i < constrainPairs; i++) {
                m[k++] = new double[]{sin(constrain[2 * i]), -cos(constrain[2 * i]), 0d};
                m[k++] = new double[]{-sin(constrain[2 * i + 1]), cos(constrain[2 * i + 1]), 0d};

            }
        }*/
        return m;
    }

    /**
     * get the unequalities matrix A "A*u <= b"
     *
     * @param constrains the constrains matrix for each thruster in a list
     * @return constrainMatrix A
     */
    private BlockRealMatrix getMatrix_A(List<double[][]> constrains) {

        int columns = 3;
        int columnsConstraint = 2 * constrains.size();
        int rows = 0; // initial 12 rows

        // add rows for unequalities / constains
        for (double[][] d : constrains) {
            rows = rows + d.length;
        }

        // dynamic column size
        columns = columns + columnsConstraint;
        BlockRealMatrix A = new BlockRealMatrix(new double[rows][columns]);

        // iterate trough all constrain matrixes
        int index = 0;
        for (int i = 0;
                i < constrains.size();
                i++) {
            // get constrain for each thruster
            BlockRealMatrix C = new BlockRealMatrix(constrains.get(i));
            C = C.getSubMatrix(0, (C.getRowDimension() - 1), 0, 1);
            index = index + C.getRowDimension();
            // set all constraints in a matrix
            A.setSubMatrix(C.getData(), i * C.getRowDimension(), 2 * i);
        }

        /*
        total matrix A in this format:
        
        | A  0   0|   |u|    |b|
        |-I  0  -1| * |s| <= |0|
        | I  0  -1|   |ü|    |0|
        
         
        RealMatrix I_6x6 = MatrixUtils.createRealIdentityMatrix(6); // identity 6x6
        RealMatrix minus_I_6x6 = MatrixUtils.createRealIdentityMatrix(6).scalarMultiply(-1d); // identity 6x6 * -1

        A.setSubMatrix(minus_I_6x6.getData(), index, 0);
        A.setSubMatrix(matrix(12, 3, -1d), index, columnsConstraint + 3);
        index = index + minus_I_6x6.getRowDimension();
        A.setSubMatrix(I_6x6.getData(), index, 0);

        /* print result
        for (int i = 0; i < A.getRowDimension(); i++) {
            System.out.println("row nr: " + i + " " + Arrays.toString(A.getRow(i)));
        }
         */
        return A;
    }

    /**
     * get configuration matrix B. configuration of thrusters from senter of
     * gravity and the effection of vektor tau
     *
     * @param Lx1
     * @param Ly1
     * @param Lx2
     * @param Ly2
     * @param Lx3
     * @param Ly3
     * @param isInTravelMode exclude the front thruster if in travelmode
     * @return
     */
    private double[][] getConfigurationMatrix_B(double Lx1, double Ly1, double Lx2, double Ly2, double Lx3, double Ly3, boolean isInTravelMode) {
        if (isInTravelMode) {
            return new double[][]{
                { 0d,   0d,     1d,  0d,     1d,   0d,  -1d,  0d,  0d},
                {  0d, 0d,    0d,  -1d,     0d,  -1d,   0d, -1d,  0d},
                {0d,  0d,  Ly2,  Lx2,   -Ly3,  Lx3,   0d,  0d, -1d}};
        } else {
            return new double[][]{
                { 1d,   0d,     1d,  0d,     1d,   0d,  -1d,  0d,  0d},
                {  0d, -1d,    0d,  -1d,     0d,  -1d,   0d, -1d,  0d},
                {Ly1,  -Lx1,  Ly2,  Lx2,   -Ly3,  Lx3,   0d,  0d, -1d}};
        }
    }

    /**
     * get constrain vector b from unequality A*u <= b
     *
     * @param constrains all the unequalities from thrusters in a list
     * @return
     */
    private ArrayRealVector getConstrainVector_b(List<double[][]> constrains) {

        int rows = 0; // initial 12 rows

        for (double[][] d : constrains) {
            rows = rows + d.length;
        }

        ArrayRealVector b = new ArrayRealVector(new double[rows]);
        int i = 0;
        for (double[][] d : constrains) {
            for (double[] dd : d) {
                if (!(dd.length != 3 && i < rows)) {
                    b.setEntry(i, dd[2]);
                    i++;
                } else {
                    throw new IndexOutOfBoundsException("index not valid in getMatrix_B");
                }
            }
        }
        return b;
    }

    /**
     * get thust forces and angles from a vector
     *
     * @param r input vector length 6
     * @return force - angle pairs of thruster in radians
     */
    private double[] getThrustAndAnglesRad(double[] r) {
        double u1 = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
        double u2 = Math.sqrt(r[2] * r[2] + r[3] * r[3]);
        double u3 = Math.sqrt(r[4] * r[4] + r[5] * r[5]);

        double phi1 = Math.atan2(r[1], r[0]);
        double phi2 = Math.atan2(r[3], r[2]);
        double phi3 = Math.atan2(r[5], r[4]);

        return new double[]{u1, phi1, u2, phi2, u3, phi3};
    }

    /**
     * get thust forces and angles from a vector
     *
     * @param r input vector length 6
     * @return force - angle pairs of thruster in degrees
     */
    private double[] getThrustAndAnglesDeg(double[] r, boolean travelMode) {
        boolean zero = true;
        int x = 0;
        for(double d : r){
            System.out.println("R" + x + " = " + d);
            if(d > 1E-10){
                zero = false;
            }
        }
        if(zero){
            return new double[]{0, 0, 0, 0, 0, 0};
        }
        double u1 = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
        double u2 = Math.sqrt(r[2] * r[2] + r[3] * r[3]);
        double u3 = Math.sqrt(r[4] * r[4] + r[5] * r[5]);

        double phi1 = Math.atan2(r[1], r[0]);
        double phi2 = Math.atan2(r[3], r[2]);
        double phi3 = Math.atan2(r[5], r[4]);
        if (travelMode) {
            return new double[]{u1, 0d, u2, deg(phi2), u3, deg(phi3)};
        } else {
            return new double[]{u1, deg(phi1), u2, deg(phi2), u3, deg(phi3)};
        }

    }

    private static double[][] matrix(int rows, int columns, double value) {
        double[][] result = new double[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result[i][j] = value;
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
