package main.java.make_a_recording;

import com.vailsys.freeclimb.webhooks.call.VoiceCallback;
import com.vailsys.freeclimb.webhooks.percl.RecordUtteranceActionCallback;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.vailsys.freeclimb.percl.PerCLScript;
import com.vailsys.freeclimb.percl.Say;
import com.vailsys.freeclimb.percl.RecordUtterance;
import com.vailsys.freeclimb.percl.FinishOnKey;
import com.vailsys.freeclimb.percl.Hangup;
import com.vailsys.freeclimb.percl.Language;
import com.vailsys.freeclimb.percl.Pause;
import com.vailsys.freeclimb.api.call.Call;
import com.vailsys.freeclimb.api.call.CallStatus;
import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;

@RestController
public class MakeARecording {
  private static final String fromNumber = System.getenv("FREE_CLIMB_PHONE_NUMBER");
  private final String recordCallBackUrl = System.getenv("HOST") + "/RecordCallBack";

  public static void run() {
    String accountId = System.getenv("ACCOUNT_ID");
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
    }
  }

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
      // Create PerCL say script with US English as the language
      Say say = new Say("Hello. Please leave a message after the beep, then press one or hangup.");
      say.setLanguage(Language.ENGLISH_US);

      // Add PerCL say script to PerCL container
      script.add(say);

      // Create PerCL record utterance script
      RecordUtterance recordUtterance = new RecordUtterance(recordCallBackUrl);
      // Set indication that audible 'beep' should be used to signal start of
      // recording
      recordUtterance.setPlayBeep(true);
      // Set indication that end of recording is touch tone key 0ne
      recordUtterance.setFinishOnKey(FinishOnKey.ONE);

      // Add PerCL record utterance script to PerCL container
      script.add(recordUtterance);

    }
    return script.toJson();

  }

  @RequestMapping(value = {
      "/RecordCallBack" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public String recordCallBack(@RequestBody String body) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    RecordUtteranceActionCallback recordUtteranceActionCallBack;
    // Convert JSON into call status callback object
    try {
      recordUtteranceActionCallBack = RecordUtteranceActionCallback.createFromJson(body);
    } catch (FreeClimbException pe) {
      // Do something
      return script.toJson();
    }
    Say say;
    // Verify call is in the InProgress state
    if (recordUtteranceActionCallBack.getRecordingId() != null) {
      // Recording was successful as recording identifier present in response

      // Create PerCL say script with US English as the language
      // Set prompt to indicate message has been recorded
      say = new Say("Thanks. The message has been recorded.");

    } else {
      // Recording was failed as there is no recording identifier present in response

      // Create PerCL say script with US English as the language
      // Set prompt to indicate message recording failed
      say = new Say("Sorry we weren't able to record the message.");
    }
    say.setLanguage(Language.ENGLISH_US);

    // Add PerCL say script to PerCL container
    script.add(say);

    // Create PerCL pause script with a duration of 500 milliseconds
    Pause pause = new Pause(500);

    // Add PerCL pause script to PerCL container
    script.add(pause);

    // Create PerCL say script with US English as the language
    Say sayGoodbye = new Say("Goodbye");
    sayGoodbye.setLanguage(Language.ENGLISH_US);

    // Add PerCL say script to PerCL container
    script.add(sayGoodbye);

    // Create PerCL hangup script
    Hangup hangup = new Hangup();

    // Add PerCL hangup script to PerCL container
    script.add(hangup);

    return script.toJson();
  }
}