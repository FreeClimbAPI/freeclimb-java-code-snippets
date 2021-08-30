/* 
 * 1. RUN PROJECT WITH COMMAND: 
 *    `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * 2. CALL FreeClimb NUMBER ASSOCIATED WITH THE ACCOUNT (CONFIGURED IN FreeClimb DASHBOARD)
 * 3. EXPECT PROMPT FOR ACCESS CODE TO BE REPEATED TO YOU
 *    ENTER ONE OF THREE conferenceRoomCodes (1, 2, OR 3)
 * 4. EXPECT MESSAGE:
 *    "You will be added to the conference momentarily."
 *    EXPECT A NEW CONFERENCE UNDER YOUR FreeClimb ACCOUNT, WHICH CAN BE FOUND IN FreeClimb DASHBOARD
*/

package main.java.create_conference;

import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

import com.vailsys.freeclimb.percl.*;

import com.vailsys.freeclimb.webhooks.percl.GetDigitsActionCallback;
import com.vailsys.freeclimb.webhooks.conference.ConferenceCreateActionCallback;
import com.vailsys.freeclimb.webhooks.conference.ConferenceStatusCallback;

import com.vailsys.freeclimb.api.conference.ConferenceStatus;
import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.conference.ConferenceUpdateOptions;
import com.vailsys.freeclimb.api.FreeClimbException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedList;

@RestController
public class CreateConferenceController {
  // Get base URL from environment variables
  private String baseUrl = System.getenv("HOST");
  private static final String[] conferenceRoomCodes = { "1", "2", "3" };
  private static HashMap<String, ConferenceRoom> conferenceRooms = new HashMap<String, ConferenceRoom>();

  private class ConferenceRoom {
    // stores conferenceId associated with this room
    public String conferenceId = null;

    // true if the CreateConference command was sent but the actionUrl has not yet
    // been called, else false
    public Boolean isConferencePending = false;

    // Set to true after the conference status is first set to EMPTY, meaning that
    // the next EMPTY status received indicates that all participants have left the
    // conference and so the conference can terminate
    public Boolean canConferenceTerminate = false;

  }

  // To properly communicate with FreeClimb's API, set your FreeClimb app's
  // VoiceURL endpoint to '{yourApplicationURL}/InboundCall' for this example
  // Your FreeClimb app can be configured in the FreeClimb Dashboard
  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCall() {

    // Add three ConferenceRooms to the map on initialization
    for (String code : conferenceRoomCodes) {
      conferenceRooms.put(code, new ConferenceRoom());
    }

    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    // Create PerCl get digits script
    GetDigits getDigits = new GetDigits(baseUrl + "/GotDigits");

    getDigits.setMaxDigits(1);

    LinkedList<GetDigitsNestable> prompts = new LinkedList<>();
    prompts.add(new Say("Hello. Welcome to the conferences tutorial, please enter your access code."));
    getDigits.setPrompts(prompts);

    script.add(getDigits);

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/GotDigits" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> gotDigits(@RequestBody String str) {
    PerCLScript script = new PerCLScript();

    GetDigitsActionCallback getDigitsActionCallback;
    try {
      // Convert JSON into a call status callback object
      getDigitsActionCallback = GetDigitsActionCallback.createFromJson(str);

      String digits = getDigitsActionCallback.getDigits();
      String callId = getDigitsActionCallback.getCallId();

      ConferenceRoom room = conferenceRooms.get(digits);
      if (room == null) {
        // Handle case where no room with the given code exists
      }

      // if participants can't be added yet (actionUrl callback has not been called)
      // notify caller and hang up
      if (room.isConferencePending) {
        script
            .add(new Say("We are sorry, you cannot be added to the conference at this time. Please try again later."));
        script.add(new Hangup());
      } else {
        script.add(new Say("You will be added to the conference momentarily."));
        script.add(makeOrAddToConference(room, digits, callId));
      }

    } catch (FreeClimbException pe) {
      System.out.print(pe);
    }

    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  private static PerCLCommand makeOrAddToConference(ConferenceRoom room, String roomCode, String callId) {
    // If a conference has not been created for this room yet, return a
    // CreateConference PerCL command
    if (room.conferenceId == null) {
      room.isConferencePending = true;
      room.canConferenceTerminate = false;

      CreateConference createConference = new CreateConference(
          System.getenv("HOST") + "/ConferenceCreated/" + roomCode);
      createConference.setStatusCallbackUrl(System.getenv("HOST") + "/ConferenceStatus/" + roomCode);
      return createConference;
    } else {
      // If a conference has been created and the actionUrl callback has been called,
      // return a AddToConference PerCL command
      return new AddToConference(room.conferenceId, callId);
    }
  }

  @RequestMapping(value = {
      "/ConferenceCreated/{roomCode}" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> conferenceCreated(@PathVariable String roomCode, @RequestBody String str) {
    PerCLScript script = new PerCLScript();

    ConferenceCreateActionCallback conferenceCreateActionCallback;
    try {
      conferenceCreateActionCallback = ConferenceCreateActionCallback.createFromJson(str);
      String conferenceId = conferenceCreateActionCallback.getConferenceId();

      // find which conference room the newly created conference belongs to
      ConferenceRoom room = conferenceRooms.get(roomCode);

      if (room == null) {
        // Handle case where callback is called for a room that does not exist
      }

      room.conferenceId = conferenceId;
      room.isConferencePending = false;

      // Add initial caller to conference
      script.add(new AddToConference(conferenceId, conferenceCreateActionCallback.getCallId()));

    } catch (FreeClimbException pe) {
      System.out.print(pe);
    }

    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/ConferenceStatus/{roomCode}" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> conferenceStatus(@PathVariable String roomCode, @RequestBody String str) {

    ConferenceStatusCallback conferenceStatusCallback;
    try {
      conferenceStatusCallback = ConferenceStatusCallback.createFromJson(str);
      ConferenceStatus status = conferenceStatusCallback.getStatus();
      String conferenceId = conferenceStatusCallback.getConferenceId();

      // find which conference room the conference belongs to
      ConferenceRoom room = conferenceRooms.get(roomCode);

      if (room == null) {
        // Handle case where callback is called for a room that does not exist
      }

      if (status.equals(ConferenceStatus.EMPTY) && room.canConferenceTerminate) {
        try {
          terminateConference(conferenceId);
          room.conferenceId = null;
        } catch (FreeClimbException pe) {
          // Handle error when terminateConference fails
          System.out.print(pe);
        }
      }

      // after first EMPTY status update conference can be terminated
      room.canConferenceTerminate = true;
    } catch (FreeClimbException pe) {
      System.out.print(pe);
    }

    return new ResponseEntity<>("", HttpStatus.OK);
  }

  private static void terminateConference(String conferenceId) throws FreeClimbException {
    String accountId = System.getenv("ACCOUNT_ID");
    String apiKey = System.getenv("API_KEY");
    FreeClimbClient client = new FreeClimbClient(accountId, apiKey);

    // Create the ConferenceUpdateOptions and set the status to terminated
    ConferenceUpdateOptions conferenceUpdateOptions = new ConferenceUpdateOptions();
    conferenceUpdateOptions.setStatus(ConferenceStatus.TERMINATED);
    client.conferences.update(conferenceId, conferenceUpdateOptions);
  }
}