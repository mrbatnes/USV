/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

/**
 *
 * @author robinbv
 */
public class RotationWriter {
    private int rotation1;
    private int rotation2;
    private int rotation3;
    
    private SerialConnection serialConnection;
    private Identifier ID;
    
    public RotationWriter(SerialConnection serialConnection, Identifier ID){
        rotation1 = 0;
        rotation2 = 0;
        rotation3 = 0;
        
        this.serialConnection = serialConnection;
        
        this.ID = ID;
        this.serialConnection.connect(this.ID);     
    }
    
    public void writeRotation(){
        if(serialConnection.isConnected()){
            serialConnection.writeRotationPos(rotation1, rotation2, rotation3);
        }
    }
    
    public void setRotationForAll(double[] r){
        int[] rot = {0, 0, 0};
        int i = 0;
        for(int x = 0; x < 3; x++, i += 2){
            double phi = Math.atan2(r[i+1],r[i]);
            rot[x] = (int) (phi * 360./(2*Math.PI));
        }
        rotation1 = rot[0];
        rotation2 = rot[1];
        rotation3 = rot[2];
    }
    
    public void closeSerialConn(){
        serialConnection.close();
        System.out.println("RotationWriter: Connection closed");
    }
}
