package com.entgra.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MqttTest {

    private static String[] deviceIds = new String[]{"2L2HE00041", "2L2HF00077", "2L2HF00009", "8DOKD01887", "2L2HH00045", "2L2HE00072",
            "8DOHD21114", "8D0HD21183", "2L2HE00038", "8DOCM12936", "8DOHK04746", "PDOJE02788", "PDOJE01062", "PDOJE01578",
            "PDOJE01120", "PDOJE00976", "PDOJE027999", "PDOJE02758", "PDOJE02851", "PDOJE02752", "PDOJE00864",
            "PDOJE002851", "PDOJE002758", "8066771H", "test_board", "Bartac-2L1AE00254", "BARTAC-2L1HE00701",
            "Bartack-2L1HE00684", "123456789", "4DOAH25874", "LZOD41069", "Bartac-test", "A1587663", "LZODA37493", "0298996",
            "810836-overlock", "0302675-overlock", "0294643-overlock", "8066771H-overlock", "0273342-coverseam",
            "2L1WG00857", "0302776-Overlock", "8DODC11605-9000B", "MO584995-double-needle", "MO584995-doubleN",
            "2L1VK01641-bartack", "M0584995", "MO584995", "test_brother", "M852-13-overlock", "E7241531-brother-D",
            "201296-PFAFF", "D7740448-BROTHER", "D7Z35029-BORHTER", "00:4F:22:00:75:01", "00:4F:22:00:75:02", "00:4F:22:00:75:03",
            "00:4F:22:00:75:04"};

    public static void main(String[] args) {

        Random rand = new Random();

        for (int i = 0; i < deviceIds.length; i++) {
            Map<String, String> params = new HashMap<>();
            int randomNumber = rand.nextInt(1000);
            params.put("deviceID", deviceIds[i]);
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
        String topic = "carbon.super/SC920/" + params.get("deviceID") + "/events";
        String content = "{\"rtc\":%d,\"stc\":%d,\"ttc\":%d,\"ss\":%s,\"ct\":%d,\"ts\":%d}";
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
                boolean isStopped = getRandomBoolean();
                MqttMessage message = new MqttMessage(String.format(content, isStopped ? 0 : generateRandomInt(), isStopped ? 0 : generateRandomInt(),
                        isStopped ? 0 : generateRandomInt(), isStopped, isStopped ? 1 : 1001, Calendar.getInstance().getTimeInMillis() / 1000).getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.out.println("Message published : " + message.toString());
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
        }
    }

    int generateRandomInt() {
        Random rand = new Random();
        int x = rand.nextInt(1000);
        return x;
    }

    boolean getRandomBoolean() {
        return Math.random() < 0.5;
    }

}
