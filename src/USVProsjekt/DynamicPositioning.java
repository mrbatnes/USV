package USVProsjekt;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public void setProcessVariables(float surge, float sway, float heading) {
        xNorthInput = surge;
        yEastInput = sway;
        headingInput = heading;
    }

    public void changeReference(int north, int east) {
        if (north == 1) {
            xNorthReference = xNorthReference + 0.5f;
        }
        else if(north==-1){
            xNorthReference = xNorthReference - 0.5f;
        }
        if (east == 1) {
            yEastReference = yEastReference + 0.5f;
        }
        else if(east==-1){
            yEastReference = yEastReference - 0.5f;
        }
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

            Rz = new RotationMatrix(headingInput);
            //Rz*Tau
            double[] XYNtransformed = Rz.multiplyRzwithV(outputX, outputY, outputN);
            forceOutputNewton = thrustAllocator.calculateOutput(XYNtransformed);
            thrustWriter.setThrustForAll(forceOutputNewton);
            //thrustWriter.writeThrust();
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
