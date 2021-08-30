package main.java.send_a_message;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.message.Status;
import com.vailsys.freeclimb.percl.PerCLScript;
import com.vailsys.freeclimb.percl.Sms;
import com.vailsys.freeclimb.webhooks.call.VoiceCallback;
import com.vailsys.freeclimb.webhooks.message.MessageStatus;

@RestController
public class SendAMessage {
  private final String fromNumber = System.getenv("FREE_CLIMB_PHONE_NUMBER");
  private final String notificationUrl = String.format("%s/notificationUrl", System.getenv("HOST"));

  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public String inboundCall(@RequestBody String body) {
    VoiceCallback voiceCallback;

    try {
      // convert json string to object
      voiceCallback = VoiceCallback.createFromJson(body);

      PerCLScript script = new PerCLScript();

      // send an sms message to the caller
      Sms sms = new Sms(voiceCallback.getFrom(), fromNumber, "Hello from FreeClimb");

      // set notificationUrl for when the message changes status
      sms.setNotificationUrl(notificationUrl);

      script.add(sms);

      return script.toJson();
    } catch (FreeClimbException pe) {
      // handle errors
    }

    return "[]";
  }

  @RequestMapping(value = {
      "/notificationUrl" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)

  public String notificationUrl(@RequestBody String body) {
    MessageStatus messageStatus;

    try {
      messageStatus = MessageStatus.createFromJson(body);
      Status status = messageStatus.getStatus();
      if (status == Status.FAILED || status == Status.REJECTED) {
        // message Failed to send
      }
    } catch (FreeClimbException e) {
      // handle errors
    }
    return "[]";
  }
}