package USVProsjekt;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.io.OutputStream;

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
                    SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
            System.out.println("Successfully connected to " + ID
                    + " via " + comPort.getSystemPortName() + " with Baud Rate:"
                    + " " + comPort.getBaudRate());
        } else {
            System.out.println("Not able to connect to " + ID + "..");

        }
    }

    public void writeThrustNewton(float thrust, int thrusterNumber, Identifier ID) {
        String thrustString;
        OutputStream os = comPort.getOutputStream();
        try {
            thrustString = ""+thrusterNumber + " " + thrust;
            os.write(thrustString.getBytes());
        } catch (IOException ex) {

        }
    }
    
        public void writeThrustMillis(int thrustMillis1, int thrustMillis2, int thrustMillis3, int thrustMillis4) {
        String thrustString;
        OutputStream os = comPort.getOutputStream();
        try {
            thrustString = "" + thrustMillis1 + ":" + thrustMillis2 + ":"+ thrustMillis3 + ":" + thrustMillis4;
            os.write(thrustString.getBytes());
        } catch (IOException ex) {

        }
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
                    if (ID.equals(Identifier.GPS)) {

                        Thread.sleep(120);
                        newData = new byte[comPort.bytesAvailable()];

                    } else if (ID.equals(Identifier.WIND)) {

                        Thread.sleep(120);
                        newData = new byte[comPort.bytesAvailable()];

                    } else {

                        //IMU
                        Thread.sleep(30);
                        newData = new byte[comPort.bytesAvailable()];

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
