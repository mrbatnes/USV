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
    
    private float incrementAmountX;
    private float incrementAmountY;
    
    private float headingReference;
    private double[] position;
    
    private ThrustAllocUSV thrustAllocator;
    
    private RotationMatrix Rz;
    private double[] forceOutputNewton;
    
    private ThrustWriter thrustWriter;
    private RotationWriter rotationWriter;
    private float xNorthReference;
    private float yEastReference;
    private PrintWriter nedWriter;
    private NorthEastPositionStorageBox northEast;
    private IMUreader imu;
    
    
    
    public DynamicPositioning(ThrustWriter thrustWriter, RotationWriter rotationWriter, NorthEastPositionStorageBox northEast, IMUreader imu) {
        xNorthPID = new PIDController();
        yEastPID = new PIDController();
        headingPID = new PIDController();
        this.northEast = northEast;
        this.imu = imu;
        outputX = 0.0f;
        outputY = 0.0f;
        outputN = 0.0f;
        position = new double[2];
        headingReference = 0.0f;
        thrustAllocator = new ThrustAllocUSV();
        forceOutputNewton = new double[4];
        this.thrustWriter = thrustWriter;
        this.rotationWriter = rotationWriter;
    }
    
    public void setPreviousGains(float[][] a) {
        xNorthPID.setTunings(a[0][0], a[0][1], a[0][2]);
        yEastPID.setTunings(a[1][0], a[1][1], a[1][2]);
        headingPID.setTunings(a[2][0], a[2][1], a[2][2]);
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
            if (northEast.isNewPosition()) {
                position = northEast.getPosition();
            }
            
            xNorthInput = (float)(position[0] + incrementAmountX);
            yEastInput = (float)(position[1] + incrementAmountY);
            headingInput = imu.getHeading();
            
            float X = xNorthPID.computeOutput(xNorthInput, xNorthReference, false);
            float Y = yEastPID.computeOutput(yEastInput, yEastReference, false);
            float N = headingPID.computeOutput(imu.getHeading(), headingReference, true);
            setPIDOutputVector(X, Y, N);//synchronized
            nedWriter.println((xNorthReference - xNorthInput) + " " + (yEastReference - yEastInput) + " " + (headingReference - headingInput));
            
            Rz = new RotationMatrix(headingInput);
            //Rz'*Tau
            double[] XYNtransformed = Rz.multiplyRzwithV(outputX, outputY, outputN);
            forceOutputNewton = thrustAllocator.calculateOutput(XYNtransformed);
            thrustWriter.setThrustForAll(forceOutputNewton);
            thrustWriter.writeThrust();
            rotationWriter.setRotationForAll(forceOutputNewton);
            rotationWriter.writeRotation();
        } catch (Exception ex) {
            System.out.println("exception dp");
        }
    }

    public synchronized void setPIDOutputVector(float X, float Y, float N) {
        outputX = X;
        outputY = Y;
        outputN = N;
    }

    public synchronized float[] getPIDOutputVector() {
        return new float[]{outputX, outputY, outputN};
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
    
    public synchronized void setIncrementAmount(int northInc, int eastInc) {
        incrementAmountX -= northInc / 2.0f;
        incrementAmountY -= eastInc / 2.0f;
    }
    
}
