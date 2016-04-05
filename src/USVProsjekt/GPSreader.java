package USVProsjekt;

import USVProsjekt.NMEAparser.GPSPosition;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requires an Adafruit Ultimate GPS device connected to an Arduino Uno.
 *
 * @author Bachelor USV
 */
public class GPSreader extends Thread {

    private NMEAparser nmea;
    private NEDtransform gpsProc;

    private float latBody;
    private float lonBody;
    private float latReference;
    private float lonReference;

    private float xNorth;
    private float yEast;

    private Identifier ID;

    private final SerialConnection serialConnection;
    private int initPeriod;
    private boolean dynamicPositioning;
    private boolean stop;

    private PrintWriter writer;

    /**
     *
     *
     * @param serialConnection
     * @param ID
     */
    public GPSreader(SerialConnection serialConnection, Identifier ID) {
        this.serialConnection = serialConnection;

        nmea = new NMEAparser();
        initPeriod = 0;
        gpsProc = new NEDtransform();

        latBody = 0.0f;
        lonBody = 0.0f;

        xNorth = 0.0f;
        yEast = 0.0f;
        this.ID = ID;
        stop = false;
        try {
            writer = new PrintWriter("GPS_Data_NMEA.txt", "UTF-8");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GPSreader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(GPSreader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String line;
        String[] lineData;
        while (serialConnection.isConnected() && !stop) {
            setReference();
            line = serialConnection.getSerialLine();
            lineData = line.split("\r\n");
            String NMEA1 = lineData[0];
            String NMEA2 = lineData[1];
            nmea.parse(NMEA1);
            nmea.parse(NMEA2);
            latBody = (float) ((nmea.position.lat) * (Math.PI) / 180.0f);
            lonBody = (float) (nmea.position.lon * Math.PI / 180.0f);
            float[] xyNorthEast = gpsProc.getFlatEarthCoordinates(latBody,
                    lonBody, latReference, lonReference);
            xNorth=xyNorthEast[0];
            yEast=xyNorthEast[1];
            
            writer.println(NMEA1);
            writer.println(NMEA2);
            writer.println("");
        }
        System.out.println("Connection lost/closed on Thread: "
                + this.getName());
        serialConnection.close();
        writer.close();
    }

    public void connectToSerialPortAndDisplayGPSInfo() {
        serialConnection.connectAndListen(ID);

    }

    /**
     * GPSposition returns useful GPS data like latitude, longitude, speed, fix
     * etc.
     *
     * @return
     */
    public GPSPosition getGPSPosition() {

        return nmea.position;
    }

    private void setReference() {
        while (!dynamicPositioning && !stop) {
            writer.close();
            //init period for å forhindre å parse korrupte data
            while (initPeriod < 10 && serialConnection.isConnected()) {
                serialConnection.getSerialLine();
                initPeriod++;
            }
            String line = serialConnection.getSerialLine();
            String[] lineData = line.split("\r\n");
            if (lineData[0].startsWith("$") && lineData[1].startsWith("$")) {
                String NMEA1 = lineData[0];
                String NMEA2 = lineData[1];
                nmea.parse(NMEA1);
                nmea.parse(NMEA2);
            }
            latReference = (nmea.position.lat * (float) Math.PI / 180.0f);
            lonReference = (nmea.position.lon * (float) Math.PI / 180.0f);
        }
    }

    public void lockReferencePosition() {
        dynamicPositioning = true;
    }

    public float getXposition() {
        return xNorth;
    }

    public float getYposition() {
        return yEast;
    }

    public float getLatRef() {
        return (float) (latReference * (180.0f / Math.PI));
    }

    float getLonRef() {
        return (float) (lonReference * (180.0f / Math.PI));
    }

    void stopThread() {
        stop = true;
    }

    void setReferencePositionOff() {
        dynamicPositioning = false;
    }

}
