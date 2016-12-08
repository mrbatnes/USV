/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import USVProsjekt.NMEAparser.GPSPosition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author odroid
 */
public class GPSCurrentPositionStorageBox {

    private NMEAparser.GPSPosition position;
    private boolean newPosition = false;

    public GPSCurrentPositionStorageBox() {
    }

    public synchronized GPSPosition getPosition() {
        newPosition = false;
        return position;
    }

    public synchronized void setPosition(GPSPosition position) {
        if (!newPosition) {
            this.position = position;
            newPosition = true;
        }

    }

    public synchronized boolean isNewPosition() {
        return newPosition;
    }
}
