import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseChecker {
    private static final String RPC_QUEUE_NAME = "dispensor_correlationid_queue";

    //The database of all article the dispensor can sell. for simplicicty it's just here a static list of articles;
    static Article ar1 = new Article("cocacola_barcode",100);
    static Article ar2 = new Article("snicker_barcode",1);
    static Article ar3 = new Article("coffee_barcode",6);
    static Article ar4 = new Article("coffee_barcode",6);
    //a static list of article, normally wil be done
    private final static List<Article> articles = Arrays.asList(ar1,ar2, ar3, ar4);


    public static int verifyOrder(Article article){
        int ret = -1;
        //if the article purchase quantity request send by the user is less than
        for(Article art : articles){
            if (article.getBarcode().equals(art.getBarcode()) && art.getQuantity() >= article.getQuantity()) {
                ret = 2;
                break;//we find the article and we can sell it, then break the loop
            }
        }
        return ret;
    }

    public static void main(String[] argv) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try  {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            System.out.println(" [x] Awaiting for clients requests");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                //get the Message Id to return as correlation Id after process
                String corrId = delivery.getProperties().getCorrelationId();

                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    //parse the message received to article
                    Article article = Article.stringToArticle(message);
                    System.out.println("ID:"+corrId+" [.] Article (" + article.getBarcode() + ")");
                    response += verifyOrder(article);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.getMessage());
                } finally {
                    try {
                        channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        // RabbitMq consumer worker thread notifies the owner thread
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(PurchaseChecker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            // Wait and be prepared to consume the message from client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PurchaseChecker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }catch(IOException | TimeoutException ex){
            Logger.getLogger(PurchaseChecker.class.getName()).log(Level.SEVERE, "An error occured in the purchase checker system !", ex);
        }
    }
}
