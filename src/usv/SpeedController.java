/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usv;

import gnu.io.SerialPortEvent;
import java.io.BufferedReader;

/**
 *
 * @author RobinBergseth
 */
public class SpeedController extends SerialHandeler {

    private BufferedReader input;
    private int speedA = 0;
    private int speedB = 0;
    private int speedC = 0;

    public SpeedController(String comPort) {
        super.connect(comPort);
        this.input = super.getInpuReader();
    }

    @Override
    public void serialEvent(SerialPortEvent spe) {
        if (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                String inputLine = input.readLine();
                if (inputLine.contains("SM")) {
                    String[] pos = inputLine.split("SM");
                    for (String s : pos) {
                        if (s.equals("")) {
                            continue;
                        }
                        char[] chars = s.toCharArray();
                        String val = "";
                        for (int x = 1; x < chars.length; x++) {
                            val += chars[x];
                        }
                        switch (chars[0]) {
                            case '1':
                                speedA = Integer.parseInt(val);
                                break;
                            case '2':
                                speedB = Integer.parseInt(val);
                                break;
                            case '3':
                                speedC = Integer.parseInt(val);
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }

    public boolean sendSpeed(int[] motors, int[] vals) {
        String data = "$";
        if (motors.length != vals.length) {
            return false;
        } else {
            String[] values = new String[vals.length];
            for (int x = 0; x < vals.length; x++) {
                if (vals[x] < 10) {
                    values[x] = "00" + vals[x];
                } else if (vals[x] < 100) {
                    values[x] = "0" + vals[x];
                } else {
                    values[x] = "" + vals[x];
                }
            }
            for (int x = 0; x < motors.length; x++) {
                switch (motors[x]) {
                    case 1:
                        data += "A" + values[x];
                        break;
                    case 2:
                        data += "B" + values[x];
                        break;
                    case 3:
                        data += "C" + values[x];
                        break;
                }
            }
            data += "%";
            System.out.println(data);
            boolean sendResult = send(data);
            return sendResult;
        }
    }

    public void requestSpeed() {
        String request = "$S%";
        send(request);
    }

    public int[] getSpeeds() {
        int[] speeds = {speedA, speedB, speedC};
        return speeds;
    }
}
