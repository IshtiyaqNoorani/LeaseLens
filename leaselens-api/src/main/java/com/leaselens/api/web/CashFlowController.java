package com.leaselens.api.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leaselens.api.service.AlgorithmRun;
import com.leaselens.api.service.CashFlowService;
import com.leaselens.api.service.CashFlowService.AdjustmentRequest;
import com.leaselens.api.service.CashFlowService.AdjustmentView;
import com.leaselens.api.service.CashFlowService.PeriodsView;
import com.leaselens.api.service.CashFlowService.RangeView;
import com.leaselens.api.service.CashFlowService.SeriesPoint;
import com.leaselens.api.store.Adjustment;

@RestController
@RequestMapping("/api/cashflow")
public class CashFlowController {

    private final CashFlowService cashFlow;

    public CashFlowController(CashFlowService cashFlow) {
        this.cashFlow = cashFlow;
    }

    @GetMapping("/range")
    public RangeView range(@RequestParam(defaultValue = "portfolio") String scope,
                           @RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to) {
        return cashFlow.range(scope, from, to);
    }

    @GetMapping("/periods")
    public PeriodsView periods(@RequestParam(defaultValue = "portfolio") String scope,
                               @RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               @RequestParam(defaultValue = "MONTH") String granularity) {
        return cashFlow.periods(scope, from, to, granularity);
    }

    public record SeriesView(String scope, List<SeriesPoint> points) {
    }

    @GetMapping("/series")
    public SeriesView series(@RequestParam(defaultValue = "portfolio") String scope,
                             @RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to) {
        return new SeriesView(scope, cashFlow.series(scope, from, to));
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustmentView adjust(@RequestBody AdjustmentRequest request) {
        return cashFlow.adjust(request);
    }

    @GetMapping("/adjustments")
    public Map<String, List<Adjustment>> adjustments() {
        return Map.of("adjustments", cashFlow.adjustments());
    }

    @DeleteMapping("/adjustments/{id}")
    public Map<String, AlgorithmRun> undo(@PathVariable String id) {
        return Map.of("algorithm", cashFlow.undo(id));
    }

    @DeleteMapping("/adjustments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        cashFlow.reset();
    }
}
