package ir.ahmadrezakhalili.arqprotocols;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.SocketException;
import java.util.Scanner;

public class ARQController {
    @FXML
    private TextField frameFileNameTxt;
    @FXML
    private TextField windowSizeTxt;
    @FXML
    private TextArea frameTxt;
    @FXML
    private TextArea resultTxt;
    @FXML
    private RadioButton srRBtn;
    @FXML
    private RadioButton gbnRBtn;

    private int[] framesArr;
    private int windowSize;
    private String protocol;
    private boolean framesAvailable = false;
    private String result = "";

    private long startTime;
    private long endTime;
    private Transmitter tx;
    private Receiver rx;

    @FXML
    protected void onInsertButtonClick() {
        File file = new File("./src/main/resources/ir/ahmadrezakhalili/arqprotocols/files/"
                            + frameFileNameTxt.getText());
        try {
            Scanner reader = new Scanner(file);

            String framesStr = "";
            int i = 0;
            while(reader.hasNextLine()) {
                framesStr = framesStr + reader.nextLine() + "\n";
                i++;
            }
            frameTxt.setText(framesStr);

            reader = new Scanner(file);
            framesArr = new int[i];
            i = 0;
            while(reader.hasNextLine()) {
                framesArr[i] = Integer.parseInt(reader.nextLine());
                i++;
            }
            reader.close();
            framesAvailable = true;

        } catch (FileNotFoundException e) {
            frameTxt.setText("File Not Found!");
            framesAvailable = false;
        }
    }

    @FXML
    protected void onTxButtonClick() throws InterruptedException {

        if (!srRBtn.isSelected() && !gbnRBtn.isSelected()) {
            resultTxt.setText("Select ARQ protocol.");
        } else if (!framesAvailable) {
            resultTxt.setText("No frame inserted.");
        } else if (!isDigits(windowSizeTxt.getText())) {
            resultTxt.setText("Invalid window size.");
        } else {
            if (srRBtn.isSelected())
                protocol = srRBtn.getText();
            else
                protocol = gbnRBtn.getText();
            windowSize = Integer.parseInt(windowSizeTxt.getText());

            Transmitter transmitter = new Transmitter(protocol ,framesArr, windowSize);
            tx = transmitter;
            Thread txThread = new Thread(transmitter);
            txThread.start();
            startTime = System.currentTimeMillis();

            result = "";
            result = result + "Waiting for connection...";
            resultTxt.setText(result);
        }
    }

    @FXML
    protected void onRxButtonClick() throws InterruptedException {
        result = "Request accepted.\nSending...";
        resultTxt.setText(result);
        Receiver receiver = new Receiver(protocol, resultTxt);
        rx = receiver;
        Thread rxThread = new Thread(receiver);
        rxThread.start();
        rxThread.join();
        endTime = System.currentTimeMillis();
        resultTxt = rx.getResultTxt();
        result = resultTxt.getText();
        calExeTime();
    }

    private boolean isDigits(String str) {
        return str.matches("\\d+");
    }

    public void calExeTime() {
        result = result + "\nTotal delay: " + tx.getTotalDelay() * 1000 + " milliseconds";
        result = result + "\nTime taken considering the total delay: " + ((endTime - startTime)) + " milliseconds";
        result = result + "\nTime taken not considering the total delay: " + ((endTime - startTime) - tx.getTotalDelay() * 1000) + " milliseconds";
        resultTxt.setText(result);
    }
}