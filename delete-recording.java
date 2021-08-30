/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * RUN CURL COMMAND:
 * `curl {HOST}/deleteRecording`
 * EXPECT NOTHING TO BE RETURNED & THE OLD RECORDING SHOULD BE DELETED UNDER YOUR FREECLIMB ACCOUNT, WHICH CAN BE FOUND VISUALLY IN FREECLIMB DASHBOARD
*/

package main.java.delete_recording;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;

@RestController
public class DeleteRecordingController {

  @RequestMapping("/deleteRecording")
  public void deleteRecording() {
    FreeClimbClient client;

    try {
      // Create FreeClimbClient object
      String accountId = System.getenv("ACCOUNT_ID");
      String apiKey = System.getenv("API_KEY");
      client = new FreeClimbClient(accountId, apiKey);

      String recordingId = "RErecordingId"; // desired recordingId to be deleted
      client.recordings.delete(recordingId);
    } catch (FreeClimbException pe) {
      System.out.println(pe.getMessage());
    }
  }
}