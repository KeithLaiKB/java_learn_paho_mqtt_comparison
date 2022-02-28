package com.learn.paho_mqtt.receiver.scenario1;


import java.util.Scanner;


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
 * @author laipl
 *
 */
public class TestMain_TestCleanStart_auth_qos1 {
	private static int expectedNumberOfMessages = 30;
	private static int numberOfMessages = 0;
	public static void main(String[] args) {


        String topic        = "Resource1";

        String content      = "receiver";
        int qos             = 0;

        String broker       = "tcp://localhost:1883";

        String clientId     = "JavaSample_revcevier";
        
        String myuserName	= "IamPublisherOne";
        String mypwd		= "123456";
        
        MemoryPersistence persistence = new MemoryPersistence();

        final Logger LOGGER = LoggerFactory.getLogger(TestMain_TestCleanStart_auth_qos1.class);
        //
        //
        try {
        	// create mqtt client
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            //MqttClient sampleClient = new MqttClient(broker, clientId);

        	// -----------------------set connection options-------------------------
            MqttConnectionOptions connOpts = new MqttConnectionOptions();

            // authentication
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
            //connOpts.setCleanStart(false);
            // 注意 订阅者 还要设置 会话过期时间, 单位是 秒, 
            // 如果不设置的话, 它默认是 0s, 则会导致 subscribing client 一共可以接受 1 2 6 而不是  1 2 3 4 5 6
            // 注意 如果 你 disconnect 超过了 这个时间, 那么你 reconnect以后 就没办法 获取中间的 3 4 5，
            // 并且你也没办法获取 reconnect 后面 publishing client 发送的6,
            // 此时如果你还想获得订阅信息, 你还需要重新subscribe
            //connOpts.setSessionExpiryInterval(500L);
            //
            //connOpts.setCleanStart(true);
            
            // -----------------------set handler for asynchronous request--------------
            sampleClient.setCallback(new MqttCallback() {

				@Override
				public void disconnected(MqttDisconnectResponse disconnectResponse) {
					//System.out.println("mqtt disconnected");
					//LOGGER.info("mqtt disconnected:"+disconnectResponse.getReturnCode()+"//"+disconnectResponse.getReasonString());
					LOGGER.info("mqtt disconnected:"+disconnectResponse.toString());
					
				}

				@Override
				public void mqttErrorOccurred(MqttException exception) {
					//System.out.println("mqtt error occurred");
					LOGGER.info("mqtt error occurred");
					
				}

				@Override
				public void deliveryComplete(IMqttToken token) {
					//System.out.println("mqtt delivery complete");
					LOGGER.info("mqtt delivery complete");
				}

				@Override
				public void connectComplete(boolean reconnect, String serverURI) {
					//System.out.println("mqtt connect complete");
					LOGGER.info("mqtt connect complete");
				}

				@Override
				public void authPacketArrived(int reasonCode, MqttProperties properties) {
					//System.out.println("mqtt auth Packet Arrived");
					LOGGER.info("mqtt auth Packet Arrived");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					System.out.println(new String(message.getPayload()));
					numberOfMessages = numberOfMessages +1;
					LOGGER.info("message Arrived:\t"+ new String(message.getPayload()));
				}


			});
            
            // ---------------- to connect and to subscribe ----------------------------
            // connect
            sampleClient.connect(connOpts);

            // subscribe
            sampleClient.subscribe(topic,1);
            while(numberOfMessages < expectedNumberOfMessages) {
            	try {
    				Thread.sleep(200);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            }
            
            
            /*
            System.out.println("wow_hello");
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
            }*/

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
        }
    }
}
