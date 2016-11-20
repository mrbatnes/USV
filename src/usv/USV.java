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
        ThrustAllocator t = new ThrustAllocator();
        
        try {
            double[] r = t.calculateOutput(new double[]{0,0,1});
            double u1 = Math.sqrt(r[0]*r[0]+r[1]*r[1]);
            double u2 = Math.sqrt(r[2]*r[2]+r[3]*r[3]);
            double u3 = Math.sqrt(r[4]*r[4]+r[5]*r[5]);
       
            double phi1 = Math.atan2(r[1],r[0]);
            double phi2 = Math.atan2(r[3],r[2]);
            double phi3 = Math.atan2(r[5],r[4]);
            
            System.out.println(Arrays.toString(r));
            System.out.println(" u1: " + u1 +" u2: " + u2 +" u3: " + u3);
            System.out.println(" phi1: " + phi1*360./(2*Math.PI) +" phi2: " + phi2*360./(2*Math.PI) +" phi3: " + phi3*360./(2*Math.PI));
        } catch (Exception ex) {
            Logger.getLogger(USV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
  
    }
    

    
}
