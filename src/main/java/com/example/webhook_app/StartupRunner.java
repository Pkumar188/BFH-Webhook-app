package com.example.webhook_app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class StartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {

        RestTemplate restTemplate = new RestTemplate();

        String name = "Pavan Kumar Reddy";
        String regNo = "REG12345";   // <-- your actual reg no
        String email = "pavan@email.com";

        System.out.println("🚀 Generating Webhook...");

        // STEP 1 — Generate Webhook
        String generateUrl =
                "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("regNo", regNo);
        body.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(generateUrl, request, Map.class);

        Map<String, Object> responseBody = response.getBody();

        String webhookUrl = responseBody.get("webhook").toString();
        String token = responseBody.get("accessToken").toString();

        System.out.println("✅ Webhook Received");
        System.out.println("Webhook URL: " + webhookUrl);


        int lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
        boolean isEven = lastTwoDigits % 2 == 0;

        String finalQuery;

        if (isEven) {

            finalQuery =
                    "SELECT d.DEPARTMENT_NAME, " +
                            "AVG(TIMESTAMPDIFF(YEAR,e.DOB,CURDATE())) AS AVERAGE_AGE, " +
                            "GROUP_CONCAT(CONCAT(e.FIRST_NAME,' ',e.LAST_NAME) ORDER BY e.EMP_ID SEPARATOR ', ') AS EMPLOYEE_LIST " +
                            "FROM DEPARTMENT d " +
                            "JOIN EMPLOYEE e ON d.DEPARTMENT_ID=e.DEPARTMENT " +
                            "JOIN PAYMENTS p ON e.EMP_ID=p.EMP_ID " +
                            "WHERE p.AMOUNT>70000 " +
                            "GROUP BY d.DEPARTMENT_ID,d.DEPARTMENT_NAME " +
                            "ORDER BY d.DEPARTMENT_ID DESC";
        } else {

            finalQuery =
                    "SELECT d.DEPARTMENT_NAME,emp.SALARY,emp.EMPLOYEE_NAME,emp.AGE " +
                            "FROM (SELECT e.DEPARTMENT,e.EMP_ID,CONCAT(e.FIRST_NAME,' ',e.LAST_NAME) EMPLOYEE_NAME," +
                            "SUM(p.AMOUNT) SALARY,TIMESTAMPDIFF(YEAR,e.DOB,CURDATE()) AGE " +
                            "FROM EMPLOYEE e JOIN PAYMENTS p ON e.EMP_ID=p.EMP_ID " +
                            "WHERE DAY(p.PAYMENT_TIME)<>1 " +
                            "GROUP BY e.DEPARTMENT,e.EMP_ID,e.FIRST_NAME,e.LAST_NAME,e.DOB) emp " +
                            "JOIN (SELECT DEPARTMENT,MAX(SALARY) MAX_SALARY FROM " +
                            "(SELECT e.DEPARTMENT,e.EMP_ID,SUM(p.AMOUNT) SALARY " +
                            "FROM EMPLOYEE e JOIN PAYMENTS p ON e.EMP_ID=p.EMP_ID " +
                            "WHERE DAY(p.PAYMENT_TIME)<>1 GROUP BY e.DEPARTMENT,e.EMP_ID) t " +
                            "GROUP BY DEPARTMENT) mx " +
                            "ON emp.DEPARTMENT=mx.DEPARTMENT AND emp.SALARY=mx.MAX_SALARY " +
                            "JOIN DEPARTMENT d ON d.DEPARTMENT_ID=emp.DEPARTMENT";
        }

        System.out.println(" Submitting SQL Solution...");

        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);


        submitHeaders.set("Authorization", token);
        System.out.println("token : "+token);
        Map<String, String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalQuery);

        HttpEntity<Map<String, String>> submitRequest =
                new HttpEntity<>(submitBody, submitHeaders);

        restTemplate.postForEntity(webhookUrl, submitRequest, String.class);

        System.out.println("Succcessfully — Solution Submitted");
    }
}
