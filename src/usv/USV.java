/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usv;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars-harald
 */
public class USV {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ThrustAllocUSV t = new ThrustAllocUSV();
        //org.apache.log4j.BasicConfigurator.configure();

        try {
            double[] r = t.calculateOutput(new double[]{100, 70, 0}, false);
            if (r != null) {
                System.out.println(Arrays.toString(r));
            } else {
                System.out.println("error calculating output");
            }

        } catch (Exception ex) {
            Logger.getLogger(USV.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println((System.currentTimeMillis()-start)/1000.);
        
    }

}
