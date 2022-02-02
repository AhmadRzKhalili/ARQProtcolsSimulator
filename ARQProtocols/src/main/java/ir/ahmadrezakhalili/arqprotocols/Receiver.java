package ir.ahmadrezakhalili.arqprotocols;

import javafx.scene.control.TextArea;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

//Transmitter and receiver must execute at the same time and in parallel, so they are runnable
public class Receiver implements Runnable {
    private Socket connection;
    private String result = "";
    private String protocol;
    private TextArea resultTxt;

    public Receiver(String protocol, TextArea resultTxt) {
        setProtocol(protocol);
        setResultTxt(resultTxt);
    }

    public void receive() throws SocketException {
        try {
            int[] framesArr;

            result = "";

            Random rands = new Random();
            int rand = 0;

            InetAddress address = InetAddress.getByName("Localhost");
            connection = new Socket(address, 8081);         //Port 8081 is for transmission control protocol
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            DataInputStream in = new DataInputStream(connection.getInputStream());

            int numOfFrames = in.read();        //Reading number of frame which must be read from the socket stream
            framesArr = new int[numOfFrames];
            result = result + "Number of Frames: " + numOfFrames + "\n";

            for (int i = 0; i < numOfFrames; i++) {
                framesArr[i] = in.read();       //Reading frames
            }

            //This part simulate bit error and assign 0 value to a random frame
            rand = rands.nextInt(numOfFrames);//FRAME NO. IS RANDOMLY GENERATED
            framesArr[rand] = 0;

            for (int i = 0; i < numOfFrames; i++)
            {
                result = result + "Received frame is: " + framesArr[i] + "\n";
            }

            ArrayList<Integer> retransmitIndex = new ArrayList<>();     //Index of the frames which have 0 value in the array
            for (int i = 0; i < numOfFrames; i++)                       //must be stored for further requesting for their retransmission
                if (framesArr[i] == 0) {
                    result = result + "Request to retransmit frame no " + (i + 1) + " again." + "\n";
                    retransmitIndex.add(i);
                }

            //Writing number of frames with bit error or time out for the transmitter
            out.write(retransmitIndex.size());
            out.flush();

            for (int i = 0; i < retransmitIndex.size(); i++) {
                out.write(retransmitIndex.get(i));      //Writing index of frames with 0 value for the transmitter, so it can retransmit them
                out.flush();
            }

            //This part is for reading retransmitted frames
            for (int i = 0; i < retransmitIndex.size(); i++) {
                if (getProtocol().equals("Selective Repeat")) {
                    framesArr[retransmitIndex.get(i)] = in.read();
                    result = result + "Received frame is: " + framesArr[retransmitIndex.get(i)] + "\n";
                } else if (getProtocol().equals("GBN")) {
                    System.out.println("********************\n");
                    for (int j = retransmitIndex.get(i); j < numOfFrames; j++) {
                        framesArr[j] = in.read();
                        System.out.println("rx frame: " + framesArr[j]);
                        result = result + "Received frame is: " + framesArr[j] + "\n";
                    }
                    System.out.println("********************\n");
                }
            }

            for (int i = 0; i < framesArr.length; i++) {
                System.out.println(framesArr[i]);
            }

            result = result + "quiting" + "\n";
        } catch (Exception e) {
            result = "No request exists.";
        }

    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public TextArea getResultTxt() {
        return resultTxt;
    }

    public void setResultTxt(TextArea resultTxt) {
        this.resultTxt = resultTxt;
    }

    public String getResult() {
        return result;
    }

    public void run() {
        try {
            receive();
            getResultTxt().setText(getResult());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
