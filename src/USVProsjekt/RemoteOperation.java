/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

import java.util.Arrays;

/**
 *
 * @author Albert
 */
public class RemoteOperation {

    private ThrustAllocUSV thrustAllocator;
    private ThrustWriter thrustWrite;
    private RotationWriter rotationWriter;

    public RemoteOperation(ThrustWriter thrustWriter, RotationWriter rotationWriter) {
        thrustAllocator = new ThrustAllocUSV();
        this.thrustWrite = thrustWriter;
        this.rotationWriter = rotationWriter;
    }

    public void remoteOperate(double[] remoteCommand) {
        System.out.println("Remote command: " + Arrays.toString(remoteCommand));
        try {
            double[] r = thrustAllocator.calculateOutput(remoteCommand, false);
            System.out.println(Arrays.toString(r));
            thrustWrite.setThrustForAll(r);
            thrustWrite.writeThrust();
            rotationWriter.setRotationForAll(r);
            rotationWriter.writeRotation();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            System.out.println("exception ro");
        }
    }
}
