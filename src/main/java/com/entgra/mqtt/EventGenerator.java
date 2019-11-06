package com.entgra.mqtt;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class EventGenerator {

    static volatile int count = 0;
    static String ip = "";
    private static String jSessionId = "";

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String TOKEN = "TOKEN12345";

    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static SecureRandom random = new SecureRandom();

    public static final Map<String, Integer> DEVICE_PIECE_COUNT_MAP = new HashMap<>();

    public static void main(String[] args) {

        ip = args[0];
        String type = args[2];
        String mode = args[3];

        List<String> macIds = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[1]); i++) {
            macIds.add(type + "00:00:00:02:" + i);
        }

        count = macIds.size();
        if (args.length > 4) {
            jSessionId = args[4];
        }

        DeviceConfiguration deviceConfiguration;
        for (String macId : macIds) {

            if (!jSessionId.isEmpty()) {
                Device device = new Device();
                device.setDeviceName(macId);
                device.setDeviceIdentifier(macId.replace(":", "-"));
                device.setMacAddress(macId);
                device.setFwVersion("v1.0.0");
                device.setFloorId("1");
                device.setMachineCategory("Category 1");
                device.setManufacturer("Brother");
                device.setModel("BK-123");
                device.setType(type);
                device.setToken(TOKEN);
                device.setSerialNo(generateRandomString(5));
                if (args.length > 5) {
                    device.setLineId(Integer.parseInt(args[5]));
                }
                device.setLinePlacementId("");
                device.setLinePlacementX("");
                device.setLinePlacementY("");
                enrollDevice(device);
            }

            deviceConfiguration = getDeviceConfig(macId);
            if (deviceConfiguration != null) {
                Runnable threadedPublisher;
                if ("raw".equals(mode)) {
                    String key = deviceConfiguration.getDeviceType() + "-" + deviceConfiguration.getDeviceId();
                    DEVICE_PIECE_COUNT_MAP.put(key, 0);
                    threadedPublisher = new RawDataPublisher(deviceConfiguration, DEVICE_PIECE_COUNT_MAP);
                } else {
                    threadedPublisher = new SummaryDataPublisher(deviceConfiguration);
                }
                new Thread(threadedPublisher).start();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(">>>>>> Configurations are not available for device with MAC: " + macId);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(DEVICE_PIECE_COUNT_MAP);
            Properties properties = new Properties();

            for (Map.Entry<String,Integer> entry : DEVICE_PIECE_COUNT_MAP.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }

            try {
                properties.store(new FileOutputStream("test-results.properties"), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

    }

    public static String generateRandomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {

            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }

        return sb.toString();

    }

    public static void enrollDevice(Device device) {
        HttpResponse response;
        HttpPost executor = new HttpPost("https://" + ip + ":9443/dashboard/api/devices/enroll");

        executor.setEntity(new StringEntity(new Gson().toJson(device), ContentType.APPLICATION_JSON));
        executor.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", jSessionId);

        cookie.setDomain(ip);
        cookie.setPath("/dashboard");

        cookieStore.addCookie(cookie);

        CloseableHttpClient client = getHTTPClient(cookieStore);
        try {
            response = client.execute(executor);

            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("Device Successfully Enrolled to FP : " + device.getMacAddress());
            } else {
                System.out.println("Device Enrollment to Dashboard error : " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static DeviceConfiguration getDeviceConfig(String mac) {
        DeviceConfiguration configuration = null;
        HttpResponse response;
        HttpGet executor = new HttpGet("https://" + ip
                + ":9443/api/device-mgt-config/v1.0/configurations?properties=macAddress%3D" + encodeValue(mac));
        executor.setHeader("content-type", "application/x-www-form-urlencoded");
        executor.setHeader("token", TOKEN);


        CloseableHttpClient client = getHTTPClient();
        try {
            response = client.execute(executor);

            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("Successfully retrieved configurations from IoT Core for : " + mac);
                BufferedReader rd;
                try {
                    rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                    System.out.println(result.toString());
                    configuration = new Gson().fromJson(result.toString(), DeviceConfiguration.class);

                } catch (IOException e) {
                    System.out.println("Error while printing converting devices of group API output to Object");
                }

            } else {
                System.out.println("Device Config retrieval error : " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configuration;
    }


    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }


    public static CloseableHttpClient getHTTPClient(CookieStore cookieStore) {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(
                    builder.build());
            return HttpClients.custom().setSSLSocketFactory(
                    sslSF).setDefaultCookieStore(cookieStore).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static CloseableHttpClient getHTTPClient() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(
                    builder.build());
            return HttpClients.custom().setSSLSocketFactory(
                    sslSF).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}

class RawDataPublisher implements Runnable {

    private DeviceConfiguration deviceConfiguration;
    private Map<String, Integer> devicePiecesMap = new HashMap<>();


    public RawDataPublisher(DeviceConfiguration deviceConfiguration, Map<String, Integer> devicePiecesMap) {
        this.deviceConfiguration = deviceConfiguration;
        this.devicePiecesMap = devicePiecesMap;
    }

//    public void run() {
//        String topic = "carbon.super/" + deviceConfiguration.getDeviceType() + "/" + deviceConfiguration.getDeviceId() + "/events";
//        String content = "{\"rotations\":%d,\"stitches\":%d,\"trims\":%d,\"state\":%s,\"cycle\":%d,\"ts\":%d}";
//        int qos = 0;
//        String broker = "tcp://" + EventGenerator.ip + ":1886";
//        String clientId = "fpd/" + deviceConfiguration.getTenantDomain() + "/" + deviceConfiguration.getDeviceType() +
//                "/" + deviceConfiguration.getDeviceId() + "/test";
//        MemoryPersistence persistence = new MemoryPersistence();
//        try {
//            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
//            MqttConnectOptions connOpts = new MqttConnectOptions();
//            connOpts.setUserName(deviceConfiguration.getAccessToken());
//            connOpts.setCleanSession(true);
//            System.out.println("Connecting to broker: " + broker + " topic: " + topic);
//            sampleClient.connect(connOpts);
//            System.out.println("Connected " + clientId);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            int r1 = 1, r2 = 5, r3 = 20, r4 = 10;
//            long lastPush = System.currentTimeMillis();
//            while (true) {
//                boolean isStopped = r1 < r2 && r2 < r3 && r3 > r4 && EventGenerator.getRandomNumberInRange(0, 1) == 0;
//                int r = 0;
//                if (!isStopped) {
//                    if (r2 < r3 && r3 < r4 && r4 < 100 && r4 > 2) {
//                        r = EventGenerator.getRandomNumberInRange(1, r4 - 1);
//                    } else if (r2 > r3 && r3 > r4 && r4 > 1 && r4 < 99) {
//                        r = EventGenerator.getRandomNumberInRange(r4 + 1, 100);
//                    } else {
//                        r = EventGenerator.getRandomNumberInRange(1, 100);
//                    }
//                }
//                r1 = r2;
//                r2 = r3;
//                r3 = r4;
//                r4 = r;
//                long currentTs = System.currentTimeMillis();
//                MqttMessage message = new MqttMessage(String.format(content, r, r,
//                        isStopped ? 1 : 0, isStopped, isStopped ? 1 : (currentTs - lastPush),
//                        currentTs / 1000).getBytes());
//                lastPush = System.currentTimeMillis();
//                message.setQos(qos);
//                sampleClient.publish(topic, message);
//                //System.out.println("Message published : " + message.toString());
//                try {
//                    Thread.sleep(1000 + (isStopped ? EventGenerator.getRandomNumberInRange(1000, 10000) : 0));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        } catch (MqttException me) {
//            System.out.println("reason " + me.getReasonCode());
//            System.out.println("msg " + me.getMessage());
//            System.out.println("loc " + me.getLocalizedMessage());
//            System.out.println("cause " + me.getCause());
//            System.out.println("excep " + me);
//            me.printStackTrace();
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> " + --EventGenerator.count);
//        }
//    }


    public void run() {
        String topic = "carbon.super/" + deviceConfiguration.getDeviceType() + "/" + deviceConfiguration.getDeviceId() + "/events";
        String content = "{\"rotations\":%d,\"stitches\":%d,\"trims\":%d,\"state\":%s,\"cycle\":%d,\"ts\":%d}";
        int qos = 0;
        String broker = "tcp://" + EventGenerator.ip + ":1886";
        String clientId = "fpd/" + deviceConfiguration.getTenantDomain() + "/" + deviceConfiguration.getDeviceType() +
                          "/" + deviceConfiguration.getDeviceId() + "/test";
        MemoryPersistence persistence = new MemoryPersistence();
        String key = deviceConfiguration.getDeviceType() + "-" + deviceConfiguration.getDeviceId();
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

            long lastPush = System.currentTimeMillis();
            while (true) {
                int[] arr = generate5SecondSequence();
                for(int val : arr){
                    boolean isStopped = val > 0 && EventGenerator.getRandomNumberInRange(0, 1) == 0;
                    long currentTs = System.currentTimeMillis();
                    MqttMessage message = new MqttMessage(String.format(content, val, val,
                                                                        isStopped ? 1 : 0, isStopped, isStopped ? 1 : (currentTs - lastPush),
                                                                        currentTs / 1000).getBytes());
                    lastPush = System.currentTimeMillis();
                    message.setQos(qos);
                    sampleClient.publish(topic, message);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                devicePiecesMap.put(key, devicePiecesMap.get(key) + 1);
            }

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> " + --EventGenerator.count);
        }
    }

    private static int[] generate5SecondSequence(){
        int[] arr = sort(generateSequence(3, 35));

        int[] paddedArr = introducePadding(arr, 5);

        return paddedArr;
    }

    private static int[] generateSequence(int count, int finalSum)
    {
        Random r = new Random();
        int numbers[] = new int[count];
        int sum = 0;
        for (int i = 0; i < count - 1; i++)
        {
            numbers[i] = r.nextInt((finalSum - sum) / 3) + 1;
            sum += numbers[i];
        }
        numbers[count - 1] = finalSum - sum;

        return numbers;
    }

    private static int[] sort(int[] arr){
        int[] sorted = arr.clone();
        final int center = (arr.length / 2) + (arr.length % 2) - 1;
        int max = 0;

        int index = 0;
        int maxIndex = 0;
        for(int i : arr){
            if(max < i){
                max = i;
                maxIndex = index;
            }
            index++;
        }
        if(sorted[center]!=max){
            int temp = sorted[center];
            sorted[center] = max;
            sorted[maxIndex] = temp;
        }
        return sorted;
    }

    private static int[] introducePadding(int[] sequence, int paddedSequenceSize){
        if(paddedSequenceSize < sequence.length)
            return null;
        int padding;
        int diff = paddedSequenceSize - sequence.length;
        int paddedSequence[] = new int[paddedSequenceSize];

        int mod = paddedSequenceSize % sequence.length;
        if(mod % 2 > 0){
            padding = (diff - 1) / 2;
        } else {
            padding = diff / 2;
        }

        for (int i = 0; i < sequence.length ; i ++){
            paddedSequence[i + padding] = sequence[i];
        }
        return paddedSequence;
    }

}

class SummaryDataPublisher implements Runnable {

    private DeviceConfiguration deviceConfiguration;

    public SummaryDataPublisher(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    public void run() {
        WebSocket websocket;
        try {
            websocket = new WebSocketFactory()
                    .createSocket("ws://" + EventGenerator.ip + ":9765/inputwebsocket/5min_summary_carbon.super_WS_receiver")
                    .connect();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        while (true) {
            int pieceCount = EventGenerator.getRandomNumberInRange(0, 10);
            int utilizedTime = 0;
            if (pieceCount != 0) {
                utilizedTime = EventGenerator.getRandomNumberInRange(pieceCount * 10, 300);
            }
            int activeTime = 300;
            if (utilizedTime < 300) {
                activeTime = EventGenerator.getRandomNumberInRange(utilizedTime, 300);
            }
            // send message to websocket
            websocket.sendText("{'event': {'metaData': {'deviceId': '" + deviceConfiguration.getDeviceId() +
                    "', 'deviceType': '" + deviceConfiguration.getDeviceType() + "', 'timestamp': " +
                    System.currentTimeMillis() / 1000 + "}, 'payloadData': {'pieceCount': "+ pieceCount
                    +", 'activeTime': "+activeTime+", 'utilizedTime': "+utilizedTime+"}}}");

            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}