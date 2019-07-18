package com.entgra.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MqttTest {

    private static String[] originalDevices = new String[]{"2L2HE00041", "2L2HF00077", "2L2HF00009", "8DOKD01887",
            "2L2HH00045", "2L2HE00072", "8DOHD21114", "8D0HD21183", "2L2HE00038", "8DOCM12936", "8DOHK04746",
            "PDOJE02788", "PDOJE01062", "PDOJE01578", "PDOJE01120", "PDOJE00976", "PDOJE027999", "PDOJE02758",
            "PDOJE02851", "PDOJE02752", "PDOJE00864", "PDOJE002851", "PDOJE002758", "8066771H", "test_board",
            "Bartac-2L1AE00254", "BARTAC-2L1HE00701", "Bartack-2L1HE00684", "123456789", "4DOAH25874", "LZOD41069",
            "Bartac-test", "A1587663", "LZODA37493", "0298996", "810836-overlock", "0302675-overlock",
            "0294643-overlock", "8066771H-overlock", "0273342-coverseam", "2L1WG00857", "0302776-Overlock",
            "8DODC11605-9000B", "MO584995-double-needle", "MO584995-doubleN", "2L1VK01641-bartack", "M0584995",
            "MO584995", "test_brother", "M852-13-overlock", "E7241531-brother-D", "201296-PFAFF"};

    static volatile int count = 0;

    public static void main(String[] args) {

        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[0]); i++) {
            deviceIds.add("Test_" + i);
        }

        deviceIds.addAll(Arrays.asList(originalDevices));

        count = deviceIds.size();
        int cid = 1;

        for (String deviceId : deviceIds) {
            Map<String, String> params = new HashMap<>();
            params.put("deviceID", deviceId);
            params.put("clientID", "test_client_" + cid++);

            Runnable threadedPublisher = new MultiThreadedPublisher(params);
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

    private Map<String, String> params;

    public MultiThreadedPublisher(Map<String, String> params) {
        this.params = params;
    }

    public void run() {
        String topic = "carbon.super/SC920/" + params.get("deviceID") + "/events";
        String content = "{\"rtc\":%d,\"stc\":%d,\"ttc\":%d,\"ss\":%s,\"ct\":%d,\"ts\":%d}";
        int qos = 0;
        String broker = "tcp://192.168.8.135:1886";
        String clientId = params.get("clientID");
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
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
