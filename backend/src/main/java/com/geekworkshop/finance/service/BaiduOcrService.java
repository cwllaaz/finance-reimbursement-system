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

            JsonNode words = root.path("words_result");
            InvoiceOcrRequest parsed = new InvoiceOcrRequest();
            parsed.setInvoiceCode(readText(words, List.of("InvoiceCode", "invoice_code", "发票代码")));
            parsed.setInvoiceNumber(readText(words, List.of("InvoiceNum", "InvoiceNumber", "invoice_number", "发票号码")));
            parsed.setInvoiceDate(parseDate(readText(words, List.of("InvoiceDate", "invoice_date", "开票日期"))));
            parsed.setAmount(parseAmount(readText(words, List.of("AmountWithoutTax", "CommodityAmount", "PretaxAmount", "金额"))));
            parsed.setTaxAmount(parseAmount(readText(words, List.of("CommodityTax", "TotalTax", "TaxAmount", "税额"))));
            parsed.setSellerName(readText(words, List.of("SellerName", "seller_name", "销售方名称")));
            parsed.setBuyerName(readText(words, List.of("PurchaserName", "BuyerName", "buyer_name", "购买方名称")));

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

    private String readText(JsonNode words, List<String> names) {
        for (String name : names) {
            JsonNode node = words.path(name);
            if (!node.isMissingNode() && !node.isNull()) {
                if (node.isObject() && node.has("word")) {
                    return node.path("word").asText(null);
                }
                return node.asText(null);
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace("¥", "").replace(",", "").trim();
        if (normalized.isBlank()) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace("年", "-").replace("月", "-").replace("日", "");
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
