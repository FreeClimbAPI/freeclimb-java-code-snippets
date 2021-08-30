/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * CALL FreeClimb NUMBER ASSOCIATED WITH THIS FreeClimb App (CONFIGURED IN FreeClimb DASHBOARD)
 * EXPECT TO RECEIVE MESSAGE REPEATEDLY:
 * "Thank you for waiting. Press any key to exit queue."
 * PRESS ANY KEY & EXPECT TO RECEIVE MESSAGE:
 * "Call exited queue."
*/

package main.java.inbound_call_queue;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.call.CallStatus;
import com.vailsys.freeclimb.api.queue.Queue;
import com.vailsys.freeclimb.api.queue.QueueCreateOptions;

import com.vailsys.freeclimb.percl.PerCLScript;
import com.vailsys.freeclimb.percl.Say;
import com.vailsys.freeclimb.percl.Language;
import com.vailsys.freeclimb.percl.Pause;
import com.vailsys.freeclimb.percl.Enqueue;

import com.vailsys.freeclimb.webhooks.application.ApplicationVoiceCallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.RestController;

import com.vailsys.freeclimb.webhooks.queue.QueueWaitCallback;
import com.vailsys.freeclimb.webhooks.percl.GetDigitsActionCallback;
import com.vailsys.freeclimb.percl.GetDigits;
import com.vailsys.freeclimb.percl.GetDigitsNestable;
import com.vailsys.freeclimb.percl.Hangup;
import com.vailsys.freeclimb.percl.Dequeue;
import com.vailsys.freeclimb.webhooks.queue.QueueActionCallback;

import java.util.LinkedList;

@RestController
public class InboundCallQueueController {
  // Get base URL, accountID, and apiKey from environment variables
  private String baseUrl = System.getenv("HOST");
  private String accountId = System.getenv("ACCOUNT_ID");
  private String apiKey = System.getenv("API_KEY");

  // To properly communicate with FreeClimb's API, set your FreeClimb app's
  // VoiceURL endpoint to '{yourApplicationURL}/InboundCall' for this example
  // Your FreeClimb app can be configured in the FreeClimb Dashboard
  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCall(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    try {
      // Create a FreeClimbClient object
      FreeClimbClient client = new FreeClimbClient(accountId, apiKey);

      if (request != null) {
        // Convert the JSON into a request object
        ApplicationVoiceCallback callback = ApplicationVoiceCallback.createFromJson(request);

        // Verify inbound call is in the proper state
        if (callback.getCallStatus() == CallStatus.RINGING) {
          // Create PerCL say script with US English as the language
          Say say = new Say("Hello. Your call will be queued.");
          say.setLanguage(Language.ENGLISH_US);

          // Add PerCL say script to PerCL container
          script.add(say);

          // Create PerCL pause script with a 100 millisecond pause
          script.add(new Pause(100));

          // Create Queue options with an alias
          QueueCreateOptions options = new QueueCreateOptions();
          options.setAlias("InboundCallQueue");

          // Create a queue with an alias
          Queue queue = client.queues.create(options);
          // Create PerCL say to enqueue the call into the newly created queue with an
          // actionUrl
          Enqueue enqueue = new Enqueue(queue.getQueueId(), baseUrl + "/InboundCallAction");

          // Add waitUrl
          enqueue.setWaitUrl(baseUrl + "/InboundCallWait");

          // Add PerCL enqueue script to PerCL container
          script.add(enqueue);
        }
      }
    } catch (FreeClimbException pe) {
      System.out.println(pe.getMessage());
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/InboundCallWait" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCallWait(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    if (request != null) {

      try {
        // Convert the JSON into a request object
        QueueWaitCallback.createFromJson(request);

        // Create PerCL getdigits script
        GetDigits digits = new GetDigits(baseUrl + "/CallDequeueSelect");

        // Create a list of prompts to use with the getdigits command
        LinkedList<GetDigitsNestable> prompts = new LinkedList<>();

        // Create PerCL say script with US English as the language
        Say say = new Say("Thank you for waiting. Press any key to exit queue.");
        say.setLanguage(Language.ENGLISH_US);

        // Add say script to the list of prompts
        prompts.add(say);

        // Set the list as the prompts to use with the getdigits command
        digits.setPrompts(prompts);

        // Add PerCL getdigits script to PerCL container
        script.add(digits);
      } catch (FreeClimbException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/CallDequeueSelect" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> callDequeue(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    if (request != null) {
      try {
        // Convert JSON into a request object
        GetDigitsActionCallback callback = GetDigitsActionCallback.createFromJson(request);

        // Check if a digit was pressed
        if (callback.getDigits() != null && callback.getDigits().length() > 0) {
          // Create PerCL dequeue script and add to PerCL container
          script.add(new Dequeue());
        } else {
          // Create PerCl getdigits script
          GetDigits digits = new GetDigits(baseUrl + "/CallDequeueSelect");

          // Create a list of prompts to use with the getdigits command
          LinkedList<GetDigitsNestable> prompts = new LinkedList<>();

          // Create PerCL say script with US English as the language
          Say say = new Say("Thank you for waiting. Press any key to exit queue.");
          say.setLanguage(Language.ENGLISH_US);

          // Add say script to prompts list
          prompts.add(say);

          // Add prompts list to the getdigits command
          digits.setPrompts(prompts);

          // Add PerCL getdgitis script to PerCL container
          script.add(digits);
        }
      } catch (FreeClimbException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/InboundCallAction" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> dequeueAction(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    if (request != null) {
      // Convert JSON into a request object
      try {
        QueueActionCallback.createFromJson(request);

        // Create PerCL say script with US English as the language
        Say say = new Say("Call exited queue.");
        say.setLanguage(Language.ENGLISH_US);

        // Add PerCL say script to PerCL container
        script.add(say);

        // Create and add PerCL hangup script to PerCL container
        script.add(new Hangup());
      } catch (FreeClimbException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }
}