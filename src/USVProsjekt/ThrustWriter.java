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
    
    private Identifier ID;

    public ThrustWriter(SerialConnection serialConnection, Identifier ID) {
        thruster_1 = 1;
        thruster_2 = 2;
        thruster_3 = 3;
        thruster_4 = 4;
        this.ID=ID;
        this.serialConnection = serialConnection;
    }

    public void writeThrust(float x, float y ,float yaw){
        
    }
    
    public int newtonToMillis(float xNewton){
        float x = xNewton;
        float millisFloat = (float) (-3.907*x*x + 89.927*x + 1539.441);
        int millis = (int) millisFloat;
        
        return millis;
    }
    
    
}
