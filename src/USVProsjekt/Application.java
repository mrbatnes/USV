package USVProsjekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Albert
 */
public class Application implements Runnable {

    private SerialConnection serialGPS;
    private SerialConnection serialIMU;
    private SerialConnection serialWind;
    private SerialConnection serialThrust;

    private GPSreader gps;
    private IMUreader imu;
    private WindReader windReader;
    private ThrustWriter thrustWriter;

    private float latitudeBody;
    private float longitudeBody;

    private float latitudeReference;
    private float longitudeReference;

    private float surge;
    private float sway;
    private float yaw, heading,headingReference;

    private float speed;
    private float direction;

    private float windSpeed;
    private float windDirection;
    private float temperature;

    private Socket csocket;

    private BufferedReader inFromServer;
    private PrintStream printStream;
    private int guiCommand;

    private Regulator pidSurge;
    private Regulator pidSway;
    private Regulator pidHeading;

    public Application(Socket csocket) {
        surge = 0.0f;
        sway = 0.0f;
        yaw = 0.0f;
        heading = 0.0f;
        speed = 0.0f;
        direction = 0.0f;
        windSpeed = 0.0f;
        windDirection = 0.0f;
        temperature = 0.0f;
        latitudeBody = 0.0f;
        longitudeBody = 0.0f;
        latitudeReference = 0.0f;
        longitudeReference = 0.0f;
        this.csocket = csocket;
        guiCommand = 0;

        initialisePIDs();
    }

    @Override
    public void run() {

        try {

            printStream = new PrintStream(csocket.getOutputStream(), true);
            BufferedReader r;

            while (guiCommand != 3) {
                r = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
                String line = r.readLine();
                String[] lineData = null;
                if (!line.isEmpty()) {
                    lineData = line.split(" ");
                    guiCommand = Integer.parseInt(lineData[0]);
                    headingReference = Float.parseFloat(lineData[1]);
                }
                switch (guiCommand) {

                    case 0:
                        idle();
                        break;
                    case 1:
                        //30 er referanse heading(eksempel), tenkte kanskje  
                        //brukeren får velge ønsket heading i GUI?
                        dynamicPositioning(headingReference);
                        break;
                    case 2:
                        remoteOperation(lineData);
                        break;

                }

            }
            printStream.close();
            csocket.close();
            stopThreads();
            System.out.println("RUN EXIT");

        } catch (IOException ex) {
            System.out.println("exception appl");

        }

    }

    private void initialisePIDs() {
        //one regulator per degree of freedom
        pidSurge = new Regulator();
        pidSway = new Regulator();
        pidHeading = new Regulator();

        // temporary tunings
        pidSurge.setTunings(1f, 0f, 0f);
        pidSway.setTunings(1f, 0f, 0f);
        pidHeading.setTunings(1f, 0f, 0f);

    }

    private void idle() {
        updateBasicFields();
        printStream.println(getDataLine());
        gps.setReferencePositionOff();
        System.out.println("idle");
    }

    private void dynamicPositioning(float referenceHeading) {
        updateAllFields();
        printStream.println(getDataLine());
        gps.lockReferencePosition();

        //computes output from actualpoint and referencepoint then returns 
        // forces and moments in SNAME notation
        float X = pidSurge.computeOutput(surge, 0);
        float Y = pidSway.computeOutput(sway, 0);
        float N = pidHeading.computeOutput(heading, referenceHeading);
    }

    private void remoteOperation(String[] lineData) {
        printStream.println("remote op");
        System.out.println("remote op");
//        float x = Float.parseFloat(lineData[2]);
//        float y = Float.parseFloat(lineData[4]);
//        float yaw = Float.parseFloat(lineData[6]);

    }

    private void updateBasicFields() {
        latitudeBody = gps.getGPSPosition().lat;
        longitudeBody = gps.getGPSPosition().lon;
        windSpeed = windReader.getWindSpeed();
        windDirection = windReader.getWindDirection();
        temperature = windReader.getTemperature();
        heading = imu.getHeading();
        speed = gps.getGPSPosition().velocity;
        direction = gps.getGPSPosition().dir;
        latitudeReference = 0;
        longitudeReference = 0;
        surge = 0;
        sway = 0;
        yaw = 0;
    }

    private void updateAllFields() {
        updateBasicFields();
        latitudeReference = gps.getLatRef();
        longitudeReference = gps.getLonRef();
        surge = gps.getXposition();
        sway = gps.getYposition();
        yaw = imu.getYawValue();

    }

    public void initializeApplication() {
        boolean odroid = false;
        String comPortGPS;
        String comPortIMU;
        String comPortWind;
        String comPortThrust;
        //communication parameters
        if (odroid) {
            comPortGPS = "ttyACM0";
            comPortIMU = "ttyACM1";
            comPortWind = "ttyACM2";
            comPortThrust = "ttyACM3";
        } else {
            comPortGPS = "COM4";
            comPortIMU = "COM5";
            comPortWind = "COM6";
            comPortThrust = "COM7";
        }
        int baudRateGPS = 38400;

        int baudRateIMU = 57600;

        int baudRateWind = 57600;

        int baudRateThrust = 115200;

        //One serial connection for each sensor/port
        serialGPS = new SerialConnection(comPortGPS,
                baudRateGPS);

        serialIMU = new SerialConnection(comPortIMU,
                baudRateIMU);

        serialWind = new SerialConnection(comPortWind,
                baudRateWind);

        serialThrust = new SerialConnection(comPortWind,
                baudRateWind);

        // Create and start threads
        gps = new GPSreader(serialGPS, Identifier.GPS);
        gps.connectToSerialPortAndDisplayGPSInfo();
        gps.setName("GPS Reader Thread");
        gps.start();

        imu = new IMUreader(serialIMU, Identifier.IMU);
        imu.connectToSerialPortAndDisplayIMUInfo();
        imu.setName("IMU Reader Thread");
        imu.start();

        windReader = new WindReader(serialWind, Identifier.WIND);
        windReader.connectToSerialPortAndDisplayWindInfo();
        windReader.setName("Wind Reader Thread");
        windReader.start();
//
//        thrustWriter = new ThrustWriter(serialThrust, Identifier.THRUSTERS);
    }

    public static void main(String[] args) throws Exception {
        ServerSocket ssocket = new ServerSocket(2345);
        System.out.println("listening");
        while (true) {
            Socket socket = ssocket.accept();
            System.out.println("Connected via TCP");
            Application app = new Application(socket);
            app.initializeApplication();
            new Thread(app).start();

        }
    }

    private String getDataLine() {
        return "Latitude: " + latitudeBody + " Longitude: "
                + longitudeBody + " Surge: " + sway + " Sway: " + surge
                + " Heading: " + heading + " Speed: " + speed + " Direction: "
                + direction + " WindSpeed: " + windSpeed
                + " WindDirection: " + windDirection
                + " Temperature: " + temperature + " LatRef: "
                + latitudeReference + " LonRef: " + longitudeReference;
    }

    /**
     * lets the sensor-threads exit run()
     */
    private void stopThreads() {
        gps.stopThread();
        imu.stopThread();
        windReader.stopThread();
    }

}
