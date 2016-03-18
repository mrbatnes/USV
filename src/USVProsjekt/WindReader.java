package USVProsjekt;

/**
 *
 * @author root
 */
public class WindReader extends Thread {

    private float windSpeed;
    private float windDirection;
    private float temperature;

    private SerialConnection serialConnection;
    private Identifier ID;
    private int initPeriod;
    private boolean stop;
    private float airPressurehPa;

    public WindReader(SerialConnection serialConnection, Identifier ID) {
        windSpeed = 0.0f;
        windDirection = 0.0f;
        temperature = 0.0f;
        this.serialConnection = serialConnection;
        this.ID = ID;
        stop = false;
    }

    @Override
    public void run() {
        String line;
        while (initPeriod < 5 && serialConnection.isConnected()) {
            line = serialConnection.getSerialLine();
            initPeriod++;
        }
        while (serialConnection.isConnected() && !stop) {
            line = serialConnection.getSerialLine();
            parseWindSerialSentence(line);
        }
        serialConnection.close();
        System.out.println("Connection lost/closed on Thread:"
                + " " + this.getName());
    }

    public void connectToSerialPortAndDisplayWindInfo() {
        serialConnection.connectAndListen(ID);
    }

    private void parseWindSerialSentence(String line) {
        if (line.startsWith("&")) {
            String[] lineData = line.split(" ");
            windSpeed = Float.parseFloat(lineData[2]);
            windDirection = Float.parseFloat(lineData[5]);
            temperature = Float.parseFloat(lineData[7]);
            airPressurehPa = Float.parseFloat(lineData[9]);
            
        }
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public float getWindDirection() {
        return windDirection;
    }

    public float getTemperature() {
        return temperature;
    }

    void stopThread() {
        stop = true;
    }

}
