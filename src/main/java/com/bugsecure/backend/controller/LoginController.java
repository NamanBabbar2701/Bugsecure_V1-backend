package com.bugsecure.backend.controller;

import com.bugsecure.backend.config.JwtUtil;
import com.bugsecure.backend.dto.UserDTO;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.get("email"), body.get("password"))
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername());
            
            // Get user details
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setRole(user.getRole());
            userDTO.setCompanyName(user.getCompanyName());
            userDTO.setBio(user.getBio());
            userDTO.setProfileImage(user.getProfileImage());
            userDTO.setPhoneNumber(user.getPhoneNumber());
            userDTO.setAddress(user.getAddress());
            userDTO.setWebsite(user.getWebsite());
            userDTO.setWalletAddress(user.getWalletAddress());
            userDTO.setBalance(user.getBalance());
            userDTO.setContractAccepted(user.getContractAccepted());
            userDTO.setContractHash(user.getContractHash());
            userDTO.setCompanyAgreementAccepted(user.getCompanyAgreementAccepted());
            userDTO.setCompanyAgreementHash(user.getCompanyAgreementHash());
            
            res.put("token", token);
            res.put("user", userDTO);
            res.put("message", "Login successful");
            return res;
        } catch (Exception e) {
            res.put("error", "Invalid credentials");
            return res;
        }
    }
}
