/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import USVProsjekt.NMEAparser.GPSPosition;

/**
 *
 * @author RobinBergseth
 */
public class AutonomousTravel extends Thread {

    private PIDController xNorthPID;
    private PIDController yEastPID;
    private PIDController headingPID;

    private float outputX;
    private float outputY;
    private float outputN;

    private float xNorthInput;
    private float yEastInput;
    private float headingInput;

    private float headingReference;
    private float xNorthReference;
    private float yEastReference;

    private boolean travel;
    private boolean newPosition;

    private double[] position;

    private ThrustAllocUSV thrustAllocator;

    private RotationMatrix Rz;
    private double[] forceOutputNewton;

    private ThrustWriter thrustWriter;
    private RotationWriter rotationWriter;
    private NorthEastPositionStorageBox nepsb;
    private IMUreader imu;
    private PrintWriter nedWriter;
    private GPSPositionStorageBox gpsSB;
    private NEDtransform gpsProc;

    public AutonomousTravel(ThrustWriter thrustWriter, RotationWriter rotatationWriter, NorthEastPositionStorageBox nepsb, GPSPositionStorageBox gpsSB, IMUreader imu) {
        xNorthPID = new PIDController();
        yEastPID = new PIDController();
        headingPID = new PIDController();
        
        this.thrustWriter = thrustWriter;
        this.rotationWriter = rotatationWriter;
        this.nepsb = nepsb;
        this.gpsSB = gpsSB;
        this.imu = imu;
        gpsProc = new NEDtransform();
        travel = false;
        newPosition = false;
    }

    public void setPreviousGains(float[][] a) {
        xNorthPID.setTunings(a[0][0], a[0][1], a[0][2]);
        yEastPID.setTunings(a[1][0], a[1][1], a[1][2]);
        headingPID.setTunings(a[2][0], a[2][1], a[2][2]);
    }

    public void setReferenceNorth(float xNorth) {
        xNorthReference = xNorth;
    }

    public void setReferenceEast(float yEast) {
        yEastReference = yEast;
    }

    public void setReferenceHeading(float heading) {
        headingReference = heading;
    }

    public synchronized void setTravel(boolean travel) {
        this.travel = travel;
    }

    public synchronized void setNewPos(boolean pos) {
        this.newPosition = pos;
    }
    
    public synchronized void setPosition(double[] position, boolean update) {
        this.position = position;
        if(update){
            newPosition = true;
        }
        
    }

    @Override
    public void run() {
        while (travel) {
            if (newPosition) {
                alignHeading();
                newPosition = false;
                System.out.println("new postion");
            }
            xNorthInput = (float) position[0];
            yEastInput = (float) position[1];
            headingInput = imu.getHeading();

            float X = xNorthPID.computeOutput(xNorthInput, xNorthReference, false);
            float Y = yEastPID.computeOutput(yEastInput, yEastReference, false);
            float N = headingPID.computeOutput(headingInput, headingReference, true);
            setPIDOutputVector(X, Y, N);
            nedWriter.println((xNorthReference - xNorthInput) + " " + (yEastReference - yEastInput) + " " + (headingReference - headingInput));

            Rz = new RotationMatrix(headingInput);

            double[] XYNtransformed = Rz.multiplyRzwithV(outputX, outputY, outputN);
            forceOutputNewton = thrustAllocator.calculateOutput(XYNtransformed, true);

            thrustWriter.setThrustForAll(forceOutputNewton);
            thrustWriter.writeThrust();
            rotationWriter.setRotationForAll(forceOutputNewton);
            rotationWriter.writeRotation();
        }
    }

    public synchronized void setPIDOutputVector(float X, float Y, float N) {
        outputX = X;
        outputY = Y;
        outputN = N;
    }

    public synchronized float[] getPIDOutputVector() {
        return new float[]{outputX, outputY, outputN};
    }

    /**
     * @return
     */
    public float[][] getAllControllerTunings() {
        return new float[][]{
            xNorthPID.getTunings(),
            yEastPID.getTunings(),
            headingPID.getTunings()
        };
    }

    public void stopWriter() {
        if (nedWriter != null) {
            nedWriter.close();
        }
    }

    public void startWriter() {
        File log = new File("NED_Data.txt");

        try {
            log.createNewFile();
            nedWriter = new PrintWriter(new FileWriter(log, true));
            nedWriter.println(new Date().toString());
        } catch (IOException ex) {
        }
    }

    public void setNewControllerGain(int gainChanged, float newControllerGain) {
        System.out.println("gainChanged: " + gainChanged);
        if (gainChanged < 4) {
            xNorthPID.setGain(gainChanged, newControllerGain);
        } else if (gainChanged > 3 && gainChanged < 7) {
            yEastPID.setGain(gainChanged, newControllerGain);
        } else {
            headingPID.setGain(gainChanged, newControllerGain);
        }

    }

    public void resetControllerErrors() {
        xNorthPID.resetErrors();
        yEastPID.resetErrors();
        headingPID.resetErrors();
    }

    private void alignHeading() {
        boolean heading = false;
        while (!heading && travel) {
            headingInput = imu.getHeading();
            headingReference = getHeadingReferance();
            if ((headingReference - headingInput) < 5
                    && (headingReference - headingInput) > -5) {
                heading = true;
            } else {

                float X = 0.0f;
                float Y = 0.0f;
                float N = headingPID.computeOutput(headingInput, headingReference, true);
                setPIDOutputVector(X, Y, N);

                Rz = new RotationMatrix(headingInput);

                double[] XYNtransformed = Rz.multiplyRzwithV(outputX, outputY, outputN);
                forceOutputNewton = thrustAllocator.calculateOutput(XYNtransformed, true);

                thrustWriter.setThrustForAll(forceOutputNewton);
                thrustWriter.writeThrust();
                rotationWriter.setRotationForAll(forceOutputNewton);
                rotationWriter.writeRotation();
            }
        }
    }

    private float getHeadingReferance() {
        double calc = position[0] / Math.sqrt((position[0]
                * position[0]) + (position[1] * position[1]));
        double angle = Math.acos(calc);
        return (float) angle;
    }

    public double getDistanceToNextPoint() {
        double calc = Math.sqrt((position[0] * position[0]) + 
                (position[1] * position[1]));
        return calc;
    }

}
