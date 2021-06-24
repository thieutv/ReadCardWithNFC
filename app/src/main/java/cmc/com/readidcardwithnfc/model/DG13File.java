package cmc.com.readidcardwithnfc.model;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cmc.com.readidcardwithnfc.util.StringUtil;

public class DG13File {
    private OptionPersonal optionPersonal;
    private String allHex;
    private static final String CCCD_NUMBER_TAG = "0113";
    private static final String NAME_PERSONAL_TAG = "020C";
    private static final String DATE_OF_BIRTH_TAG = "300F02010313";
    private static final String SEX_TAG = "040C";
    private static final String NATIONALITY_TAG = "050C";
    private static final String NATION_TAG = "060C";
    private static final String RELIGION_TAG = "070C";
    private static final String DISTRICT_TAG = "080C";
    private static final String PERMANENT_ADDRESS_TAG = "090C";
    private static final String IDENTIFY_CHARACTER_TAG = "0A0C";
    private static final String DATE_RANGE_TAG = "0B13";
    private static final String DATE_EXPRIRATION_TAG = "0C0C";
    private static final String CMND_NUMBER_TAG = "0F13";
    private static final int FATHER_NAME_TAG_LEN = 16;
    private static final int MOTHER_NAME_TAG_LEN = 6;
    private static final int HUSBAND_WIFE_NAME_TAG_LEN = 16;


    public DG13File() {
        super();
    }

    public DG13File(InputStream inputStream) throws IOException {
        byte[] arr;
        arr = IOUtils.toByteArray(inputStream);
        this.allHex = StringUtil.bytesToHex(arr);
    }

    public void readContent() throws Exception {
        Log.d("hau3", allHex);
        String cccdNumber = readObject(allHex, CCCD_NUMBER_TAG);
        Log.d("hau3", "Số căn cước công dân: " + cccdNumber);
        String personName = readObject(allHex, NAME_PERSONAL_TAG);
        Log.d("hau3", "Họ và tên: " + personName);
        String dateOfBirth = readObject(allHex, DATE_OF_BIRTH_TAG);
        Log.d("hau3", "Ngày sinh: " + dateOfBirth);
        String sex = readObject(allHex, SEX_TAG);
        Log.d("hau3", "Giới tính: " + sex);
        String nationality = readObject(allHex, NATIONALITY_TAG);
        Log.d("hau3", "Quốc tịch: " + nationality);
        String nation = readObject(allHex, NATION_TAG);
        Log.d("hau3", "Dân tộc: " + nation);
        String religion = readObject(allHex, RELIGION_TAG);
        Log.d("hau3", "Tôn giáo: " + religion);
        String district = readObject(allHex, DISTRICT_TAG);
        Log.d("hau3", "quê quán: " + district);
        String permanentAddress = readObject(allHex, PERMANENT_ADDRESS_TAG);
        Log.d("hau3", "Nơi thường trú: " + permanentAddress);
        String identifyingCharacteristics = readObject(allHex, IDENTIFY_CHARACTER_TAG);
        Log.d("hau3", "Đặc điểm nhận dạng: " + identifyingCharacteristics);
        String dateRange = readObject(allHex, DATE_RANGE_TAG);
        Log.d("hau3", "Ngày phát hành: " + dateRange);
        String dateExpiration = readObject(allHex, DATE_EXPRIRATION_TAG);
        Log.d("hau3", "Ngày hết hạn: " + dateExpiration);
        String fatherName = readFatherName(allHex, FATHER_NAME_TAG_LEN, DATE_EXPRIRATION_TAG);
        Log.d("hau3", "Họ tên bố: " + fatherName);
        String motherName = readMotherName(allHex, MOTHER_NAME_TAG_LEN, fatherNameHex);
        Log.d("hau3", "Họ tên mẹ: " + motherName);
        String HusbandAndWifeName = readHusbandAndWifeName(allHex, HUSBAND_WIFE_NAME_TAG_LEN, motherNameHex);
        Log.d("hau3", "Tên vợ hoặc Chồng: " + HusbandAndWifeName);
        String CMNDNumber = readObject(allHex, CMND_NUMBER_TAG);
        Log.d("hau3", "Số CMND cũ: " + CMNDNumber);

        this.optionPersonal = new OptionPersonal(cccdNumber, personName, dateOfBirth, sex, nationality, nation, religion,
                district, permanentAddress, identifyingCharacteristics, dateRange, dateExpiration, fatherName, motherName,
                HusbandAndWifeName, CMNDNumber);
    }


    public void writeContent(OutputStream outputStream) throws IOException {

    }

    public String readObject(String allHex, String tag) throws Exception {
        if (allHex.length() == 0 || tag.length() == 0) {
            return null;
        }
        String countString = StringUtil.cutTLVString(allHex, tag);
        int countValue = Integer.parseInt(countString, 16);
        String dataHex = allHex.substring(StringUtil.indexOfTagWithLength(allHex, tag), StringUtil.indexOfTagWithLength(allHex, tag) + countValue * 2);
        String data = StringUtil.hexToUTF8(dataHex);
        return data;
    }

    private String fatherNameHex;
    private int countFatherName;
    private String motherNameHex;
    private int countMotherName;

    public String readFatherName(String allHex, int tagLength, String beforeTag) throws Exception {
        if (allHex.length() == 0 || beforeTag.length() == 0 || tagLength <= 0) {
            return null;
        }
        String countString = StringUtil.cutTLVString(allHex, beforeTag);
        int countValue = Integer.parseInt(countString, 16);

        String dataHex = allHex.substring(StringUtil.indexOfTagWithLength(allHex, beforeTag),
                StringUtil.indexOfTagWithLength(allHex, beforeTag) + countValue * 2);

        int fIndexLen = allHex.indexOf(dataHex) + countValue * 2 + tagLength;
        int lIndexLen = fIndexLen + 2;
        String lenString = allHex.substring(fIndexLen, lIndexLen);
        countFatherName = Integer.parseInt(lenString, 16);
        fatherNameHex = allHex.substring(lIndexLen, lIndexLen + countFatherName * 2);
        String fatherName = StringUtil.hexToUTF8(fatherNameHex);
        return fatherName;
    }

    public String readMotherName(String allHex, int tagLength, String identifiString) throws Exception {
        int fIndexLen = allHex.indexOf(identifiString) + countFatherName * 2 + tagLength;
        int lIndexLen = fIndexLen + 2;
        String lenString = allHex.substring(fIndexLen, lIndexLen);
        countMotherName = Integer.parseInt(lenString, 16);
        motherNameHex = allHex.substring(lIndexLen, lIndexLen + countMotherName * 2);
        String motherName = StringUtil.hexToUTF8(motherNameHex);
        return motherName;
    }

    public String readHusbandAndWifeName(String allHex, int tagLength, String identifiString) throws Exception {
        int fIndexLen = allHex.indexOf(identifiString) + countMotherName * 2 + tagLength;
        int lIndexLen = fIndexLen + 2;
        String lenString = allHex.substring(fIndexLen, lIndexLen);
        int countHusAndWife = Integer.parseInt(lenString, 16);
        return StringUtil.hexToUTF8(allHex.substring(lIndexLen, lIndexLen + countHusAndWife * 2));
    }


    public OptionPersonal getOptionPersonal() {
        return optionPersonal;
    }

    public String getAllHex() {
        return allHex;
    }
}
