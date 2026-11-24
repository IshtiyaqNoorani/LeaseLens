package com.leaselens.api.web;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leaselens.api.service.ExpenseService;
import com.leaselens.api.service.ExpenseService.SummaryView;
import com.leaselens.api.service.ExpenseService.UnusualView;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenses;

    public ExpenseController(ExpenseService expenses) {
        this.expenses = expenses;
    }

    @GetMapping("/summary")
    public SummaryView summary(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               @RequestParam(required = false) String propertyId) {
        return expenses.summary(from, to, propertyId);
    }

    @GetMapping("/unusual")
    public UnusualView unusual(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               @RequestParam(required = false) Double fence) {
        return expenses.unusual(from, to, fence);
    }
}
