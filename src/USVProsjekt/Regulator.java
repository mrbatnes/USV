package USVProsjekt;

/**
 *
 * @author Albert
 */
public class Regulator {

    //IO Variables
    private float outputVariable;

    //Gains
    private float Kp;
    private float Ki;
    private float Kd;

    //Class variables
    private float errorSum;
    private float lastError;

    public Regulator() {
        outputVariable = 0.0f;
        errorSum = 0.0f;
        lastError = 0.0f;

        Kp = 1.0f;
        Ki = 0.0f;
        Kd = 0.0f;

    }

    /**
     * Computes the output
     * @param newInput
     * @param referenceVariable
     * @return
     */
    public float computeOutput(float newInput, float referenceVariable) {

        float error = referenceVariable - newInput;
        errorSum += error;
        float dError = (error - lastError);
        //Compute PID Output
        outputVariable = Kp * error + Ki * errorSum + Kd * dError;
        lastError = error;
        return outputVariable;

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
            case 1:case 4:case 7:
                Kp = newControllerGain;
                break;
            case 2:case 5:case 8:
                Ki = newControllerGain;
                break;
            case 3: case 6: case 9:
                Kd = newControllerGain;
                break;
        }
    }

}
