/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * RUN CURL COMMAND:
 * `curl {baseUrl}/calls`
 * EXPECT JSON TO BE RETURNED:
 * [{"uri":"/Accounts/{accountId}/Calls/{callId}",
 * "dateCreated":"{dateCreated}",
 * "dateUpdated":"{dateUpdated}",
 * "revision":1,
 * "callId":"{callId}",
 * "parentCallId":null,
 * "accountId":"{accountId}",
 * "to":"+1{to_phone_number}",
 * "from":"+1{from_phone_number}",
 * "phoneNumberId":null,
 * "status":"COMPLETED",
 * "startTime":"{startTime}",
 * "connectTime":"{connectTime}",
 * "endTime":"{endTime}",
 * "duration":34,
 * "connectDuration":24,
 * "direction":"OUTBOUND_API",
 * "answeredBy":null,
 * "callerName":null,
 * "subresourceUris":{"recordings":"/Accounts/{accountId}/Calls/{callId}/Recordings",
 * "logs":"/Accounts/{accountId}/Calls/{callId}/Logs"}}, ...]
*/

package main.java.list_calls;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.call.Call;
import com.vailsys.freeclimb.api.call.CallList;

import java.util.ArrayList;

@RestController
public class ListCallsController {
  // Get accountID and apiKey from environment variables
  private String accountId = System.getenv("ACCOUNT_ID");
  private String apiKey = System.getenv("API_KEY");

  @RequestMapping("/calls")
  public ArrayList<Call> listCalls() {
    FreeClimbClient client;
    CallList callsList;

    try {
      // Create FreeClimbClient object
      // accountId & apiKey can be found under API keys on the FreeClimb Dashboard
      client = new FreeClimbClient(accountId, apiKey);
      callsList = client.calls.get(); // Retrieve the paginated list of calls

      // Don't bother trying to grab more pages if there is only one or zero
      // pages of results
      if (callsList.getTotalSize() > callsList.getPageSize()) {
        // Load in all the calls returned.
        while (callsList.getLocalSize() < callsList.getTotalSize()) {
          callsList.loadNextPage(); // Load in the next page of calls.
        }
      }

      ArrayList<Call> allCalls = callsList.export(); // Extract the array from the CallList

      for (Call r : allCalls) {
        // do something with each call
      }

      return allCalls;
    } catch (FreeClimbException pe) {
      System.out.println(pe.getMessage());
    }
    return null;
  }
}