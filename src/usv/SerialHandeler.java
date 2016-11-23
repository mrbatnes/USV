/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usv;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;

/**
 *
 * @author RobinBergseth
 */
public class SerialHandeler implements SerialPortEventListener {

    private Enumeration portList;
    private CommPortIdentifier portId;
    private HashMap<String, CommPortIdentifier> comList;
    private SerialPort serialPort;
    private BufferedReader input;
    private OutputStream output;
    private boolean serialReady = false;
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 9600;

    public SerialHandeler() {
        comList = new HashMap<>();
        portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            comList.put(portId.getName(), portId);
        }
    }

    public void connect(String port) {
        try {
            CommPortIdentifier portId = comList.get(port);
            serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();
            serialReady = true;
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.err.println(e.toString());
            System.out.println("Not connected");
        }
    }

    @Override
    public void serialEvent(SerialPortEvent spe) {
        if(spe.getEventType() == SerialPortEvent.DATA_AVAILABLE){
            try{
                String inputLine = input.readLine();
                System.out.println(inputLine);
            }
            catch(Exception e){
                System.err.println(e.toString());
            }
        }
    }
    
    public boolean send(String sendString){
        if(serialReady){
            try{
                output.write(sendString.getBytes());
                return true;
            }
            catch(IOException e){
                return false;
            }
        }
        else{
            return false;
        }
    }
    
    public BufferedReader getInpuReader(){
        return input;
    }
}