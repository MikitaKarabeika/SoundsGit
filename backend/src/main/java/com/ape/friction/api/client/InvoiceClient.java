package com.ape.friction.api.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.ape.friction.api.dto.request.InvoiceRequest;

@FeignClient(name = "invoice-service")
public interface InvoiceClient {

    @PostMapping(value = "/invoices/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> generateInvoice(@RequestBody @Valid InvoiceRequest request);
}
