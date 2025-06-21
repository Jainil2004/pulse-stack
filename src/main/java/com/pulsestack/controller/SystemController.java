package com.pulsestack.controller;

import com.pulsestack.dto.SystemDto;
import com.pulsestack.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/api/systems")
public class SystemController {

    private final SystemService systemService;

    @Autowired
    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @PostMapping("/register")
    public ResponseEntity<SystemDto> registerSystem(@RequestParam String systemName) throws NoSuchAlgorithmException {
        return ResponseEntity.ok(systemService.registerSystem(systemName));
    }

    @GetMapping()
    public List<SystemDto> getAllSystems() {
        return systemService.getAllSystems();
    }

    @DeleteMapping("/{systemName}")
    public ResponseEntity<Void> deleteSystemByName(@PathVariable String systemName) {
        boolean isDeleted = systemService.deleteSystemByName(systemName);
        return isDeleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

//    add endpoints for name update, fetching information about a specific endpoint

//    @PatchMapping("update/{originalName}")
//    public ResponseEntity<SystemDto> updateSystemName(@PathVariable String originalName, @RequestBody Map<String, Object> updates) {
//        String newName;
//        updates.forEach((key, value) -> {
//            switch (key) {
//                case "newName":
//                    newName = (String) value;
//            }
//        });
//        return ResponseEntity.ok(systemService.updateSystemName(originalName, newName));
//    }

    @PatchMapping("update/{originalName}")
    public ResponseEntity<SystemDto> updateSystem(@PathVariable String originalName, @RequestBody Map<String, Object> updates) {

        String newName = null;

        if (updates.containsKey("newName")) {
            Object value = updates.get("newName");
            if (value instanceof String) {
                newName = (String) value;
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        // In future: other update fields can be extracted similarly
        // e.g., Boolean isActive = updates.containsKey("isActive") ? (Boolean) updates.get("isActive") : null;

        SystemDto updatedSystem = systemService.updateSystemName(originalName, newName);

        return ResponseEntity.ok(updatedSystem);
    }

    @GetMapping("/get/{systemName}")
    public ResponseEntity<SystemDto> getSystemDetailsUsingSystemName(@PathVariable String systemName) {
        return ResponseEntity.ok(systemService.getSystemUsingSystemName(systemName));
    }

    @GetMapping("/{systemId}")
    public ResponseEntity<SystemDto> getSystemDetailsUsingSystemId(@PathVariable String systemId) {
        return ResponseEntity.ok(systemService.getSystemUsingSystemId(systemId));
    }
}
