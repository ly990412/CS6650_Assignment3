package org.example;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
public class Servlet extends HttpServlet{
    private RmqConnection rmqConnection;
    private DatabaseConnector databaseConnector;
    public static final int numChannel = 100;
    public static final int maxSwiperId = 5000;
    public static final int maxSwipeeId = 50000;
    public static final int commentLength = 256;
    public static final String rmqConnectionName = "Servlet";
    public static final String rmqHostName = "54.68.3.66";
    public static final String exchangeName = "swipe_exchange";
    public static final String exchangeType = "fanout";
    public static final String matchTag = "matches";
    private static Gson gson = new Gson();
    private static final Pattern validPostPaths[] = {
            Pattern.compile("/swipe/(left|right)"),
    };
    private static final Pattern validGetPaths[] = {
            Pattern.compile("/matches/([1-9][0-9]{0,3}|[1-4][0-9]{0,4}|50000)/?"),
            Pattern.compile("/stats/([1-9][0-9]{0,3}|[1-4][0-9]{0,4}|50000)/?"),
    };

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            rmqConnection = RmqConnection.create(numChannel, rmqHostName,
                    rmqConnectionName);
            rmqConnection.exchange(exchangeName, exchangeType, true);
            databaseConnector = DatabaseConnector.createDatabaseConnector();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();
        if (!isValidGetPath(urlPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid GET path"); // HTTP 404
            return;
        }
        String[] pathParams = urlPath.split("/");
        String tag = pathParams[1];
        int userId = Integer.parseInt(pathParams[2]);
        try {
            String respBody;
            if (tag.equals(matchTag)) {
                User userData = databaseConnector.getGoodUsers(userId);
                if (userData == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found"); // HTTP 404
                    return;
                }
                respBody = gson.toJson(new GetMatchesResponseJson(userData.getUsers()));
            } else {
                SwipeGood userData = databaseConnector.getSwipeGood(userId);
                if (userData == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found"); // HTTP 404
                    return;
                }
                respBody = gson.toJson(new GetStatsResponseJson(userData.getGood(), userData.getBad()));
            }
            response.setStatus(HttpServletResponse.SC_OK); // HTTP 200
            response.getWriter().write(respBody);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Inputs"); // HTTP 400
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();
        // Validate path
        if (!isValidPostPath(urlPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid POST path"); // HTTP 404
            return;
        }
        // Borrow a channel from the channel pool
        Channel channel = rmqConnection.borrowChannel();
        try {
            String requestBody = readRequestBody(request);
            // Parse request's JSON into a PostRequestJson Object
            PostRequestJson jsonPayload = gson.fromJson(requestBody, Servlet.PostRequestJson.class);
            // Validate post request JSON
            if (!isValidPostRequestJson(jsonPayload)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid inputs"); // HTTP 400
                return;
            }
            // Build the message to be sent to the RMQ broker
            boolean liked = urlPath.contains("right") ? true : false;
            String msg = "{swiper:" + jsonPayload.swiper + ",swipee:" + jsonPayload.swipee + ",like:" + liked + "}";
            // Publish the JSON message to the fanout exchange
            channel.basicPublish(exchangeName,
                    "",
                    null, // Message properties
                    msg.getBytes(StandardCharsets.UTF_8));
            response.setStatus(HttpServletResponse.SC_CREATED); // HTTP 201
            response.getWriter().write("Write successful");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Issue updating user data"); // HTTP 404
            e.printStackTrace();
        } finally {
            // Return the channel to the channel pool
            rmqConnection.returnChannel(channel);
        }
    }

    private boolean isValidGetPath(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        for (int i=0; i < validGetPaths.length; i++) {
            if (validGetPaths[i].matcher(url).matches()) { return true; }
        }
        return false;
    }

    private boolean isValidPostPath(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        for (int i=0; i < validPostPaths.length; i++) {
            if (validPostPaths[i].matcher(url).matches()) { return true; }
        }
        return false;
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader requestBody = request.getReader();
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = requestBody.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    private boolean isValidPostRequestJson(PostRequestJson json) {
        return json.swiper >= 1 && json.swiper <= maxSwiperId &&
                json.swipee >= 1 && json.swipee <= maxSwipeeId &&
                json.comment != null && json.comment.length() == commentLength;
    }

    private static class GetMatchesResponseJson {
        public List<Integer> matchList;

        public GetMatchesResponseJson(List<Integer> matchList) {
            this.matchList = matchList;
        }
    }
    private static class GetStatsResponseJson {
        public int numGood;
        public int numBad;

        public GetStatsResponseJson(int numGood, int numBad) {
            this.numGood = numGood;
            this.numBad = numBad;
        }
    }
    private static class PostRequestJson {
        public int swiper;
        public int swipee;
        public String comment;
    }
}
