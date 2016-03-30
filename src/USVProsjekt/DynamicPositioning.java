package USVProsjekt;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Albert
 */
public class DynamicPositioning extends TimerTask {

    private Regulator pidSurge;
    private Regulator pidSway;
    private Regulator pidHeading;

    private float outputSurge;
    private float outputSway;
    private float outputHeading;

    private float inputSurge;
    private float inputSway;
    private float inputHeading;

    private float referenceHeading;

    private ThrustAllocator thrustAlloc;

    private RotationMatrix Rz;
    private double[] torqueOutput;

    private ThrustWriter thrustWriter;

    public DynamicPositioning(ThrustWriter tw) {
        pidSurge = new Regulator();
        pidSway = new Regulator();
        pidHeading = new Regulator();

        pidSurge.setTunings(1f, 0f, 0f);
        pidSway.setTunings(1f, 0f, 0f);
        pidHeading.setTunings(1f, 0f, 0f);

        outputSurge = 0.0f;
        outputSway = 0.0f;
        outputHeading = 0.0f;

        referenceHeading = 0.0f;
        thrustAlloc = new ThrustAllocator(-1.5, 1.2, -0.5, 0.5);
        torqueOutput = new double[4];
        thrustWriter = tw;
    }

    public void setProcessVariables(float surge, float sway, float heading) {
        inputSurge = surge;
        inputSway = sway;
        inputHeading = heading;
    }

    public void setReferenceHeading(float heading) {
        referenceHeading = heading;
    }

    @Override
    public void run() {

        try {
            outputSurge = pidSurge.computeOutput(inputSurge, 0);
            outputSway = pidSway.computeOutput(inputSway, 0);
            outputHeading = pidHeading.computeOutput(inputHeading, referenceHeading);

            Rz = new RotationMatrix(inputHeading);

            //Rz*Tau
            double[] XYNtransformed = Rz.multiplyRzwithV(outputSurge, outputSway, outputHeading);
            torqueOutput = thrustAlloc.calculateOutput(XYNtransformed);
            thrustWriter.setThrustForAll(torqueOutput);
            //thrustWriter.writeThrust();
        } catch (Exception ex) {
            System.out.println("exception dp");
        }

    }

    /**
     * Surge
     *
     * @return
     */
    public float[][] getAllControllerTunings() {
        return new float[][]{
            pidSurge.getTunings(),
            pidSurge.getTunings(),
            pidSurge.getTunings()
        };
    }

    public void setNewControllerGain(int gainChanged, float newControllerGain) {
        if (gainChanged < 4) {
            pidSurge.setGain(gainChanged,newControllerGain);
        } else if (gainChanged > 3 && gainChanged < 7) {
            pidSway.setGain(gainChanged,newControllerGain);
        } else {
            pidHeading.setGain(gainChanged,newControllerGain);
        }
    }


}
