package ir.ahmadrezakhalili.arqprotocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

//Transmitter and receiver must execute at the same time and in parallel, so they are runnable
public class Transmitter implements Runnable {
    private ServerSocket serverSocket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private int[] framesArr;
    private int windowSize;
    long totalDelay = 0;
    private String protocol;

    private double TIME_LIMIT = 3;

    public Transmitter(String protocol, int[] framesArr, int windowSize) {
        setProtocol(protocol);
        setFramesArr(framesArr);
        setWindowSize(windowSize);
    }

    public void transmit() throws SocketException
    {

        try
        {
            int numOfFrames = framesArr.length;
            serverSocket = new ServerSocket(8081);      //Port 8081 is for transmission control protocol

            Socket client = serverSocket.accept();
            dis = new DataInputStream(client.getInputStream());
            dos = new DataOutputStream(client.getOutputStream());

            //Writing number of frames in order to make receiver acknowledged
            //how many frames it must read
            dos.write(numOfFrames);
            dos.flush();

            int transmitted = windowSize;
            long delay = 1 + (long) (Math.random() * (5 - 1));  //Creating random delay times for each frame
            for (int i = 0; i < numOfFrames; i++)
            {
                totalDelay += delay;
                try {
                    TimeUnit.SECONDS.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Frame no"+ (i + 1) + " delay: " + (delay * 1000) + " milliseconds");

                if (delay > TIME_LIMIT) {               //If a frame delay is more than time out limit program will print time out error
                    System.err.println("TIME_OUT!");
                    dos.write(256);                  //In case of time out, program will send 0 (256 two's complement) as the frame
                }
                else
                    dos.write(framesArr[i]);            //Writing frames into socket
                dos.flush();

                transmitted--;
                if (transmitted == 0) {
                    transmitted = windowSize;
                }
                delay = 1 + (long) (Math.random() * (5 - 1));  //Creating random delay times for each frame
            }

            int numOfRetransmit = dis.read();                 //Reading number of frames which receiver wants to be retransmitted
            int indexOfRequestedFrame;

            for (int i = 0; i < numOfRetransmit; i++) {
                indexOfRequestedFrame = dis.read();           //Reading index of the frame in the frames array which must be retransmitted

                if (getProtocol().equals("Selective Repeat")) {
                    dos.write(framesArr[indexOfRequestedFrame]);    //Retransmitting a specific frame (Selective Repeat)
                    dos.flush();
                } else if (getProtocol().equals("GBN")) {
                    for (int j = indexOfRequestedFrame; j < numOfFrames; j++) {
                        System.out.println("tx frame: " + framesArr[j]);    //Retransmitting all the frames from a specific frame (GBN)
                        dos.write(framesArr[j]);
                        dos.flush();
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        finally
        {
            try
            {
                dis.close();
                dos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    public void setFramesArr(int[] framesArr) {
        this.framesArr = framesArr;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public long getTotalDelay() {
        return totalDelay;
    }

    public void run() {
        try {
            transmit();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
