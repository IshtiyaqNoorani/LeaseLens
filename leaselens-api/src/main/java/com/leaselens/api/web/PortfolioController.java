package com.leaselens.api.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leaselens.api.service.PortfolioService;
import com.leaselens.api.service.PortfolioService.Overview;
import com.leaselens.api.service.PortfolioService.PropertyDetail;
import com.leaselens.api.service.PortfolioService.PropertyMetrics;

@RestController
@RequestMapping("/api")
public class PortfolioController {

    private final PortfolioService portfolio;

    public PortfolioController(PortfolioService portfolio) {
        this.portfolio = portfolio;
    }

    @GetMapping("/portfolio/overview")
    public Overview overview(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to) {
        return portfolio.overview(from, to);
    }

    public record PerformanceView(int count, List<PropertyMetrics> rows) {
    }

    @GetMapping("/properties/performance")
    public PerformanceView performance(@RequestParam(required = false) LocalDate from,
                                       @RequestParam(required = false) LocalDate to,
                                       @RequestParam(required = false) String sort,
                                       @RequestParam(required = false) String order) {
        List<PropertyMetrics> rows = portfolio.performance(from, to, sort, order);
        return new PerformanceView(rows.size(), rows);
    }

    @GetMapping("/properties/{id}")
    public PropertyDetail property(@PathVariable String id,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to) {
        return portfolio.property(id, from, to);
    }
}
