package USVProsjekt;

/**
 *
 * @author root
 */
public class ThrustWriter {

    private int pulseWidth1;
    private int pulseWidth2;
    private int pulseWidth3;
    
    private double[][] forcePWMList;

    private SerialConnection serialConnection;
    private Identifier ID;

    public ThrustWriter(SerialConnection serialConnection, Identifier ID) {
        pulseWidth1 = 1500;
        pulseWidth2 = 1500;
        pulseWidth3 = 1500;

        this.serialConnection = serialConnection;

        this.ID = ID;
        this.serialConnection.connect(this.ID);
        
        forcePWMList = new double[2][1000];
        for(int x = 1000; x < 2000; x++){
            forcePWMList[0][x-1000] = x;
            double calac = ((x * x) / 3992900f) * x - ((x/885f) * x) + 1.74f * x - 912.7f;
            forcePWMList[1][x-1000] = calac;
        }
    }

    public void writeThrust() {
        if (serialConnection.isConnected()) {
            serialConnection.writeThrustMicros(pulseWidth1, pulseWidth2, pulseWidth3);
        }
    }

    public void setThrustForAll(double[] r) {
        double[] newton = {0, 0, 0};
        int i = 0;
        for(int x = 0; x < 3; x++, i += 2){
            newton[x] = Math.sqrt(r[i] * r[i] + r[i+1] * r[i+1]);
        }
        pulseWidth1 = newtonToPulseWidth(newton[0]);
        pulseWidth2 = newtonToPulseWidth(newton[1]);
        pulseWidth3 = newtonToPulseWidth(newton[2]);
    }

    public int newtonToPulseWidth(double xNewton) {
        double x = xNewton;

        int pulseWidth;

        if (xNewton != 0.0f) {
            pulseWidth = getPWMVal(x);
        }
        else{
            pulseWidth = 1500;
        }

        if (pulseWidth < 1000) {
            pulseWidth = 1000;
        }
        if (pulseWidth > 2000) {
            pulseWidth = 2000;
        }
        return pulseWidth;
    }

    void closeSerialConn() {
        serialConnection.close();
        System.out.println("ThrustWriter: Connection closed");
    }
    
    private int getPWMVal(double F){
        int index = 0;
        double lastDif = 10000f; 
        for(int x = 0; x < forcePWMList[1].length; x++){
            double dif = (F-forcePWMList[1][x]) * (F-forcePWMList[1][x]);
            if(dif < lastDif){
                index = x;
                lastDif = dif;
            }
        }
        return (int) forcePWMList[0][index];
    }

}
