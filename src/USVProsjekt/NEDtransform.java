package USVProsjekt;

/**
 *
 * @author Albert
 */
public class NEDtransform extends Thread {

    //World Geodetic System 1984 constants
    //Longest radius of earth's ellipsoid
    private float semimajorAxis = 6378137.0f;
    //Shortest radius of earth's ellipsoid
    private float semiminorAxis = 6356752.0f;
    private float flattening = (semimajorAxis - semiminorAxis) / semimajorAxis;

    public NEDtransform() {

    }

    /**
     * Flat Earth Coordinates, optimal for small changes in lat/lon used.
     *
     * @param latBody
     * @param lonBody
     * @param latRef
     * @param lonRef
     * @return
     */
    public float[] getFlatEarthCoordinates(float latBody, float lonBody,
            float latRef, float lonRef) {
        float dMy = latBody - latRef;
        float dL = lonBody - lonRef;
        float rN = (float) (semimajorAxis / Math.sqrt(1 - (2.0 * flattening
                - Math.pow(flattening, 2)) * Math.pow(Math.sin(latRef), 2)));
        float rM = (float) (rN * (1 - (2.0 * flattening
                - Math.pow(flattening, 2)))
                / (1 - (2.0 * flattening - Math.pow(flattening, 2))
                * Math.pow(Math.sin(latRef), 2)));
        float dN = (float) (dMy / Math.atan(1 / rM));
        float dE = (float) (dL / Math.atan(1 / (rN * Math.cos(latRef))));
        return new float[]{dN, dE};
    }

    /**
     * Works for all distances.
     *
     * @param latitudeBody
     * @param longitudeBody
     * @param latitudeReference
     * @param longitudeReference
     * @return
     */
//    public double[] getBodyCInNEDByGeodeticBodyPosAndRef(double latitudeBody,
//            double longitudeBody, double latitudeReference,
//            double longitudeReference) {
//        double NRef = getN(latitudeReference, longitudeReference);
//        double[] xyzRef = llh2ECEF(latitudeReference, longitudeReference,
//                0, NRef);
//        double NBody = getN(latitudeReference, longitudeReference);
//        double[] xyzBody = llh2ECEF(latitudeBody, longitudeBody, 0, NBody);
//        double dx = xyzBody[0] - xyzRef[0];
//        double dy = xyzBody[1] - xyzRef[1];
//        double dz = xyzBody[2] - xyzRef[2];
//
//        double cosPhi = Math.cos(latitudeReference);
//        double sinPhi = Math.sin(latitudeReference);
//        double cosLambda = Math.cos(longitudeReference);
//        double sinLambda = Math.sin(longitudeReference);
//
//        double t = cosLambda * dx + sinLambda * dy;
//
//        double dxEast = -sinLambda * dx + cosLambda * dy;
//        double dzUp = cosPhi * t + sinPhi * dz;
//        double dyNorth = -sinPhi * t + cosPhi * dz;
//
//        double xNorth = dyNorth;
//        double yEast = dxEast;
//        double zDown = -dzUp;
//        return new double[]{xNorth, yEast, zDown};
//    }
//
//    private double getN(double lat, double lon) {
//        double a = semimajorAxis;
//        double b = semiminorAxis;
//        double acos = a * Math.cos(lat);
//        double bsin = b * Math.cos(lon);
//        double aa = a * a;
//        return aa / Math.sqrt(acos * acos + bsin * bsin);
//    }

    /**
     * Converts latitude, longitude and height to ECEF coordinate system
     *
     * @param latitude
     * @param longitude
     * @param height
     * @return var-index: x-0, y-1, z-2
     */
//    private double[] llh2ECEF(double lat,
//            double lon, double height, double N) {
//        double x = (N + height) * Math.cos(lat) * Math.cos(lon);
//        double y = (N + height) * Math.cos(lat) * Math.sin(lon);
//        double bOvera = semiminorAxis / semimajorAxis;
//        double z = (N * bOvera * bOvera + height) * Math.sin(lat);
//        double[] xyz = new double[]{x, y, z};
//        return xyz;
//    }
}
