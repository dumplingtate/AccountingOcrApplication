package com.accounting.ocr.controller;

import com.accounting.ocr.model.Counterparty;
import com.accounting.ocr.repository.CounterpartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {

    private final CounterpartyRepository repository;

    @GetMapping
    public List<Counterparty> getAll() {
        return repository.findAll();
    }
}