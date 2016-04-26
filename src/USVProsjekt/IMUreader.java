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
            setYawValue(magnData[0]);
            //System.out.println(yaw);
//            System.out.println("Yaw: " + getHeadingFromYawValue(yaw));//+ " | Pitch: "
            // + pitch + " | Roll: " + roll);
        }
        System.out.println("Connection lost/closed on Thread: "
                + this.getName());
        serialConnection.close();
    }

    private float getHeadingFromYawValue(float yaw) {
        if (yaw >= 90 && yaw <=180) {
            
            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAA" + (yaw - 90));
            return yaw - 90;
        }
        if (yaw >= -180 && yaw <=90) {
            System.out.println("BBBBBBBBBBBBBBBBBBBBBBBBBBB" + (yaw + 270));
            return yaw + 270;
        }
        else {
            return yaw;
        }
    }
    //yaw:     (-179.9) - 0    //yaw:     0 - 180
    //heading:  0 - 179.9     //heading: 180 - 359.9
    public void connectToSerialPortAndDisplayIMUInfo() {
        serialConnection.connectAndListen(ID);
    }

    private float[] parseReceivedIMUline(String line) {
        if (line.startsWith("#")) {

            String[] lineData = line.split(",");
            if (lineData.length == 3) {
                String xString = lineData[0].substring(5);

                float yw = Float.parseFloat(xString);
                float pch =  0.0f;
                float rll = 0.0f;
                return new float[]{yw, pch, rll};
            }
        }
        return new float[]{0.0f, 0.0f, 0.0f};
    }

    public synchronized float getYawValue() {
        return yaw;
    }

    public synchronized void setYawValue(float yaw){
        this.yaw=yaw;
    }
    public synchronized float getHeading() {
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
