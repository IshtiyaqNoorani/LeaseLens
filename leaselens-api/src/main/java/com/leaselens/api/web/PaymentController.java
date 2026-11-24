package com.leaselens.api.web;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leaselens.api.service.PaymentService;
import com.leaselens.api.service.PaymentService.Filter;
import com.leaselens.api.service.PaymentService.OutstandingView;
import com.leaselens.api.service.PaymentService.PercentilesView;
import com.leaselens.api.service.PaymentService.ThresholdView;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    /** {@code p} is a comma-separated list of percentiles, e.g. {@code 50,75,90,99.5}. */
    @GetMapping("/percentiles")
    public PercentilesView percentiles(@RequestParam(required = false) String p,
                                       @RequestParam(required = false) String propertyId,
                                       @RequestParam(required = false) String propertyType,
                                       @RequestParam(required = false) LocalDate from,
                                       @RequestParam(required = false) LocalDate to,
                                       @RequestParam(required = false) Long seed) {
        return payments.percentiles(p, new Filter(propertyId, propertyType, from, to), seed);
    }

    @GetMapping("/threshold")
    public ThresholdView threshold(@RequestParam(defaultValue = "90") String p,
                                   @RequestParam(required = false) String propertyId,
                                   @RequestParam(required = false) String propertyType,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) Long seed,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        return payments.threshold(p, new Filter(propertyId, propertyType, from, to), seed, page, size);
    }

    @GetMapping("/outstanding")
    public OutstandingView outstanding(@RequestParam(required = false) String propertyId,
                                       @RequestParam(required = false) String propertyType,
                                       @RequestParam(required = false) LocalDate from,
                                       @RequestParam(required = false) LocalDate to) {
        return payments.outstanding(new Filter(propertyId, propertyType, from, to));
    }
}
