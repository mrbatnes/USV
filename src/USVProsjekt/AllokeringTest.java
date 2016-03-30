/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import org.apache.commons.math3.linear.*;

/**
 *
 * @author vegard
 */
public class AllokeringTest {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) throws Exception {
        ThrustAllocator t = new ThrustAllocator(-1.5, 1.2, -0.5, 0.5);
        double[] a1 = new double[]{1., 1., 1.5,};
        double[] a2 = new double[]{3., 2.5, 0.5};
        double[] a3 = new double[]{5., 5., 0.};
        double[] a4 = new double[]{100., 200., 5.};
        double[] a5 = new double[]{1000., 0., 50.};

        double[] result1 = t.calculateOutput(a1);
        System.out.println("Result 1: " + result1[0] + " " + result1[1] + " " + result1[2] + " " + result1[3]);
        System.out.println(result1[6]);
        double[] result2 = t.calculateOutput(a2);
        System.out.println("Result 2: " + result2[0] + " " + result2[1] + " " + result2[2] + " " + result2[3]);

        double[] result3 = t.calculateOutput(a3);
        System.out.println("Result 3: " + result3[0] + " " + result3[1] + " " + result3[2] + " " + result3[3]);

        double[] result4 = t.calculateOutput(a4);
        System.out.println("Result 4: " + result4[0] + " " + result4[1] + " " + result4[2] + " " + result4[3]);
        
        double[] result5 = t.calculateOutput(a5);
        System.out.println("Result 5: " + result5[0] + " " + result5[1] + " " + result5[2] + " " + result5[3]);
    }

}
