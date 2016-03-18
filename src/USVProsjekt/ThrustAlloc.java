/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.linear.*;

/**
 *
 * @author vegard Klasse for thrusterallokering. Løser problemet med å finne U,
 * gitt en kraftvektor tau, med minste kvadraters metode Minimert med tanke på
 * effektforbruk Manuell metningssjekk
 */
public class ThrustAlloc {

    private BlockRealMatrix T;
    private BlockRealMatrix H;
    private RealVector U;
    private RealVector Umod;
    private RealVector tau;

    private int counter;

    public ThrustAlloc(double Lx1, double Lx2, double Ly1, double Ly2) {
        T = new BlockRealMatrix(3, 4);
        H = setUpMatricesAndPropulsors(Lx1, Lx2, Ly1, Ly2);
        Umod = new ArrayRealVector();
        counter = 1;
    }

    private BlockRealMatrix setUpMatricesAndPropulsors(double Lx1, double Lx2, double Ly1, double Ly2) {

        T.addToEntry(0, 0, 1);
        T.addToEntry(0, 1, 1);
        T.addToEntry(1, 2, 1);
        T.addToEntry(1, 3, 1);
        T.addToEntry(2, 0, -Ly1);
        T.addToEntry(2, 1, -Ly2);
        T.addToEntry(2, 2, Lx1);
        T.addToEntry(2, 3, Lx2);
        return H = T.transpose().multiply(MatrixUtils.inverse(T.multiply(T.transpose())));
    }

    public void calculateOutput(RealVector tau) {
        counter = 1;
        this.tau = tau;
        U = H.operate(tau);
        checkIfValid(U, T);
        printValues(U);
    }

    private void checkIfValid(RealVector U, BlockRealMatrix A) {
        for (int i = 1; i <= U.getDimension(); i++) {
            if (U.getEntry(i - 1) > 35.0d) {
//                Iterator it = propulsorList.iterator();
//                while (it.hasNext()) {
//                    Propulsor p = (Propulsor) it.next();
//                    if (p.getIndex() == i) {
//                        p.setValue(35.0d);
//                        propulsorListUpdated.add(p);
//                        it.remove();
//
//                    }
//                }
                recalculateTau(i, 35.0d, A);
                break;
            }
//            if (U.getEntry(i) < -8) {
//                Umod.addToEntry(i, -35.0d);
//                Iterator it = propulsorList.iterator();
//                while (it.hasNext()) {
//                    Propulsor p = (Propulsor) it.next();
//                    if (p.getIndex() == i) {
//                        p.setValue(-35.0d);
//                        propulsorListUpdated.add(p);
//                        it.remove();
//                    }
//                }
//                recalculateTau(i, -35.0d, A);
//                break;
//            } else {
//                Iterator it = propulsorList.iterator();
//                while (it.hasNext()) {
//                    Propulsor p = (Propulsor) it.next();
//                    if (p.getIndex() == i) {
//                        p.setValue(U.getEntry(i));
//                    }
//
//                }
//            }
        }
//        for(Propulsor p:propulsorList) {
//            propulsorListUpdated.add(p);
//        }
    }

    private void recalculateTau(int i, double d, BlockRealMatrix B) {

        RealVector A = new ArrayRealVector();
        A = T.getColumnVector(i);
        tau = tau.subtract(A.mapMultiplyToSelf(d));
        recalculateOutput(i, tau, B);

    }

    private void printValues(RealVector U) {
        System.out.println("Values in U: " + "U1: " + U.getEntry(0) + " U2: " + U.getEntry(1) + " U3: " + U.getEntry(2) + " U4: " + U.getEntry(3));
    }

    private void recalculateOutput(int i, RealVector tau, BlockRealMatrix C) {
        BlockRealMatrix Tnew = null;
        BlockRealMatrix T1;
        BlockRealMatrix T2;
        if (counter == 1) {
            if (i != 0) {
                T1 = C.getSubMatrix(0, 2, 0, i - 1);
                T2 = C.getSubMatrix(0, 2, i + 1, 3);
                double[][] m1 = T1.getData();
                double[][] m2 = T2.getData();
                double m[][] = (double[][]) ArrayUtils.addAll(m1, m2);
                Tnew = new BlockRealMatrix(m);
            } else {
                Tnew = C.getSubMatrix(0, 2, 1, 3);
            }

        }
        if (counter == 2) {
            if (i != 0) {
                T1 = C.getSubMatrix(0, 2, 0, i - 1);
                T2 = C.getSubMatrix(0, 2, i + 1, 2);
                double[][] m1 = T1.getData();
                double[][] m2 = T2.getData();
                double m[][] = (double[][]) ArrayUtils.addAll(m1, m2);
                Tnew = new BlockRealMatrix(m);
            } else {
                Tnew = C.getSubMatrix(0, 2, 1, 2);
            }
        }
        if (counter == 3) {
            if (i != 0) {
                Tnew = C.getSubMatrix(0, 2, 0, 0);
            } else {
                Tnew = C.getSubMatrix(0, 2, 1, 1);
            }
        }
        if (counter == 4) {
            Tnew = C.copy();
        }

        BlockRealMatrix Hnew = Tnew.transpose().multiply(MatrixUtils.inverse(Tnew.multiply(Tnew.transpose())));
        U = Hnew.operate(tau);
        counter++;
        checkIfValid(U, Tnew);
    }
}
