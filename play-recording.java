package main.java.play_a_recording;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.call.Call;
import com.vailsys.freeclimb.api.call.CallStatus;
import com.vailsys.freeclimb.percl.PerCLScript;
import com.vailsys.freeclimb.webhooks.call.VoiceCallback;
import com.vailsys.freeclimb.percl.Play;

@RestController
public class PlayARecording {
  private static final String fromNumber = System.getenv("FREE_CLIMB_PHONE_NUMBER");
  private static final String accountId = System.getenv("ACCOUNT_ID");
  private final String recordingUrl = ""; // Provide recording ID

  public static void run() {
    String apiKey = System.getenv("API_KEY");
    String applicationId = System.getenv("TUTORIAL_APPLICATION_ID");
    String toNumber = System.getenv("TO_PHONE_NUMBER");

    outDial(accountId, apiKey, toNumber, applicationId);
  }

  public static void outDial(String accountId, String apiKey, String toNumber, String applicationId) {
    try {
      // Create FreeClimbClient object
      FreeClimbClient client = new FreeClimbClient(accountId, apiKey);

      Call call = client.calls.create(toNumber, fromNumber, applicationId);
    } catch (FreeClimbException ex) {
      // Exception throw upon failure
      System.out.print(ex);
    }
  }

  // This the callback which should be specified in your FreeClimb dashboard App
  // Config under callConnectUrl
  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public String inboundCall(@RequestBody String body) {
    VoiceCallback callStatusCallback;
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    try {
      // Convert JSON into call status call back object
      callStatusCallback = VoiceCallback.createFromJson(body);
    } catch (FreeClimbException pe) {
      // Do something with the failure to parse the request
      return script.toJson();
    }

    // Verify call is in the InProgress state
    if (callStatusCallback.getDialCallStatus() == CallStatus.IN_PROGRESS) {
      // Create PerCL play script with US English as the language
      Play play = new Play(recordingUrl);

      // Add PerCL play script to PerCL container
      script.add(play);
    }
    return script.toJson();
  }
}