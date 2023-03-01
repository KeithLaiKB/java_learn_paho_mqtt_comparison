package com.learn.paho_mqtt_one.receiver_tls;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * 
 * <p>
 * 							description:																			</br>	
 * &emsp;						It would use the authentication(user name and password).							</br>
 * &emsp;&emsp;						because some broker(like mosquitto, etc) needs authentication, 					</br>
 * &emsp;&emsp;						if your client is not in the same local machine where your broker is deployed.	</br>	
 * &emsp;						It uses qos1.																		</br>
 * &emsp;						in this class, it just change qos0 to qos1											</br>
 * 																													</br>
 * &emsp; 					稍微注意一下, 个人不太建议 关闭一个 程序之前, 还要 unsubscribe											</br>
 * &emsp; 					假设 我们这个subscriber设置了 Qos1 或 Qos2 (总之不是qos0, 不然会重启收获不到 未收到的信息的)						</br>
 * &emsp; 						和 设置了 setCleanStart(false)															</br>
 * &emsp;&emsp;	 					然后 我用了 在 machineA 部署了 clientID_1 												</br>
 * &emsp;&emsp;&emsp;	 					此时, 我直接shutdown了 这个subscriber,   										</br>
 * &emsp;&emsp;&emsp;	 					在shutdown之后, publisher发送了 78910到broker, 因为shutdown 所以没有被subscriber 接收 </br>
 * &emsp;&emsp;&emsp;	 					当我  用同样的clientID_1   部署在 另外一个  machineB, 那么它仍然能获得 78910, 如果你使用了unsubscribe 那么 machineB 就无法获得 78910 了 	</br>
 * </p>
 * 
 * 
 * 只需要server的crt就可以了, 不需要自己的私钥
 * 
 * @author laipl
 *
 */
public class TestMain_TestCleanStart_auth_qos1 {
	
	

	
	public static void main(String[] args) {

        //String topic        = "MQTT Examples";
        String topic        = "Resource1";
        //String content      = "Message from MqttPublishSample";
        String content      = "receiver";
        int qos             = 1;
        //String broker       = "tcp://iot.eclipse.org:1883";
        //String broker       = "tcp://localhost:1883";
        String brokerUri    = "ssl://192.168.239.137:8883";
        //String brokerUri    = "ssl://127.0.0.1:8883";				//我发现ca是noname servercrt虽然设置的是192.168.239.137:8883 但是还是可以用127.0.0.1来访问
        
        //String clientId     = "JavaSample";
        String clientId     = "JavaSample_recver";
        
        String myuserName	= "IamPublisherOne";
        String mypwd		= "123456";
        
        MemoryPersistence persistence = new MemoryPersistence();

        final Logger LOGGER = LoggerFactory.getLogger(TestMain_TestCleanStart_auth_qos1.class);
        //
        
    	//public String serverCaCrt_file					="server_cert.crt";
    	String serverCaCrt_file					="s_cacert.crt";
    	//public String serverCaCrt_file					="s_cacert.pem";
    	//public String serverCaCrt_file_dir				="/mycerts/my_own/samecn";	//从这里就可以看出, 我如果用不正确的证书会出问题的
    	String serverCaCrt_file_dir				="/mycerts/my_own";
    	String serverCaCrt_file_loc = null;
    	
    	
        
        
        
        String myusr_path = System.getProperty("user.dir");

		serverCaCrt_file_loc 							= 	myusr_path	+ serverCaCrt_file_dir		+"/" + 	serverCaCrt_file;
	         
        
		////////////////////file->FileInputStream->BufferedInputStream->X509Certificate //////////////////////////////////////
		// ref: https://gist.github.com/erickok/7692592
		
		FileInputStream fis= null;
		CertificateFactory cf = null;
		Certificate ca=null;
		try {
		cf = CertificateFactory.getInstance("X.509");
		// From https://www.washington.edu/itconnect/security/ca/load-der.crt
		fis = new FileInputStream(serverCaCrt_file_loc);
		InputStream caInput = new BufferedInputStream(fis);
		
		try {
			ca = cf.generateCertificate(caInput);
			// System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
		} finally {
			caInput.close();
		}
		} catch (FileNotFoundException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
		} catch (CertificateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		
		
		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore=null;
		TrustManagerFactory tmf = null;
		try {
		// Create a KeyStore containing our trusted CAs
		keyStoreType = KeyStore.getDefaultType();
		keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("ca", ca);
		
		// Create a TrustManager that trusts the CAs in our KeyStore
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		tmf.init(keyStore);
		} catch (KeyStoreException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (CertificateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		
		
		
		
		
		// finally, create SSL socket factory
		SSLContext context=null;
		SSLSocketFactory mysocketFactory=null;
		try {
		//context = SSLContext.getInstance("SSL");
		context = SSLContext.getInstance("TLSv1.3");
		
		//context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		//context.init(null,tmf.getTrustManagers(), new java.security.SecureRandom());
		//context.init(null,tmf.getTrustManagers(), null);
		context.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
		} catch (KeyManagementException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		mysocketFactory = context.getSocketFactory();
		        
        
        //
        try {
        	// create mqtt client
            MqttClient sampleClient = new MqttClient(brokerUri, clientId, new MemoryPersistence());
            //MqttClient sampleClient = new MqttClient(broker, clientId);
        	//
        	// -----------------------set connection options-------------------------
        	// 
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            //
            //
            // ------------------
            // authentication
            //
            connOpts.setUserName(myuserName);
            connOpts.setPassword(mypwd.getBytes());
            //
            // ------------------
            // set persistence
            //
            // 如果 setCleanStart(false) 意味着: 
            // 你想要让 	订阅者		在	disconnect 之后  reconnect 
            // 此外 该 		订阅者 	能够把  disconnect 到 reconnect 期间 	发布者  发送的消息 都全部获得
            // 例如
            // publishing client 	发送 		1	到	broker
            // subscribing client	接受		1	从	broker
            // publishing client 	发送 		2	到	broker
            // subscribing client	接受		2	从	broker
            // subscribing client	disconnect
            // publishing client	发送		3	到	broker
            // publishing client	发送		4	到	broker
            // publishing client	发送		5	到	broker
            // subscribing client	reconnect
            // subscribing client	接受		3	从	broker
            // subscribing client	接受		4	从	broker
            // subscribing client	接受		5	从	broker
            //
            // publishing client	发送		6	到	broker
            // subscribing client	接受		6	从	broker
            //
            // 也就是说 该subscribing client 
            // 		一共可以接受 1 2 3 4 5 6 (假设 设置的会话过期时间(setSessionExpiryInterval) 足够的长, 能够保存所有的离线信息)
            //
            // 如果setCleanStart(true) 意味着:
            // 也就是说 该subscribing client 
            //		一共可以接受 1 2 6
            //
            // 我发现 publishing client 可以不用设置 	connOpts.setCleanStart(false) 和下面的	setSessionExpiryInterval
            // 而且我还发现 publishing client 就算是 设置 connOpts.setCleanStart(true)  也没关系
            connOpts.setCleanStart(false);
            // 注意 订阅者 还要设置 会话过期时间, 单位是 秒, 
            // 如果不设置的话, 它默认是 0s, 则会导致 subscribing client 一共可以接受 1 2 6 而不是  1 2 3 4 5 6
            // 注意 如果 你 disconnect 超过了 这个时间, 那么你 reconnect以后 就没办法 获取中间的 3 4 5，
            // 并且你也没办法获取 reconnect 后面 publishing client 发送的6,
            // 此时如果你还想获得订阅信息, 你还需要重新subscribe
            connOpts.setSessionExpiryInterval(500L);
            //
            //connOpts.setCleanStart(true);
            //            //
            //
            //-------------set TLS/SSL-------
            connOpts.setSocketFactory(mysocketFactory);
            connOpts.setHttpsHostnameVerificationEnabled(false);
            //
            //
            // ------------------
            //
            // -------------------------------------------------------------------------
            // -----------------------set handler for asynchronous request--------------
            //
            sampleClient.setCallback(new MqttCallback() {

				@Override
				public void disconnected(MqttDisconnectResponse disconnectResponse) {
					// TODO Auto-generated method stub
					//System.out.println("mqtt disconnected");
					//
					//LOGGER.info("mqtt disconnected:"+disconnectResponse.getReturnCode()+"//"+disconnectResponse.getReasonString());
					LOGGER.info("mqtt disconnected:"+disconnectResponse.toString());
					
				}

				@Override
				public void mqttErrorOccurred(MqttException exception) {
					// TODO Auto-generated method stub
					//System.out.println("mqtt error occurred");
					//
					LOGGER.info("mqtt error occurred");
					
				}

				@Override
				public void deliveryComplete(IMqttToken token) {
					// TODO Auto-generated method stub
					//System.out.println("mqtt delivery complete");
					//
					LOGGER.info("mqtt delivery complete");
				}

				@Override
				public void connectComplete(boolean reconnect, String serverURI) {
					// TODO Auto-generated method stub
					//System.out.println("mqtt connect complete");
					//
					LOGGER.info("mqtt connect complete");
				}

				@Override
				public void authPacketArrived(int reasonCode, MqttProperties properties) {
					// TODO Auto-generated method stub
					//System.out.println("mqtt auth Packet Arrived");
					//
					LOGGER.info("mqtt auth Packet Arrived");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					// TODO Auto-generated method stub
					System.out.println("message Arrived:\t" + new String(message.getPayload()));
					//
					//LOGGER.info("message Arrived:\t"+ new String(message.getPayload()));
				}


			});
            // -------------------------------------------------------------------------
            // ---------------- to connect and to subscribe ----------------------------
            //
            // connect
            System.out.println("Connecting to broker: "+brokerUri);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("subsribing message topic: " + topic);
            //
            //
            // subscribe
            sampleClient.subscribe(topic,1);
            //
            //
            //
            System.out.println("wow_hello");
            //
            //
            //------------------------------------------------------
            Scanner in =new Scanner(System.in) ;
            int int_choice = 0;
            while(int_choice!=-1) {
            	System.out.println("here is the choice:");
            	System.out.println("-1: to exit");
            	System.out.println("1: to disconnect broker");
            	System.out.println("2: to reconnect broker");
            	System.out.println("3: to unsubscribe");
            	System.out.println("4: to subscribe");
            	System.out.println("enter the choice:");
            	// input
            	int_choice = in.nextInt();
            	if(int_choice==-1) {
            		//System.exit(0);
            		break;
            	}
            	else if(int_choice==1) {
            		sampleClient.disconnect();
            		System.out.println("disconnected broker");
            	}
            	else if(int_choice==2) {
            		sampleClient.reconnect();
            		System.out.println("reconnect broker");
            	}
            	else if(int_choice==3) {
            		sampleClient.unsubscribe(topic);
            		System.out.println("unsubscribed topic");
            	}
            	else if(int_choice==4) {
            		sampleClient.subscribe(topic,qos);
            		System.out.println("subscribed topic");
            	}
            }
            //
            //
            //
            //
            //
            //
            sampleClient.disconnect();
            System.out.println("Disconnected");
            sampleClient.close();
            System.out.println("closed");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
}
