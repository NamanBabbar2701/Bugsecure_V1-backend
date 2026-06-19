package com.bugsecure.backend.dto;

public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String role;
    private String companyName;
    private String bio;
    private String profileImage;
    private String phoneNumber;
    private String address;
    private String website;
    private String walletAddress;
    private Double balance;
    private Boolean contractAccepted;
    private String contractHash;
    private Boolean companyAgreementAccepted;
    private String companyAgreementHash;

    // Constructors
    public UserDTO() {
    }

    // Getters and Setters
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
}

