package com.learn.paho_mqtt.sender.scenario1.totesttp.trywill;

import java.util.ArrayList;

import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
/**
 * 
 * @author laipl
 *
 *	我想要做到
 *	step1(数据):	publisher 	发送 123
 *	step2(数据):	subscriber 	接受123
 *
 *	step3(操作):	关闭 subscriber 
 *
 *	step4(数据):	publisher 	发送45678
 *  
 *	step8(操作):	然后 启动 subscriber
 *	step9(数据):	然后 subscriber 能接受 
 *								1 2 3
 *								      和
 *								4 5 6 7 8
 *
 *  publisher(online)	-------------> 	mosquitto(online)  -------------->	subscriber(online)
 *  publisher(online) 	----123------> 	mosquitto(online)  -------------->	subscriber(online)
 *  publisher(online) 	-------------> 	mosquitto(online)  -------------->	subscriber(online)
 *                     						123
 *  publisher(online) 	-------------> 	mosquitto(online)  ------123----->	subscriber(online)
 *  publisher(online) 	-------------> 	mosquitto(online)  ------123----->	subscriber(online)
 *  																			1 2 3
 *  
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *  +++++++++++++++++++++++++			turn off subscriber		+++++++++++++++++++++++++++++++
 *  ++++++	要设置 subscriber 的 setCleantStart(false) 和 interval, 	使得 subscriber 重启 后   broker     仍然记得 这个subscriber 						+++++++
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *  publisher(online) 	-------------> 	mosquitto(online)  -------------->	subscriber(offline)
 *  publisher(online) 	----45678----> 	mosquitto(online)  -------------->	subscriber(offline)
 *  publisher(online) 	-------------> 	mosquitto(online)  -------------->	subscriber(offline)
 *  									   4 5 6 7 8
 *  
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *  ++++++++++++++++++++++++++ 			turn on subscriber			+++++++++++++++++++++++++++
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *  publisher(online)	-------------> 	mosquitto(online)  --4-5-6-7-8--->	subscriber(online)
 *  									   4 5 6 7 8
 *  publisher(online)	-------------> 	mosquitto(online)  -------------->	subscriber(online)
 *  							     										4 5 6 7 8							
 *
 * 
 * 因为broker需要记得 subscriber 在这里只需要设置 subscriber 
 * 	connOpts.setCleanStart(false);
 * 	connOpts.setSessionExpiryInterval(500L);		//500是个时间 你可以随便设置
 * 
 * 注意 你还需要设置subscriber 的qos不能为0
 * 因为 subscriber的 qos0 是无法reconnect的时候 或者  重新启动这个subscribe(从connect到 subscribe)继续 获得信息
 * 
 * subscriber关闭后	 重启 		就可以直接获得 45678
 *
 *
 * 如果你不关闭 broker, 那么就 不需要 在mosquitto.config 中 设置 persistence true
 */
/*
 * mqtt 不需要像 coap那样的resource 所以 循环可以直接放在主函数
 * */
public class TestMain_Auth_SubOffl_SubOnl {

	public static void main(String[] args) {

        String topic        = "Resource1";

        String content      = "Hello_World!";
        int qos             = 1;

        String brokerUri    = "tcp://192.168.239.137:1883";	
       // String brokerUri    = "tcp://138.229.113.84:1883";	
        String clientId     = "JavaSample_sender";									//测试中会看查这个是否会有影响字节数!!!!!!!!!!!!!!!!!!!
        
        String myuserName	= "IamPublisherOne";
        String mypwd		= "123456";
        
		int statusUpdateMaxTimes = 35;
		int statusUpdate = 0;

		
		
		
		
		//ref:https://github.com/eclipse/paho.mqtt.java/blob/6f35dcb785597a6fd49091efe2dba47513939420/org.eclipse.paho.mqttv5.client.test/src/test/java/org/eclipse/paho/mqttv5/common/packet/MqttConnectTest.java
		// paho.mqtt.java/org.eclipse.paho.mqttv5.client.test/src/test/java/org/eclipse/paho/mqttv5/common/packet/MqttConnectTest.java 
		//String clientId = "testClientId";
		int mqttVersion = 5;
		boolean cleanStart = true;
		int keepAliveInterval = 60;
		String userName = "username";
		 byte[] password = "password".getBytes();
		 String willPayload = "Will Message";
		 String willDestination = "will/destination";
		 int willQoS = 1;

		 Long sessionExpiryInterval = 60L;
		 Long maxPacketSize = 128000L;
		 Integer topicAliasMax = 5;
		 boolean requestResponseInfo = true;
		 boolean requestPropblemInfo = true;
		 String authMethod = "PASSWORD";
		 byte[] authData = "secretPassword123".getBytes();
		 String userKey1 = "userKey1";
		 String userKey2 = "";
		 String userKey3 = "userKey3";
		 String userKey4 = "";
		 String userValue1 = "userValue1";
		 String userValue2 = "userValue2";
		 String userValue3 = "";
		 String userValue4 = "";

		 Long willDelayInterval = 3L;			//关掉sender 等3秒后 broker 会把 will topic和它的data 发送给 订阅了   这个willtopic的subscrbier
		 boolean willIsUTF8 = true;
		 Long willPublicationExpiryInterval = 60L;
		 String willResponseTopic = "replyTopic";
		 byte[] willCorrelationData = "correlationData".getBytes();			//设置为"" 看字节数
		 String willContentType = "sssssJSON";										//设置为"" 看字节数
		
		
		
        try {
    		MqttProperties properties = new MqttProperties();
    		MqttProperties willProperties = new MqttProperties();
    		MqttMessage willMessage = new MqttMessage(willPayload.getBytes());
    		willMessage.setQos(willQoS);

    		properties.setSessionExpiryInterval(sessionExpiryInterval);
    		properties.setMaximumPacketSize(maxPacketSize);
    		properties.setTopicAliasMaximum(topicAliasMax);
    		properties.setRequestResponseInfo(requestResponseInfo);
    		properties.setRequestProblemInfo(requestPropblemInfo);
    		properties.setAuthenticationMethod(authMethod);
    		properties.setAuthenticationData(authData);

    		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
    		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
    		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
    		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
    		userDefinedProperties.add(new UserProperty(userKey4, userValue4));
    		properties.setUserProperties(userDefinedProperties);

    		willProperties.setMessageExpiryInterval(willPublicationExpiryInterval);
    		willProperties.setWillDelayInterval(willDelayInterval);
    		willProperties.setPayloadFormat(willIsUTF8);
    		willProperties.setContentType(willContentType);
    		willProperties.setResponseTopic(willResponseTopic);
    		willProperties.setCorrelationData(willCorrelationData);

    		willProperties.setUserProperties(userDefinedProperties);

        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	
        	//MqttAsyncClient sampleClient = new MqttAsyncClient(brokerUri, clientId, new MqttDefaultFilePersistence());
        	MqttClient sampleClient = new MqttClient(brokerUri, clientId, new MemoryPersistence());
        	
        	// -----------------------set connection options-------------------------
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(false);
            
            
            //connOpts.setSessionExpiryInterval(500L);								//测试中会看查这个是否会有影响字节数!!!!!!!!!!!!!!!!!!!
            
            
            
            //connOpts.setCleanStart(true);

            // authentication
            // https://mosquitto.org/man/mosquitto-conf-5.html
            // for mosquitto, anonymous log in is just allowed in local machine
            // however, gernerally, the broker is deployed in the server, so the client would not in the same machine
            connOpts.setUserName(myuserName);
            connOpts.setPassword(mypwd.getBytes());

            
            
            
            
            
            
            
            /*
            //用来看authdata 占多少字节  但是 好像暂时不能运行, 只是说可以用来看包大小
            connOpts.setAuthMethod(authMethod);
            connOpts.setAuthData(authData);
            */
            
            
            connOpts.setUserProperties(userDefinedProperties);
            connOpts.setWillMessageProperties(willProperties);
            connOpts.setWill(willResponseTopic, willMessage);

            
            
            
            
            
            
            
            
            
            // connect to broker
            sampleClient.connect(connOpts);											//如果是MqttClient 贼需要这个
            //sampleClient.connect(connOpts, null, null).waitForCompletion(-1); 	//如果是MqttAsyncClient 贼需要这个
            //
            MqttMessage message_tmp=null;
            StringBuffer str_content_tmp = new StringBuffer("");
            for(; statusUpdate<=statusUpdateMaxTimes-1; statusUpdate++) {

            	str_content_tmp.delete(0, str_content_tmp.length()-1+1);
            	str_content_tmp.append(content +(statusUpdate+1));

            	message_tmp = new MqttMessage(str_content_tmp.toString().getBytes());
            	//message_tmp = new MqttMessage("".getBytes());						//测试中会看查这个是否会有影响字节数!!!!!!!!!!!!!!!!!!!
            	
            	message_tmp.setQos(qos);
            	message_tmp.setRetained(false);
       
            	System.out.println("published:"+str_content_tmp);
                sampleClient.publish(topic, message_tmp);
                sampleClient.publish(topic+"2", message_tmp);
                sampleClient.publish(topic+"3", message_tmp);
                sampleClient.publish(topic+"4", message_tmp);
                sampleClient.publish(topic+"5", message_tmp);
                sampleClient.publish(topic+"6", message_tmp);
                sampleClient.publish(topic+"7", message_tmp);
                sampleClient.publish(topic+"8", message_tmp);
                sampleClient.publish(topic+"9", message_tmp);
                sampleClient.publish(topic+"10", message_tmp);
                sampleClient.publish(topic+"11", message_tmp);
                sampleClient.publish(topic+"12", message_tmp);
                
                
                //
                Thread.sleep(1000);
            }
            //
            sampleClient.disconnect();
            sampleClient.close();
            //System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
