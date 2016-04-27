package USVProsjekt;

/**
 *
 * @author root
 */
public class ThrustWriter{

    private int thruster_1;
    private int thruster_2;
    private int thruster_3;
    private int thruster_4;

    private int pulseWidth1;
    private int pulseWidth2;
    private int pulseWidth3;
    private int pulseWidth4;

    private SerialConnection serialConnection;
    private Identifier ID;

    public ThrustWriter(SerialConnection serialConnection, Identifier ID) {
        thruster_1 = 1;
        thruster_2 = 2;
        thruster_3 = 3;
        thruster_4 = 4;
        
        pulseWidth1 = 1500;
        pulseWidth2 = 1500;
        pulseWidth3 = 1500;
        pulseWidth4 = 1500;
        
        this.serialConnection = serialConnection;
        
        this.ID = ID;
        this.serialConnection.connect(this.ID);
    }

    public void writeThrust() {
        serialConnection.writeThrustMicros(pulseWidth1, pulseWidth2, pulseWidth3, pulseWidth4);

    }

    public void setThrust(int thrusterNumber, double newton) {
        if (thrusterNumber == thruster_1) {
            pulseWidth1 = newtonToPulseWidth(newton);
        } else if (thrusterNumber == thruster_2) {
            pulseWidth2 = newtonToPulseWidth(newton);
        } else if (thrusterNumber == thruster_3) {
            pulseWidth3 = newtonToPulseWidth(newton);
        } else if (thrusterNumber == thruster_4) {
            pulseWidth4 = newtonToPulseWidth(newton);
        } else{
            System.out.println("ThrustWriter:setThrust This thruster does not exist.");
        }
    }


    public void setThrustForAll(double[] newton) {
        pulseWidth1 = newtonToPulseWidth(newton[0]);
        pulseWidth2 = newtonToPulseWidth(newton[1]);
        pulseWidth3 = newtonToPulseWidth(newton[2]);
        pulseWidth4 = newtonToPulseWidth(newton[3]);
    }

    public int newtonToPulseWidth(double xNewton) {
        double x = xNewton;

        int pulseWidth = 1500;

        if (xNewton > 2.5) {
            double pulseWidthFloat = 11.8658*x +1437;
            pulseWidth = (int) pulseWidthFloat;
        } else if (xNewton < 2.5) {
            double pulseWidthFloat = 10.18*x+1558;
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

    void closeSerialConn() {
       serialConnection.close();
    }

}
