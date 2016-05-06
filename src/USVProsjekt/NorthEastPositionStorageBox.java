/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

/**
 *
 * @author vegard
 */
public class NorthEastPositionStorageBox {
    private double[] position;
    private boolean newPosition;
    
    public NorthEastPositionStorageBox() {
        position = new double[2];
        newPosition = true;
    }
    
    public synchronized void setPosition(double[] position) {
        this.position = position;
        newPosition = true;
    }
    
    public synchronized double[] getPosition() {
        newPosition = false;
        return position;
    }
    
    public synchronized boolean isNewPosition() {
        return newPosition;
    }
}
