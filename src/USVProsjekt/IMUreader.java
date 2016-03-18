package USVProsjekt;

/**
 *
 * @author Albert
 */
public class IMUreader extends Thread {

    //IMU/Compass variables
    private float yaw;
    private float pitch;
    private float roll;

    private Identifier ID;
    private final SerialConnection serialConnection;
    private int initPeriod;
    private boolean stop;

    public IMUreader(SerialConnection serialConnection, Identifier ID) {
        this.serialConnection = serialConnection;
        this.ID = ID;
        yaw = 0.0f;
        pitch = 0.0f;
        roll = 0.0f;
        stop = false;

    }

    @Override
    public void run() {
        String line;
        float[] magnData;

        while (initPeriod < 5 && serialConnection.isConnected()) {
            line = serialConnection.getSerialLine();
            initPeriod++;
        }
        while (serialConnection.isConnected() && !stop) {
            line = serialConnection.getSerialLine();

            magnData = parseReceivedIMUline(line);
            yaw = magnData[0];
            pitch = magnData[1];
            roll = magnData[2];
//            System.out.println("Yaw: " + getHeadingFromYawValue(yaw));//+ " | Pitch: "
            // + pitch + " | Roll: " + roll);
        }
        System.out.println("Connection lost/closed on Thread: "
                + this.getName());
        serialConnection.close();
    }

    private float getHeadingFromYawValue(float yaw) {
        if (yaw > 90) {
            return yaw - 90;
        }
        if (yaw < -90) {
            return yaw + 270;
        }
        if (yaw < 90 && yaw >= 0) {
            return yaw + 270;
        }
        if (yaw < 0 && yaw >= -90) {
            return yaw + 270;
        } else {
            return yaw;
        }
    }
//heading: 0-179.9
//yaw:     -179.9 -0

//heading: 180-360
//yaw:     0-180
    public void connectToSerialPortAndDisplayIMUInfo() {
        serialConnection.connectAndListen(ID);
    }

    private float[] parseReceivedIMUline(String line) {
        if (line.startsWith("#")) {

            String[] lineData = line.split(",");
            if (lineData.length == 3) {
                String xString = lineData[0].substring(5);
                String yString = lineData[1];
                String zString = lineData[2];
                float yw = Float.parseFloat(xString);
                float pch = Float.parseFloat(yString);
                float rll = Float.parseFloat(zString);
                return new float[]{yw, pch, rll};
            }
        }
        return new float[]{0.0f, 0.0f, 0.0f};
    }

    public float getYawValue() {
        return yaw;
    }

    public float getHeading() {
        return getHeadingFromYawValue(yaw);
    }

    public float getPitchAngle() {
        return pitch;
    }

    public float getRollAngle() {
        return roll;
    }

    void stopThread() {
        stop = true;
    }

}
