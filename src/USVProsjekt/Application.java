package USVProsjekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private float yaw, heading, headingReference;

    private float speed;
    private float direction;

    private float windSpeed;
    private float windDirection;
    private float temperature;

    private Socket csocket;

    private BufferedReader inFromServer;
    private PrintStream printStream;
    private int guiCommand;
    
    boolean dpStarted;
    private DynamicPositioning dp;
    
    private Timer timer;

    public Application(){//Socket csocket) {
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
        //this.csocket = csocket;
        guiCommand = 0;
        
    }

    @Override
    public void run() {
        //try {

            //printStream = new PrintStream(csocket.getOutputStream(), true);
            BufferedReader r;

            while (guiCommand != 3) {
               // r = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
                //String line = r.readLine();
                //String[] lineData = null;
                //if (!line.isEmpty()) {
                   // lineData = line.split(" ");
                    //guiCommand = Integer.parseInt(lineData[0]);
                    guiCommand =1;
                    //headingReference = Float.parseFloat(lineData[1]);
                    headingReference = 20f;
               // }
                switch (guiCommand) {

                    case 0:
                        idle();
                        timer.cancel();
                        dpStarted=false;
                        break;
                    case 1:
                        updateAllFields();
                        //dynamicPositioning(headingReference);
                        gps.lockReferencePosition();
                        //printStream.println(getDataLine());
                        dp.setProcessVariables(surge, sway, heading);
                        if(!dpStarted){
                            int startTime =0;
                            int periodTime =50;
                            dp.setReferenceHeading(heading); 
                            timer = new Timer();
                            timer.scheduleAtFixedRate(dp, startTime, periodTime);
                            dpStarted=true;
                        }                       
                       //System.out.println(gps.getXposition() + " " + gps.getYposition());
                        break;
                    case 2:
                        //remoteOperation(lineData);
                        dpStarted=false;
                        timer.cancel();
                        break;

                }
                

            }
            //printStream.close();
            //csocket.close();
            stopThreads();
            System.out.println("RUN EXIT");

       // } catch (IOException ex) {
            System.out.println("exception appl");

        //}

    }

    private void idle() {
        updateBasicFields();
        //printStream.println(getDataLine());
        gps.setReferencePositionOff();
        System.out.println("idle");
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
        dp = new DynamicPositioning(thrustWriter);
    }

    public static void main(String[] args) throws Exception {
        //ServerSocket ssocket = new ServerSocket(2345);
        //System.out.println("listening");
        //while (true) {
         //   Socket socket = ssocket.accept();
          //  System.out.println("Connected via TCP");
            Application app = new Application();//socket);
            app.initializeApplication();
            new Thread(app).start();

       // }
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
