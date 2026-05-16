package com.testplatform.dto;

public class RegisterRequest {
    private String phone;
    private String orgName;
    private String description;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
