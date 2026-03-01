package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.EnrollmentRequest;
import com.ogabek.istudy.dto.response.EnrollmentDto;
import com.ogabek.istudy.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentDto> createEnrollment(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.createEnrollment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnrollmentDto> updateEnrollment(@PathVariable Long id,
                                                          @RequestBody EnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.updateEnrollment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentDto>> getEnrollments(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(enrollmentService.getEnrollments(studentId, groupId));
    }
}
