package cmc.com.readidcardwithnfc.model;

public class OptionPersonal {
    private String CCCDNumber;//031182011719
    private String name;
    private String dateOfBirth;
    private String sex;
    private String nationality;
    private String nation;//Dân tộc
    private String religion;//Tôn giáo
    private String district;//quận huyện
    private String permanentAddress;//địa chỉ thường trú
    private String identifyingCharacteristics;//đặc điêm nhận diện
    private String dateRange;//ngày cấp
    private String dateExpiration;//ngày hết hạn
    private String fatherName;//tên cha
    private String mothername;//tên mẹ
    private String husbandAndWifeName;//tên chồng hoặc vợ
    private String CMNDNumber;

    public OptionPersonal() {
    }

    public OptionPersonal(String CCCDNumber, String name,
                          String dateOfBirth, String sex,
                          String nationality, String nation,
                          String religion, String district,
                          String permanentAddress, String identifyingCharacteristics,
                          String dateRange, String dateExpiration, String fatherName,
                          String mothername, String husbandAndWifeName, String CMNDNumber) {
        this.CCCDNumber = CCCDNumber;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
        this.nationality = nationality;
        this.nation = nation;
        this.religion = religion;
        this.district = district;
        this.permanentAddress = permanentAddress;
        this.identifyingCharacteristics = identifyingCharacteristics;
        this.dateRange = dateRange;
        this.dateExpiration = dateExpiration;
        this.fatherName = fatherName;
        this.mothername = mothername;
        this.husbandAndWifeName = husbandAndWifeName;
        this.CMNDNumber = CMNDNumber;
    }

    public String getCCCDNumber() {
        return CCCDNumber;
    }

    public void setCCCDNumber(String CCCDNumber) {
        this.CCCDNumber = CCCDNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getIdentifyingCharacteristics() {
        return identifyingCharacteristics;
    }

    public void setIdentifyingCharacteristics(String identifyingCharacteristics) {
        this.identifyingCharacteristics = identifyingCharacteristics;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(String dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMothername() {
        return mothername;
    }

    public void setMothername(String mothername) {
        this.mothername = mothername;
    }

    public String getHusbandAndWifeName() {
        return husbandAndWifeName;
    }

    public void setHusbandAndWifeName(String husbandAndWifeName) {
        this.husbandAndWifeName = husbandAndWifeName;
    }

    public String getCMNDNumber() {
        return CMNDNumber;
    }

    public void setCMNDNumber(String CMNDNumber) {
        this.CMNDNumber = CMNDNumber;
    }

    @Override
    public String toString() {
        return "OptionDetails{" +
                "CCCDNumber='" + CCCDNumber + '\'' +
                ", name='" + name + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", sex='" + sex + '\'' +
                ", nationality='" + nationality + '\'' +
                ", nation='" + nation + '\'' +
                ", religion='" + religion + '\'' +
                ", district='" + district + '\'' +
                ", permanentAddress='" + permanentAddress + '\'' +
                ", identifyingCharacteristics='" + identifyingCharacteristics + '\'' +
                ", dateRange='" + dateRange + '\'' +
                ", dateExpiration='" + dateExpiration + '\'' +
                ", fatherName='" + fatherName + '\'' +
                ", mothername='" + mothername + '\'' +
                ", husbandAndWifeName='" + husbandAndWifeName + '\'' +
                ", CMNDNumber='" + CMNDNumber + '\'' +
                '}';
    }
}
