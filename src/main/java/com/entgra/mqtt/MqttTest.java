package com.entgra.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MqttTest {

    static volatile int count = 0;
    static String ip = "";

    public static void main(String[] args) {

        ip = args[0];

        List<String> macIds = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[1]); i++) {
            macIds.add("00:00:00:00:" + i);
        }

        count = macIds.size();
        DeviceConfiguration deviceConfiguration;
        for (String macId : macIds) {
            deviceConfiguration = new DeviceConfiguration(); //TODO: Get this from config endpoint per device
            Runnable threadedPublisher = new MultiThreadedPublisher(deviceConfiguration);
            new Thread(threadedPublisher).start();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

class MultiThreadedPublisher implements Runnable {

    private DeviceConfiguration deviceConfiguration;

    public MultiThreadedPublisher(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    public void run() {
        String topic = "carbon.super/" + deviceConfiguration.getDeviceType() + "/" + deviceConfiguration.getDeviceId() + "/events";
        String content = "{\"rotations\":%d,\"stitches\":%d,\"trims\":%d,\"state\":%s,\"cycle\":%d,\"timestamp\":%d}";
        int qos = 0;
        String broker = "tcp://" + MqttTest.ip + ":1886";
        String clientId = "fpd/" + deviceConfiguration.getTenantDomain() + "/" + deviceConfiguration.getDeviceType() +
                "/" + deviceConfiguration.getDeviceId() + "/test";
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(deviceConfiguration.getAccessToken());
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker + " topic: " + topic);
            sampleClient.connect(connOpts);
            System.out.println("Connected " + clientId);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                boolean isStopped = getRandomBoolean();
                MqttMessage message = new MqttMessage(String.format(content, isStopped ? 0 : generateRandomInt(), isStopped ? 0 : generateRandomInt(),
                        isStopped ? 0 : generateRandomInt(), isStopped, isStopped ? 1 : 1001, Calendar.getInstance().getTimeInMillis() / 1000).getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                //System.out.println("Message published : " + message.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> " + --MqttTest.count);
        }
    }

    private int generateRandomInt() {
        Random rand = new Random();
        int x = rand.nextInt(1000);
        return x;
    }

    private boolean getRandomBoolean() {
        return Math.random() < 0.5;
    }

}
