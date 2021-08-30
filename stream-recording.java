package main.java.stream_a_recording;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.KnownSizeInputStream;
import com.vailsys.freeclimb.api.FreeClimbException;

public class StreamRecording {

  public static void main(String[] args) {
    String accountId = System.getenv("ACCOUNT_ID");
    String apiKey = System.getenv("API_KEY");
    String recordingId = "";

    streamRecording(recordingId, accountId, apiKey);
  }

  public static void streamRecording(String recordingId, String accountId, String apiKey) {
    FreeClimbClient client;
    KnownSizeInputStream stream;

    try {
      // Create FreeClimbClient object
      client = new FreeClimbClient(accountId, apiKey);

      /*
       * Make the request for the recording. Receiving an InputStream in return which
       * can be used to stream the recording.
       */
      stream = client.recordings.stream(recordingId);

    } catch (FreeClimbException pe) {
      System.out.println(pe.getMessage());
    }
  }
}