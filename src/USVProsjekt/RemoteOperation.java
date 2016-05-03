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

    public RemoteOperation(ThrustWriter thrustWriter) {
        thrustAllocator = new ThrustAllocator();
        this.thrustWrite = thrustWriter;
    }

    public void remoteOperate(double[] remoteCommand) {
        try {
            thrustWrite.setThrustForAll(thrustAllocator.calculateOutput(remoteCommand));
            thrustWrite.writeThrust();
        } catch (Exception ex) {
            System.out.println("exception ro");
        }
    }
}
