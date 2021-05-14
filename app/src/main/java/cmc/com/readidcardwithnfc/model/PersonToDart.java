package cmc.com.readidcardwithnfc.model;

import android.graphics.Bitmap;

import java.util.List;

public class PersonToDart {
    private String name;
    private String surname;
    private String personalNumber;
    private String gender;
    private String birthDate;
    private String expiryDate;
    private String serialNumber;
    private String nationality;
    private String issuerAuthority;
    private String faceImage;//bit map, byte[]=>base64
    private String faceImageBase64;
    private String portraitImage;//bit map, byte[]=>base64
    private String portraitImageBase64;
    private String signature;//bit map, byte[]=>base64
    private String signatureBase64;
    private List<String> fingerprints;//bit map, byte[]=>base64

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getIssuerAuthority() {
        return issuerAuthority;
    }

    public void setIssuerAuthority(String issuerAuthority) {
        this.issuerAuthority = issuerAuthority;
    }

    public String getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }

    public String getFaceImageBase64() {
        return faceImageBase64;
    }

    public void setFaceImageBase64(String faceImageBase64) {
        this.faceImageBase64 = faceImageBase64;
    }

    public String getPortraitImage() {
        return portraitImage;
    }

    public void setPortraitImage(String portraitImage) {
        this.portraitImage = portraitImage;
    }

    public String getPortraitImageBase64() {
        return portraitImageBase64;
    }

    public void setPortraitImageBase64(String portraitImageBase64) {
        this.portraitImageBase64 = portraitImageBase64;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignatureBase64() {
        return signatureBase64;
    }

    public void setSignatureBase64(String signatureBase64) {
        this.signatureBase64 = signatureBase64;
    }

    public List<String> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(List<String> fingerprints) {
        this.fingerprints = fingerprints;
    }
}
