package com.geekworkshop.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekworkshop.finance.dto.InvoiceOcrRequest;
import com.geekworkshop.finance.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class BaiduOcrService {

    private final String apiKey;
    private final String secretKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String cachedAccessToken;

    public BaiduOcrService(
            @Value("${baidu.ocr.api-key:}") String apiKey,
            @Value("${baidu.ocr.secret-key:}") String secretKey
    ) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

    public RecognizedInvoice recognizeVatInvoice(Path filePath) {
        if (!isConfigured()) {
            throw new BusinessException("baidu ocr api key is not configured");
        }

        try {
            String accessToken = getAccessToken();
            String image = Base64.getEncoder().encodeToString(Files.readAllBytes(filePath));
            String body = "image=" + URLEncoder.encode(image, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice?access_token=" + accessToken))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error_code")) {
                throw new BusinessException("baidu ocr failed: " + root.path("error_msg").asText());
            }

            JsonNode words = unwrapWordsResult(root.path("words_result"));
            InvoiceOcrRequest parsed = new InvoiceOcrRequest();
            parsed.setInvoiceCode(readText(words, List.of("InvoiceCode", "invoice_code")));
            parsed.setInvoiceNumber(readText(words, List.of("InvoiceNum", "InvoiceNumber", "invoice_number")));
            parsed.setInvoiceDate(parseDate(readText(words, List.of("InvoiceDate", "invoice_date"))));
            parsed.setAmount(readInvoiceAmount(words));
            parsed.setTaxAmount(parseAmount(readText(words, List.of(
                    "TotalTax", "TaxAmount", "CommodityTax", "total_tax", "tax_amount"
            ))));
            parsed.setSellerName(readText(words, List.of("SellerName", "seller_name")));
            parsed.setBuyerName(readText(words, List.of("PurchaserName", "BuyerName", "buyer_name")));

            return new RecognizedInvoice(parsed, response.body());
        } catch (IOException exception) {
            throw new BusinessException("failed to read invoice file");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("baidu ocr request interrupted");
        }
    }

    private String getAccessToken() throws IOException, InterruptedException {
        if (cachedAccessToken != null) {
            return cachedAccessToken;
        }

        String uri = "https://aip.baidubce.com/oauth/2.0/token"
                + "?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        String accessToken = root.path("access_token").asText();
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException("failed to get baidu access token");
        }
        cachedAccessToken = accessToken;
        return cachedAccessToken;
    }

    private JsonNode unwrapWordsResult(JsonNode wordsResult) {
        JsonNode words = wordsResult;
        if (words.isArray() && !words.isEmpty()) {
            words = words.get(0);
        }
        if (words.isObject() && words.has("result")) {
            words = words.path("result");
        }
        return words;
    }

    private String readText(JsonNode words, List<String> names) {
        for (String name : names) {
            JsonNode node = words.path(name);
            if (!node.isMissingNode() && !node.isNull()) {
                String value = extractText(node);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String extractText(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = extractText(item);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
        if (node.isObject()) {
            if (node.has("word")) {
                return node.path("word").asText(null);
            }
            if (node.has("words")) {
                return node.path("words").asText(null);
            }
            if (node.has("value")) {
                return node.path("value").asText(null);
            }
            return null;
        }
        return node.asText(null);
    }

    private BigDecimal readInvoiceAmount(JsonNode words) {
        // AmountInFiguers is Baidu's documented spelling for the tax-inclusive total.
        String total = readText(words, List.of(
                "AmountInFiguers",
                "AmountInFigures",
                "TotalAmount",
                "InvoiceAmount",
                "total_amount",
                "invoice_amount"
        ));
        BigDecimal parsedTotal = parseAmount(total);
        if (parsedTotal != null) {
            return parsedTotal;
        }

        // Older invoice variants may only expose the pre-tax amount.
        return parseAmount(readText(words, List.of(
                "AmountWithoutTax",
                "PretaxAmount",
                "CommodityAmount",
                "amount_without_tax",
                "pretax_amount"
        )));
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value
                .replace(",", "")
                .replaceAll("[^0-9.\\-]", "")
                .trim();
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "");
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ofPattern("yyyy-M-d")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (RuntimeException ignored) {
                // Try the next common invoice date format.
            }
        }
        return null;
    }

    public record RecognizedInvoice(InvoiceOcrRequest invoice, String rawJson) {
    }
}
