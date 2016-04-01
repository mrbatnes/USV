package USVProsjekt;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Timer;

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

    private float xNorth;
    private float yEast;
    private float yaw, heading, headingReference;

    private float speed;
    private float direction;

    private float windSpeed;
    private float windDirection;
    private float temperature;

    private PrintStream printStream;
    private int guiCommand;
    private Server server;

    boolean dpStarted;
    private DynamicPositioning dynamicPositioning;

    private Timer timer;
    private int gainChanged;
    private float newControllerGain;
    private int northInc;
    private int eastInc;

    public Application(Server server) {
        xNorth = 0.0f;
        yEast = 0.0f;
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

        guiCommand = 0;
        headingReference = 0;
        newControllerGain = 0;
        gainChanged = 0;
        northInc = 0;
        eastInc = 0;

        this.server = server;

    }

    @Override
    public void run() {
        while (true) {
            //************************************************
            //Stores command values from the Client
            guiCommand = server.getGuiCommand();
            headingReference = server.getHeadingReference();

            gainChanged = server.getGainChanged();
            newControllerGain = server.getControllerGain();

            northInc = server.getNorthIncDecRequest();
            eastInc = server.getEastIncDecRequest();

            //*************************************************
            //Executes secondary tasks based on client commands
            //Stops the timer and resets flag
            if (guiCommand != 1 && dpStarted) {
                timer.cancel();
                dpStarted = false;
            }
            //Edits the gains if change is detected
            if (gainChanged != 0) {
                dynamicPositioning.setNewControllerGain(gainChanged, newControllerGain);
            }
            //changes the references if one or both parameters is != 0
            if (northInc != 0 || eastInc != 0) {
                dynamicPositioning.changeReference(northInc, eastInc);
            }
            //************************************************
            //Executes primary tasks based on clients commands
            switch (guiCommand) {
                case 0:
                    //idle();
                    break;
                case 1:
                    updateAllFields();
                    dynamicPositioning();
                    break;
                case 2:
                    //remoteOperation();
                    break;
            }
            server.setDataFields(getDataLine());

        }
        
        //stopThreads();
        //System.out.println("Run()-method in Application Class finished");
    }

    private void idle() {
        updateBasicFields();
        gps.setReferencePositionOff();
        System.out.println("Idle");
    }

    private void dynamicPositioning() {
        gps.lockReferencePosition();
        dynamicPositioning.setProcessVariables(xNorth, yEast, heading);
        if (!dpStarted) {
            dynamicPositioning.resetControllerErrors();//reset the errors before starting
            int startTime = 0;
            int periodTime = 200;
            dynamicPositioning.setReferenceHeading(headingReference);
            timer = new Timer();
            timer.scheduleAtFixedRate(dynamicPositioning, startTime, periodTime); //start controllers on a fixed interval
            dpStarted = true;
        }
    }

    private void remoteOperation() {
        System.out.println("remote op");

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
        xNorth = 0;
        yEast = 0;
        yaw = 0;
    }

    private void updateAllFields() {
        updateBasicFields();
        latitudeReference = gps.getLatRef();
        longitudeReference = gps.getLonRef();
        xNorth = gps.getXposition();
        yEast = gps.getYposition();
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
        int baudRateGPS = 115200;

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

        serialThrust = new SerialConnection(comPortThrust,
                baudRateThrust);

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

        thrustWriter = new ThrustWriter(serialThrust, Identifier.THRUSTERS);
        dynamicPositioning = new DynamicPositioning(thrustWriter);
    }

    public static void main(String[] args) throws Exception {
        //ServerSocket ssocket = new ServerSocket(2345);
        //System.out.println("listening");
        //while (true) {
        //   Socket socket = ssocket.accept();
        //  System.out.println("Connected via TCP");
        Server server = new Server();
        Application app = new Application(server);
        app.initializeApplication();
        server.start();
        new Thread(app).start();

        // }
    }

    private String getDataLine() {
        float[][] a = dynamicPositioning.getAllControllerTunings();
        return "Latitude: " + latitudeBody + " Longitude: "
                + longitudeBody + " xNorth: " + xNorth + " Sway: " + yEast
                + " Heading: " + heading + " Speed: " + speed + " Direction: "
                + direction + " WindSpeed: " + windSpeed
                + " WindDirection: " + windDirection
                + " Temperature: " + temperature + " LatRef: "
                + latitudeReference + " LonRef: " + longitudeReference + " "
                + a[0][0] + " " + a[0][1] + " " + a[0][2] + " "
                + a[1][0] + " " + a[1][1] + " " + a[1][2] + " "
                + a[2][0] + " " + a[2][1] + " " + a[2][2];
    }

    /**
     * lets the sensor-threads exit run()
     */
    private void stopThreads() {
        gps.stopThread();
        imu.stopThread();
        windReader.stopThread();
        thrustWriter.closeSerialConn();
    }

}
