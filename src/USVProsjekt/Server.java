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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private float headingReference;
    private double[] remoteCommand;

    public Server() throws IOException {
        ssocket = new ServerSocket(2345);
        remoteCommand = new double[4];
    }

    public synchronized int getGuiCommand() {
        return guiCommand;
    }

    public synchronized void setGuiCommand(int guiCommand) {
        this.guiCommand = guiCommand;
    }

    public synchronized float getHeadingReference() {
        return headingReference;
    }

    public synchronized void setHeadingReference(float headingReference) {
        this.headingReference = headingReference;
    }
    
    public synchronized void setRemoteCommand(String[] lineData) {
        
    }
    
    public synchronized double[] getRemoteCommand() {
        
        return remoteCommand;
    }

    @Override
    public void run() {
        try {
            csocket = ssocket.accept();

            while (csocket.isConnected()) {

                printStream = new PrintStream(csocket.getOutputStream(), true);
                BufferedReader r = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
                String line = r.readLine();
                String[] lineData = null;
                if (!line.isEmpty()) {
                    lineData = line.split(" ");
                    setGuiCommand(Integer.parseInt(lineData[0]));
                    setHeadingReference(Float.parseFloat(lineData[1]));
                }
                if (guiCommand == 2 && !(lineData == null)) {
                    setRemoteCommand(lineData);
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);

        }
    }
}
