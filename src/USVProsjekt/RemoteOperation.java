/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package USVProsjekt;

/**
 *
 * @author Albert
 */
public class RemoteOperation {

    private ThrustAllocator thrustAllocator;
    private ThrustWriter thrustWrite;
    private RotationWriter rotationWriter;

    public RemoteOperation(ThrustWriter thrustWriter, RotationWriter rotationWriter) {
        thrustAllocator = new ThrustAllocator();
        this.thrustWrite = thrustWriter;
        this.rotationWriter = rotationWriter;
    }

    public void remoteOperate(double[] remoteCommand) {
        try {
            double[] r = thrustAllocator.calculateOutput(remoteCommand);
            thrustWrite.setThrustForAll(r);
            thrustWrite.writeThrust();
            rotationWriter.setRotationForAll(r);
            rotationWriter.writeRotation();
        } catch (Exception ex) {
            System.out.println("exception ro");
        }
    }
}
