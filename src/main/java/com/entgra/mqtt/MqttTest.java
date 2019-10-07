package com.entgra.mqtt;

import com.google.gson.Gson;
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

public class MqttTest {

    static volatile int count = 0;
    static String ip = "";
    static String type = "";
    static String jSessionId = "";

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String TOKEN = "TOKEN12345";

    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static SecureRandom random = new SecureRandom();

    public static void main(String[] args) {

        ip = args[0];
        type = args[2];

        List<String> macIds = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[1]); i++) {
            macIds.add(type + "00:00:00:02:" + i);
        }

        count = macIds.size();
        if (args.length > 3) {
            jSessionId = args[3];
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
                if (args.length > 4) {
                    device.setLineId(Integer.parseInt(args[4]));
                }
                device.setLinePlacementId("");
                device.setLinePlacementX("");
                device.setLinePlacementY("");
                enrollDevice(device);
            }

            deviceConfiguration = getDeviceConfig(macId);
            if (deviceConfiguration != null) {
                Runnable threadedPublisher = new MultiThreadedPublisher(deviceConfiguration);
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
}

class MultiThreadedPublisher implements Runnable {

    private DeviceConfiguration deviceConfiguration;

    public MultiThreadedPublisher(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    public void run() {
        String topic = "carbon.super/" + deviceConfiguration.getDeviceType() + "/" + deviceConfiguration.getDeviceId() + "/events";
        String content = "{\"rotations\":%d,\"stitches\":%d,\"trims\":%d,\"state\":%s,\"cycle\":%d,\"ts\":%d}";
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
            int r1 = 0, r2 = 0, r3 = 0;
            long lastPush = System.currentTimeMillis();
            while (true) {
                boolean isStopped = r1 < r2 && r2 > r3;
                int r = generateRandomInt();
                if (isStopped) {
                    r = 0;
                }
                r1 = r2;
                r2 = r3;
                r3 = r;
                long currentTs = System.currentTimeMillis();
                MqttMessage message = new MqttMessage(String.format(content, r, isStopped ? 0 : generateRandomInt(),
                        isStopped ? 0 : generateRandomInt(), isStopped, isStopped ? 1 : (currentTs - lastPush),
                        currentTs / 1000).getBytes());
                lastPush = System.currentTimeMillis();
                message.setQos(qos);
                sampleClient.publish(topic, message);
                //System.out.println("Message published : " + message.toString());
                try {
                    Thread.sleep(1000 + (isStopped ? 200 * generateRandomInt() : 0));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isStopped) {
                    currentTs = System.currentTimeMillis();
                    message = new MqttMessage(String.format(content, 0, 0, 0, false, 1, currentTs / 1000).getBytes());
                    lastPush = System.currentTimeMillis();
                    message.setQos(qos);
                    sampleClient.publish(topic, message);
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
        return rand.nextInt(100) + 1;
    }

}
