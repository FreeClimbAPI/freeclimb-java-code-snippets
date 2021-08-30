package main.java.speech_recognition;

import com.vailsys.freeclimb.webhooks.call.VoiceCallback;
import com.vailsys.freeclimb.webhooks.percl.GetSpeechActionCallback;
import com.vailsys.freeclimb.webhooks.percl.SpeechReason;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.vailsys.freeclimb.percl.PerCLScript;
import com.vailsys.freeclimb.percl.Say;
import com.vailsys.freeclimb.percl.GetSpeech;
import com.vailsys.freeclimb.percl.GetSpeechNestable;
import com.vailsys.freeclimb.percl.GrammarType;
import com.vailsys.freeclimb.percl.Hangup;
import com.vailsys.freeclimb.percl.Language;
import com.vailsys.freeclimb.percl.Pause;
import com.vailsys.freeclimb.api.call.Call;
import com.vailsys.freeclimb.api.call.CallStatus;

import java.io.File;
import java.util.LinkedList;

import javax.servlet.http.HttpServletResponse;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;

@RestController
public class SpeechRecognition {
  private static final String fromNumber = System.getenv("FREE_CLIMB_PHONE_NUMBER");
  private final String selectColorDone = System.getenv("HOST") + "/SelectColorDone";
  private final String grammarDownload = System.getenv("HOST") + "/grammarFile";

  public static void run() {
    String accountId = System.getenv("ACCOUNT_ID");
    String apiKey = System.getenv("API_KEY");
    String applicationId = System.getenv("TUTORIAL_APPLICATION_ID");
    String toNumber = "";

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
    try {
      // Convert JSON into call status callback object
      callStatusCallback = VoiceCallback.createFromJson(body);
    } catch (FreeClimbException pe) {
      PerCLScript errorScript = new PerCLScript();
      Say sayError = new Say("There was a problem processing the incoming call.");
      errorScript.add(sayError);
      return errorScript.toJson();
    }

    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    // Verify call is in the InProgress state
    if (callStatusCallback.getCallStatus() == CallStatus.IN_PROGRESS) {
      // Create PerCL get speech script (see grammar file contents below)
      GetSpeech getSpeech = new GetSpeech(selectColorDone, grammarDownload);
      // Set location and type of grammar as well as the grammar rule
      getSpeech.setGrammarType(GrammarType.URL);
      getSpeech.setGrammarRule("FreeClimbColor");

      // Create PerCL GetSpeechNestable list
      LinkedList<GetSpeechNestable> prompts = new LinkedList<>();

      // Create PerCL say script with US English as the language
      Say say = new Say("Please select a color. Select green, red, or yellow.");
      say.setLanguage(Language.ENGLISH_US);

      // Add PerCL say script to GetSpeechNestable list
      prompts.add(say);

      // Set GetSpeechNestable list as PerCL get speech prompt list
      getSpeech.setPrompts(prompts);

      // Add PerCL get speech script to PerCL container
      script.add(getSpeech);
    }
    return script.toJson();

  }

  @RequestMapping(value = {
      "/SelectColorDone" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public String recordCallBack(@RequestBody String body) {
    GetSpeechActionCallback getSpeechActionCallback;
    try {
      // convert JSON into get speech status callback object
      getSpeechActionCallback = GetSpeechActionCallback.createFromJson(body);
    } catch (FreeClimbException pe) {
      PerCLScript errorScript = new PerCLScript();
      Say sayError = new Say("Error with get speech callback.");
      errorScript.add(sayError);
      return errorScript.toJson();
    }

    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    // Check if recognition was successful
    if (getSpeechActionCallback.getReason() == SpeechReason.RECOGNITION) {
      // Create PerCL say script with US English as the language
      Say say = new Say("Selected color was " + getSpeechActionCallback.getRecognitionResult());
      say.setLanguage(Language.ENGLISH_US);
      // Add PerCL say script to PerCL container
      script.add(say);
    } else {
      // Create PerCL say script with english as the language
      Say say = new Say("There was an error in selecting a color.");
      say.setLanguage(Language.ENGLISH_US);
      // Add PerCL say script to PerCL container
      script.add(say);
    }

    // Create PerCL pause script with a duration of 100 milliseconds
    Pause pause = new Pause(100);
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

    // Convert PerCL container to JSON and append to response
    return script.toJson();
  }

  @RequestMapping(value = { "/Status" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public String ColorSelectionStatus(@RequestBody String str) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    // Convert PerCL container to JSON and append to response
    return script.toJson();
  }

  @RequestMapping(value = "/grammarFile", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseBody
  public FileSystemResource getFile(HttpServletResponse response) {
    response.setContentType("application/xml");
    response.setHeader("Content-Disposition", "attachment; filename=\"colorGrammar.xml\"");
    return new FileSystemResource(new File("colorGrammar.xml"));
  }
}