package USVProsjekt;

/**
 *
 * @author Albert
 */
public class Regulator{

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

        Kp = 1.0f;
        Ki = 0.0f;
        Kd = 0.0f;

    }

    /**
     * Computes the output
     *
     * @param newInput
     * @param referenceVariable
     * @return
     */

    public float computeOutput(float newInput, float referenceVariable) {

            float error = referenceVariable - newInput;
            errorSum += error;
            float dErr = (error - lastError);
            //Compute PID Output
            outputVariable = Kp * error + Ki * errorSum + Kd * dErr;
            lastError = error;
            return outputVariable;

    }

    public void setTunings(float Kp, float Ki, float Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

}
