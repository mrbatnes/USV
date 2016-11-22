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
import java.util.Arrays;

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

    private double r; // inner radius of approximation thurster region

    public ThrustAllocUSV() {
        //    x1   y1    x2     y2    x3    y3  slack   q weight
        setup(0d, 1.5d, -.5d, -.5d, .5d, -.5d, 0.001d, 100d);
    }

    private void setup(double Lx1, double Ly1, double Lx2, double Ly2, double Lx3, double Ly3, double slack, double q) {

        // position matrix
        B = new double[][]{
            {1d,  0d,   1d,   0d,   1d,   0d},
            {0d, -1d,   0d,  -1d,   0d,  -1d},
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
            {q,    0d,   0d,   0d,   0d,   0d},
            {0d,   q,    0d,   0d,   0d,   0d},
            {0d,   0d,   q,    0d,   0d,   0d},
            {0d,   0d,   0d,   q,    0d,   0d},
            {0d,   0d,   0d,   0d,   q,    0d},
            {0d,   0d,   0d,   0d,   0d,   q}
        };

        // slack vector
        s = new double[Q.length];
        for(int i = 0; i < s.length; i++){
            s[i] = slack;
        }

        // force vector
        tau = new ArrayRealVector(new double[3]);

        // u variable vector
        u = new double[W.length];

        // thuster force region linearization start******************************

            //TODO: measure max thruster force
            double R = 10d; // thruster max force
            double epsilon = R/100; // max percent error = 1%

            Double N = new Double( Math.round(Math.PI/Math.acos(1-(epsilon/R))));
            r = R*Math.cos(Math.PI/N); // inner radius of thruster region linearized

            // inequality matrix A start **********************************************


            // create polygon of thruster forces (linear approx)
            double[][] m = new double[N.intValue()][2];
            for(int k = 0; k < N;k++){
                double phiK = (2*k+1)*Math.PI/N;
                m[k]= new double[]{Math.cos(phiK),Math.sin(phiK)};
            }
            
            // fill up thuster contstrain matrix with values
            A = new BlockRealMatrix(new double[N.intValue()*(W.length/2)][W.length]);
            int row = A.getRowDimension();
            int column = A.getColumnDimension();
            int index = 0;
            for(int i = 1; i <= W.length/2; i++){
                for(int j = 0; j < N;j++){
                    ArrayRealVector arv = new ArrayRealVector(W.length);
                    arv.setSubVector((i-1)*2, m[j]);
                    A.setRowVector(index, arv);

                    //System.out.println("row nr: " + index + " " +Arrays.toString(A.getRow(index)));
                    index++;
                
                }
            }
            
            // inequality matrix A end ************************************************

            // set b vector values
            b = new ArrayRealVector(new double[row]);
            for(int k = 0; k < b.getDimension(); k++){
                b.setEntry(k, r);
            }

        // thuster force region linearization end ******************************

        // create objective function
                                                                   //  S    beta*u_max
        objectiveFunction = new PDQuadraticMultivariateRealFunction(W, null, 0);

        // set equality B*u = tau


        inequalities = new ConvexMultivariateRealFunction[A.getRowDimension()];
        opt = new JOptimizer();
    }

    public double[] calculateOutput(double[] tau) throws Exception {

        for(int i = 0; i < A.getRowDimension(); i++){
            // hver ulikhet settes opp
            //System.out.println(Arrays.toString(A.getRow(i))+ "   b = " + b.getEntry(i));
            
            inequalities[i] = new LinearMultivariateRealFunction(A.getRow(i),-b.getEntry(i));
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

        if(returnCode == OptimizationResponse.FAILED){
            System.out.println("FAILED");
        }

        return opt.getOptimizationResponse().getSolution();



    }
}
