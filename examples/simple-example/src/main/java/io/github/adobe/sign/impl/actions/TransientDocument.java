package io.github.adobe.sign.impl.actions;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import io.github.adobe.sign.core.actions.SignAction;
import io.github.adobe.sign.core.actions.SignActionException;
import io.github.adobe.sign.core.actions.SignActionMetadata;
import io.github.adobe.sign.core.auth.Authenticated;
import io.github.adobe.sign.core.auth.CredentialLoader;
import io.github.adobe.sign.core.auth.SignAuth;
import io.github.adobe.sign.core.logger.SignLogger;

public class TransientDocument implements SignAction {

    private CredentialLoader credentialLoader;
    private String TRANSIENT_DOCUMENTS = "https://api.na1.echosign.com/api/rest/v6/transientDocuments";

    public TransientDocument(CredentialLoader credentialLoader) {
        this.credentialLoader = credentialLoader;
    }

    @Override
    public SignActionMetadata beforeAction(SignAuth signAuth, SignActionMetadata metadata, SignLogger logger)
            throws SignActionException {
        return metadata;
    }

    @Override
    public SignActionMetadata doAction(SignAuth signAuth, SignActionMetadata metadata, SignLogger logger)
            throws SignActionException {
        List<Header> headers = null;

        try {
            Authenticated authenticated = signAuth.authenticate(this.credentialLoader);
            Header bearerToken = new BasicHeader("Authorization",
                    String.format("Bearer %s", authenticated.getAccessToken()));
            Header apiUser = new BasicHeader("x-api-user", "email:patrique.legault+dev@aftia.com");
            headers = List.of(bearerToken, apiUser);
        } catch (Exception e) {
            throw new SignActionException(e.getMessage(), e);
        }

        try (CloseableHttpClient client = HttpClients.custom().setDefaultHeaders(headers).build()) {

            HttpPost httpPost = new HttpPost(TRANSIENT_DOCUMENTS);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("File-Name", "Sample.pdf");
            builder.addTextBody("Mime-Type", "application/pdf");
            builder.addBinaryBody("File", this.getClass().getResourceAsStream("/sample.pdf"),
                    ContentType.APPLICATION_OCTET_STREAM, "Sample.pdf");

            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            // Create a custom response handler
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        JsonObject payload = JsonParser.parseString(EntityUtils.toString(responseEntity)).getAsJsonObject();
                        return payload.get("transientDocumentId").getAsString();
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            String transientId = client.execute(httpPost, responseHandler);
            transientId = transientId + "a";
        } catch (Exception e) {
            throw new SignActionException(e.getMessage(), e);
        }

        return metadata;
    }

    @Override
    public SignActionMetadata postAction(SignAuth signAuth, SignActionMetadata metadata, SignLogger logger)
            throws SignActionException {
        return metadata;

    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
