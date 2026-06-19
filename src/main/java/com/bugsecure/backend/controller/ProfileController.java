package com.bugsecure.backend.controller;

import com.bugsecure.backend.dto.UserDTO;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserDTO userDTO = convertToDTO(user);
            response.put("success", true);
            response.put("data", userDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProfileById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserDTO userDTO = convertToDTO(user);
            response.put("success", true);
            response.put("data", userDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update profile fields
            if (body.containsKey("username")) {
                user.setUsername(body.get("username"));
            }
            if (body.containsKey("bio")) {
                user.setBio(body.get("bio"));
            }
            if (body.containsKey("profileImage")) {
                user.setProfileImage(body.get("profileImage"));
            }
            if (body.containsKey("phoneNumber")) {
                user.setPhoneNumber(body.get("phoneNumber"));
            }
            if (body.containsKey("address")) {
                user.setAddress(body.get("address"));
            }
            if (body.containsKey("website")) {
                user.setWebsite(body.get("website"));
            }
            if (body.containsKey("companyName")) {
                user.setCompanyName(body.get("companyName"));
            }

            User updated = userRepository.save(user);
            UserDTO userDTO = convertToDTO(updated);
            response.put("success", true);
            response.put("data", userDTO);
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("error", "File must be an image");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("error", "File size must be less than 5MB");
                return ResponseEntity.badRequest().body(response);
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Convert image to base64
            byte[] imageBytes = file.getBytes();
            String base64Image = "data:" + contentType + ";base64," + 
                    Base64.getEncoder().encodeToString(imageBytes);

            user.setProfileImage(base64Image);
            User updated = userRepository.save(user);

            UserDTO userDTO = convertToDTO(updated);
            response.put("success", true);
            response.put("data", userDTO);
            response.put("message", "Profile image uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "Failed to process image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                response.put("success", false);
                response.put("error", "Current password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setCompanyName(user.getCompanyName());
        dto.setBio(user.getBio());
        dto.setProfileImage(user.getProfileImage());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setWebsite(user.getWebsite());
        dto.setWalletAddress(user.getWalletAddress());
        dto.setBalance(user.getBalance());
        dto.setContractAccepted(user.getContractAccepted());
        dto.setContractHash(user.getContractHash());
        dto.setCompanyAgreementAccepted(user.getCompanyAgreementAccepted());
        dto.setCompanyAgreementHash(user.getCompanyAgreementHash());
        return dto;
    }
}

