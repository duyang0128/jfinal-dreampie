/*
 * Copyright 2014 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cn.dreampie.common.plugin.atmosphere.chat;

import org.atmosphere.config.service.*;
import org.atmosphere.cpr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple annotated class that demonstrate the power of Atmosphere. This class supports all transports, support
 * message length guarantee, heart beat, message cache thanks to the {@link org.atmosphere.config.service.ManagedService}.
 */
@ManagedService(path = "/im/{room: [a-zA-Z][a-zA-Z_0-9]*}")
public class ChatRoom {
  private final Logger logger = LoggerFactory.getLogger(ChatRoom.class);

  private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<String, String>();

  private final static String CHAT = "/im/";

  @PathParam("room")
  private String chatroomName;

  private BroadcasterFactory factory;

  private AtmosphereResourceFactory resourceFactory;

  /**
   * Invoked when the connection as been fully established and suspended, e.g ready for receiving messages.
   *
   * @param r resource
   * @return chatprotocol
   */
  @Ready(value = Ready.DELIVER_TO.ALL, encoders = {JsonEncoder.class})
  public ChatProtocol onReady(final AtmosphereResource r) {
    logger.info("Browser {} connected.", r.uuid());

    factory = r.getAtmosphereConfig().getBroadcasterFactory();
    resourceFactory = r.getAtmosphereConfig().resourcesFactory();

    return new ChatProtocol(users.keySet(), getRooms(factory.lookupAll()));
  }

  private static Collection<String> getRooms(Collection<Broadcaster> broadcasters) {
    Collection<String> result = new ArrayList<String>();
    for (Broadcaster broadcaster : broadcasters) {
      if (!("/*".equals(broadcaster.getID()))) {
        result.add(broadcaster.getID().split("/")[2]);
      }
    }
    ;
    return result;
  }

  /**
   * Invoked when the client disconnect or when an unexpected closing of the underlying connection happens.
   *
   * @param event resourceevent
   */
  @Disconnect
  public void onDisconnect(AtmosphereResourceEvent event) {
    if (event.isCancelled()) {
      // We didn't get notified, so we remove the user.
      users.values().remove(event.getResource().uuid());
      logger.info("Browser {} unexpectedly disconnected", event.getResource().uuid());
    } else if (event.isClosedByClient()) {
      logger.info("Browser {} closed the connection", event.getResource().uuid());
    }
  }

  @Message(encoders = {JsonEncoder.class}, decoders = {ProtocolDecoder.class})
  public ChatProtocol onMessage(ChatProtocol message) throws IOException {

    if (!users.containsKey(message.getAuthor())) {
      users.put(message.getAuthor(), message.getUuid());
      return new ChatProtocol(message.getAuthor(), " entered room " + chatroomName, users.keySet(), getRooms(factory.lookupAll()));
    }

    if (message.getMessage().contains("disconnecting")) {
      users.remove(message.getAuthor());
      return new ChatProtocol(message.getAuthor(), " disconnected from room " + chatroomName, users.keySet(), getRooms(factory.lookupAll()));
    }

    message.setUsers(users.keySet());
    logger.info("{} just send {}", message.getAuthor(), message.getMessage());
    return new ChatProtocol(message.getAuthor(), message.getMessage(), users.keySet(), getRooms(factory.lookupAll()));
  }

  @Message(decoders = {UserDecoder.class})
  public void onPrivateMessage(UserMessage user) throws IOException {
    String userUUID = users.get(user.getAuthor());
    if (userUUID != null) {
      // Retrieve the original AtmosphereResource
      AtmosphereResource r = resourceFactory.find(userUUID);

      if (r != null) {
        ChatProtocol m = new ChatProtocol(user.getAuthor(), " sent you a private message: " + (user.getMessage().indexOf(":") > 0 ? user.getMessage().split(":")[1] : user.getMessage()), users.keySet(), getRooms(factory.lookupAll()));
        if (!user.getReceiver().equalsIgnoreCase("all")) {
          factory.lookup(CHAT + chatroomName).broadcast(m, r);
        }
      }
    } else {
      ChatProtocol m = new ChatProtocol(user.getAuthor(), " sent a message to all chatroom: " + (user.getMessage().indexOf(":") > 0 ? user.getMessage().split(":")[1] : user.getMessage()), users.keySet(), getRooms(factory.lookupAll()));
      MetaBroadcaster.getDefault().broadcastTo("/*", m);
    }
  }

}