import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dispensor implements  AutoCloseable{

    private final Connection connection;
    private final Channel channel;
    private final String requestQueueName = "dispensor_correlationid_queue";

    public Dispensor() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }


    public boolean dispense(Article article){
        int ret = -1;
        try(this){
            String article_str = article.articleToString();
            String response = this.check(article_str);
            ret = Integer.parseInt(response);
        }catch (IOException | TimeoutException | RuntimeException | InterruptedException e) {
            Logger.getLogger(Dispensor.class.getName()).log(Level.SEVERE, null, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ret > 0;
    }

    private String check(String message) throws IOException, InterruptedException {
        //generate an unique id for the request here
        final String corrId = UUID.randomUUID().toString();
        System.out.println(" [x] Checking distribution :(" + Article.stringToArticle(message).getBarcode() + ") with correlation id: "+corrId);

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes(StandardCharsets.UTF_8));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
