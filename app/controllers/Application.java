package controllers;

import play.*;
import play.libs.F;
import play.mvc.*;

import java.util.*;

import models.*;

import static play.libs.F.Matcher.ClassOf;
import static play.libs.F.Matcher.Equals;
import static play.libs.F.Matcher.String;
import static play.mvc.Http.WebSocketEvent.SocketClosed;
import static play.mvc.Http.WebSocketEvent.TextFrame;

public class Application extends Controller {

    /** The live stream. */
    public static play.libs.F.EventStream<String> liveStream = new play.libs.F.EventStream<String>();


    public static void index() {
        render();
    }


    public static class PushSelectionSocket extends WebSocketController {

        public static void process() {

            while (inbound.isOpen()) {
                try {
                    Logger.info("Waiting for next event...");
                    F.Either<Http.WebSocketEvent,String> e = await(F.Promise.waitEither(
                            inbound.nextEvent(),
                            liveStream.nextEvent()
                    ));

                    for(String msg: TextFrame.and(Equals("close")).match(e._1)) {
                        Logger.info("socket close requested");
                        disconnect();
                    }

                    // Case: TextEvent received on the socket
                    for(String event: TextFrame.match(e._1)) {
                        Logger.info("socket outbound send:"+ event);
                        liveStream.publish(event);
                    }

                    for(String event: ClassOf(String.class).match(e._2)) {
                        Logger.info("Publishing selection %s to Outbound Subscribers", event);
                        outbound.send(event);
                    }

                    // Case: The socket has been closed
                    for(Http.WebSocketClose closed: SocketClosed.match(e._1)) {
                        Logger.info("socket closed by "+ session.getId());
                        disconnect();
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

    }

}