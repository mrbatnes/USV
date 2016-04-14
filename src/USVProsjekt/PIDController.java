package USVProsjekt;

/**
 *
 * @author Albert
 */
public class PIDController {
//
    //IO Variables
    private float outputVariable;

    //Gains
    private float Kp;
    private float Ki;
    private float Kd;

    //Class variables
    private float errorSum;
    private float lastError;

    public PIDController() {
        outputVariable = 0.0f;
        errorSum = 0.0f;
        lastError = 0.0f;

        Kp = 1.0f;
        Ki = 0.0f;
        Kd = 0.0f;

    }

    /**
     * Computes the output
     *
     * @param newInput
     * @param referenceVariable
     * @param continuous
     * @return
     */
    public float computeOutput(float newInput, float referenceVariable, boolean continuous) {

        float error = referenceVariable - newInput;

        // If continuous is set to true allow wrap around
        if (continuous) {
            if (Math.abs(error)> 180) {
                if (error > 0) {
                    error = error - 360.0f;
                } else {
                    error = error + 360.0f;
                }
            }
        }

        errorSum += error;
        float dError = (error - lastError);
        //Compute PID Output
        outputVariable = Kp * error + Ki * errorSum + Kd * dError;
        lastError = error;
        return outputVariable;

    }
    public void resetErrors(){
        errorSum=0;
        lastError=0;
    }
    public void setTunings(float Kp, float Ki, float Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

    public float[] getTunings() {
        return new float[]{Kp, Ki, Kd};
    }

    void setGain(int gainChanged, float newControllerGain) {
        switch (gainChanged) {
            case 1:
            case 4:
            case 7:
                Kp = newControllerGain;
                break;
            case 2:
            case 5:
            case 8:
                Ki = newControllerGain;
                break;
            case 3:
            case 6:
            case 9:
                Kd = newControllerGain;
                break;
        }
    }

}
