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
    private int northIncRequest;
    private int eastIncRequest;
    private boolean gainChanged;

    public Server() throws IOException {
        ssocket = new ServerSocket(2345);
        remoteCommand = new double[3];
        gainChanged = false;
    }

    public synchronized int getGuiCommand() {
        return guiCommand;
    }

    public synchronized boolean isGainChanged() {
        return gainChanged;
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
        gainChanged = false;
        return controllerGain;
    }

    public synchronized void setControllerGain(int id, float controllerGain) {
        if (id != 0) {
            gainChanged = true;
            this.id = id;
            this.controllerGain = controllerGain;
        }
    }

    public synchronized int getGainChanged() {
        return id;
    }

    public synchronized int getNorthIncDecRequest() {
        return northIncRequest;
    }

    public synchronized int getEastIncDecRequest() {
        return eastIncRequest;
    }

    public synchronized void setNorthIncDecRequest(int request) {
        northIncRequest = request;
    }

    public synchronized void setEastIncDecRequest(int request) {
        eastIncRequest = request;
    }

    private synchronized void setRemoteCommand(String[] lineData) {
        for (int i = 0; i < 3; i++) {
            remoteCommand[i] = Double.parseDouble(lineData[i + 3]);
        }
    }

    public synchronized double[] getRemoteCommand() {

        return remoteCommand;
    }

    @Override
    public void run() {
        try {
            System.out.println("SERVER BLOCKING");
            csocket = ssocket.accept();
            System.out.println("SERVER ACCEPT");
            while (csocket.isConnected()) {
                printStream = new PrintStream(csocket.getOutputStream(), true);
                BufferedReader r = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
                String line = r.readLine();//
                String[] lineData = null;
                // System.out.println(line);
                if (!line.isEmpty()) {
                    System.out.println("Server received data");
                    lineData = line.split(" ");
                    setGuiCommand(Integer.parseInt(lineData[0]));
                    setHeadingReference(Float.parseFloat(lineData[2]));
                    setControllerGain(Integer.parseInt(lineData[1]), Float.parseFloat(lineData[3]));
                    setNorthIncDecRequest(Integer.parseInt(lineData[4]));
                    setEastIncDecRequest(Integer.parseInt(lineData[5]));

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
