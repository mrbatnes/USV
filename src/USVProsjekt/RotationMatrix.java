package USVProsjekt;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author Albert
 */
public class RotationMatrix {

    private BlockRealMatrix Rz;
    private double[][] raw;
    ArrayRealVector vector;

    public RotationMatrix(float headingRadians) {
        raw = new double[][]{
            {c(headingRadians), -s(headingRadians), 0},
            {s(headingRadians), c(headingRadians), 0},
            {0, 0, 1}};
        Rz = new BlockRealMatrix(raw);
    }
    
    public double[] multiplyRzwithV(float u,float v, float w)
    {
        vector = new ArrayRealVector(new double[]{u,v,w});
        //Rz*returnVector 3x3  * 3x1 = 3x1
        RealVector returnVector = Rz.operate(vector);
        return returnVector.toArray();
    }

    private float c(float radians) {
        return (float) Math.cos(radians);
    }

    private float s(float radians) {
        return (float) Math.sin(radians);
    }
}
