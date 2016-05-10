package USVProsjekt;

import USVProsjekt.NMEAparser.GPSPosition;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Requires an Adafruit Ultimate GPS device connected to an Arduino Uno.
 *
 * @author Bachelor USV
 */
public class GPSreader extends Thread {

    private NMEAparser nmea;
    private NEDtransform gpsProc;

    private double latBody;
    private double lonBody;
    private double latReference;
    private double lonReference;

    private double xNorth;
    private double yEast;

    private Identifier ID;

    private final SerialConnection serialConnection;
    private int initPeriod;
    private boolean dynamicPositioning;
    private boolean stop;

    private GPSPositionStorageBox gpsStorage;
    private NorthEastPositionStorageBox northEastStorage;

    private PrintWriter nmeaWriter;

    private boolean writerStarted;

    /**
     *
     *
     * @param serialConnection
     * @param ID
     * @param northEastStorage
     */
    public GPSreader(SerialConnection serialConnection, Identifier ID,
            NorthEastPositionStorageBox northEastStorage) {
        this.serialConnection = serialConnection;
        this.northEastStorage = northEastStorage;
        nmea = new NMEAparser();
        initPeriod = 0;
        gpsProc = new NEDtransform();

        latBody = 0.0f;
        lonBody = 0.0f;

        xNorth = 0.0f;
        yEast = 0.0f;
        this.ID = ID;
        stop = false;

    }

    @Override
    public void run() {
        String line;
        String[] lineData;
        while (serialConnection.isConnected() && !stop) {
            setReference();
            if (!writerStarted) {
                try {
                    File log = new File("NMEAData.txt");
                    nmeaWriter = new PrintWriter(new FileWriter(log, true));
                } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                } catch (IOException ex) {
                    System.out.println("IO");
                }
                writerStarted = true;
            }
            line = serialConnection.getSerialLine();
            lineData = line.split("\r\n");
            if (lineData[0].startsWith("$") && lineData[1].startsWith("$") && checkEmpty(lineData[0])) {
                String NMEA1 = lineData[0];
                String NMEA2 = lineData[1];
                nmea.parse(NMEA1);
                nmea.parse(NMEA2);
                nmeaWriter.println(NMEA1);
                nmeaWriter.println(NMEA2);
                nmeaWriter.println(" ");
                latBody = (nmea.position.lat * (Math.PI) / 180.0);
                lonBody = (nmea.position.lon * (Math.PI) / 180.0);
                // Store gps coordinates
                gpsStorage.setPosition(nmea.position);
            }

            double[] xyNorthEast = gpsProc.getFlatEarthCoordinates(latBody,
                    lonBody, latReference, lonReference);
            // store N-E position relative to reference
            northEastStorage.setPosition(xyNorthEast);

            System.out.println("----------------------------------------------");
            System.out.println("X position: " + xyNorthEast[0]);
            System.out.println("Y position: " + xyNorthEast[1]);
            System.out.println("----------------------------------------------");

        }
        System.out.println("Connection lost/closed on Thread: "
                + this.getName());
        serialConnection.close();
        if (nmeaWriter != null) {
            nmeaWriter.close();
        }
    }

    public void connectToSerialPortAndDisplayGPSInfo() {
        serialConnection.connectAndListen(ID);

    }

    private void setReference() {
        while (!dynamicPositioning && !stop) {
            if (writerStarted) {
                nmeaWriter.close();
                writerStarted = false;
            }
            //init period for å forhindre å parse korrupte data
            while (initPeriod < 10 && serialConnection.isConnected()) {
                serialConnection.getSerialLine();
                initPeriod++;
            }
            String line = serialConnection.getSerialLine();
            String[] lineData = line.split("\r\n");
            if (lineData[0].startsWith("$") && lineData[1].startsWith("$") && checkEmpty(lineData[0])) {
                String NMEA1 = lineData[0];
                String NMEA2 = lineData[1];
                nmea.parse(NMEA1);
                nmea.parse(NMEA2);
                latReference = (nmea.position.lat * Math.PI / 180.0);
                lonReference = (nmea.position.lon * Math.PI / 180.0);
                gpsStorage.setPosition(nmea.position);
            }

        }
    }

    private boolean checkEmpty(String lineData) {
        String[] checkData = lineData.split(",");
        for (int i = 0; i < 6; i++) {
            if (checkData[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void lockReferencePosition() {
        dynamicPositioning = true;
    }

    public double getLatRef() {
        return (latReference * (180.0 / Math.PI));
    }

    public void setStorageBox(GPSPositionStorageBox storage) {
        this.gpsStorage = storage;
        gpsStorage.setPosition(nmea.position);
    }

    public double getLonRef() {
        return (lonReference * (180.0 / Math.PI));
    }

    void stopThread() {
        stop = true;
    }

    void setReferencePositionOff() {
        dynamicPositioning = false;
    }

}
