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

    private ThrustAllocator thrustAlloc;
    private ThrustWriter thrustWrite;

    public RemoteOperation(ThrustWriter thrustWriter) {
        thrustAlloc = new ThrustAllocator(0, 0, 0, 0);
        this.thrustWrite = thrustWriter;
    }

    public void remoteOperate(double[] remoteCommand) {
        try {
            thrustWrite.setThrustForAll(thrustAlloc.calculateOutput(remoteCommand));
        } catch (Exception ex) {
            System.out.println("exception ro");
        }
        thrustWrite.writeThrust();
    }
}
