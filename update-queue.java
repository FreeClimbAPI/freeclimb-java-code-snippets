package main.java.update_a_queue;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.queue.Queue;
import com.vailsys.freeclimb.api.queue.QueueUpdateOptions;

public class UpdateAQueue {

  public static void main(String[] args) {
    String accountId = System.getenv("ACCOUNT_ID");
    String apiKey = System.getenv("API_KEY");
    String queueId = ""; // Provide QUEUE_ID

    updateAQueue(queueId, accountId, apiKey);
  }

  public static void updateAQueue(String queueId, String accountId, String apiKey) {
    try {
      // Create FreeClimbClient object
      FreeClimbClient client = new FreeClimbClient(accountId, apiKey);

      // Options payload to change the alias and payload of the specified queue
      QueueUpdateOptions options = new QueueUpdateOptions();
      options.setAlias("Updated Alias");
      options.setMaxSize(12);

      // Invoke update method to modify the queue
      Queue queue = client.queues.update(queueId, options);

    } catch (FreeClimbException e) {
      // Exception throw upon failure
    }
  }
}