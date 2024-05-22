package mesavage.k;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    private final Semaphore semaphore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        semaphore = new Semaphore(requestLimit);
        executor.scheduleAtFixedRate(semaphore::release, 0, 1, timeUnit);
    }

    public void createDocument(String token, Document document, String signature) {
        try {
            semaphore.acquire();
            String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("document", document);
            requestBody.put("signature", signature);
            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response code: " + response.statusCode());
            if (response.statusCode() != 200) {
                System.out.println("Error: " + response.statusCode() + " - " + response.body());
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;
    }

    private static class Description {
        private String participantInn;
    }

    private static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}