package USVProsjekt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.TimerTask;
import org.seventytwomiles.springframework.io.FileUtils;

/**
 *
 * @author Albert
 */
public class DynamicPositioning extends TimerTask {

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

    private ThrustAllocator thrustAllocator;

    private RotationMatrix Rz;
    private double[] forceOutputNewton;

    private ThrustWriter thrustWriter;
    private float xNorthReference;
    private float yEastReference;
    private PrintWriter nedWriter;

    public DynamicPositioning(ThrustWriter thrustWriter) {
        xNorthPID = new PIDController();
        yEastPID = new PIDController();
        headingPID = new PIDController();

        outputX = 0.0f;
        outputY = 0.0f;
        outputN = 0.0f;

        headingReference = 0.0f;
        thrustAllocator = new ThrustAllocator(-1.5, 1.2, -0.5, 0.5);
        forceOutputNewton = new double[4];
        this.thrustWriter = thrustWriter;
    }

    public void setPreviousGains(float[][] a) {
        xNorthPID.setTunings(a[0][0], a[0][1], a[0][2]);
        yEastPID.setTunings(a[1][0], a[1][1], a[1][2]);
        headingPID.setTunings(a[2][0], a[2][1], a[2][2]);
    }

    public void setProcessVariables(float surge, float sway, float heading) {
        xNorthInput = surge;
        yEastInput = sway;
        headingInput = heading;
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

    @Override
    public void run() {
        try {//XYN from SNAME notation
            outputX = xNorthPID.computeOutput(xNorthInput, xNorthReference, false);
            outputY = yEastPID.computeOutput(yEastInput, yEastReference, false);
            outputN = headingPID.computeOutput(headingInput, headingReference, true);
            nedWriter.println(xNorthInput + " " + yEastInput + " " + headingInput);

            Rz = new RotationMatrix(headingInput);
            //Rz'*Tau
            double[] XYNtransformed = Rz.multiplyRzwithV(outputX, outputY, outputN);
            forceOutputNewton = thrustAllocator.calculateOutput(XYNtransformed);
            thrustWriter.setThrustForAll(forceOutputNewton);
            thrustWriter.writeThrust();
        } catch (Exception ex) {
            System.out.println("exception dp");
        }
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

}
