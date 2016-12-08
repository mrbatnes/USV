package USVProsjekt;

import USVProsjekt.NMEAparser.GPSPosition;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import de.taimos.gpsd4java.api.ObjectListener;
import de.taimos.gpsd4java.backend.GPSdEndpoint;
import de.taimos.gpsd4java.backend.ResultParser;
import de.taimos.gpsd4java.types.ATTObject;
import de.taimos.gpsd4java.types.DeviceObject;
import de.taimos.gpsd4java.types.DevicesObject;
import de.taimos.gpsd4java.types.SATObject;
import de.taimos.gpsd4java.types.SKYObject;
import de.taimos.gpsd4java.types.TPVObject;
import de.taimos.gpsd4java.types.subframes.SUBFRAMEObject;
import java.sql.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.marineapi.nmea.util.Time;
import org.json.JSONException;

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

    private int initPeriod;
    private boolean dynamicPositioning;
    private boolean stop;

    private GPSPositionStorageBox gpsStorage;
    private NorthEastPositionStorageBox northEastStorage;
    private GPSCurrentPositionStorageBox currentPositionStorageBox;

    private NMEASentenceGenerator NMEAsentenceGenerator;

    private GPSdEndpoint GPSdEndpoint;
    private String host = "localhost";
    private int port = 2947;

    private Scanner scan;
    private boolean simulate = false;

    private PrintWriter nmeaWriter;

    private boolean writerStarted;
    private boolean timerStarted;

    /**
     *
     *
     * @param serialConnection
     * @param ID
     * @param currentPositionStorageBox
     * @param northEastStorage
     */
    public GPSreader(Identifier ID,
            NorthEastPositionStorageBox northEastStorage, GPSCurrentPositionStorageBox currentPositionStorageBox) {
        try {
            this.northEastStorage = northEastStorage;
            this.currentPositionStorageBox = currentPositionStorageBox;
            nmea = new NMEAparser();
            initPeriod = 0;
            gpsProc = new NEDtransform();

            latBody = 0.0f;
            lonBody = 0.0f;

            xNorth = 0.0f;
            yEast = 0.0f;
            this.ID = ID;
            stop = false;

            GPSdEndpoint = new GPSdEndpoint(host, port, new ResultParser());
            NMEAsentenceGenerator = new NMEASentenceGenerator();

            if (simulate) {
                try {
                    scan = new Scanner(new File("route.txt"));
                } catch (FileNotFoundException e) {
                    System.err.println(e.toString());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GPSreader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        
        
        while (simulate) {
            if (!timerStarted) {
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setReference();
                    }
                }, 0, 500);
                timerStarted = true;
            }
        }

        while (GPSdEndpoint != null && !stop && !simulate) {
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

            if (currentPositionStorageBox.isNewPosition()) {

                nmea.position = currentPositionStorageBox.getPosition();

                Time t = new Time();
                t.setTime(new Date(((long) nmea.position.time)));
                System.out.println("GPS pos = " + nmea.position.lat + " lat " + nmea.position.lon + " lon");
                nmeaWriter.println(NMEAsentenceGenerator.generateGPGGASentence(
                        nmea.position.lat,
                        nmea.position.lon,
                        t));

                nmeaWriter.println(NMEAsentenceGenerator.generateGPRMCSentence(
                        nmea.position.lat,
                        nmea.position.lon,
                        t));
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
        if (nmeaWriter != null) {
            nmeaWriter.close();
        }
    }

    private void setReference() {
        if (simulate) {
            if (scan.hasNextLine()) {
                String NMEA1 = scan.nextLine();
                String NMEA2 = scan.nextLine();
                nmea.parse(NMEA1);
                nmea.parse(NMEA2);
                latReference = (nmea.position.lat * Math.PI / 180.0);
                lonReference = (nmea.position.lon * Math.PI / 180.0);
                gpsStorage.setPosition(nmea.position);
            } else {
                try {
                    scan = new Scanner(new File("route.txt"));
                } catch (FileNotFoundException e) {
                    System.err.println(e.toString());
                }
                System.out.println("reset scanner");
            }

        } else {
            while (!dynamicPositioning && !stop) {
                if (writerStarted) {
                    nmeaWriter.close();
                    writerStarted = false;
                }

                if (currentPositionStorageBox.isNewPosition()) {
                    nmea.position = currentPositionStorageBox.getPosition();
                    latReference = (nmea.position.lat * Math.PI / 180.0);
                    lonReference = (nmea.position.lon * Math.PI / 180.0);
                    gpsStorage.setPosition(nmea.position);
                }
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

    public void stopThread() {
        stop = true;
    }

    public void setReferencePositionOff() {
        dynamicPositioning = false;
    }

    public void startGpsReader() {

        GPSdEndpoint.addListener(new ObjectListener() {

            @Override
            public void handleTPV(final TPVObject tpv) {
                NMEAparser nmea = new NMEAparser();
                
                nmea.position.altitude = tpv.getAltitude();
                nmea.position.dir = tpv.getCourse();
                nmea.position.lat = tpv.getLatitude();
                nmea.position.lon = tpv.getLongitude();
                nmea.position.quality = 0;
                nmea.position.velocity = tpv.getSpeed();
                nmea.position.time = tpv.getTimestamp();
                            
                currentPositionStorageBox.setPosition(nmea.position);

            }

        });

        GPSdEndpoint.start();
        try {
            GPSdEndpoint.watch(true, true);
        } catch (IOException ex) {
            Logger.getLogger(GPSreader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(GPSreader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
