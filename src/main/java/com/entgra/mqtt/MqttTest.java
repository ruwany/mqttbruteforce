package com.entgra.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MqttTest {

    public static void main(String[] args) {

        Random rand = new Random();

        int threadCount = 100;

        for (int i = 0; i < threadCount; i++) {
            Map<String, String> params = new HashMap<>();
            int randomNumber = rand.nextInt(1000);
            params.put("deviceID", "device_" + i);
            params.put("clientID", String.valueOf(randomNumber));

            Runnable threadedPublisher = new MultiThreadedPublisher(params);
            new Thread(threadedPublisher).start();
        }

    }
}


class MultiThreadedPublisher implements Runnable {

    Map params;

    public MultiThreadedPublisher(Map params) {
        this.params = params;
    }

    public void run() {

        System.out.println("Thread " + Thread.currentThread().getId() + " is running");

        String topic = "/carbon.super/SC920/" + params.get("deviceID") + "/events"; //TODO: Use actual device IDs
        String content = "{\"rtc\":%d,\"stc\":%d,\"ttc\":%d,\"ss\":%s,\"ct\":%d,\"ts\":%d}"; //TODO: Populate with dummy values
        int qos = 0;
        String broker = "tcp://localhost:1886";
        String clientId = "test-" + params.get("clientID");
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: " + content);

            while (true) {
                MqttMessage message = new MqttMessage(String.format(content, generateRandomInt(), generateRandomInt(),
                        generateRandomInt(), getRandomBoolean(), generateRandomInt(), generateRandomInt()).getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.out.println("Message published : " + message.getPayload());
            }

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    int generateRandomInt(){
        Random rand = new Random();
        int x = rand.nextInt(1000);
        return x;
    }

    boolean getRandomBoolean() {
        return Math.random() < 0.5;
    }

}
