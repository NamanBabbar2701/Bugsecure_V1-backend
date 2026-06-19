package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;
    private String email;
    private String password;
    private String role = "USER";   // Default role: USER, COMPANY, ADMIN
    private String companyName;   // For COMPANY role
    private String bio;   // User biography
    private String profileImage;   // Profile image URL/path (Base64 or URL)
    private String phoneNumber;   // Phone number
    private String address;   // Address
    private String website;   // Website URL
    
    // Wallet fields
    private String walletAddress;   // Unique wallet identifier
    private Double balance = 0.0;   // Current wallet balance
    // Preferred display currency for wallet/UI (balance is stored in USD as single source of truth)
    private String currency = "USD";   // USD, INR, EUR
    private Boolean contractAccepted = false;   // Smart contract acceptance (for researchers)
    private String contractHash;   // Contract signature/hash (for researchers)
    private java.time.LocalDateTime contractAcceptedAt;   // When contract was accepted (for researchers)
    
    // Company agreement fields
    private Boolean companyAgreementAccepted = false;   // Company agreement acceptance
    private String companyAgreementHash;   // Company agreement signature/hash
    private java.time.LocalDateTime companyAgreementSignedOn;   // When company agreement was signed

    // ✅ Constructors
    public User() {
    }

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ✅ Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    // Wallet Getters and Setters
    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getContractAccepted() {
        return contractAccepted;
    }

    public void setContractAccepted(Boolean contractAccepted) {
        this.contractAccepted = contractAccepted;
    }

    public String getContractHash() {
        return contractHash;
    }

    public void setContractHash(String contractHash) {
        this.contractHash = contractHash;
    }

    public java.time.LocalDateTime getContractAcceptedAt() {
        return contractAcceptedAt;
    }

    public void setContractAcceptedAt(java.time.LocalDateTime contractAcceptedAt) {
        this.contractAcceptedAt = contractAcceptedAt;
    }

    // Company Agreement Getters and Setters
    public Boolean getCompanyAgreementAccepted() {
        return companyAgreementAccepted;
    }

    public void setCompanyAgreementAccepted(Boolean companyAgreementAccepted) {
        this.companyAgreementAccepted = companyAgreementAccepted;
    }

    public String getCompanyAgreementHash() {
        return companyAgreementHash;
    }

    public void setCompanyAgreementHash(String companyAgreementHash) {
        this.companyAgreementHash = companyAgreementHash;
    }

    public java.time.LocalDateTime getCompanyAgreementSignedOn() {
        return companyAgreementSignedOn;
    }

    public void setCompanyAgreementSignedOn(java.time.LocalDateTime companyAgreementSignedOn) {
        this.companyAgreementSignedOn = companyAgreementSignedOn;
    }

    // ✅ Optional: Override toString() (useful for debugging)
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
