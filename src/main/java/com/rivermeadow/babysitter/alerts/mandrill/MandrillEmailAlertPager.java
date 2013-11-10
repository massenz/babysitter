package com.rivermeadow.babysitter.alerts.mandrill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rivermeadow.babysitter.alerts.Pager;
import com.rivermeadow.babysitter.model.Server;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
@Component
public class MandrillEmailAlertPager implements Pager {

    private static final Logger logger = Logger.getLogger(MandrillEmailAlertPager.class);

    public static final String URL = "https://mandrillapp.com/api/1.0/messages/send.json";
    private final String apiKey;
    private final String templateLocation;

    ObjectMapper mapper = new ObjectMapper();

    public MandrillEmailAlertPager(String apiKey, String templateLocation) {
        this.apiKey = apiKey;
        this.templateLocation = templateLocation;
    }

    @Override
    public void page(Server server) {
        logger.info(String.format(">>>>>>>>> ALERT for %s <<<<<<<<<<",
                server.getName()));
        try {
            // Build the POST to sent to Mandrill API
            // TODO: update usage, apparently this is deprecated
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(URL);
            StringEntity body = new StringEntity(buildPostAlertBody(server));
            body.setContentType("application/json");
            post.setEntity(body);
            HttpClientParams.setRedirecting(post.getParams(), false);
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.error(String.format("Error returned by Mandrill API: %s",
                        response.getStatusLine().getReasonPhrase()));
            }
        } catch (Exception ex) {
            logger.error("Could not send POST to Mandrill: " + ex.getLocalizedMessage(), ex);
        }
    }

    private String buildPostAlertBody(Server server) {
        try {
            // TODO: this is assumed to be in the classpath, is this the right thing to do in production?
            Map<String, Object> postData = mapper.readValue(getClass().getResourceAsStream
                    (templateLocation), Map.class);
            postData.put("key", apiKey);
            logger.debug("POST Email template: " + postData);

            // TODO: replace all this casting (evil!) with a proper object model
            Map<String, Object> message = (Map<String, Object>) postData.get("message");
            String htmlBody = (String) message.get("html");
            message.put("html", buildHtmlEmailBody(htmlBody, server));
            String result = mapper.writeValueAsString(postData);
            logger.debug("After substitution: " + result);
            return result;
        } catch (IOException e) {
            logger.error("Could not build the POST body: " + e.getLocalizedMessage());
            throw new IllegalStateException(e);
        }
    }

    private String buildHtmlEmailBody(String htmlBody, Server server) throws JsonProcessingException {
        String title = String.format("Server %s [%s@%s] terminated unexpectedly",
                server.getName(),
                server.getServerAddress().getHostname(),
                server.getServerAddress().getIp());
        String summary = String.format("Server %s unexpectedly failed to communicate with the " +
                "monitoring service at %s: last known payload was:<br><pre>%s</pre>",
                server.getServerAddress().getHostname(), new Date(), server.getData());
        String details = mapper.enable(SerializationFeature.INDENT_OUTPUT)
                               .writeValueAsString(server);
        return MessageFormat.format(htmlBody, title, summary, details);
    }
}
