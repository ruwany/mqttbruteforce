package com.entgra.mqtt;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
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
            createDeviceType(type);
        }

        DeviceConfiguration deviceConfiguration;
        for (String macId : macIds) {

            if (!jSessionId.isEmpty()) {
                Device device = new Device();
                device.setDeviceName(macId);
                device.setDeviceIdentifier(macId.replace(":", "-"));
                device.setMacAddress(macId);
                device.setFwVersion("v1.0.0");
                device.setFloorId(1);
                device.setMachineCategory("Category 1");
                device.setManufacturer("Brother");
                device.setModel("BK-123");
                device.setType(type);
                device.setToken(TOKEN);
                device.setSerialNo(generateRandomString(5));
                if (args.length > 5) {
                    device.setGroupId(Integer.parseInt(args[5]));
                }
                enrollDevice(device);
            }

            deviceConfiguration = getDeviceConfig(macId);
            if (deviceConfiguration != null) {
                Runnable threadedPublisher;
                if ("raw".equals(mode)) {
                    threadedPublisher = new RawDataPublisher(deviceConfiguration);
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

    }

    private static void createDeviceType(String type) {
        HttpResponse response;
        HttpPost executor = new HttpPost("http://" + ip + ":9763/dashboard/api/device-types/create");
        String typeJson = "{\"name\":\"" + type +
                "\",\"deviceTypeMetaDefinition\":{\"properties\":[\"macAddress\",\"fwVersion\",\"floorId\",\"token\"," +
                "\"manufacturer\",\"model\",\"machineCategory\",\"serialNo\",\"placementId\",\"placementX\",\"placementY\"]," +
                "\"pushNotificationConfig\":{\"type\":\"MQTT\",\"scheduled\":true},\"description\":\"Test " + type +
                "\",\"features\":[{\"id\":0,\"code\":\"upgrade_firmware\",\"name\":\"Upgrade Firmware\"," +
                "\"description\":\"Update Device Firmware\",\"deviceType\":\"" + type + "\"}]}," +
                "\"propertyMappings\":[{\"name\":\"rotations\",\"type\":\"INT\"},{\"name\":\"stitches\",\"type\":\"INT\"}," +
                "{\"name\":\"trims\",\"type\":\"INT\"},{\"name\":\"state\",\"type\":\"BOOL\"},{\"name\":\"cycle\",\"type\":\"INT\"}]}";

        executor.setEntity(new StringEntity(typeJson, ContentType.APPLICATION_JSON));
        executor.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", jSessionId);

        cookie.setDomain(ip);
        cookie.setPath("/dashboard");

        cookieStore.addCookie(cookie);

        CloseableHttpClient client = getHTTPClient(cookieStore);
        try {
            response = client.execute(executor);

            if (response.getStatusLine().getStatusCode() == 201) {
                System.out.println("Device type created");
            } else {
                System.out.println("Device type creation fail. Status: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        HttpPost executor = new HttpPost("http://" + ip + ":9763/dashboard/api/devices/enroll");

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

    public static DeviceRef getDeviceRef(String tenantDomain, String deviceType, String deviceId) {
        DeviceRef deviceRef = null;
        HttpResponse response;
        HttpGet executor = new HttpGet("http://" + ip
                + ":9763/dashboard/api/devices/" + tenantDomain + "/" + deviceType + "/" + deviceId);
        executor.setHeader("content-type", "application/json");

        CloseableHttpClient client = getHTTPClient();
        try {
            response = client.execute(executor);

            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("Successfully retrieved device ref");
                BufferedReader rd;
                try {
                    rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                    System.out.println(result.toString());
                    deviceRef = new Gson().fromJson(result.toString(), DeviceRef.class);

                } catch (IOException e) {
                    System.out.println("Error while printing converting devices of group API output to Object");
                }

            } else {
                System.out.println("Device Config retrieval error : " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deviceRef;
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

    public RawDataPublisher(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    public void run() {
        String topic = deviceConfiguration.getTenantDomain() + "/" + deviceConfiguration.getDeviceType() + "/" + deviceConfiguration.getDeviceId() + "/events";
        String content = "{\"rotations\":%d,\"stitches\":%d,\"trims\":%d,\"state\":%s,\"cycle\":%d,\"ts\":%d}";
        int qos = 0;
        String broker = "tcp://" + EventGenerator.ip + ":1886";
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
            int r1 = 1, r2 = 5, r3 = 20, r4 = 10;
            long lastPush = System.currentTimeMillis();
            while (true) {
                boolean isStopped = r1 < r2 && r2 < r3 && r3 > r4 && EventGenerator.getRandomNumberInRange(0, 1) == 0;
                int r = 0;
                if (!isStopped) {
                    if (r2 < r3 && r3 < r4 && r4 < 100 && r4 > 2) {
                        r = EventGenerator.getRandomNumberInRange(1, r4 - 1);
                    } else if (r2 > r3 && r3 > r4 && r4 > 1 && r4 < 99) {
                        r = EventGenerator.getRandomNumberInRange(r4 + 1, 100);
                    } else {
                        r = EventGenerator.getRandomNumberInRange(1, 100);
                    }
                }
                r1 = r2;
                r2 = r3;
                r3 = r4;
                r4 = r;
                long currentTs = System.currentTimeMillis();
                MqttMessage message = new MqttMessage(String.format(content, r, r,
                        isStopped ? 1 : 0, isStopped, isStopped ? 1 : (currentTs - lastPush),
                        currentTs / 1000).getBytes());
                lastPush = System.currentTimeMillis();
                message.setQos(qos);
                sampleClient.publish(topic, message);
                //System.out.println("Message published : " + message.toString());
                try {
                    Thread.sleep(1000 + (isStopped ? EventGenerator.getRandomNumberInRange(1000, 10000) : 0));
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
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> " + --EventGenerator.count);
        }
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
            DeviceRef deviceRef = EventGenerator.getDeviceRef(deviceConfiguration.getTenantDomain(),
                    deviceConfiguration.getDeviceType(), deviceConfiguration.getDeviceId());
            if (!"Yard".equals(deviceRef.getStyleName()) && !"Maintenance".equals(deviceRef.getStyleName())) {
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
                String event = "{'event': {'metaData': {'tenantDomain': '" + deviceConfiguration.getTenantDomain() +
                        "', 'deviceId': '" + deviceConfiguration.getDeviceId() + "', 'deviceType': '" +
                        deviceConfiguration.getDeviceType() + "', 'timestamp': " + System.currentTimeMillis() / 1000 +
                        "}, 'payloadData': {'pieceCount': " + pieceCount + ", 'styleId': " + deviceRef.getStyleId() +
                        ", 'placementId': " + deviceRef.getPlacementId() + ", 'activeTime': " + activeTime +
                        ", 'utilizedTime': " + utilizedTime + "}}}";
                websocket.sendText(event);
                System.out.println("Published event: " + event);
            }
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
                websocket.sendClose();
                e.printStackTrace();
                break;
            }
        }
    }

}