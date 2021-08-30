/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * RUN CURL COMMAND:
 * `curl {baseUrl}/queue -d 'queueId={queueId}'`
 * EXPECT JSON TO BE RETURNED:
 *    {"uri":"/Accounts/{accountId}/Queues/{queueId}/Members/{callId}",
        "dateCreated":"{dateCreated}",
        "dateUpdated":"{dateUpdated}",
        "revision":{revision},
        "queueId":"{queueId}",
        "alias":"{queueAlias}",
        "currentSize":0,
        "maxSize":{maxSize},
        "averageWaitTime":0,
        "subresourceUris":{"members":"/Accounts/accountId/Queues/queueId/Members"}}
*/

package main.java.get_queue;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.queue.Queue;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class GetQueueController {
  // get accountId and apiKey from environment variables
  private String accountId = System.getenv("ACCOUNT_ID");
  private String apiKey = System.getenv("API_KEY");

  @RequestMapping("/queue")
  public Queue getQueue(String queueId) {
    try {
      // Create FreeClimbClient object
      FreeClimbClient client = new FreeClimbClient(accountId, apiKey);

      // Invoke get method to retrieve queue metadata
      Queue queue = client.queues.get(queueId);
      return queue;
    } catch (FreeClimbException ex) {
      // Exception throw upon failure
      System.out.println(ex.getMessage());
    }
    return null;
  }
}