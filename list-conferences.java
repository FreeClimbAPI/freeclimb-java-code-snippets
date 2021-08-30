/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * RUN CURL COMMAND TO GET LIST OF CONFERENCES:
 *    `curl {baseUrl}/conferences`
 * EXPECT JSON TO BE RETURNED:
 * [{"uri":"/Accounts/{accountId}/Conferences/{conferenceId}",
 * "dateCreated":"{dateCreated}",
 * "dateUpdated":"{dateUpdated}",
 * "revision":2,
 * "conferenceId":"{conferenceId}",
 * "accountId":"{accountId}",
 * "alias":"Tutorial Conference",
 * "playBeep":"ALWAYS",
 * "record":false,
 * "status":"TERMINATED",
 * "waitUrl":"",
 * "statusCallBackUrl":null,
 * "subresourceUris":{"recordings":"/Accounts/{accountId}/Conferences/{conferenceId}/Recordings",
 * "participants":"/Accounts/{accountId}/Conferences/{conferenceId}/Participants"}}, FORMAT REPEATED FOR OTHER ELEMENTS]
*/

package main.java.list_conferences;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.conference.Conference;
import com.vailsys.freeclimb.api.conference.ConferenceStatus;
import com.vailsys.freeclimb.api.conference.ConferenceList;
import com.vailsys.freeclimb.api.conference.ConferencesSearchFilters;

import java.util.ArrayList;

@RestController
public class ListConferencesController {
  // Get accountID and apiKey from environment variables
  private String accountId = System.getenv("ACCOUNT_ID");
  private String apiKey = System.getenv("API_KEY");

  @RequestMapping("/conferences")
  public ArrayList<Conference> listConferences() {
    // Create a filter object to retrieve list of conferences with matching alias
    // and status
    ConferencesSearchFilters filters = new ConferencesSearchFilters();
    filters.setAlias("Tutorial Conference");
    filters.setStatus(ConferenceStatus.TERMINATED); // statuses include EMPTY, IN_PROGRESS, POPULATED, TERMINATED
    try {
      // Create FreeClimbClient object
      FreeClimbClient client = new FreeClimbClient(accountId, apiKey);
      // Invoke get method to retrieve first page of conferences with matching alias
      ConferenceList conferenceList = client.conferences.get(filters);

      // Check if the list is empty by checking its total size
      if (conferenceList.getTotalSize() > 0) {
        // Retrieve all pages of results
        while (conferenceList.getLocalSize() < conferenceList.getTotalSize()) {
          conferenceList.loadNextPage();
        }

        // Retrieve the inner ArrayList of conferences to process
        ArrayList<Conference> conferences = conferenceList.export();
        for (Conference conference : conferences) {
          // Process conference element in some way
          System.out.println(conference.getConferenceId());
        }
        return conferences;
      }
    } catch (FreeClimbException pe) {
      System.out.println(pe.getMessage());
    }
    return null;
  }
}