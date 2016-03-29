package USVProsjekt;

/**
 *
 * @author root
 */
public class ThrustWriter extends Thread {

    private int thruster_1;
    private int thruster_2;
    private int thruster_3;
    private int thruster_4;

    private int pulseWidth1;
    private int pulseWidth2;
    private int pulseWidth3;
    private int pulseWidth4;

    private SerialConnection serialConnection;

    public ThrustWriter(SerialConnection serialConnection) {
        thruster_1 = 1;
        thruster_2 = 2;
        thruster_3 = 3;
        thruster_4 = 4;
        
        pulseWidth1 = 1500;
        pulseWidth2 = 1500;
        pulseWidth3 = 1500;
        pulseWidth4 = 1500;
        
        this.serialConnection = serialConnection;
    }

    public void writeThrust() {
        serialConnection.writeThrustMillis(pulseWidth1, pulseWidth2, pulseWidth3, pulseWidth4);
    }

    public void setThrust(int thrusterNumber, double newton) {
        if (thrusterNumber == thruster_1) {
            pulseWidth1 = newtonToMillis(newton);
        } else if (thrusterNumber == thruster_2) {
            pulseWidth2 = newtonToMillis(newton);
        } else if (thrusterNumber == thruster_3) {
            pulseWidth3 = newtonToMillis(newton);
        } else if (thrusterNumber == thruster_4) {
            pulseWidth4 = newtonToMillis(newton);
        } else{
            System.out.println("ThrustWriter:setThrust This thruster does not exist.");
        }
    }

    public synchronized void setThrustForAll(double newton1, double newton2, double newton3, double newton4) {
        pulseWidth1 = newtonToMillis(newton1);
        pulseWidth2 = newtonToMillis(newton2);
        pulseWidth3 = newtonToMillis(newton3);
        pulseWidth4 = newtonToMillis(newton4);
    }

    public int newtonToMillis(double xNewton) {
        double x = xNewton;

        int pulseWidth = 1500;

        if (xNewton > 0) {
            double pulseWidthFloat = -3.907 * x * x + 89.927 * x + 1539.441;
            pulseWidth = (int) pulseWidthFloat;
        } else if (xNewton < 0) {
            double pulseWidthFloat = 3.132 * x * x * x + 24.457 * x * x + 135.170 * x + 1466.098;
            pulseWidth = (int) pulseWidthFloat;
        } else {
            pulseWidth = 1500;
        }

        if (pulseWidth < 1100) {
            pulseWidth = 1100;
        }
        if (pulseWidth > 1900) {
            pulseWidth = 1900;
        }
        return pulseWidth;
    }

}
