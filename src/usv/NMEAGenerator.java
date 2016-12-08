/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usv;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author RobinBergseth
 */
public class NMEAGenerator {
    
    private PrintWriter out; 
            
    public NMEAGenerator(){
        try {
            out = new PrintWriter("genRoute.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.err.println(e.toString());
        }
        int incA = 0;
        for(int i = 0; i < 5; i++){
            for(int x = 0; x < 10; x++){
                out.println("$GPGGA,085954.775,6228.236,N,00614.6" + i + x + ",E,7,,,0.0,M,,,,*2C");
                out.println("$GPRMC,085954.775,,6228.236,N,00614.6" + i + x + ",E,,,071216,,,*66");
            }
        }
        out.close();
    }
    
    
    public static void main(String[] args) {
        new NMEAGenerator();
    }
}
