package main.java.list_messages;

import com.vailsys.freeclimb.api.FreeClimbClient;
import com.vailsys.freeclimb.api.FreeClimbException;
import com.vailsys.freeclimb.api.message.MessageList;
import com.vailsys.freeclimb.api.message.Message;
import java.util.ArrayList;

public class Application {

  public static void main(String[] args) {
    FreeClimbClient client;
    MessageList messageList;
    String accountId = System.getenv("ACCOUNT_ID");
    String apiKey = System.getenv("API_KEY");
    try {
      client = new FreeClimbClient(accountId, apiKey); // Create FreeClimbClient object
      messageList = client.messages.get();

      // Don't bother trying to grab more pages if there is only one or zero
      // pages of results
      if (messageList.getTotalSize() > messageList.getPageSize()) {
        // Load in all the messages returned.
        while (messageList.getLocalSize() < messageList.getTotalSize()) {
          messageList.loadNextPage(); // Load in the next page of messages.
        }
      }

      ArrayList<Message> allMessages = messageList.export(); // Extract the array from the messageList

      for (Message m : allMessages) {
        // do something with each message
        System.out.print(m);
      }
    } catch (FreeClimbException e) {
      // Handle Errors
    }
  }
}