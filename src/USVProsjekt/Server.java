/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


/**
 *
 * @author vegard
 */
public class Server extends Thread {

    private Socket csocket;
    private BufferedReader inFromServer;
    private PrintStream printStream;
    private ServerSocket ssocket;
    private int guiCommand;
    private String data;
    private float headingReference;
    private double[] remoteCommand;
    private float controllerGain;
    private int id;

    public Server() throws IOException {
        ssocket = new ServerSocket(2345);
        remoteCommand = new double[3];
    }

    public synchronized int getGuiCommand() {
        return guiCommand;
    }

    private synchronized void setGuiCommand(int guiCommand) {
        this.guiCommand = guiCommand;
    }

    public synchronized float getHeadingReference() {
        return headingReference;
    }

    private synchronized void setHeadingReference(float headingReference) {
        this.headingReference = headingReference;
    }
    public synchronized float getControllerGain() {
        return controllerGain;
    }
    public synchronized void setControllerGain(int id,float controllerGain) {
        this.id=id;
        this.controllerGain= controllerGain;
    }
    public synchronized int getGainChanged(){
        return id;
    }

    private synchronized void setRemoteCommand(String[] lineData) {
        for(int i = 0; i < 3; i++) {
            remoteCommand[i] = Double.parseDouble(lineData[i+3]);
        }
    }

    public synchronized double[] getRemoteCommand() {

        return remoteCommand;
    }

    @Override
    public void run() {
        try {
            csocket = ssocket.accept();

            while (csocket.isConnected() && guiCommand != 3) {
                printStream = new PrintStream(csocket.getOutputStream(), true);
                BufferedReader r = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
                String line = r.readLine();
                String[] lineData = null;
                if (!line.isEmpty()) {
                    lineData = line.split(" ");
                    setGuiCommand(Integer.parseInt(lineData[0]));
                    setHeadingReference(Float.parseFloat(lineData[1]));
                    setControllerGain(Integer.parseInt(lineData[666]), Integer.parseInt(lineData[666]));
                    if (guiCommand == 2) {
                        setRemoteCommand(lineData);
                    }
                }
                
                printStream.println(getDataFields());
            }
            csocket.close();
        } catch (IOException ex) {
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);

        }
    }
    
    public synchronized void setDataFields(String data) {
        this.data = data;
    }
    
    private synchronized String getDataFields() {
        return data;
    }
}
