package com.leaselens.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leaselens.api.service.BenchmarkService;
import com.leaselens.api.service.BenchmarkService.LiveRequest;
import com.leaselens.api.service.BenchmarkService.LiveResult;
import com.leaselens.api.service.BenchmarkService.Results;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final BenchmarkService benchmarks;

    public BenchmarkController(BenchmarkService benchmarks) {
        this.benchmarks = benchmarks;
    }

    @GetMapping("/results")
    public Results results() {
        return benchmarks.results();
    }

    @PostMapping("/run")
    public LiveResult run(@RequestBody LiveRequest request) {
        return benchmarks.runLive(request);
    }
}
