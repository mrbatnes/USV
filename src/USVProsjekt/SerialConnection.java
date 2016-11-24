package USVProsjekt;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 *
 * @author Albert
 */
public class SerialConnection {

    private int baudRate;
    private SerialPort comPort;
    private String serialString;
    private boolean available;

    public SerialConnection(String comPortString, int baudRate) {
        this.comPort = SerialPort.getCommPort(comPortString);
        this.baudRate = baudRate;
        serialString = "";
        available = false;
    }

    public void listAvailableSerialPorts() {
        SerialPort ports[] = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println(port.getSystemPortName());
        }

    }

    public void connect(Identifier ID) {
        if (comPort.openPort()) {
            comPort.setBaudRate(baudRate);
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
            System.out.println("Successfully connected to " + ID
                    + " via " + comPort.getSystemPortName() + " with Baud Rate:"
                    + " " + comPort.getBaudRate());
        } else {
            System.out.println("Not able to connect to " + ID + "..");

        }
    }

    public void writeThrustMicros(int thrustMicros1, int thrustMicros2, int thrustMicros3) {
        int[] thrust = {thrustMicros1, thrustMicros2, thrustMicros3};
        //skriver til arduino
        String writeString;
        writeString = getWriteString(thrust, Identifier.THRUSTERS);

        PrintWriter output = new PrintWriter(comPort.getOutputStream());
        output.write(writeString);
        output.flush();
        //Leser tilbake dataene fra arduino
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(comPort.getInputStream(), "UTF-8"));
            System.out.println(br.readLine());
        } catch (IOException e) {
            System.out.println("ioex writeThrustMicros() in SerialConnection");
        }
    }

    void writeRotationPos(int rotation1, int rotation2, int rotation3) {
        int[] rotations = {rotation1, rotation2, rotation3};
        String writeString;
        writeString = getWriteString(rotations, Identifier.ROTATION);
    }

    private String getWriteString(int[] in, Identifier ID) {
        String[] values = new String[in.length];
        switch (ID){
            case THRUSTERS:
                for (int x = 0; x < values.length; x++) {
                    if(in[x] < 1000){
                        in[x] = 1000;
                    }
                    else if(in[x] > 2000){
                        in[x] = 2000;
                    }
                    values[x] = "" + in[x];
                }
                break;
            case ROTATION:
               
            for (int x = 0; x < values.length; x++) {
                in[x] += 180;
                if (in[x] < 10) {
                    values[x] = "00" + in[x];
                } else if (in[x] < 100) {
                    values[x] = "0" + in[x];
                } else {
                    values[x] = "" + in[x];
                }
            }
                break;
        }
        return "$A" + values[0] + "B" + values[1] + "C" + values[2] + "%";
    }

    public void connectAndListen(Identifier ID) {
        connect(ID);
        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType()
                        != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    return;
                }
                byte[] newData = null;
                try {
                    switch (ID) {
                        case GPS:
                            Thread.sleep(90);
                            newData = new byte[comPort.bytesAvailable()];
                            break;
                        case WIND:
                            Thread.sleep(120);
                            newData = new byte[comPort.bytesAvailable()];
                            break;
                        case IMU:
                            //IMU
                            Thread.sleep(30);
                            newData = new byte[comPort.bytesAvailable()];
                            break;
                    }
                } catch (InterruptedException ex) {
                }
                comPort.readBytes(newData, newData.length);
                setSerialLine(new String(newData));
            }
        }
        );
    }

    public synchronized void setSerialLine(String serialLine) {
        while (available) {
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }

        available = true;
        notifyAll();
        serialString = serialLine;
    }

    public synchronized String getSerialLine() {
        while (!available) {
            try {
                wait();
            } catch (InterruptedException ex) {

            }
        }
        available = false;
        notifyAll();
        return serialString;
    }

    public boolean isConnected() {
        return comPort.isOpen();
    }

    public void close() {
        comPort.closePort();
    }
}
