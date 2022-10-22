package com.learn.paho_mqtt.sender.scenario1;

import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
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

        String content      = "Hello World!";
        int qos             = 0;

        String brokerUri    = "tcp://localhost:1883";
        String clientId     = "JavaSample_publisher";
        
        String myuserName	= "IamPublisherOne";
        String mypwd		= "123456";
        
		int statusUpdateMaxTimes = 35;
		int statusUpdate = 0;

        try {
        	//MqttAsyncClient sampleClient = new MqttAsyncClient(brokerUri, clientId, new MqttDefaultFilePersistence());
        	MqttClient sampleClient = new MqttClient(brokerUri, clientId, new MemoryPersistence());
        	
        	// -----------------------set connection options-------------------------
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            //connOpts.setCleanStart(false);
            //connOpts.setSessionExpiryInterval(500L);
            connOpts.setCleanStart(true);

            // authentication
            // https://mosquitto.org/man/mosquitto-conf-5.html
            // for mosquitto, anonymous log in is just allowed in local machine
            // however, gernerally, the broker is deployed in the server, so the client would not in the same machine
            connOpts.setUserName(myuserName);
            connOpts.setPassword(mypwd.getBytes());

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
            	message_tmp.setQos(qos);
            	message_tmp.setRetained(false);
       
            	System.out.println("published:"+str_content_tmp);
                sampleClient.publish(topic, message_tmp);
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
