package com.orque.crm.settings.controller;

import com.orque.crm.settings.dto.UserSettingsDto;
import com.orque.crm.settings.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

@RestController
@RequestMapping("/api/v1/user-settings")
@RequiredArgsConstructor
@CrossOrigin
public class UserSettingsController {

    private final UserSettingsService service;

    @GetMapping
    public ResponseEntity<UserSettingsDto> get() {
        return ResponseEntity.ok(service.getForCurrentUser());
    }

    @PutMapping
    public ResponseEntity<UserSettingsDto> save(@RequestBody UserSettingsDto dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    @GetMapping("/detected-printers")
    public ResponseEntity<List<String>> getDetectedPrinters() {
        List<String> printerNames = new ArrayList<>();
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService printer : printServices) {
                printerNames.add(printer.getName());
            }
        } catch (Exception e) {
            // Ignore print daemon errors
        }
        return ResponseEntity.ok(printerNames);
    }
}
