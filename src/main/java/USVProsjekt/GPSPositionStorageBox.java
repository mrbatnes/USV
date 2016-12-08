/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import USVProsjekt.NMEAparser.GPSPosition;

/**
 *
 * @author vegard
 */
public class GPSPositionStorageBox {
    private GPSPosition position;
    private boolean newPosition;
    
    public GPSPositionStorageBox() {
        newPosition = true;
    }
    
    public synchronized GPSPosition getPosition() {
        newPosition = false;
        return position;
    }
    
    public synchronized void setPosition(GPSPosition position) {
        this.position = position;
        newPosition = true;
    }
    
    public synchronized boolean isNewPosition() {
        return newPosition;
    }
    
    
}
